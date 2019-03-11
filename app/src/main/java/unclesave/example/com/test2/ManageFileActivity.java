package unclesave.example.com.test2;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.mobile.auth.core.IdentityHandler;
import com.amazonaws.mobile.auth.core.IdentityManager;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.AWSStartupHandler;
import com.amazonaws.mobile.client.AWSStartupResult;
import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.AmazonS3Client;

import java.io.File;

public class ManageFileActivity extends AppCompatActivity {

    private AWSCredentialsProvider credentialsProvider;
    private AWSConfiguration configuration;
    private EditText fileInfo;
    private Button downloadFileButton, deleteFileButton;
    private String androidID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_file);

        responseToNoInternetAccess();

        // AWS Mobile hub backend integration
        AWSMobileClient.getInstance().initialize(this, new AWSStartupHandler() {
            @Override
            public void onComplete(AWSStartupResult awsStartupResult) {

                // Obtain the reference to the AWSCredentialsProvider and AWSConfiguration objects
                credentialsProvider = AWSMobileClient.getInstance().getCredentialsProvider();
                configuration = AWSMobileClient.getInstance().getConfiguration();

                // Use IdentityManager#getUserID to fetch the identity id.
                IdentityManager.getDefaultIdentityManager().getUserID(new IdentityHandler() {
                    @Override
                    public void onIdentityId(String identityId) {
                        Log.d("YourMainActivity", "Identity ID = " + identityId);

                        // Use IdentityManager#getCachedUserID to
                        //  fetch the locally cached identity id.
                        final String cachedIdentityId =
                                IdentityManager.getDefaultIdentityManager().getCachedUserID();
                    }

                    @Override
                    public void handleError(Exception exception) {
                        Log.d("YourMainActivity", "Error in retrieving the identity" + exception);
                    }
                });
            }
        }).execute();

        androidID = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ANDROID_ID);
        fileInfo = findViewById(R.id.file_info);
        downloadFileButton = findViewById(R.id.download_button);
        deleteFileButton = findViewById(R.id.delete_button);
    }

    // Checks if external storage is available for read and write
    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state))
            return true;
        return false;
    }

    // Checks if internet access is available
    private boolean isInternetAccessAvailable() {
        // Checks whether wifi or mobile data is connected
        ConnectivityManager cm =
                (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    private void responseToNoInternetAccess() {
        if (!isInternetAccessAvailable()) {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setMessage("Please enable wifi or mobile data");
            alertDialogBuilder.setPositiveButton("Close",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            finish();
                        }
                    });
            alertDialogBuilder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    // if back button is pressed
                    finish();
                }
            });
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.setCanceledOnTouchOutside(false);
            alertDialog.show();
        }
    }

    public void deleteFile(View view) {
        responseToNoInternetAccess();
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                String bucketName = "test-userfiles-mobilehub-1913606923";
                String keyName = androidID + "/" + fileInfo.getText().toString();
                AmazonS3Client s3client = new AmazonS3Client(credentialsProvider);
                boolean test = false;
                try {
                    test = s3client.doesObjectExist(bucketName, keyName);
                    if (test)
                        s3client.deleteObject(bucketName, keyName);
                } catch (AmazonServiceException e) {
                    // The call was transmitted successfully, but Amazon S3 couldn't process
                    // it, so it returned an error response.
                    e.printStackTrace();
                }
                return test;
            }

            @Override
            protected void onPostExecute(Boolean result) {
                super.onPostExecute(result);
                if (result)
                    Toast.makeText(ManageFileActivity.this, "File deleted successfully", Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(ManageFileActivity.this, "File does not exist", Toast.LENGTH_SHORT).show();
            }
        }.execute();
    }

    public void downloadFile(View view) {
        if (!isExternalStorageWritable()) {
            Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show();
            return;
        }
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                String bucketName = "test-userfiles-mobilehub-1913606923";
                String keyName = androidID + "/" + fileInfo.getText().toString();
                AmazonS3Client s3client = new AmazonS3Client(credentialsProvider);

                boolean test = false;
                try {
                    test = s3client.doesObjectExist(bucketName, keyName);
                    if (test) {
                        File targetDir = new File(Environment.getExternalStorageDirectory()
                                + "/Download/" + fileInfo.getText().toString());
                        TransferUtility transferUtility =
                                TransferUtility.builder()
                                        .context(getApplicationContext())
                                        .awsConfiguration(configuration)
                                        .s3Client(s3client)
                                        .build();
                        TransferObserver downloadObserver = transferUtility.download(
                                keyName, targetDir);

                        // Attach a listener to the observer to get state update and progress notifications
                        downloadObserver.setTransferListener(new TransferListener() {
                            @Override
                            public void onStateChanged(int id, TransferState state) {
                                if (TransferState.COMPLETED == state) {
                                    // Handle a completed upload.
                                    Toast.makeText(getApplicationContext(), "CSV file uploaded to AWS S3", Toast.LENGTH_LONG).show();
                                }
                            }

                            @Override
                            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                                float percentDonef = ((float) bytesCurrent / (float) bytesTotal) * 100;
                                int percentDone = (int) percentDonef;

                                Log.d("YourActivity", "ID:" + id + " bytesCurrent: " + bytesCurrent
                                        + " bytesTotal: " + bytesTotal + " " + percentDone + "%");
                            }

                            @Override
                            public void onError(int id, Exception ex) {
                                // Handle errors
                                Toast.makeText(getApplicationContext(), "Please check internet connection", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } catch (AmazonServiceException e) {
                    // The call was transmitted successfully, but Amazon S3 couldn't process
                    // it, so it returned an error response.
                    e.printStackTrace();
                }
                return test;
            }

            @Override
            protected void onPostExecute(Boolean result) {
                super.onPostExecute(result);
                if (result)
                    Toast.makeText(ManageFileActivity.this, "File downloaded successfully", Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(ManageFileActivity.this, "File does not exist", Toast.LENGTH_SHORT).show();
            }
        }.execute();
    }
}
