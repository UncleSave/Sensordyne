package unclesave.example.com.test2;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {
    public static final String KEY_PREF_GYROSCOPE_SWITCH = "gyroscope_switch";
    public static final String KEY_PREF_ACCELEROMETER_SWITCH = "accelerometer_switch";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }
}
