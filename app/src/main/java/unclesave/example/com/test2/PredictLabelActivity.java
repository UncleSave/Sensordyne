package unclesave.example.com.test2;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class PredictLabelActivity extends AppCompatActivity implements SensorEventListener {

    private static final int N_SAMPLES = 15;
    private TextView leftTextView, rightTextView, onTableTextView, resultTextView;
    private SensorManager sensorManager;
    private Sensor gyroscope, accelerometer, magnetometer, gravmeter;
    private Boolean gyroSwitchPref, accSwitchPref, magSwitchPref, gravSwitchPref,
        orientationSwitchPref, textToSpeechSwitchPref;
    private static List<Float> gyroX, gyroY, gyroZ, accX, accY, accZ,
            magX, magY, magZ, azimuth, pitch, roll, gravX, gravY, gravZ;
    private float gyroscopeVal[] = new float[3];
    private float acceleroVal[] = new float[3];
    private float magnetoVal[] = new float[3];
    private float orientationVal[] = new float[3];
    private float gravVal[] = new float[3];
    private float r[] = new float[9];
    private float[] results;
    private TensorFlowClassifier classifier;
    private List<Float> data = new ArrayList<>();
    private TextToSpeech tts;
    private Timer collectTimer, logTimer;
    private TimerTask collectTimerTask, logTimerTask;
    private Button startPredictButton, stopPredictButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_predict_label);
        gyroX = new ArrayList<>();
        gyroY = new ArrayList<>();
        gyroZ = new ArrayList<>();
        accX = new ArrayList<>();
        accY = new ArrayList<>();
        accZ = new ArrayList<>();
        azimuth = new ArrayList<>();
        pitch = new ArrayList<>();
        roll = new ArrayList<>();
        magX = new ArrayList<>();
        magY = new ArrayList<>();
        magZ = new ArrayList<>();
        gravX = new ArrayList<>();
        gravY = new ArrayList<>();
        gravZ = new ArrayList<>();

        leftTextView = findViewById(R.id.left_prob);
        rightTextView = findViewById(R.id.right_prob);
        onTableTextView = findViewById(R.id.ontable_prob);
        resultTextView = findViewById(R.id.result_values);

        classifier = new TensorFlowClassifier(getApplicationContext());
        sensorManager = getSensorManager();
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        gravmeter = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
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
        textToSpeechSwitchPref = sharedPref.getBoolean
                (SettingsActivity.KEY_PREF_TEXT_TO_SPEECH, true);
        startPredictButton = findViewById(R.id.start_predict_button);
        stopPredictButton = findViewById(R.id.stop_predict_button);
        stopPredictButton.setClickable(false);
        if (!gyroSwitchPref || !accSwitchPref || !magSwitchPref || !orientationSwitchPref || !gravSwitchPref) {
            String message = "Please enable required sensors in the settings: ";
            if (!gyroSwitchPref)
                message += "gyroscope\n";
            if (!accSwitchPref)
                message += "accelerometer\n";
            if (!magSwitchPref)
                message += "magnetometer\n";
            if (!orientationSwitchPref)
                message += "orientation sensor\n";
            if (!gravSwitchPref)
                message += "gravity sensor\n";
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setMessage(message);

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

    @Override
    protected void onStart() {
        super.onStart();
        if (gyroSwitchPref) {
            sensorManager.registerListener(this, gyroscope,
                    SensorManager.SENSOR_DELAY_FASTEST);
        }
        if (accSwitchPref) {
            sensorManager.registerListener(this, accelerometer,
                    SensorManager.SENSOR_DELAY_FASTEST);
        }
        if (magSwitchPref) {
            sensorManager.registerListener(this, magnetometer,
                    SensorManager.SENSOR_DELAY_FASTEST);
        }
        if (gravSwitchPref) {
            sensorManager.registerListener(this, gravmeter,
                    SensorManager.SENSOR_DELAY_FASTEST);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (gyroSwitchPref) {
            sensorManager.unregisterListener(this, gyroscope);
        }
        if (accSwitchPref) {
            sensorManager.unregisterListener(this, accelerometer);
        }
        if (magSwitchPref) {
            sensorManager.unregisterListener(this, magnetometer);
        }
        if (gravSwitchPref) {
            sensorManager.unregisterListener(this, gravmeter);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gyroSwitchPref) {
            sensorManager.registerListener(this, gyroscope,
                    SensorManager.SENSOR_DELAY_FASTEST);
        }
        if (accSwitchPref) {
            sensorManager.registerListener(this, accelerometer,
                    SensorManager.SENSOR_DELAY_FASTEST);
        }
        if (magSwitchPref) {
            sensorManager.registerListener(this, magnetometer,
                    SensorManager.SENSOR_DELAY_FASTEST);
        }
        if (gravSwitchPref) {
            sensorManager.registerListener(this, gravmeter,
                    SensorManager.SENSOR_DELAY_FASTEST);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (gyroSwitchPref) {
            sensorManager.unregisterListener(this, gyroscope);
        }
        if (accSwitchPref) {
            sensorManager.unregisterListener(this, accelerometer);
        }
        if (magSwitchPref) {
            sensorManager.unregisterListener(this, magnetometer);
        }
        if (gravSwitchPref) {
            sensorManager.unregisterListener(this, gravmeter);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        int sensorType = event.sensor.getType();
        switch (sensorType) {
            case Sensor.TYPE_GYROSCOPE:
                System.arraycopy(event.values, 0, gyroscopeVal, 0, event.values.length);
                break;
            case Sensor.TYPE_ACCELEROMETER:
                System.arraycopy(event.values, 0, acceleroVal, 0, event.values.length);
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                System.arraycopy(event.values, 0, magnetoVal, 0, event.values.length);
                break;
            case Sensor.TYPE_GRAVITY:
                System.arraycopy(event.values, 0, gravVal, 0, event.values.length);
                break;
            default:
                break;
        }
        if (accSwitchPref && magSwitchPref && acceleroVal != null && magnetoVal != null) {
            SensorManager.getRotationMatrix(r, null, acceleroVal, magnetoVal);
            SensorManager.getOrientation(r, orientationVal);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

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

    public void startPredict(View view) {
        startPredictButton.setClickable(false);
        stopPredictButton.setClickable(true);
        collectTimer = new Timer();
        collectTimerTask = new TimerTask() {
            @Override
            public void run() {
                if (gyroX.size() == N_SAMPLES && gyroY.size() == N_SAMPLES && gyroZ.size() == N_SAMPLES &&
                        accX.size() == N_SAMPLES && accY.size() == N_SAMPLES && accZ.size() == N_SAMPLES &&
                        azimuth.size() == N_SAMPLES && pitch.size() == N_SAMPLES && roll.size() == N_SAMPLES &&
                        magX.size() == N_SAMPLES && magY.size() == N_SAMPLES && magZ.size() == N_SAMPLES &&
                        gravX.size() == N_SAMPLES && gravY.size() == N_SAMPLES && gravZ.size() == N_SAMPLES) {
                    data.addAll(gyroX);
                    data.addAll(gyroY);
                    data.addAll(gyroZ);
                    data.addAll(accX);
                    data.addAll(accY);
                    data.addAll(accZ);
                    data.addAll(azimuth);
                    data.addAll(pitch);
                    data.addAll(roll);
                    data.addAll(magX);
                    data.addAll(magY);
                    data.addAll(magZ);
                    data.addAll(gravX);
                    data.addAll(gravY);
                    data.addAll(gravZ);
                    results = classifier.predictProbabilities(toFloatArray(data));
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            leftTextView.setText("Left: " + Float.toString(round(results[0], 2)));
                            rightTextView.setText("Right: " + Float.toString(round(results[2], 2)));
                            onTableTextView.setText("On table: " + Float.toString(round(results[1], 2)));
                            resultTextView.setText("Result: " + Arrays.toString(results));
                        }
                    });
                }
                data.clear();
                gyroX.clear();
                gyroY.clear();
                gyroZ.clear();
                accX.clear();
                accY.clear();
                accZ.clear();
                azimuth.clear();
                pitch.clear();
                roll.clear();
                magX.clear();
                magY.clear();
                magZ.clear();
                gravX.clear();
                gravY.clear();
                gravZ.clear();
            }
        };
        logTimer = new Timer();
        logTimerTask = new TimerTask() {
            @Override
            public void run() {
                if (gyroX.size() < N_SAMPLES && gyroY.size() < N_SAMPLES && gyroZ.size() < N_SAMPLES &&
                        accX.size() < N_SAMPLES && accY.size() < N_SAMPLES && accZ.size() < N_SAMPLES &&
                        azimuth.size() < N_SAMPLES && pitch.size() < N_SAMPLES && roll.size() < N_SAMPLES &&
                        magX.size() < N_SAMPLES && magY.size() < N_SAMPLES && magZ.size() < N_SAMPLES &&
                        gravX.size() < N_SAMPLES && gravY.size() < N_SAMPLES && gravZ.size() < N_SAMPLES) {
                    gyroX.add(gyroscopeVal[0]);
                    gyroY.add(gyroscopeVal[1]);
                    gyroZ.add(gyroscopeVal[2]);
                    accX.add(acceleroVal[0]);
                    accY.add(acceleroVal[1]);
                    accZ.add(acceleroVal[2]);
                    azimuth.add(orientationVal[0]);
                    pitch.add(orientationVal[1]);
                    roll.add(orientationVal[2]);
                    magX.add(acceleroVal[0]);
                    magY.add(acceleroVal[1]);
                    magZ.add(acceleroVal[2]);
                    gravX.add(gravVal[0]);
                    gravY.add(gravVal[1]);
                    gravZ.add(gravVal[2]);
                }
            }
        };
        collectTimer.schedule(collectTimerTask, 0, 3000);
        logTimer.schedule(logTimerTask, 0, 200);
    }

    public void stopPredict(View view) {
        collectTimer.cancel();
        logTimer.cancel();
        startPredictButton.setClickable(true);
        stopPredictButton.setClickable(false);
    }

    private float[] toFloatArray(List<Float> list) {
        int i = 0;
        float[] array = new float[list.size()];
        for (Float f : list) {
            array[i++] = (f != null ? f : Float.NaN);
        }
        return array;
    }

    private static float round(float d, int decimalPlace) {
        BigDecimal bd = new BigDecimal(Float.toString(d));
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        return bd.floatValue();
    }

    private SensorManager getSensorManager() {
        return (SensorManager) getSystemService(SENSOR_SERVICE);
    }
}
