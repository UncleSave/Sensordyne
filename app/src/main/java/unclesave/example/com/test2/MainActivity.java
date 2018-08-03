package unclesave.example.com.test2;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

// AWS Mobile hub backend integration
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobile.auth.core.IdentityHandler;
import com.amazonaws.mobile.auth.core.IdentityManager;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.AWSStartupHandler;
import com.amazonaws.mobile.client.AWSStartupResult;
import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobileconnectors.lambdainvoker.LambdaFunctionException;
import com.amazonaws.mobileconnectors.lambdainvoker.*;
import com.amazonaws.regions.Regions;
import com.amazonaws.mobileconnectors.s3.transferutility.*;
import com.amazonaws.services.s3.AmazonS3Client;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, SensorEventListener {

    private SensorManager sensorManager;
    private Sensor gyroscope;
    private TextView gyroscopeInfo;
    private TextView outputInfo;
    private Button exportCSVButton;
    private Button startCollectButton;
    private Button stopCollectButton;
    private Button outputLeftButton;
    private Button outputRightButton;
    private Button outputBothButton;
    private Button outputUnknownButton;
    private AWSCredentialsProvider credentialsProvider;
    private AWSConfiguration configuration;
    private double gyroscopeVal[] = new double[3];
    private long timeStamp;
    private SQLiteDatabase sensorDataDB = null;
    private LambdaInvokerFactory factory;
    private LambdaInterface lambdaInterface;
    private String gesture = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
        factory = new LambdaInvokerFactory(this.getApplicationContext(),
                Regions.US_EAST_1, cognitoProvider);
        lambdaInterface = factory.build(LambdaInterface.class);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        gyroscopeInfo = findViewById(R.id.gyroscope_info);
        if (gyroscope == null)
            gyroscopeInfo.setText("Gyroscope cannot be found");
        outputInfo = findViewById(R.id.output_info);

        exportCSVButton = findViewById(R.id.export_button);
        startCollectButton = findViewById(R.id.start_button);
        stopCollectButton = findViewById(R.id.stop_button);
        outputLeftButton = findViewById(R.id.left_button);
        outputRightButton = findViewById(R.id.right_button);
        outputBothButton = findViewById(R.id.both_button);
        outputUnknownButton = findViewById(R.id.unknown_button);

        outputLeftButton.setVisibility(View.GONE);
        outputRightButton.setVisibility(View.GONE);
        outputBothButton.setVisibility(View.GONE);
        outputUnknownButton.setVisibility(View.GONE);

        getApplicationContext().deleteDatabase("CurInstSensorDB");
        createDatabase();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (gyroscope != null) {
            sensorManager.registerListener(this, gyroscope,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        sensorManager.unregisterListener(this);
        super.onStop();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id) {
            case R.id.action_settings: Intent getSettingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(getSettingsIntent);
                return true;
            case R.id.action_about: DialogFragment aboutMeFragment = new AboutMeFragment();
                aboutMeFragment.show(getFragmentManager(), "aboutMeDialog");
                return true;
            case R.id.action_exit: finish();
                return true;
            default: break;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_available_sensor) {
            Intent getAvailableSensorIntent = new Intent(this, CheckSensorActivity.class);
            startActivity(getAvailableSensorIntent);
            return true;
        } else if (id == R.id.nav_device_info) {
            Intent getDeviceInfoIntent = new Intent(this, DeviceInfoActivity.class);
            startActivity(getDeviceInfoIntent);
            return true;
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        int sensorType = event.sensor.getType();
        switch (sensorType) {
            case Sensor.TYPE_GYROSCOPE:
                gyroscopeVal[0] = event.values[0];
                gyroscopeVal[1] = event.values[1];
                gyroscopeVal[2] = event.values[2];
                timeStamp = event.timestamp;
                gyroscopeInfo.setText(getResources().getString(
                        R.string.gyroscope_text, event.values[0],
                        event.values[1], event.values[2]));
                if (!gesture.equals("")) {
                    sensorDataDB.execSQL("INSERT INTO sensordata (timestamp, gyroscopeX, " +
                            "gyroscopeY, gyroscopeZ, gesture" +
                            ") VALUES " +
                            "(" + timeStamp + ", " + gyroscopeVal[0] + ", " + gyroscopeVal[1] + ", "
                            + gyroscopeVal[2] + ", '" + gesture + "');");
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void createDatabase() {
        try {
            sensorDataDB = this.openOrCreateDatabase("CurInstSensorDB", MODE_PRIVATE, null);
            sensorDataDB.execSQL("CREATE TABLE IF NOT EXISTS sensordata " +
                    "(id INTEGER primary key, timestamp REAL, gyroscopeX REAL, gyroscopeY REAL, " +
                    "gyroscopeZ REAL, gesture TEXT);");
            File outputFile = getApplicationContext().getDatabasePath("CurInstSensorDB");
            if (outputFile.exists())
                Toast.makeText(this, "Database created", Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(this, "Database missing", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e("CURINSTSENSORDB ERROR", "Error creating database");
        }
        exportCSVButton.setClickable(true);
    }

    public void startCollect(View view) {
        startCollectButton.setClickable(false);
        exportCSVButton.setClickable(false);
        outputLeftButton.setVisibility(View.VISIBLE);
        outputRightButton.setVisibility(View.VISIBLE);
        outputBothButton.setVisibility(View.VISIBLE);
        outputUnknownButton.setVisibility(View.VISIBLE);
        stopCollectButton.setClickable(true);
        Toast.makeText(this, "Process starts now, " +
                "click either one of the button below to put label for the data",
                Toast.LENGTH_LONG).show();
    }

    public void stopCollect(View view) {
        startCollectButton.setClickable(true);
        exportCSVButton.setClickable(true);
        outputLeftButton.setVisibility(View.GONE);
        outputRightButton.setVisibility(View.GONE);
        outputBothButton.setVisibility(View.GONE);
        outputUnknownButton.setVisibility(View.GONE);
        stopCollectButton.setClickable(false);
        gesture = "";
        Toast.makeText(this, "Process stops now",
                Toast.LENGTH_SHORT).show();
    }

    public void labelOutput(View view) {
        switch(view.getId()) {
            case R.id.left_button:
                gesture = "Left";
                Toast.makeText(this, "Please hold your phone in left hand", Toast.LENGTH_LONG).show();
                break;
            case R.id.right_button:
                gesture = "Right";
                Toast.makeText(this, "Please hold your phone in right hand", Toast.LENGTH_LONG).show();
                break;
            case R.id.both_button:
                gesture = "Both";
                Toast.makeText(this, "Please hold your phone in both hand", Toast.LENGTH_LONG).show();
                break;
            case R.id.unknown_button:
                gesture = "Unknown";
                Toast.makeText(this, "Put your phone on table", Toast.LENGTH_LONG).show();
                break;
            default:
                break;
        }
    }

    public void exportToCSV(View view) {
        Cursor cur = sensorDataDB.rawQuery("SELECT * FROM sensordata", null);
        cur.moveToFirst();
        FileOutputStream outputStream;
        File file = new File(getApplicationContext().getFilesDir(), "output.csv");
        if (cur.getCount() > 0) {
            try {
                outputStream = new FileOutputStream(file);
                do {
                    String recordCount = cur.getString(0);
                    String tStamp = cur.getString(1);
                    String gyroX = cur.getString(2);
                    String gyroY = cur.getString(3);
                    String gyroZ = cur.getString(4);
                    String gesture = cur.getString(5);
                    outputStream.write(recordCount.getBytes());
                    outputStream.write(",".getBytes());
                    outputStream.write(tStamp.getBytes());
                    outputStream.write(",".getBytes());
                    outputStream.write(gyroX.getBytes());
                    outputStream.write(",".getBytes());
                    outputStream.write(gyroY.getBytes());
                    outputStream.write(",".getBytes());
                    outputStream.write(gyroZ.getBytes());
                    outputStream.write(",".getBytes());
                    outputStream.write(gesture.getBytes());
                    outputStream.write("\n".getBytes());
                } while (cur.moveToNext());

                outputStream.flush();
                outputStream.close();
                Toast.makeText(this, "CSV file created and uploaded to AWS S3" +
                        "\n" + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        else
            Toast.makeText(this, "There are no result", Toast.LENGTH_SHORT).show();
        cur.close();

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
                    Settings.Secure.getString(getContentResolver(),
                    Settings.Secure.ANDROID_ID) + "/" + sdf + ".csv",
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
                }
            });

            // If you prefer to poll for the data, instead of attaching a
            // listener, check for the state and progress in the observer.
            if (TransferState.COMPLETED == uploadObserver.getState()) {
                // Handle a completed upload.
            }

            Log.d("YourActivity", "Bytes Transferred: " + uploadObserver.getBytesTransferred());
            Log.d("YourActivity", "Bytes Total: " + uploadObserver.getBytesTotal());
        }
    }


    // Still in development
    public void predictOutput(View view) {
        RequestClass request = new RequestClass(timeStamp, gyroscopeVal);
        // The Lambda function invocation results in a network call.
        // Make sure it is not called from the main thread.
        new AsyncTask<RequestClass, Void, ResponseClass>() {
            @Override
            protected ResponseClass doInBackground(RequestClass... params) {
                // invoke "echo" method. In case it fails, it will throw a
                // LambdaFunctionException.
                try {
                    return lambdaInterface.AndroidBackendLambdaFunction(params[0]);
                } catch (LambdaFunctionException lfe) {
                    Log.e("Tag", "Failed to invoke prediction function", lfe);
                    return null;
                }
            }

            @Override
            protected void onPostExecute(ResponseClass result) {
                if (result == null) {
                    return;
                }

                // Do a toast
                outputInfo.setText("Predicted output: " + result.getOutput());
            }
        }.execute(request);
    }
}
