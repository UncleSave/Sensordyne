package unclesave.example.com.test2;


import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.SwitchPreferenceCompat;
import android.widget.Toast;

/**
 * A simple {@link Fragment} subclass.
 */
public class SettingsFragment extends PreferenceFragmentCompat {

    private SwitchPreferenceCompat gyroscopeSetting;
    private SwitchPreferenceCompat accelerometerSetting;
    private SensorManager sensorManager;
    private Sensor gyroscope;
    private Sensor accelerometer;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
        gyroscopeSetting = (SwitchPreferenceCompat) findPreference(SettingsActivity.KEY_PREF_GYROSCOPE_SWITCH);
        accelerometerSetting = (SwitchPreferenceCompat) findPreference(SettingsActivity.KEY_PREF_ACCELEROMETER_SWITCH);
        try {
            sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
            gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            if (gyroscope == null) {
                gyroscopeSetting.setEnabled(false);
                gyroscopeSetting.setChecked(false);
            }
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (accelerometer == null) {
                accelerometerSetting.setEnabled(false);
                accelerometerSetting.setChecked(false);
            }
        }
        catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

}
