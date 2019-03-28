package unclesave.example.com.test2;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;

public class FitModelActivity extends AppCompatActivity {

    private String androidID;
    private Button fitModelButton;
    private String graphVisualization;
    private String example;
    private TextView trainingOutputInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fit_model);

        responseToNoInternetAccess();
        android.support.v7.preference.PreferenceManager
                .setDefaultValues(this, R.xml.preferences, false);
        SharedPreferences sharedPref =
                android.support.v7.preference.PreferenceManager
                        .getDefaultSharedPreferences(this);
        androidID = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ANDROID_ID);
        fitModelButton = findViewById(R.id.fit_model_button);
        trainingOutputInfo = findViewById(R.id.training_output);
    }

    // Checks if internet access is available
    private boolean isInternetAccessAvailable() {
        // Checks whether wifi or mobile data is connected
        ConnectivityManager cm =
                (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork;
        if (cm != null)
            activeNetwork = cm.getActiveNetworkInfo();
        else {
            Log.e("Network error", "Cannot get network info");
            return false;
        }
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    private void responseToNoInternetAccess() {
        if (!isInternetAccessAvailable()) {
            CustomDialogFragment internetAccessDialog = CustomDialogFragment.newInstance(102);
            internetAccessDialog.show(getFragmentManager(), null);
        }
    }

    public void fitModel(View view) {
        responseToNoInternetAccess();
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
                Request request = new Request.Builder().url(urllink + androidID + ";" +
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
                trainingOutputInfo.setVisibility(View.VISIBLE);
                String formattedResult = result.replace(";", "\n");
                trainingOutputInfo.setText(formattedResult);
            }

            @Override
            protected void onProgressUpdate(Void... values) {
                super.onProgressUpdate(values);
            }
        }.execute();
    }

    public void wantGraph(String graphVisualization) {
        this.graphVisualization = graphVisualization;
    }

    public void useExample(String example) {
        this.example = example;
    }
}
