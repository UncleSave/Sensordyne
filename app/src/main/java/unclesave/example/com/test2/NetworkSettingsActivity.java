package unclesave.example.com.test2;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;

public class NetworkSettingsActivity extends AppCompatActivity
        implements AdapterView.OnItemSelectedListener {

    private Spinner optimizerSpinner, lossSpinner, activationSpinner;
    private EditText dropoutInput, batchSizeInput, epochInput;
    private Button saveButton;
    private String androidID;
    private static final String[] optimizers = {"Adadelta", "Adagrad", "Adam", "Adamax", "Nadam",
            "RMSprop", "SGD"};
    private static final String[] losses = {"mean_squared_error", "mean_absolute_error", "mean_absolute_percentage",
            "mean_squared_logarithmic_error", "squared_hinge", "hinge", "categorical_hinge", "logcosh",
            "categorical_crossentropy", "sparse_categorical_crossentropy", "binary_crossentropy",
            "kullback_leibler_divergence", "poisson", "cosine_proximity"};
    private static final String[] activations = {"softmax", "elu", "selu", "softplus", "softsign",
            "relu", "tanh", "sigmoid", "hard_sigmoid", "exponential", "linear"};
    private String optimizer, loss, activation;
    private float dropout;
    private int batchSize, epoch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_network_settings);

        // Checks whether wifi or mobile data is connected
        ConnectivityManager cm =
                (ConnectivityManager)this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
        if (!isConnected) {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setMessage("Please enable wifi or mobile data");
            alertDialogBuilder.setPositiveButton("Close",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            finish();
                        }
                    });
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        }

        androidID = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ANDROID_ID);

        optimizerSpinner = findViewById(R.id.optimizer_input);
        ArrayAdapter<String> optimizerAdapter = new ArrayAdapter<String>(NetworkSettingsActivity.this,
                android.R.layout.simple_spinner_item, optimizers);
        optimizerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        optimizerSpinner.setAdapter(optimizerAdapter);
        optimizerSpinner.setOnItemSelectedListener(this);

        lossSpinner = findViewById(R.id.loss_input);
        ArrayAdapter<String> lossAdapter = new ArrayAdapter<String>(NetworkSettingsActivity.this,
                android.R.layout.simple_spinner_item, losses);
        lossAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        lossSpinner.setAdapter(lossAdapter);
        lossSpinner.setOnItemSelectedListener(this);

        activationSpinner = findViewById(R.id.activation_input);
        ArrayAdapter<String> activationAdapter = new ArrayAdapter<String>(NetworkSettingsActivity.this,
                android.R.layout.simple_spinner_item, activations);
        activationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        activationSpinner.setAdapter(activationAdapter);
        activationSpinner.setOnItemSelectedListener(this);

        dropoutInput = findViewById(R.id.dropout_input);
        epochInput = findViewById(R.id.epoch_input);
        batchSizeInput = findViewById(R.id.batchsize_input);
        saveButton = findViewById(R.id.save_button);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
        switch (parent.getId()) {
            case R.id.optimizer_input:
                optimizer = optimizerSpinner.getSelectedItem().toString();
                break;
            case R.id.loss_input:
                loss = lossSpinner.getSelectedItem().toString();
                break;
            case R.id.activation_input:
                activation = activationSpinner.getSelectedItem().toString();
                break;
            default: break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    public void saveSettings(View view) {
        String urllink = "http://13.228.232.119/savesettings/";
        dropout = Float.parseFloat(dropoutInput.getText().toString());
        epoch = Integer.parseInt(epochInput.getText().toString());
        batchSize = Integer.parseInt(batchSizeInput.getText().toString());
        if (dropout < 0.01f) {
            dropout = 0.16f;
        }
        if (epoch < 1) {
            epoch = 5;
        }
        if (batchSize < 1) {
            batchSize = 12;
        }
        final ParameterSettings parameters = new ParameterSettings(urllink, androidID, optimizer, loss,
                activation, dropout, batchSize, epoch);
        new AsyncTask<ParameterSettings, Void, String>() {
            @Override
            protected String doInBackground(ParameterSettings... params) {
                OkHttpClient okHttpClient = new OkHttpClient();
                okHttpClient = okHttpClient.newBuilder().connectTimeout(36, TimeUnit.MINUTES)
                        .writeTimeout(36, TimeUnit.MINUTES)
                        .readTimeout(36, TimeUnit.MINUTES)
                        .build();
                Request request = new Request.Builder().url(params[0].getUrllink() + params[0].getAndroidID()
                + params[0].getSettingsInput()).build();
                try {
                    return okHttpClient.newCall(request).execute().body().string();
                } catch (IOException e) { return "IOException occurs"; }
            }

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                Toast.makeText(getApplicationContext(), "Saving..." + parameters.getSettingsInput(), Toast.LENGTH_SHORT).show();
            }

            @Override
            protected void onPostExecute(String result) {
                super.onPostExecute(result);
                Toast.makeText(getApplicationContext(), "Successfully save settings!" + result, Toast.LENGTH_LONG).show();
            }

            @Override
            protected void onProgressUpdate(Void... values) {
                super.onProgressUpdate(values);
            }
        }.execute(parameters);
    }
}
