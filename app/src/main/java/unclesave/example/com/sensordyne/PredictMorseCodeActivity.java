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

// An activity that predicts morse code activity
public class PredictMorseCodeActivity extends AppCompatActivity implements SensorEventListener {

    private static final int N_SAMPLES = 300;
    private TextView tTextView;
    private TextView mTextView;
    private TextView nTextView;
    private TextView resultTextView;
    private SensorManager sensorManager;
    private Sensor linearAccelerometer;
    private static List<Float> linearAccX;
    private static List<Float> linearAccY;
    private static List<Float> linearAccZ;
    private float linearAcceleroVal[];
    private float results[];
    private TensorFlowClassifier classifier;
    private List<Float> data = new ArrayList<>();
    private Timer collectTimer;
    private Timer logTimer;
    private TimerTask collectTimerTask;
    private TimerTask logTimerTask;
    private Button startPredictButton;
    private Button stopPredictButton;
    private TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_predict_morse_code);
        tTextView = findViewById(R.id.t_prob);
        mTextView = findViewById(R.id.m_prob);
        nTextView = findViewById(R.id.n_prob);
        resultTextView = findViewById(R.id.morse_code_result_values);

        sensorManager = getSensorManager();
        linearAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        linearAccX = new ArrayList<>();
        linearAccY = new ArrayList<>();
        linearAccZ = new ArrayList<>();
        linearAcceleroVal = new float[3];
        classifier = new TensorFlowClassifier(getApplicationContext(),
                "file:///android_asset/morsecode.pb",
                "bidirectional_1_input",
                new String[]{"dense_2/Softmax"},
                "dense_2/Softmax",
                new long[]{1, 170, 3},
                3);
        startPredictButton = findViewById(R.id.start_predict_button);
        stopPredictButton = findViewById(R.id.stop_predict_button);
        stopPredictButton.setClickable(false);
        if (sensorManager == null || linearAccelerometer == null) {
            CustomDialogFragment requiredSensorDialog = CustomDialogFragment.newInstance(200);
            requiredSensorDialog.show(getFragmentManager(), "dialog");
            startPredictButton.setClickable(false);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Registers sensor listener
        sensorManager.registerListener(this, linearAccelerometer,
                0);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Deregisters sensor listener
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Registers sensor listener
        if (linearAccelerometer.isWakeUpSensor())
            sensorManager.registerListener(this, linearAccelerometer,
                    0);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Deregisters sensor listener
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        int sensorType = event.sensor.getType();
        // Gets the sensor data and stores in array
        switch (sensorType) {
            case Sensor.TYPE_LINEAR_ACCELERATION:
                System.arraycopy(event.values, 0, linearAcceleroVal, 0, event.values.length);
                break;
            default:
                break;
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

    // Starts predict (collect sensor data and put in inference classes)
    public void startPredict(View view) {
        startPredictButton.setClickable(false);
        stopPredictButton.setClickable(true);
        collectTimer = new Timer();
        collectTimerTask = new TimerTask() {
            @Override
            public void run() {
                if (linearAccX.size() == N_SAMPLES && linearAccY.size() == N_SAMPLES && linearAccZ.size() == N_SAMPLES) {
                    List<Float> tempX = new ArrayList<>();
                    List<Float> tempY = new ArrayList<>();
                    List<Float> tempZ = new ArrayList<>();
                    for (int i = 0; i < 170; i++) {
                        tempX.add(linearAccX.get(i));
                        tempY.add(linearAccY.get(i));
                        tempZ.add(linearAccZ.get(i));
                    }
                    data.addAll(tempX);
                    data.addAll(tempY);
                    data.addAll(tempZ);
                    tempX.clear();
                    tempY.clear();
                    tempZ.clear();
                    results = classifier.predictProbabilities(toFloatArray(data));
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tTextView.setText("t: " + Float.toString(round(results[0], 2)));
                            mTextView.setText("m: " + Float.toString(round(results[1], 2)));
                            nTextView.setText("n: " + Float.toString(round(results[2], 2)));
                            resultTextView.setText("Result: " + Arrays.toString(results));
                        }
                    });
                }
                data.clear();
                linearAccX.clear();
                linearAccY.clear();
                linearAccZ.clear();
            }
        };
        logTimer = new Timer();
        logTimerTask = new TimerTask() {
            @Override
            public void run() {
                if (linearAccX.size() < N_SAMPLES && linearAccY.size() < N_SAMPLES && linearAccZ.size() < N_SAMPLES) {
                    linearAccX.add(linearAcceleroVal[0]);
                    linearAccY.add(linearAcceleroVal[1]);
                    linearAccZ.add(linearAcceleroVal[2]);
                }
            }
        };
        collectTimer.scheduleAtFixedRate(collectTimerTask, 0, 3000);
        logTimer.scheduleAtFixedRate(logTimerTask, 0, 10);
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
