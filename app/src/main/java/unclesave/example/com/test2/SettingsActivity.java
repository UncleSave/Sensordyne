package unclesave.example.com.test2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {
    public static final String KEY_PREF_GYROSCOPE_SWITCH = "gyroscope_switch";
    public static final String KEY_PREF_ACCELEROMETER_SWITCH = "accelerometer_switch";
    public static final String KEY_PREF_MAGNETOMETER_SWITCH = "magnetometer_switch";
    public static final String KEY_PREF_ORIENTATION_SWITCH = "orientation_switch";
    public static final String KEY_PREF_GRAVITY_SWITCH = "gravity_switch";
    public static final String KEY_PREF_LINEAR_ACCELEROMETER_SWITCH = "linear_accelerometer_switch";
    public static final String KEY_PREF_TIME_LABEL_INTERVAL = "time_label_interval";
    public static final String KEY_PREF_TIME_LOG_INTERVAL = "time_logging_interval";
    public static final String KEY_PREF_SENSOR_SAMPLING_DELAY = "sensor_sampling_delay";
    public static final String KEY_PREF_COLLECT_MODE = "collect_mode";
    public static final String KEY_PREF_TIMER_MODE = "timer_mode";
    public static final String KEY_PREF_TEXT_TO_SPEECH = "texttospeech_switch";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Intent getMainIntent = new Intent(this, MainActivity.class);
        startActivity(getMainIntent);
    }
}
