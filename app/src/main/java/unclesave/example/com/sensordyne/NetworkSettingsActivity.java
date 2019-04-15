package unclesave.example.com.sensordyne;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;

// An activity that customizes the model building process and sets training hyperparameters
public class NetworkSettingsActivity extends AppCompatActivity
        implements AdapterView.OnItemSelectedListener {

    private PermissionAccessHandler permissionAccessHandler;
    private S3Handler s3Handler;
    private EditText unitInput;
    private EditText inputShapeInput;
    private EditText dropoutInput;
    private Spinner layerTypeSpinner;
    private Spinner activationSpinner;
    private Spinner lossSpinner;
    private Spinner optimizerSpinner;
    private EditText batchSizeInput;
    private EditText epochInput;
    private EditText timeStepInput;
    private Button addLayerButton;
    private Button saveModelButton;
    private Button saveSettingsButton;
    private String layerType;
    private String inputShape;
    private String optimizer;
    private String loss;
    private String activation;
    private float dropout;
    private int unit;
    private int batchSize;
    private int epoch;
    private int timeStep;
    private ArrayList<String> pyCodeList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_network_settings);

        permissionAccessHandler = new PermissionAccessHandler(this);
        if (!permissionAccessHandler.isInternetAccessAvailable())
            permissionAccessHandler.responseToNoInternetAccess();
        s3Handler = new S3Handler(NetworkSettingsActivity.this, this);

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

    // Adds layer to model building
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
            if (unit == 0) {
                Toast.makeText(this, "Unit cannot be zero", Toast.LENGTH_LONG).show();
                return;
            }
            else if ("".equals(dropoutInput.getText().toString()) || dropout > 1.0f)
                dropout = -1.0f;
            else
                dropout = Float.parseFloat(dropoutInput.getText().toString());
            if (pyCodeList.size() == 0) {
                // Invokes regular expressions to check the format of input shape
                inputShape = inputShapeInput.getText().toString();
                String denseInputShapePattern = "^[(]{1}"
                        + "([\\d]{1,}(,){0,1}){1,}"
                        + "[)]{1}$";
                String errdenseInputShapePattern = "^[(]{1}"
                        + "([\\d]){1,}"
                        + "[)]{1}$";
                String errlstmInputShapePattern = "^[(]{1}"
                        + "([\\d]{1,}[,]{0,1}){1}"
                        + "[)]{1}$";
                if (layerType.equals("Dense")) {
                    if (!inputShape.matches(denseInputShapePattern) || inputShape.matches(errdenseInputShapePattern)) {
                        Toast.makeText(this, "Incorrect format for input shape", Toast.LENGTH_LONG).show();
                        return;
                    }
                } else {
                    if (!inputShape.matches(denseInputShapePattern) || inputShape.matches(errlstmInputShapePattern)) {
                        Toast.makeText(this, "Incorrect format for input shape", Toast.LENGTH_LONG).show();
                        return;
                    }
                }
            }
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
            inputShapeInput.setEnabled(false);
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
        Toast.makeText(this, "Layer added", Toast.LENGTH_LONG).show();
        unitInput.setText("");
        dropoutInput.setText("");
        inputShapeInput.setText("");
    }

    // Saves model to S3 cloud storage after building
    public void saveModel(View view) {
        if (!permissionAccessHandler.isInternetAccessAvailable())
            permissionAccessHandler.responseToNoInternetAccess();
        else {
            if (pyCodeList.size() == 0) {
                Toast.makeText(this, "No layer in the model created", Toast.LENGTH_LONG).show();
                return;
            } else if ("".equals(optimizer) || "".equals(loss)) {
                Toast.makeText(this, "Please enter optimizer and loss", Toast.LENGTH_LONG).show();
                return;
            } else {
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
                    TransferUtility transferUtility =
                            TransferUtility.builder()
                                    .context(getApplicationContext())
                                    .s3Client(s3Handler.getS3client())
                                    .build();
                    TransferObserver uploadObserver = transferUtility.upload(
                            s3Handler.getAndroidID() + "/pyCode.txt",
                            file);
                    // Attach a listener to the observer to get state update and progress notifications
                    uploadObserver.setTransferListener(new TransferListener() {
                        @Override
                        public void onStateChanged(int id, TransferState state) {
                            if (TransferState.COMPLETED == state) {
                                // Handle a completed upload.
                                Toast.makeText(getApplicationContext(), "TXT file uploaded to AWS S3", Toast.LENGTH_LONG).show();
                                final String urllink = "http://13.228.232.119/savemodel/" + s3Handler.getAndroidID();
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
                                        } catch (IOException e) {
                                            return "IOException occurs";
                                        }
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
        }
        inputShapeInput.setEnabled(true);
    }

    // Gets the optimizer list from EC2 instance / web server based on which version they has
    private void getOptimizersList() {
        if (!permissionAccessHandler.isInternetAccessAvailable())
            permissionAccessHandler.responseToNoInternetAccess();
        else {
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
    }

    // Gets the activation list from EC2 instance / web server based on which version they has
    private void getActivationsList() {
        if (!permissionAccessHandler.isInternetAccessAvailable())
            permissionAccessHandler.responseToNoInternetAccess();
        else {
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
                    for (int i = 0; i < activations.length; i++)
                        activationsList.add(activations[i]);
                    String[] activationsArr = activationsList.toArray(activations);
                    ArrayAdapter<String> activationAdapter = new ArrayAdapter<String>(NetworkSettingsActivity.this,
                            android.R.layout.simple_spinner_item, activationsArr);
                    activationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    activationSpinner.setAdapter(activationAdapter);
                }
            }.execute();
        }
    }

    // Gets the losses list from EC2 instance / web server based on which version they has
    public void getLossesList() {
        if (!permissionAccessHandler.isInternetAccessAvailable())
            permissionAccessHandler.responseToNoInternetAccess();
        else {
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

    // Saves the fitting / training hyperparameters
    public void saveFitModelSettings(View view) {
        if (!permissionAccessHandler.isInternetAccessAvailable())
            permissionAccessHandler.responseToNoInternetAccess();
        else {
            epoch = Integer.parseInt(epochInput.getText().toString());
            batchSize = Integer.parseInt(batchSizeInput.getText().toString());
            timeStep = Integer.parseInt(timeStepInput.getText().toString());
            if (epoch <= 0 || batchSize <= 0 || timeStep <= 0)
                Toast.makeText(this, "Error dropout, epoch or batch size input", Toast.LENGTH_LONG).show();
            else {
                new AsyncTask<Void, Void, String>() {
                    @Override
                    protected String doInBackground(Void... params) {
                        try {
                            File file = new File(getApplicationContext().getFilesDir(), "fitModelSettings.txt");
                            FileOutputStream outputStream = new FileOutputStream(file, false);
                            String settingsInput = epochInput.getText().toString() + ';' + batchSizeInput.getText().toString() + ';' + timeStepInput.getText().toString();
                            outputStream.write(settingsInput.getBytes());
                            outputStream.close();
                            if (file.exists()) {
                                TransferUtility transferUtility =
                                        TransferUtility.builder()
                                                .context(getApplicationContext())
                                                .s3Client(s3Handler.getS3client())
                                                .build();
                                Date date = new Date();
                                String sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(date);
                                TransferObserver uploadObserver = transferUtility.upload(
                                        s3Handler.getAndroidID() + "/fitModelSettings.txt",
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
                }.execute();
            }
        }
    }
}
