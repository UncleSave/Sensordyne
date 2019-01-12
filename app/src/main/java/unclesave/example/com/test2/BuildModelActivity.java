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
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;

public class BuildModelActivity extends AppCompatActivity {

    private String androidID;
    private Button buildModelButton;
    private TextView trainingOutputInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_build_model);

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
        buildModelButton = findViewById(R.id.build_model_button);
        trainingOutputInfo = findViewById(R.id.training_output);
    }

    public void buildModel(View view) {
        String urllink = "http://13.228.232.119/trainmodel/";
        RequestClass request = new RequestClass(urllink, androidID);
        new AsyncTask<RequestClass, Void, String>() {
            @Override
            protected String doInBackground(RequestClass... params) {
                OkHttpClient okHttpClient = new OkHttpClient();
                okHttpClient = okHttpClient.newBuilder().connectTimeout(36, TimeUnit.MINUTES)
                        .writeTimeout(36, TimeUnit.MINUTES)
                        .readTimeout(36, TimeUnit.MINUTES)
                        .build();
                Request request = new Request.Builder().url(params[0].getUrllink() + params[0].getAndroidID()).build();
                try {
                    return okHttpClient.newCall(request).execute().body().string();
                } catch (IOException e) { return "IOException occurs"; }
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
            }.execute(request);
    }
}
