package unclesave.example.com.sensordyne;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

// An activity that allows files downloading / deleting / sharing
public class ManageFileActivity extends AppCompatActivity {

    private PermissionAccessHandler permissionAccessHandler;
    private S3Handler s3Handler;
    private TextView filesInfo;
    private EditText fileInfo;
    private Button downloadFileButton;
    private Button deleteFileButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_file);
        permissionAccessHandler = new PermissionAccessHandler(this);
        if (!permissionAccessHandler.isInternetAccessAvailable())
            permissionAccessHandler.responseToNoInternetAccess();
        s3Handler = new S3Handler(ManageFileActivity.this, this);

        fileInfo = findViewById(R.id.file_info);
        filesInfo = findViewById(R.id.files_info);
        if (permissionAccessHandler.isInternetAccessAvailable()) {
            // Gets the list of objects available in individual device folder
            new AsyncTask<Void, Void, ArrayList<String>>() {
                @Override
                protected ArrayList<String> doInBackground(Void... voids) {
                    ListObjectsV2Result result = s3Handler.getS3client().listObjectsV2(s3Handler.getBucketName());
                    List<S3ObjectSummary> objectsInfo = result.getObjectSummaries();
                    ArrayList<String> objects = new ArrayList<>();
                    for (S3ObjectSummary objectInfo : objectsInfo) {
                        if (!objectInfo.getKey().equals(s3Handler.getAndroidID() + '/') &&
                                objectInfo.getKey().contains(s3Handler.getAndroidID()))
                            objects.add(objectInfo.getKey().replaceFirst(s3Handler.getAndroidID() + '/', ""));
                    }
                    return objects;
                }

                @Override
                protected void onPostExecute(ArrayList<String> objects) {
                    super.onPostExecute(objects);
                    filesInfo.setText(objects.toString());
                }
            }.execute();
        }
        else {
            permissionAccessHandler.responseToNoInternetAccess();
            filesInfo.setText("No internet, cannot access online file in cloud storage.");
        }
        downloadFileButton = findViewById(R.id.download_button);
        deleteFileButton = findViewById(R.id.delete_button);
    }

    // Deletes the file available
    public void deleteFile(View view) {
        if (permissionAccessHandler.isInternetAccessAvailable()) {
            final String keyName = s3Handler.getAndroidID() + "/" + fileInfo.getText().toString();
            new AsyncTask<Void, Void, Boolean>() {
                @Override
                protected Boolean doInBackground(Void... params) {
                    boolean test = false;
                    try {
                        test = s3Handler.getS3client().doesObjectExist(s3Handler.getBucketName(), keyName);
                        if (test)
                            s3Handler.getS3client().deleteObject(s3Handler.getBucketName(), keyName);
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
        } else
            permissionAccessHandler.responseToNoInternetAccess();
    }

    // Downloads the file available
    public void downloadFile(View view) {
        if (permissionAccessHandler.isInternetAccessAvailable()) {
            final String keyName = s3Handler.getAndroidID() + "/" + fileInfo.getText().toString();
            new AsyncTask<Void, Void, Boolean>() {
                @Override
                protected Boolean doInBackground(Void... params) {
                    boolean test = false;
                    try {
                        test = s3Handler.getS3client().doesObjectExist(s3Handler.getBucketName(), keyName);
                        if (test) {
                            File targetDir = new File(Environment.getExternalStorageDirectory()
                                    + "/Download/" + fileInfo.getText().toString());
                            TransferUtility transferUtility =
                                    TransferUtility.builder()
                                            .context(getApplicationContext())
                                            .s3Client(s3Handler.getS3client())
                                            .build();
                            TransferObserver downloadObserver = transferUtility.download(s3Handler.getBucketName(),
                                    keyName, targetDir);

                            // Attach a listener to the observer to get state update and progress notifications
                            downloadObserver.setTransferListener(new TransferListener() {
                                @Override
                                public void onStateChanged(int id, TransferState state) {
                                    if (TransferState.COMPLETED == state) {
                                        // Handle a completed upload.
                                        Toast.makeText(getApplicationContext(), "File downloaded successfully", Toast.LENGTH_LONG).show();
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
                    if (!result)
                        Toast.makeText(ManageFileActivity.this, "File does not exist", Toast.LENGTH_SHORT).show();
                }
            }.execute();
        } else
            permissionAccessHandler.responseToNoInternetAccess();
    }

    // Share file available to the other devices
    public void shareFile(View view) {
        if (permissionAccessHandler.isInternetAccessAvailable()) {
            CustomDialogFragment customDialogFragment = CustomDialogFragment.newInstance(401);
            customDialogFragment.show(getFragmentManager(), null);
        } else
            permissionAccessHandler.responseToNoInternetAccess();
    }

    // The function is used in the custom dialog fragment
    public void shareFileFunc(final String targetAndroidID) {
        if (permissionAccessHandler.isInternetAccessAvailable()) {
            final String sourceKeyName = s3Handler.getAndroidID() + '/' + fileInfo.getText().toString();
            final String targetKeyName = targetAndroidID + '/' + fileInfo.getText().toString();
            new AsyncTask<Void, Void, Boolean>() {
                @Override
                protected Boolean doInBackground(Void... params) {
                    boolean test = false;
                    boolean test2 = false;
                    try {
                        test = s3Handler.getS3client().doesObjectExist(s3Handler.getBucketName(), sourceKeyName);
                        test2 = s3Handler.getS3client().doesObjectExist(s3Handler.getBucketName(), targetAndroidID);
                        if (test && test2)
                            s3Handler.getS3client().copyObject(s3Handler.getBucketName(), sourceKeyName, s3Handler.getBucketName(), targetKeyName);
                    } catch (AmazonServiceException e) {
                        // The call was transmitted successfully, but Amazon S3 couldn't process
                        // it, so it returned an error response.
                        e.printStackTrace();
                    }
                    return (test && test2);
                }

                @Override
                protected void onPostExecute(Boolean result) {
                    super.onPostExecute(result);
                    if (!result)
                        Toast.makeText(getApplicationContext(), "File or specified android ID does not exist", Toast.LENGTH_SHORT).show();
                    else
                        Toast.makeText(getApplicationContext(), "File shared successfully to specified android ID", Toast.LENGTH_SHORT).show();
                }
            }.execute();
        } else
            permissionAccessHandler.responseToNoInternetAccess();
    }
}
