package unclesave.example.com.test2;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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
import android.widget.EditText;
import android.widget.TextView;
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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;


// AWS Mobile hub backend integration

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, SensorEventListener {

    private static final int PERMISSION_ALL = 1;
    private float cutoffFrequency = 0.3f;
    private int accFilterCount = 0;
    private long accTimeStamp;
    private SensorManager sensorManager;
    private Sensor gyroscope;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private Sensor gravmeter;
    private Sensor linearaccelerometer;
    private Sensor proximity;
    private boolean gyroSwitchPref;
    private boolean accSwitchPref;
    private boolean magSwitchPref;
    private boolean gravSwitchPref;
    private boolean orientationSwitchPref;
    private boolean linearAccSwitchPref;
    private boolean proximitySwitchPref;
    private boolean textToSpeechSwitchPref;
    private boolean logTimerSwitchPref;
    private boolean lpfLinearAccCheckBoxPref;
    private int timeLabelIntervalPref;
    private int timeLoggingIntervalPref;
    private int sensorSamplingDelayPref;
    private String collectModePref;
    private String timerModePref;
    private boolean linearAccUnsupported;
    //private TextView gyroscopeInfo, accelerometerInfo;
    private Button exportCloudButton;
    private Button localCSVButton;
    private TextView labelInstruct;
    private EditText labelInfo;
    private Button addLabelButton;
    private Button clearLabelButton;
    private TextView labelListInfo;
    private Button startCollectButton;
    private Button stopCollectButton;
    private TextView automatedEventInfo;
    private AWSCredentialsProvider credentialsProvider;
    private AWSConfiguration configuration;
    private String androidID;
    private float gyroscopeVal[];
    private float acceleroVal[];
    private float magnetoVal[];
    private float orientationVal[];
    private float gravVal[];
    private float linearAcceleroVal[];
    private long proximityVal;
    private float r[];
    private long timeStamp;
    private SQLiteDatabase sensorDataDB = null;
    private ArrayList<String> labels = new ArrayList<>();
    private ArrayList<Integer> indexForLabel = new ArrayList<>();
    private Iterator<Integer> integerIterator;
    private boolean collectStatus = false;
    private boolean gravEmpty = true;
    private boolean accEmpty = true;
    private String label = "";
    private StringBuilder insertCommand = new StringBuilder();
    private SQLiteStatement insert;
    private Timer collectTimer;
    private Timer logTimer;
    private TextToSpeech tts;
    private CustomDialogFragment permissionDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        responseToNoPermission();
        permissionDialog = null;
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
        /*CognitoCachingCredentialsProvider cognitoProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                "us-east-1:0c63f012-ff89-4907-88ea-527828cd5db0", // Identity pool ID
                Regions.US_EAST_1 // Region
        );*/

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        android.support.v7.preference.PreferenceManager
                .setDefaultValues(this, R.xml.preferences, false);
        SharedPreferences sharedPref =
                android.support.v7.preference.PreferenceManager
                        .getDefaultSharedPreferences(this);
        gyroSwitchPref = sharedPref.getBoolean
                (SettingsActivity.KEY_PREF_GYROSCOPE_SWITCH, false);
        accSwitchPref = sharedPref.getBoolean
                (SettingsActivity.KEY_PREF_ACCELEROMETER_SWITCH, false);
        magSwitchPref = sharedPref.getBoolean
                (SettingsActivity.KEY_PREF_MAGNETOMETER_SWITCH, false);
        orientationSwitchPref = sharedPref.getBoolean
                (SettingsActivity.KEY_PREF_ORIENTATION_SWITCH, false);
        gravSwitchPref = sharedPref.getBoolean
                (SettingsActivity.KEY_PREF_GRAVITY_SWITCH, false);
        linearAccSwitchPref = sharedPref.getBoolean
                (SettingsActivity.KEY_PREF_LINEAR_ACCELEROMETER_SWITCH, false);
        lpfLinearAccCheckBoxPref = sharedPref.getBoolean
                (SettingsActivity.KEY_PREF_LPF_LINEAR_ACC_SWITCH, false);
        proximitySwitchPref = sharedPref.getBoolean
                (SettingsActivity.KEY_PREF_PROXIMITY_SWITCH, false);
        timeLabelIntervalPref = sharedPref.getInt
                (SettingsActivity.KEY_PREF_TIME_LABEL_INTERVAL, 4000);
        timeLoggingIntervalPref = sharedPref.getInt
                (SettingsActivity.KEY_PREF_TIME_LOG_INTERVAL, 20);
        sensorSamplingDelayPref = sharedPref.getInt
                (SettingsActivity.KEY_PREF_SENSOR_SAMPLING_DELAY, 5000);
        collectModePref = sharedPref.getString
                (SettingsActivity.KEY_PREF_COLLECT_MODE, "Random");
        logTimerSwitchPref = sharedPref.getBoolean
                (SettingsActivity.KEY_PREF_LOG_TIMER_SWITCH, true);
        timerModePref = sharedPref.getString
                (SettingsActivity.KEY_PREF_TIMER_MODE, "Schedule");
        textToSpeechSwitchPref = sharedPref.getBoolean
                (SettingsActivity.KEY_PREF_TEXT_TO_SPEECH, false);
        androidID = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ANDROID_ID);
        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            if (gyroSwitchPref) {
                gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
                gyroscopeVal = new float[3];
            }
            if (accSwitchPref
                    || (linearAccSwitchPref && linearAccUnsupported)
                    || (linearAccSwitchPref && !linearAccUnsupported && lpfLinearAccCheckBoxPref)) {
                accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                acceleroVal = new float[3];
            }
            if (magSwitchPref) {
                magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
                magnetoVal = new float[3];
            }
            if (gravSwitchPref
                    || (linearAccSwitchPref && linearAccUnsupported)
                    || (linearAccSwitchPref && !linearAccUnsupported && lpfLinearAccCheckBoxPref)) {
                gravmeter = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
                gravVal = new float[3];
            }
            if (linearAccSwitchPref) {
                linearaccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
                linearAccUnsupported = (linearaccelerometer == null);
                //if (!linearAccUnsupported && !lpfLinearAccCheckBoxPref) // use system linear acceleration
                linearAcceleroVal = new float[3];
            }
            if (orientationSwitchPref) {
                orientationVal = new float[3];
                r = new float[9];
            }
            if (proximitySwitchPref)
                proximity = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        } else {
            Log.e("Sensor Manager", "Null pointer exception");
            CustomDialogFragment sensorManagerDialog = CustomDialogFragment.newInstance(103);
            sensorManagerDialog.show(getFragmentManager(), "dialog");
            //createAlertDialog("Cannot start sensor manager");
        }
        exportCloudButton = findViewById(R.id.export_button);
        localCSVButton = findViewById(R.id.local_csv_button);
        labelInstruct = findViewById(R.id.label_instruct);
        labelInfo = findViewById(R.id.label_info);
        addLabelButton = findViewById(R.id.add_label_button);
        clearLabelButton = findViewById(R.id.clear_label_button);
        labelListInfo = findViewById(R.id.label_list_info);
        startCollectButton = findViewById(R.id.start_collect_button);
        stopCollectButton = findViewById(R.id.stop_collect_button);
        automatedEventInfo = findViewById(R.id.automated_label_event);
        stopCollectButton.setClickable(false);
        automatedEventInfo.setVisibility(View.GONE);

        getApplicationContext().deleteDatabase("CurInstSensorDB");
        createDatabase();
        if (textToSpeechSwitchPref)
            speak("Welcome to Sensordyne!");

        //559, 560
        insertCommand.append("INSERT INTO sensordata (timestamp, label");
        if (gyroSwitchPref)
            insertCommand.append(", gyroX, gyroY, gyroZ");
        if (accSwitchPref)
            insertCommand.append(", accX, accY, accZ");
        if (magSwitchPref)
            insertCommand.append(", magX, magY, magZ");
        if (orientationSwitchPref)
            insertCommand.append(", azimut, pitch, roll");
        if (gravSwitchPref)
            insertCommand.append(", gravX, gravY, gravZ");
        if (linearAccSwitchPref)
            insertCommand.append(", linearAccX, linearAccY, linearAccZ");
        if (proximitySwitchPref)
            insertCommand.append(", proximity");
        insertCommand.append(") VALUES (?, ?");
        if (gyroSwitchPref)
            insertCommand.append(", ?, ?, ?");
        if (accSwitchPref)
            insertCommand.append(", ?, ?, ?");
        if (magSwitchPref)
            insertCommand.append(", ?, ?, ?");
        if (orientationSwitchPref)
            insertCommand.append(", ?, ?, ?");
        if (gravSwitchPref)
            insertCommand.append(", ?, ?, ?");
        if (linearAccSwitchPref)
            insertCommand.append(", ?, ?, ?");
        if (proximitySwitchPref)
            insertCommand.append(", ?");
        insertCommand.append(");");
        insert = sensorDataDB.compileStatement(insertCommand.toString());
        /*Toast.makeText(this, "Gyro: " + Boolean.toString(gyroSwitchPref) +
                " " + "Acc: " + Boolean.toString(accSwitchPref) +
                        " " + "Mag: " + Boolean.toString(magSwitchPref) +
                        " " + "Orien: " + Boolean.toString(orientationSwitchPref) +
                        " " + "Grav: " + Boolean.toString(gravSwitchPref) +
                        " " + "Linear acc: " + Boolean.toString(linearAccSwitchPref)
                , Toast.LENGTH_LONG).show();*/
        /*Toast.makeText(this, "Label: " + timeLabelIntervalPref
                + " Log: " + timeLoggingIntervalPref + " Sensor: " + sensorSamplingDelayPref, Toast.LENGTH_LONG).show();*/
        //Toast.makeText(this, "Collect mode: " + collectModePref + " Timer mode: " + timerModePref, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        onListener();
    }

    @Override
    protected void onPause() {
        super.onPause();
        offListener();
    }

    @Override
    protected void onResume() {
        super.onResume();
        onListener();
    }

    @Override
    protected void onStop() {
        super.onStop();
        offListener();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        File file = new File(getApplicationContext().getFilesDir(), "output.csv");
        if (file.exists())
            file.delete();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START))
            drawer.closeDrawer(GravityCompat.START);
        else
            super.onBackPressed();
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
                finish();
                startActivity(getSettingsIntent);
                return true;
            case R.id.action_about: CustomDialogFragment aboutMeFragment = CustomDialogFragment.newInstance(1);
                aboutMeFragment.show(getFragmentManager(), null);
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
        } else if (id == R.id.nav_manage_file_info) {
            Intent getManageFileIntent = new Intent(this, ManageFileActivity.class);
            startActivity(getManageFileIntent);
            return true;
        } else if (id == R.id.nav_build_model_info) {
            Intent getBuildModelIntent = new Intent(this, FitModelActivity.class);
            startActivity(getBuildModelIntent);
            return true;
        } else if (id == R.id.nav_network_settings_info) {
            Intent getNetworkSettingsIntent = new Intent(this, NetworkSettingsActivity.class);
            startActivity(getNetworkSettingsIntent);
            return true;
        } else if (id == R.id.nav_predict_gesture) {
            Intent getPredictGestureIntent = new Intent(this, PredictGestureActivity.class);
            startActivity(getPredictGestureIntent);
            return true;
        } else if (id == R.id.nav_predict_morse_code) {
            Intent getPredictMorseCodeIntent = new Intent(this, PredictMorseCodeActivity.class);
            startActivity(getPredictMorseCodeIntent);
            return true;
        } else;
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        int sensorType = event.sensor.getType();
        if (sensorType == Sensor.TYPE_GRAVITY || sensorType == Sensor.TYPE_ACCELEROMETER) {
            if (gravEmpty || accEmpty) {
                gravEmpty = (gravVal[0] == 0.0f && gravVal[1] == 0.0f && gravVal[2] == 0.0f) ? true : false;
                accEmpty = (acceleroVal[0] == 0.0f && acceleroVal[1] == 0.0f && acceleroVal[2] == 0.0f) ? true : false;
            }
        }
        switch (sensorType) {
            case Sensor.TYPE_GYROSCOPE:
                System.arraycopy(event.values, 0, gyroscopeVal, 0, event.values.length);
                break;
            case Sensor.TYPE_ACCELEROMETER:
                if (linearAccSwitchPref && lpfLinearAccCheckBoxPref) {
                    if (!gravEmpty && !accEmpty) {
                        float[] helperGravVal = lowPassFilter(event.timestamp, gravVal);
                        linearAcceleroVal[0] = event.values[0] - helperGravVal[0];
                        linearAcceleroVal[1] = event.values[1] - helperGravVal[1];
                        linearAcceleroVal[2] = event.values[2] - helperGravVal[2];
                    }
                }
                System.arraycopy(event.values, 0, acceleroVal, 0, event.values.length);
                timeStamp = event.timestamp;
                accTimeStamp = timeStamp;
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                System.arraycopy(event.values, 0, magnetoVal, 0, event.values.length);
                timeStamp = event.timestamp;
                break;
            case Sensor.TYPE_GRAVITY:
                System.arraycopy(event.values, 0, gravVal, 0, event.values.length);
                timeStamp = event.timestamp;
                break;
            case Sensor.TYPE_LINEAR_ACCELERATION:
                System.arraycopy(event.values, 0, linearAcceleroVal, 0, event.values.length);
                timeStamp = event.timestamp;
                break;
            case Sensor.TYPE_PROXIMITY:
                proximityVal = (event.values[0] == 0.0f)? 0L: 1L;
                timeStamp = event.timestamp;
                break;
            default: break;
        }
        if (linearAccSwitchPref && linearAccUnsupported && !lpfLinearAccCheckBoxPref) {
            linearAcceleroVal[0] = event.values[0] - gravVal[0];
            linearAcceleroVal[1] = event.values[1] - gravVal[1];
            linearAcceleroVal[2] = event.values[2] - gravVal[2];
        }

        if (accSwitchPref && magSwitchPref && orientationSwitchPref) {
            SensorManager.getRotationMatrix(r, null, acceleroVal, magnetoVal);
            SensorManager.getOrientation(r, orientationVal);
        }
        if (!logTimerSwitchPref && collectStatus && !label.equals(""))
            new loggingTask(this).execute();
    }

    private static class loggingTask extends AsyncTask<Void, Void, Void> {
        private WeakReference<MainActivity> activityReference;

        // only retain a weak reference to the activity
        loggingTask(MainActivity context) {
            activityReference = new WeakReference<>(context);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            int index = 1;
            activityReference.get().insert.bindLong(index++, activityReference.get().timeStamp);
            activityReference.get().insert.bindString(index++, activityReference.get().label);
            if (activityReference.get().gyroSwitchPref) {
                activityReference.get().insert.bindDouble(index++, activityReference.get().gyroscopeVal[0]);
                activityReference.get().insert.bindDouble(index++, activityReference.get().gyroscopeVal[1]);
                activityReference.get().insert.bindDouble(index++, activityReference.get().gyroscopeVal[2]);
            }
            if (activityReference.get().accSwitchPref) {
                activityReference.get().insert.bindDouble(index++, activityReference.get().acceleroVal[0]);
                activityReference.get().insert.bindDouble(index++, activityReference.get().acceleroVal[1]);
                activityReference.get().insert.bindDouble(index++, activityReference.get().acceleroVal[2]);
            }
            if (activityReference.get().magSwitchPref) {
                activityReference.get().insert.bindDouble(index++, activityReference.get().magnetoVal[0]);
                activityReference.get().insert.bindDouble(index++, activityReference.get().magnetoVal[1]);
                activityReference.get().insert.bindDouble(index++, activityReference.get().magnetoVal[2]);
            }
            if (activityReference.get().orientationSwitchPref) {
                activityReference.get().insert.bindDouble(index++, activityReference.get().orientationVal[0]);
                activityReference.get().insert.bindDouble(index++, activityReference.get().orientationVal[1]);
                activityReference.get().insert.bindDouble(index++, activityReference.get().orientationVal[2]);
            }
            if (activityReference.get().gravSwitchPref) {
                activityReference.get().insert.bindDouble(index++, activityReference.get().gravVal[0]);
                activityReference.get().insert.bindDouble(index++, activityReference.get().gravVal[1]);
                activityReference.get().insert.bindDouble(index++, activityReference.get().gravVal[2]);
            }
            if (activityReference.get().linearAccSwitchPref) {
                if (!activityReference.get().linearAccUnsupported) {
                    activityReference.get().insert.bindDouble(index++, activityReference.get().linearAcceleroVal[0]);
                    activityReference.get().insert.bindDouble(index++, activityReference.get().linearAcceleroVal[1]);
                    activityReference.get().insert.bindDouble(index++, activityReference.get().linearAcceleroVal[2]);
                } else {
                    activityReference.get().insert.bindDouble(index++, activityReference.get().acceleroVal[0] - activityReference.get().gravVal[0]);
                    activityReference.get().insert.bindDouble(index++, activityReference.get().acceleroVal[1] - activityReference.get().gravVal[1]);
                    activityReference.get().insert.bindDouble(index++, activityReference.get().acceleroVal[2] - activityReference.get().gravVal[2]);
                }
            }
            if (activityReference.get().proximitySwitchPref)
                activityReference.get().insert.bindLong(index, activityReference.get().proximityVal);
            activityReference.get().insert.execute();
            return null;
        }
    }

    private float calcAlpha(long curTimeStamp) {
        float timeConstant = 1 / cutoffFrequency;
        float deltaT = 1 / (accFilterCount++ / (curTimeStamp - accTimeStamp) / 1000000000.0f);
        return timeConstant / (timeConstant+deltaT);
    }

    private float[] lowPassFilter(long curTimeStamp, float[] values) {
        float alpha = calcAlpha(curTimeStamp);
        float[] output = new float[3];
        output[0] = alpha * output[0] + (1 - alpha) * values[0];
        output[1] = alpha * output[1] + (1 - alpha) * values[1];
        output[2] = alpha * output[2] + (1 - alpha) * values[2];
        return output;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_ALL:
                for (int i = 0; i < grantResults.length; i++) {
                    if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                                permissions[i])) {
                            CustomDialogFragment permissionSettingsDialog = CustomDialogFragment.newInstance(100);
                            permissionSettingsDialog.show(getFragmentManager(), "dialog");
                        } else {
                            if (permissionDialog == null) {
                                permissionDialog = CustomDialogFragment.newInstance(101);
                                permissionDialog.show(getFragmentManager(), "dialog");
                            }
                        }
                    }
                }
                break;
            default: break;
        }
    }

    private String[] convertArrayListStringToStringOfArray(ArrayList<String> source) {
        String[] result = new String[source.size()];
        for (int i = 0; i < source.size(); i++)
            result[i] = source.get(i);
        return result;
    }

    public void responseToNoPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            ArrayList<String> permissionsNeeded = new ArrayList<>();
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET)
                    != PackageManager.PERMISSION_GRANTED)
                permissionsNeeded.add(Manifest.permission.INTERNET);
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE)
                    != PackageManager.PERMISSION_GRANTED)
                permissionsNeeded.add(Manifest.permission.ACCESS_WIFI_STATE);
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE)
                    != PackageManager.PERMISSION_GRANTED)
                permissionsNeeded.add(Manifest.permission.ACCESS_NETWORK_STATE);
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED)
                permissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED)
                permissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED)
                permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);

            if (permissionsNeeded.size() != 0)
                ActivityCompat.requestPermissions(this,
                        convertArrayListStringToStringOfArray(permissionsNeeded),
                        PERMISSION_ALL);
        }
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
            //createAlertDialog("Please enable wifi or mobile data");
    }

    public void onListener() {
        if (gyroSwitchPref)
            sensorManager.registerListener(this, gyroscope,
                    sensorSamplingDelayPref);
        if (accSwitchPref
                || (linearAccSwitchPref && linearAccUnsupported)
                || (linearAccSwitchPref && !linearAccUnsupported && lpfLinearAccCheckBoxPref))
            sensorManager.registerListener(this, accelerometer,
                    sensorSamplingDelayPref);
        if (magSwitchPref)
            sensorManager.registerListener(this, magnetometer,
                    sensorSamplingDelayPref);
        if (gravSwitchPref
                || (linearAccSwitchPref && linearAccUnsupported)
                || (linearAccSwitchPref && !linearAccUnsupported && lpfLinearAccCheckBoxPref))
            sensorManager.registerListener(this, gravmeter,
                    sensorSamplingDelayPref);
        if (linearAccSwitchPref && !linearAccUnsupported && !lpfLinearAccCheckBoxPref)
            sensorManager.registerListener(this, linearaccelerometer,
                    sensorSamplingDelayPref);
        if (proximitySwitchPref)
            sensorManager.registerListener(this, proximity,
                    sensorSamplingDelayPref);
    }

    public void offListener() {
        /*if (gyroSwitchPref)
            sensorManager.unregisterListener(this, gyroscope);
        if (accSwitchPref
                || (linearAccSwitchPref && linearAccUnsupported)
                || (linearAccSwitchPref && !linearAccUnsupported && lpfLinearAccCheckBoxPref))
            sensorManager.unregisterListener(this, accelerometer);
        if (magSwitchPref)
            sensorManager.unregisterListener(this, magnetometer);
        if (gravSwitchPref
                || (linearAccSwitchPref && linearAccUnsupported)
                || (linearAccSwitchPref && !linearAccUnsupported && lpfLinearAccCheckBoxPref))
            sensorManager.unregisterListener(this, gravmeter);
        if (linearAccSwitchPref && !linearAccUnsupported && !lpfLinearAccCheckBoxPref)
            sensorManager.unregisterListener(this, linearaccelerometer);
        if (proximitySwitchPref)
            sensorManager.unregisterListener(this, proximity);*/
        sensorManager.unregisterListener(this);
    }

    private void speak(final String speech) {
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit (int status) {
                // TODO Auto-generated method stub
                if (status == TextToSpeech.SUCCESS) {
                    int result = tts.setLanguage(Locale.US);
                    if (result == TextToSpeech.LANG_MISSING_DATA ||
                            result == TextToSpeech.LANG_NOT_SUPPORTED)
                        Log.e("error", "This Language is not supported");
                    else
                        tts.speak(speech, TextToSpeech.QUEUE_ADD, null);
                } else
                    Log.e("error", "Initialization Failed!");
            }
        });
    }

    private void createDatabase() {
        try {
            StringBuilder createCommand = new StringBuilder("CREATE TABLE IF NOT EXISTS sensordata " +
                    "(id INTEGER primary key, timestamp INTEGER, label TEXT");
            if (gyroSwitchPref)
                createCommand.append(", gyroX REAL, gyroY REAL, gyroZ REAL");
            if (accSwitchPref)
                createCommand.append(", accX REAL, accY REAL, accZ REAL");
            if (magSwitchPref)
                createCommand.append(", magX REAL, magY REAL, magZ REAL");
            if (orientationSwitchPref)
                createCommand.append(", azimut REAL, pitch REAL, roll REAL");
            if (gravSwitchPref)
                createCommand.append(", gravX REAL, gravY REAL, gravZ REAL");
            if (linearAccSwitchPref)
                createCommand.append(", linearAccX REAL, linearAccY REAL, linearAccZ REAL");
            if (proximitySwitchPref)
                createCommand.append(", proximity INTEGER");
            createCommand.append(");");
            sensorDataDB = this.openOrCreateDatabase("CurInstSensorDB", MODE_PRIVATE, null);
            sensorDataDB.beginTransaction();
            sensorDataDB.execSQL(createCommand.toString());
            sensorDataDB.setTransactionSuccessful();
            sensorDataDB.endTransaction();
            File outputFile = getApplicationContext().getDatabasePath("CurInstSensorDB");
            if (outputFile.exists())
                Toast.makeText(this, "Database created", Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(this, "Database missing", Toast.LENGTH_SHORT).show();
            if (!gyroSwitchPref && !accSwitchPref && !magSwitchPref
                    && !orientationSwitchPref && !gravSwitchPref && !linearAccSwitchPref) {
                Toast.makeText(this,
                        "No sensor chosen, database will not be created", Toast.LENGTH_LONG).show();
                Toast.makeText(this,
                        "Please choose to monitor a sensor in Settings", Toast.LENGTH_LONG).show();
                addLabelButton.setClickable(false);
            }
        } catch (Exception e) {
            Log.e("CURINSTSENSORDB ERROR", "Error creating database");
        }
        exportCloudButton.setClickable(true);
    }

    public void clearLabel(View view) {
        labelListInfo.setText(R.string.list_of_labels);
        labels.clear();
    }

    public void deleteLabel(View view) {
        if (labels.contains(labelInfo.getText().toString())) {
            labels.remove(labelInfo.getText().toString());
            Toast.makeText(this, "Label deleted",
                    Toast.LENGTH_SHORT).show();
            labelListInfo.setText("List of labels: " + labels);
        }
        labelInfo.setText("");
    }

    public void startCollect(View view) {
        // Switching collect mode around after first collection causes crashes
        if (!labels.isEmpty()) {
            addLabelButton.setClickable(false);
            startCollectButton.setClickable(false);
            exportCloudButton.setClickable(false);
            stopCollectButton.setClickable(true);
            automatedEventInfo.setVisibility(View.VISIBLE);
            if (collectModePref.equals("Turn")) {
                for (int i = 0; i < labels.size(); i++)
                    indexForLabel.add(i);
                integerIterator = indexForLabel.iterator();
            }
            try {
                collectTimer = new Timer();
                logTimer = new Timer();
                TimerTask collectTimerTask = new TimerTask() {
                    @Override
                    public void run() {
                        int nextLabel;
                        if (collectModePref.equals("Random")) {
                            Random rand = new Random();
                            int randLabel = rand.nextInt(labels.size());
                            if (!label.equals("")) {
                                while (randLabel == labels.indexOf(label))
                                    randLabel = rand.nextInt(labels.size());
                            }
                            nextLabel = randLabel;
                        }
                        else {
                            /*if (integerIterator.hasNext())
                                nextLabel = integerIterator.next();
                            else {
                                integerIterator = indexForLabel.iterator();
                                nextLabel = integerIterator.next();
                            }*/
                            if (!integerIterator.hasNext())
                                integerIterator = indexForLabel.iterator();
                            nextLabel = integerIterator.next();
                        }
                        label = labels.get(nextLabel);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (textToSpeechSwitchPref)
                                    speak(label);
                                automatedEventInfo.setText("Show label: " + label);
                            }
                        });
                    }
                };
                TimerTask logTimerTask = null;
                if (logTimerSwitchPref) {
                    logTimerTask = new TimerTask() {
                        @Override
                        public void run() {
                            if (collectStatus && !label.equals("")) {
                                int index = 1;
                                insert.bindLong(index++, timeStamp);
                                insert.bindString(index++, label);
                                if (gyroSwitchPref) {
                                    insert.bindDouble(index++, gyroscopeVal[0]);
                                    insert.bindDouble(index++, gyroscopeVal[1]);
                                    insert.bindDouble(index++, gyroscopeVal[2]);
                                }
                                if (accSwitchPref) {
                                    insert.bindDouble(index++, acceleroVal[0]);
                                    insert.bindDouble(index++, acceleroVal[1]);
                                    insert.bindDouble(index++, acceleroVal[2]);
                                }
                                if (magSwitchPref) {
                                    insert.bindDouble(index++, magnetoVal[0]);
                                    insert.bindDouble(index++, magnetoVal[1]);
                                    insert.bindDouble(index++, magnetoVal[2]);
                                }
                                if (orientationSwitchPref) {
                                    insert.bindDouble(index++, orientationVal[0]);
                                    insert.bindDouble(index++, orientationVal[1]);
                                    insert.bindDouble(index++, orientationVal[2]);
                                }
                                if (gravSwitchPref) {
                                    insert.bindDouble(index++, gravVal[0]);
                                    insert.bindDouble(index++, gravVal[1]);
                                    insert.bindDouble(index++, gravVal[2]);
                                }
                                if (linearAccSwitchPref) {
                                    if (!linearAccUnsupported) {
                                        insert.bindDouble(index++, linearAcceleroVal[0]);
                                        insert.bindDouble(index++, linearAcceleroVal[1]);
                                        insert.bindDouble(index++, linearAcceleroVal[2]);
                                    } else {
                                        insert.bindDouble(index++, acceleroVal[0] - gravVal[0]);
                                        insert.bindDouble(index++, acceleroVal[1] - gravVal[1]);
                                        insert.bindDouble(index++, acceleroVal[2] - gravVal[2]);
                                    }
                                }
                                if (proximitySwitchPref)
                                    insert.bindLong(index, proximityVal);
                                insert.execute();
                            }
                        }
                    };
                }
                collectStatus = true;
                switch (timerModePref) {
                    case "Schedule": collectTimer.schedule(collectTimerTask, 0, timeLabelIntervalPref);
                        if (logTimerSwitchPref) {
                            accTimeStamp = System.nanoTime();
                            logTimer.schedule(logTimerTask, 0, timeLoggingIntervalPref);
                        }
                        break;
                    case "ScheduleAtFixedRate": collectTimer.scheduleAtFixedRate(collectTimerTask, 0, timeLabelIntervalPref);
                        if (logTimerSwitchPref) {
                            accTimeStamp = System.nanoTime();
                            logTimer.scheduleAtFixedRate(logTimerTask, 0, timeLoggingIntervalPref);
                        }
                        break;
                    default: break;
                }
            } catch (IllegalStateException e) {
                Toast.makeText(this, "Some error occurs, please try again",
                        Toast.LENGTH_SHORT).show();
            }
        } else
            Toast.makeText(this, "There is no label", Toast.LENGTH_SHORT).show();
    }

    public void stopCollect(View view) {
        offListener();
        collectStatus = false;
        label = "";
        logTimer.cancel();
        integerIterator = null;
        indexForLabel.clear();
        collectTimer.cancel();
        addLabelButton.setClickable(true);
        startCollectButton.setClickable(true);
        exportCloudButton.setClickable(true);
        stopCollectButton.setClickable(false);
        automatedEventInfo.setVisibility(View.GONE);
        final Cursor cur = sensorDataDB.rawQuery("SELECT * FROM sensordata", null);
        cur.moveToFirst();
        final File file = new File(getApplicationContext().getFilesDir(), "output.csv");
        if (cur.getCount() > 0) {
            final CustomProgressDialog writeProgressDialog = new CustomProgressDialog(
                    this, "Writing file...",
                    ProgressDialog.STYLE_HORIZONTAL, false, false,
                    0, cur.getCount());
            new AsyncTask<Void, Integer, Void>() {
                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                    writeProgressDialog.show();
                }

                @Override
                protected Void doInBackground(Void... params) {
                    try {
                        FileOutputStream outputStream = new FileOutputStream(file, false);
                        StringBuilder csvHeader = new StringBuilder("No,Timestamp,Label");
                        if (gyroSwitchPref)
                            csvHeader.append(",GyroX,GyroY,GyroZ");
                        if (accSwitchPref)
                            csvHeader.append(",AccX,AccY,AccZ");
                        if (magSwitchPref)
                            csvHeader.append(",MagX,MagY,MagZ");
                        if (orientationSwitchPref)
                            csvHeader.append(",Azimut,Pitch,Roll");
                        if (gravSwitchPref)
                            csvHeader.append(",GravX,GravY,GravZ");
                        if (linearAccSwitchPref)
                            csvHeader.append(",LinearAccX,LinearAccY,LinearAccZ");
                        if (proximitySwitchPref)
                            csvHeader.append(",Proximity");
                        csvHeader.append('\n');
                        outputStream.write(csvHeader.toString().getBytes());
                        do {
                            publishProgress(cur.getPosition());
                            int columnIndex = 0;
                            StringBuilder row = new StringBuilder();
                            String recordCount = cur.getString(columnIndex++);
                            String tStamp = cur.getString(columnIndex++);
                            String label = cur.getString(columnIndex++);
                            row.append(recordCount + ',' + tStamp + ',' + label + ',');
                            if (gyroSwitchPref) {
                                String gyroX = cur.getString(columnIndex++);
                                String gyroY = cur.getString(columnIndex++);
                                String gyroZ = cur.getString(columnIndex++);
                                row.append(gyroX + ',' + gyroY + ',' + gyroZ + ',');
                            }
                            if (accSwitchPref) {
                                String accX = cur.getString(columnIndex++);
                                String accY = cur.getString(columnIndex++);
                                String accZ = cur.getString(columnIndex++);
                                row.append(accX + ',' + accY + ',' + accZ + ',');
                            }
                            if (magSwitchPref) {
                                String magX = cur.getString(columnIndex++);
                                String magY = cur.getString(columnIndex++);
                                String magZ = cur.getString(columnIndex++);
                                row.append(magX + ',' + magY + ',' + magZ + ',');
                            }
                            if (orientationSwitchPref) {
                                String azimut = cur.getString(columnIndex++);
                                String pitch = cur.getString(columnIndex++);
                                String roll = cur.getString(columnIndex++);
                                row.append(azimut + ',' + pitch + ',' + roll + ',');
                            }
                            if (gravSwitchPref) {
                                String gravX = cur.getString(columnIndex++);
                                String gravY = cur.getString(columnIndex++);
                                String gravZ = cur.getString(columnIndex++);
                                row.append(gravX + ',' + gravY + ',' + gravZ + ',');
                            }
                            if (linearAccSwitchPref) {
                                String linearAccX = cur.getString(columnIndex++);
                                String linearAccY = cur.getString(columnIndex++);
                                String linearAccZ = cur.getString(columnIndex++);
                                row.append(linearAccX + ',' + linearAccY + ',' + linearAccZ + ',');
                            }
                            if (proximitySwitchPref) {
                                String proximity = cur.getString(columnIndex++);
                                row.append(proximity);
                            }
                            row.append('\n');
                            outputStream.write(row.toString().getBytes());
                        } while (cur.moveToNext());
                        outputStream.flush();
                        outputStream.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    super.onPostExecute(aVoid);
                    cur.close();
                    if (writeProgressDialog.isShowing())
                        writeProgressDialog.dismiss();
                    Toast.makeText(getApplicationContext(), "CSV created locally" + "\n" + file.getAbsolutePath(),
                            Toast.LENGTH_LONG).show();
                }

                @Override
                protected void onProgressUpdate(Integer... values) {
                    writeProgressDialog.setProgress(values[0]);
                }
            }.execute();

        }
    }

    public void labelOutput(View view) {
        if ((labelInfo.getText().toString().toLowerCase()).equals(""))
            Toast.makeText(this, "Please fill in something",
                    Toast.LENGTH_SHORT).show();
        else if (labels.contains(labelInfo.getText().toString().toLowerCase()))
            Toast.makeText(this, "Existing label",
                    Toast.LENGTH_SHORT).show();
        else {
            labels.add(labelInfo.getText().toString().toLowerCase());
            Toast.makeText(this, "New label added",
                    Toast.LENGTH_SHORT).show();
            labelListInfo.setText("List of labels: " + labels);
        }
        labelInfo.setText("");
    }

    public void getLocalCSV(View view) {
        File srcFile = new File(getApplicationContext().getFilesDir(), "output.csv");
        if (srcFile.exists()) {
            final CustomProgressDialog localProgressDialog = new CustomProgressDialog(
                    this, "Getting file locally...",
                    ProgressDialog.STYLE_SPINNER, true, false,
                    0, 1024);
            new AsyncTask<File, Integer, File>() {
                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                    localProgressDialog.show();
                }

                @Override
                protected File doInBackground(File... files) {
                    try {
                        File defaultDownloadDir = Environment.getExternalStoragePublicDirectory
                                (Environment.DIRECTORY_DOWNLOADS);
                        FileInputStream in = new FileInputStream(files[0]);
                        FileOutputStream out = new FileOutputStream(defaultDownloadDir + "/output.csv");
                        // Copy the bits from instream to outstream
                        byte[] buf = new byte[1024];
                        int len;
                        while ((len = in.read(buf)) != -1) {
                            out.write(buf, 0, len);
                            publishProgress(1);
                        }
                        in.close();
                        out.close();
                        return defaultDownloadDir;
                    } catch (FileNotFoundException ex) {
                        Log.e("FileNotFound", ex.getMessage());
                    } catch (IOException ex) {
                        Log.e("IO", ex.getMessage());
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(File defaultDownloadDir) {
                    super.onPostExecute(defaultDownloadDir);
                    if (localProgressDialog.isShowing())
                        localProgressDialog.dismiss();
                    if (defaultDownloadDir == null)
                        Toast.makeText(getApplicationContext(), "File/IO Exception", Toast.LENGTH_SHORT).show();
                    else
                        Toast.makeText(getApplicationContext(), "CSV file was downloaded locally to" +
                                "\n" + defaultDownloadDir, Toast.LENGTH_LONG).show();
                }

                @Override
                protected void onProgressUpdate(Integer... values) {
                    super.onProgressUpdate(values);
                }
            }.execute(srcFile);
        } else {
            Toast.makeText(this, "Source file not exists", Toast.LENGTH_LONG).show();
        }
        return;
    }

    public void exportToCloud(View view) {
        if (sensorDataDB == null) {
            Toast.makeText(this, "No database created, cannot upload csv", Toast.LENGTH_LONG).show();
            return;
        }
        responseToNoInternetAccess();
        File file = new File(getApplicationContext().getFilesDir(), "output.csv");
        if (file.exists()) {
            final CustomProgressDialog exportProgressDialog = new CustomProgressDialog(
                    this, "Uploading...",
                    ProgressDialog.STYLE_HORIZONTAL, false, false,
                    0, 100);
            exportProgressDialog.show();
            /*final ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("Uploading...");
            progressDialog.setIndeterminate(false);
            progressDialog.setCancelable(false);
            progressDialog.setProgress(0);
            progressDialog.setMax(100);
            progressDialog.show();*/
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
                    androidID + '/' + sdf + ".csv",
                    file);

            // Attach a listener to the observer to get state update and progress notifications
            uploadObserver.setTransferListener(new TransferListener() {
                @Override
                public void onStateChanged(int id, TransferState state) {
                    if (TransferState.COMPLETED == state) {
                        if (exportProgressDialog.isShowing())
                            exportProgressDialog.dismiss();
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
                    exportProgressDialog.setProgress(percentDone);
                }

                @Override
                public void onError(int id, Exception ex) {
                    // Handle errors
                    if (exportProgressDialog.isShowing())
                        exportProgressDialog.dismiss();
                    Toast.makeText(getApplicationContext(), "Please check internet connection", Toast.LENGTH_SHORT).show();
                }
            });

            Log.d("YourActivity", "Bytes Transferred: " + uploadObserver.getBytesTransferred());
            Log.d("YourActivity", "Bytes Total: " + uploadObserver.getBytesTotal());
        } else
            Toast.makeText(this, "File does not exist", Toast.LENGTH_SHORT).show();
    }
}
