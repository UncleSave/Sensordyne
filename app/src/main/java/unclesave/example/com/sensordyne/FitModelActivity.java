package unclesave.example.com.sensordyne;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;

// An activity that fits/train the model after it is being built with NetworkSettingsActivity
public class FitModelActivity extends AppCompatActivity {

    private PermissionAccessHandler permissionAccessHandler;
    private S3Handler s3Handler;
    private Button fitModelButton, convertModelButton;
    private String graphVisualization;
    private String example;
    //private TextView trainingOutputInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fit_model);
        // Checks the permission given with permission handler
        permissionAccessHandler = new PermissionAccessHandler(this);
        // Checks the internet access with permission handler
        if (!permissionAccessHandler.isInternetAccessAvailable())
            permissionAccessHandler.responseToNoInternetAccess();
        // Allows the access to S3 cloud storage with s3 handler
        s3Handler = new S3Handler(FitModelActivity.this, this);

        android.support.v7.preference.PreferenceManager
                .setDefaultValues(this, R.xml.preferences, false);
        SharedPreferences sharedPref =
                android.support.v7.preference.PreferenceManager
                        .getDefaultSharedPreferences(this);
        fitModelButton = findViewById(R.id.fit_model_button);
        convertModelButton = findViewById(R.id.convert_model_button);
        //trainingOutputInfo = findViewById(R.id.training_output);
    }

    // Fits / trains the built model
    public void fitModel(View view) {
        // Checks the internet access
        if (!permissionAccessHandler.isInternetAccessAvailable())
            permissionAccessHandler.responseToNoInternetAccess();
        else {
            CustomDialogFragment promptGraphDialog = CustomDialogFragment.newInstance(300);
            promptGraphDialog.show(getFragmentManager(), null);
            final String urllink = "http://13.228.232.119/fitmodel/";
            new AsyncTask<Void, Void, String>() {
                @Override
                protected String doInBackground(Void... params) {
                    OkHttpClient okHttpClient = new OkHttpClient();
                    okHttpClient = okHttpClient.newBuilder().connectTimeout(36, TimeUnit.MINUTES)
                            .writeTimeout(36, TimeUnit.MINUTES)
                            .readTimeout(36, TimeUnit.MINUTES)
                            .build();
                    Request request = new Request.Builder().url(urllink + s3Handler.getAndroidID() + ";" +
                            graphVisualization + ";" + example).build();
                    try {
                        return okHttpClient.newCall(request).execute().body().string();
                    } catch (IOException e) {
                        return "IOException occurs";
                    } catch (NullPointerException e) {
                        return "NullPointerException occurs";
                    }
                }

                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                    Toast.makeText(getApplicationContext(), "Please wait for a few minutes for the result, it might take a long time", Toast.LENGTH_LONG).show();
                }

                @Override
                protected void onPostExecute(String result) {
                    super.onPostExecute(result);
                /*trainingOutputInfo.setVisibility(View.VISIBLE);
                String formattedResult = result.replace(";", "\n");
                trainingOutputInfo.setText(formattedResult);*/
                }

                @Override
                protected void onProgressUpdate(Void... values) {
                    super.onProgressUpdate(values);
                }
            }.execute();
        }
    }

    // Convert model after satisfied with the training result
    public void convertModel(View view) {
        if (!permissionAccessHandler.isInternetAccessAvailable())
            permissionAccessHandler.responseToNoInternetAccess();
        else {
            final String urllink = "http://13.228.232.119/convertmodel/";
            new AsyncTask<Void, Void, String>() {
                @Override
                protected String doInBackground(Void... params) {
                    OkHttpClient okHttpClient = new OkHttpClient();
                    okHttpClient = okHttpClient.newBuilder().connectTimeout(36, TimeUnit.MINUTES)
                            .writeTimeout(36, TimeUnit.MINUTES)
                            .readTimeout(36, TimeUnit.MINUTES)
                            .build();
                    Request request = new Request.Builder().url(urllink + s3Handler.getAndroidID()).build();
                    try {
                        return okHttpClient.newCall(request).execute().body().string();
                    } catch (IOException e) {
                        return "IOException occurs";
                    } catch (NullPointerException e) {
                        return "NullPointerException occurs";
                    }
                }

                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                    Toast.makeText(getApplicationContext(), "Please wait for a few minutes for the result, it might take a long time", Toast.LENGTH_LONG).show();
                }

                @Override
                protected void onPostExecute(String result) {
                    super.onPostExecute(result);
                    if (result.equals("Fail"))
                        Toast.makeText(getApplicationContext(), "Fail! Please recheck S3 for json and h5 file", Toast.LENGTH_LONG).show();
                    else
                        Toast.makeText(getApplicationContext(), "Success! Please check S3 for converted model", Toast.LENGTH_LONG).show();
                }

                @Override
                protected void onProgressUpdate(Void... values) {
                    super.onProgressUpdate(values);
                }
            }.execute();
        }
    }

    // Create the training accuracy / loss graph
    public void wantGraph(String graphVisualization) { this.graphVisualization = graphVisualization; }

    public void useExample(String example) {
        this.example = example;
    }
}
