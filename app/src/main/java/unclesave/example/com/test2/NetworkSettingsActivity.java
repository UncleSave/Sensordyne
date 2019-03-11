package unclesave.example.com.test2;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
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
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;

public class NetworkSettingsActivity extends AppCompatActivity
        implements AdapterView.OnItemSelectedListener {

    private AWSCredentialsProvider credentialsProvider;
    private AWSConfiguration configuration;
    private EditText unitInput, inputShapeInput, dropoutInput;
    private Spinner layerTypeSpinner, activationSpinner, lossSpinner, optimizerSpinner;
    private EditText batchSizeInput, epochInput, timeStepInput, hiddenLayerInput;
    private Button addLayerButton, saveModelButton, saveSettingsButton;
    private String androidID;
    private String layerType, inputShape, optimizer, loss, activation;
    private float dropout;
    private int unit, batchSize, epoch, timeStep;
    private ArrayList<String> pyCodeList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_network_settings);

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
        CognitoCachingCredentialsProvider cognitoProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                "us-east-1:0c63f012-ff89-4907-88ea-527828cd5db0", // Identity pool ID
                Regions.US_EAST_1 // Region
        );

        androidID = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ANDROID_ID);
        layerTypeSpinner = findViewById(R.id.layer_type_input);
        layerTypeSpinner.setOnItemSelectedListener(this);
        unitInput = findViewById(R.id.unit_input);
        inputShapeInput = findViewById(R.id.input_shape_input);
        activationSpinner = findViewById(R.id.activation_input);
        getActivationsList();
        activationSpinner.setOnItemSelectedListener(this);
        dropoutInput = findViewById(R.id.dropout_input);
        optimizerSpinner = findViewById(R.id.optimizer_input);
        getOptimizersList();
        optimizerSpinner.setOnItemSelectedListener(this);
        lossSpinner = findViewById(R.id.loss_input);
        getLossesList();
        lossSpinner.setOnItemSelectedListener(this);
        addLayerButton = findViewById(R.id.add_layer_button);
        saveModelButton = findViewById(R.id.save_configured_model_button);

        epochInput = findViewById(R.id.epoch_input);
        batchSizeInput = findViewById(R.id.batchsize_input);
        timeStepInput = findViewById(R.id.timesteps_input);
        saveSettingsButton = findViewById(R.id.save_button);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        File file1 = new File(getApplicationContext().getFilesDir(), "pyCode.txt");
        if (file1.exists())
            file1.delete();
        File file2 = new File(getApplicationContext().getFilesDir(), "settings.txt");
        if (file2.exists())
            file2.delete();
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

    public void addLayer(View view) {
        if ("".equals(unitInput.getText().toString())
                || ("".equals(inputShapeInput.getText().toString()) && pyCodeList.size() == 0)) {
            Toast.makeText(this, "Missing input", Toast.LENGTH_LONG).show();
            unitInput.setText("");
            dropoutInput.setText("");
            inputShapeInput.setText("");
            return;
        } else {
            unit = Integer.parseInt(unitInput.getText().toString());
            if ("".equals(dropoutInput.getText().toString())) {
                dropout = -1.0f;
            } else {
                dropout = Float.parseFloat(dropoutInput.getText().toString());
                if (dropout < 0.0f || dropout > 1.0f)
                    dropout = -1.0f;
            }
            inputShape = inputShapeInput.getText().toString();
        }
        String pyCode;
        String dropoutCode = "";
        if (("\'None\'").equals(activation))
            activation = "None";
        if (pyCodeList.size() == 0) { // first line of code, must specify the input shape
            if (dropout == -1.0f)
                pyCode = "model.add(" + layerType + "(" + unit + ", input_shape=" + inputShape + ", activation=" +
                        activation + "))";
            else {
                if ("LSTM".equals(layerType))
                    pyCode = "model.add(" + layerType + "(" + unit + ", input_shape=" + inputShape + ", dropout="
                            + dropout + ", activation=" + activation + "))";
                else {
                    pyCode = "model.add(" + layerType + "(" + unit + ", input_shape=" + inputShape + ", activation="
                            + activation + "))";
                    dropoutCode = "model.add(Dropout(" + dropout + "))";
                }
            }
        } else {
            if (dropout == -1.0f)
                pyCode = "model.add(" + layerType + "(" + unit + ", activation=" + activation + "))";
            else {
                if ("LSTM".equals(layerType))
                    pyCode = "model.add(" + layerType + "(" + unit + ", dropout="
                            + dropout + ", activation=" + activation + "))";
                else {
                    pyCode = "model.add(" + layerType + "(" + unit + ", activation=" + activation + "))";
                    dropoutCode = "model.add(Dropout(" + dropout + "))";
                }
            }
        }
        pyCodeList.add(pyCode);
        if (!"".equals(dropoutCode))
            pyCodeList.add(dropoutCode);
        Toast.makeText(this, pyCodeList.toString(), Toast.LENGTH_LONG).show();
        unitInput.setText("");
        dropoutInput.setText("");
        inputShapeInput.setText("");
        return;
    }

    public void saveModel(View view) {
        if ("".equals(optimizer) || "".equals(loss)) {
            Toast.makeText(this, "Please enter optimizer and loss", Toast.LENGTH_LONG).show();
            return;
        }
        File file = new File(getApplicationContext().getFilesDir(), "pyCode.txt");
        try {
            String compileModelCode = "model.compile(optimizer=\'" + optimizer + "\', loss=\'" + loss + "\')";
            FileOutputStream outputStream = new FileOutputStream(file, false);
            for (String code : pyCodeList)
                outputStream.write((code + '\n').getBytes());
            outputStream.write((compileModelCode + '\n').getBytes());
            outputStream.close();
        } catch (FileNotFoundException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        if (file.exists()) {
            AmazonS3Client s3client = new AmazonS3Client(credentialsProvider);
            TransferUtility transferUtility =
                    TransferUtility.builder()
                            .context(getApplicationContext())
                            .awsConfiguration(configuration)
                            .s3Client(s3client)
                            .build();
            TransferObserver uploadObserver = transferUtility.upload(
                    androidID + "/pyCode.txt",
                    file);

            // Attach a listener to the observer to get state update and progress notifications
            uploadObserver.setTransferListener(new TransferListener() {
                @Override
                public void onStateChanged(int id, TransferState state) {
                    if (TransferState.COMPLETED == state) {
                        // Handle a completed upload.
                        Toast.makeText(getApplicationContext(), "TXT file uploaded to AWS S3", Toast.LENGTH_LONG).show();
                        final String urllink = "http://13.228.232.119/savemodel/" + androidID;
                        new AsyncTask<Void, Void, String>() {
                            @Override
                            protected String doInBackground(Void... params) {
                                OkHttpClient okHttpClient = new OkHttpClient();
                                okHttpClient = okHttpClient.newBuilder().connectTimeout(3600, TimeUnit.MINUTES)
                                        .writeTimeout(3600, TimeUnit.MINUTES)
                                        .readTimeout(3600, TimeUnit.MINUTES)
                                        .build();
                                Request request = new Request.Builder().url(urllink).build();
                                try {
                                    return okHttpClient.newCall(request).execute().body().string();
                                } catch (IOException e) { return "IOException occurs"; }
                            }

                            @Override
                            protected void onPostExecute(String result) {
                                super.onPostExecute(result);
                                Toast.makeText(NetworkSettingsActivity.this, result, Toast.LENGTH_SHORT).show();
                            }
                        }.execute();
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
            Log.d("YourActivity", "Bytes Transferred: " + uploadObserver.getBytesTransferred());
            Log.d("YourActivity", "Bytes Total: " + uploadObserver.getBytesTotal());
        }
        else
            Toast.makeText(this, "File does not exist", Toast.LENGTH_SHORT).show();
    }

    public void getOptimizersList() {
        responseToNoInternetAccess();
        final String urllink = "http://13.228.232.119/getoptimizers";
        new AsyncTask<Void, Void, String>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected String doInBackground(Void... params) {
                OkHttpClient okHttpClient = new OkHttpClient();
                okHttpClient = okHttpClient.newBuilder().connectTimeout(36, TimeUnit.MINUTES)
                        .writeTimeout(36, TimeUnit.MINUTES)
                        .readTimeout(36, TimeUnit.MINUTES)
                        .build();
                Request request = new Request.Builder().url(urllink).build();
                try {
                    return okHttpClient.newCall(request).execute().body().string();
                } catch (IOException e) {
                    return "IOException occurs";
                }
            }

            @Override
            protected void onPostExecute(String result) {
                super.onPostExecute(result);
                String[] optimizers = result.replace("'", "").split(",");
                ArrayAdapter<String> optimizerAdapter = new ArrayAdapter<String>(NetworkSettingsActivity.this,
                        android.R.layout.simple_spinner_item, optimizers);
                optimizerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                optimizerSpinner.setAdapter(optimizerAdapter);
            }
        }.execute();
    }

    public void getActivationsList() {
        final String urllink = "http://13.228.232.119/getactivations";
        new AsyncTask<Void, Void, String>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected String doInBackground(Void... params) {
                OkHttpClient okHttpClient = new OkHttpClient();
                okHttpClient = okHttpClient.newBuilder().connectTimeout(36, TimeUnit.MINUTES)
                        .writeTimeout(36, TimeUnit.MINUTES)
                        .readTimeout(36, TimeUnit.MINUTES)
                        .build();
                Request request = new Request.Builder().url(urllink).build();
                try {
                    return okHttpClient.newCall(request).execute().body().string();
                } catch (IOException e) {
                    return "IOException occurs";
                }
            }

            @Override
            protected void onPostExecute(String result) {
                super.onPostExecute(result);
                String[] activations = result.replace("'", "").split(",");
                ArrayList<String> activationsList = new ArrayList<>();
                activationsList.add("None");
                for (int i=0; i<activations.length; i++)
                    activationsList.add(activations[i]);
                String[] activationsArr = activationsList.toArray(activations);
                ArrayAdapter<String> activationAdapter = new ArrayAdapter<String>(NetworkSettingsActivity.this,
                        android.R.layout.simple_spinner_item, activationsArr);
                activationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                activationSpinner.setAdapter(activationAdapter);
            }
        }.execute();
    }

    public void getLossesList() {
        final String urllink = "http://13.228.232.119/getlosses";
        new AsyncTask<Void, Void, String>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected String doInBackground(Void... params) {
                OkHttpClient okHttpClient = new OkHttpClient();
                okHttpClient = okHttpClient.newBuilder().connectTimeout(36, TimeUnit.MINUTES)
                        .writeTimeout(36, TimeUnit.MINUTES)
                        .readTimeout(36, TimeUnit.MINUTES)
                        .build();
                Request request = new Request.Builder().url(urllink).build();
                try {
                    return okHttpClient.newCall(request).execute().body().string();
                } catch (IOException e) {
                    return "IOException occurs";
                }
            }

            @Override
            protected void onPostExecute(String result) {
                super.onPostExecute(result);
                String[] losses = result.replace("'", "").split(",");
                ArrayAdapter<String> lossAdapter = new ArrayAdapter<String>(NetworkSettingsActivity.this,
                        android.R.layout.simple_spinner_item, losses);
                lossAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                lossSpinner.setAdapter(lossAdapter);
            }
        }.execute();
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
        switch (parent.getId()) {
            case R.id.layer_type_input:
                layerType = layerTypeSpinner.getSelectedItem().toString();
                break;
            case R.id.optimizer_input:
                optimizer = optimizerSpinner.getSelectedItem().toString();
                break;
            case R.id.loss_input:
                loss = lossSpinner.getSelectedItem().toString();
                break;
            case R.id.activation_input:
                activation = "\'" + activationSpinner.getSelectedItem().toString() + "\'";
                break;
            default: break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    public void saveSettings(View view) {
        epoch = Integer.parseInt(epochInput.getText().toString());
        batchSize = Integer.parseInt(batchSizeInput.getText().toString());
        timeStep = Integer.parseInt(timeStepInput.getText().toString());
        if (epoch <= 0 || batchSize <= 0 || timeStep <= 0)
            Toast.makeText(this, "Error dropout, epoch or batch size input", Toast.LENGTH_LONG).show();
        else {
            final ParameterSettings parameters = new ParameterSettings(batchSize, epoch, timeStep);
            new AsyncTask<ParameterSettings, Void, String>() {
                @Override
                protected String doInBackground(ParameterSettings... params) {
                    try {
                        File file = new File(getApplicationContext().getFilesDir(), "settings.txt");
                        FileOutputStream outputStream = new FileOutputStream(file, false);
                        outputStream.write(epochInput.getText().toString().getBytes());
                        outputStream.write(";".getBytes());
                        outputStream.write(batchSizeInput.getText().toString().getBytes());
                        outputStream.write(";".getBytes());
                        outputStream.write(timeStepInput.getText().toString().getBytes());
                        outputStream.close();
                        if (file.exists()) {
                            AmazonS3Client s3client = new AmazonS3Client(credentialsProvider);
                            TransferUtility transferUtility =
                                    TransferUtility.builder()
                                            .context(getApplicationContext())
                                            .awsConfiguration(configuration)
                                            .s3Client(s3client)
                                            .build();
                            Date date = new Date();
                            String sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(date);
                            TransferObserver uploadObserver = transferUtility.upload(
                                    androidID + "/settings.txt",
                                    file);

                            // Attach a listener to the observer to get state update and progress notifications
                            uploadObserver.setTransferListener(new TransferListener() {
                                @Override
                                public void onStateChanged(int id, TransferState state) {
                                    if (TransferState.COMPLETED == state) {
                                        // Handle a completed upload.
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
                            Log.d("YourActivity", "Bytes Transferred: " + uploadObserver.getBytesTransferred());
                            Log.d("YourActivity", "Bytes Total: " + uploadObserver.getBytesTotal());
                        } else {
                            return "Settings file is not created!";
                        }
                        return "Successfully save settings!";
                    } catch (IOException e) {
                        return "IOException occurs";
                    }
                }

                @Override
                protected void onPostExecute(String result) {
                    super.onPostExecute(result);
                    Toast.makeText(getApplicationContext(), result, Toast.LENGTH_SHORT).show();
                }
            }.execute(parameters);
        }
    }
}
