package unclesave.example.com.sensordyne;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
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
import java.util.Timer;
import java.util.TimerTask;

// An activity that predicts hand gesture
public class PredictGestureActivity extends AppCompatActivity implements SensorEventListener {

    private static final int N_SAMPLES = 15;
    private TextView leftTextView;
    private TextView rightTextView;
    private TextView onTableTextView;
    private TextView resultTextView;
    private SensorManager sensorManager;
    private Sensor gyroscope;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private Sensor gravmeter;
    private static List<Float> gyroX;
    private static List<Float> gyroY;
    private static List<Float> gyroZ;
    private static List<Float> accX;
    private static List<Float> accY;
    private static List<Float> accZ;
    private static List<Float> magX;
    private static List<Float> magY;
    private static List<Float> magZ;
    private static List<Float> azimuth;
    private static List<Float> pitch;
    private static List<Float> roll;
    private static List<Float> gravX;
    private static List<Float> gravY;
    private static List<Float> gravZ;
    private float gyroscopeVal[];
    private float acceleroVal[];
    private float magnetoVal[];
    private float orientationVal[];
    private float gravVal[];
    private float r[];
    private float results[];
    private TensorFlowClassifier classifier;
    private List<Float> data = new ArrayList<>();
    private Timer collectTimer;
    private Timer logTimer;
    private TimerTask collectTimerTask;
    private TimerTask logTimerTask;
    private Button startPredictButton;
    private Button stopPredictButton;
    private boolean acceleroEmpty = true;
    private boolean magnetoEmpty = true;
    private TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_predict_gesture);
        leftTextView = findViewById(R.id.left_prob);
        rightTextView = findViewById(R.id.right_prob);
        onTableTextView = findViewById(R.id.ontable_prob);
        resultTextView = findViewById(R.id.gesture_result_values);

        sensorManager = getSensorManager();
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        gravmeter = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
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
        gyroscopeVal = new float[3];
        acceleroVal = new float[3];
        magnetoVal = new float[3];
        gravVal = new float[3];
        orientationVal = new float[3];
        r = new float[9];
        classifier = new TensorFlowClassifier(getApplicationContext(),
                "file:///android_asset/handgesture.pb",
                "lstm_1_input",
                new String[]{"dense_2/Softmax"},
                "dense_2/Softmax",
                new long[]{1, 15, 15},
                3);
        startPredictButton = findViewById(R.id.start_predict_button);
        stopPredictButton = findViewById(R.id.stop_predict_button);
        stopPredictButton.setClickable(false);
        if (sensorManager == null || gyroscope == null || accelerometer == null
                || magnetometer == null || gravmeter == null) {
            CustomDialogFragment requiredSensorDialog = CustomDialogFragment.newInstance(200);
            requiredSensorDialog.show(getFragmentManager(), "dialog");
            startPredictButton.setClickable(false);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Registers sensors' listener
        sensorManager.registerListener(this, gyroscope,
                SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, accelerometer,
                SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, magnetometer,
                SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, gravmeter,
                SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Deregisters sensors' listener
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Registers sensors' listener
        sensorManager.registerListener(this, gyroscope,
                SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, accelerometer,
                SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, magnetometer,
                SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, gravmeter,
                SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Deregisters sensors' listener
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // Stores sensor data in array respectively
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
        if (acceleroEmpty || magnetoEmpty) {
            acceleroEmpty = (acceleroVal[0] == 0.0f && acceleroVal[1] == 0.0f && acceleroVal[2] == 0.0f);
            magnetoEmpty = (magnetoVal[0] == 0.0f && magnetoVal[1] == 0.0f && magnetoVal[2] == 0.0f);
        }
        if (acceleroVal != null && magnetoVal != null) {
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

    // Finds the highest probabilities between few classes
    private String findHighestProb(float[] results) {
        int maxIndex = 0;
        float max = 0.0f;
        String maxClass = "";
        for (int i = 0; i < results.length; i++) {
            if (results[i] >= max) {
                max = results[i];
                maxIndex = i;
            }
        }
        switch (maxIndex) {
            case 0: maxClass = "left";
                break;
            case 1: maxClass = "on table";
                break;
            case 2: maxClass = "right";
                break;
            default: break;
        }
        return maxClass;
    }

    // Starts predict (collect sensor data and put in inference classes)
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
                            speak(findHighestProb(results));
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

    // Stops predict
    public void stopPredict(View view) {
        collectTimer.cancel();
        logTimer.cancel();
        startPredictButton.setClickable(true);
        stopPredictButton.setClickable(false);
    }

    // Accepts a List<Float> and converts to an array of float
    private float[] toFloatArray(List<Float> list) {
        int i = 0;
        float[] array = new float[list.size()];
        for (Float f : list) {
            array[i++] = (f != null ? f : Float.NaN);
        }
        return array;
    }

    // Rounds off the value to specified decimal place
    private static float round(float d, int decimalPlace) {
        BigDecimal bd = new BigDecimal(Float.toString(d));
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        return bd.floatValue();
    }

    private SensorManager getSensorManager() {
        return (SensorManager) getSystemService(SENSOR_SERVICE);
    }
}
