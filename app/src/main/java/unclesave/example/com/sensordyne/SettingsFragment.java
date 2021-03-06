package unclesave.example.com.sensordyne;


import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.SwitchPreferenceCompat;

// A settings fragment which setup all settings
public class SettingsFragment extends PreferenceFragmentCompat {

    private SwitchPreferenceCompat gyroscopeSetting;
    private SwitchPreferenceCompat accelerometerSetting;
    private SwitchPreferenceCompat magnetometerSetting;
    private SwitchPreferenceCompat orientationSetting;
    private SwitchPreferenceCompat gravitySetting;
    private SwitchPreferenceCompat linearaccelerometerSetting;
    private SwitchPreferenceCompat proximitySetting;
    private SwitchPreferenceCompat logTimerSetting;
    private CheckBoxPreference lowPassFilterAccSetting;
    private boolean linearAccUnsupported;
    private IntEditTextPreference timeLabelIntervalSetting;
    private IntEditTextPreference timeLoggingIntervalSetting;
    private IntEditTextPreference sensorSamplingDelaySetting;
    private ListPreference collectModeSetting;
    private ListPreference timerModeSetting;
    private SwitchPreferenceCompat textToSpeechSetting;
    private SensorManager sensorManager;
    private Sensor gyroscope;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private Sensor gravmeter;
    private Sensor linearaccelerometer;
    private Sensor proximity;
    private Preference.OnPreferenceChangeListener prefChangeListener;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        gyroscopeSetting = (SwitchPreferenceCompat) findPreference(SettingsActivity.KEY_PREF_GYROSCOPE_SWITCH);
        accelerometerSetting = (SwitchPreferenceCompat) findPreference(SettingsActivity.KEY_PREF_ACCELEROMETER_SWITCH);
        magnetometerSetting = (SwitchPreferenceCompat) findPreference(SettingsActivity.KEY_PREF_MAGNETOMETER_SWITCH);
        orientationSetting = (SwitchPreferenceCompat) findPreference(SettingsActivity.KEY_PREF_ORIENTATION_SWITCH);
        gravitySetting = (SwitchPreferenceCompat) findPreference(SettingsActivity.KEY_PREF_GRAVITY_SWITCH);
        linearaccelerometerSetting = (SwitchPreferenceCompat) findPreference(SettingsActivity.KEY_PREF_LINEAR_ACCELEROMETER_SWITCH);
        lowPassFilterAccSetting = (CheckBoxPreference) findPreference(SettingsActivity.KEY_PREF_LPF_LINEAR_ACC_SWITCH);
        proximitySetting = (SwitchPreferenceCompat) findPreference(SettingsActivity.KEY_PREF_PROXIMITY_SWITCH);
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
            magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            if (magnetometer == null) {
                magnetometerSetting.setEnabled(false);
                magnetometerSetting.setChecked(false);
            }
            if (accelerometerSetting.isChecked() && magnetometerSetting.isChecked())
                orientationSetting.setEnabled(true);
            else {
                orientationSetting.setEnabled(false);
                orientationSetting.setChecked(false);
            }
            gravmeter = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            if (gravmeter == null) {
                gravitySetting.setEnabled(false);
                gravitySetting.setChecked(false);
            }
            linearaccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
            linearAccUnsupported = (linearaccelerometer == null);
            if (linearAccUnsupported) {
                if (accelerometerSetting.isChecked() && gravitySetting.isChecked())
                    linearaccelerometerSetting.setEnabled(true);
                else {
                    linearaccelerometerSetting.setEnabled(false);
                    linearaccelerometerSetting.setChecked(false);
                }
            }
            if (linearaccelerometerSetting.isChecked())
                lowPassFilterAccSetting.setEnabled(true);
            else {
                lowPassFilterAccSetting.setEnabled(false);
                lowPassFilterAccSetting.setChecked(false);
            }
            proximity = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
            if (proximity == null) {
                proximitySetting.setEnabled(false);
                proximitySetting.setChecked(false);
            }
        }
        catch (NullPointerException e) {
            e.printStackTrace();
        }
        timeLabelIntervalSetting = (IntEditTextPreference) findPreference(SettingsActivity.KEY_PREF_TIME_LABEL_INTERVAL);
        timeLoggingIntervalSetting = (IntEditTextPreference) findPreference(SettingsActivity.KEY_PREF_TIME_LOG_INTERVAL);
        sensorSamplingDelaySetting = (IntEditTextPreference) findPreference(SettingsActivity.KEY_PREF_SENSOR_SAMPLING_DELAY);
        collectModeSetting = (ListPreference) findPreference(SettingsActivity.KEY_PREF_COLLECT_MODE);
        logTimerSetting = (SwitchPreferenceCompat) findPreference(SettingsActivity.KEY_PREF_LOG_TIMER_SWITCH);
        timerModeSetting = (ListPreference) findPreference(SettingsActivity.KEY_PREF_TIMER_MODE);
        textToSpeechSetting = (SwitchPreferenceCompat) findPreference(SettingsActivity.KEY_PREF_TEXT_TO_SPEECH);
        prefChangeListener = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (newValue.toString().equals("true")) {
                    if (preference == accelerometerSetting) {
                        if (magnetometerSetting.isChecked())
                            orientationSetting.setEnabled(true);
                        if (gravitySetting.isChecked() && linearAccUnsupported)
                            linearaccelerometerSetting.setEnabled(true);
                    } else if (preference == magnetometerSetting) {
                        if (accelerometerSetting.isChecked())
                            orientationSetting.setEnabled(true);
                    } else if (preference == gravitySetting) {
                        if (accelerometerSetting.isChecked())
                            linearaccelerometerSetting.setEnabled(true);
                    } else if (preference == linearaccelerometerSetting)
                        lowPassFilterAccSetting.setEnabled(true);
                } else {
                    if (preference == accelerometerSetting) {
                        orientationSetting.setEnabled(false);
                        orientationSetting.setChecked(false);
                        if (linearAccUnsupported) {
                            linearaccelerometerSetting.setEnabled(false);
                            linearaccelerometerSetting.setChecked(false);
                        }
                    } else if (preference == magnetometerSetting) {
                        orientationSetting.setEnabled(false);
                        orientationSetting.setChecked(false);
                    } else if (preference == gravitySetting) {
                        linearaccelerometerSetting.setEnabled(false);
                        linearaccelerometerSetting.setChecked(false);
                    } else if (preference == linearaccelerometerSetting) {
                        lowPassFilterAccSetting.setEnabled(false);
                        lowPassFilterAccSetting.setChecked(false);
                    }
                }
                return true;
            }
        };
        accelerometerSetting.setOnPreferenceChangeListener(prefChangeListener);
        magnetometerSetting.setOnPreferenceChangeListener(prefChangeListener);
        if (linearAccUnsupported)
            gravitySetting.setOnPreferenceChangeListener(prefChangeListener);
        linearaccelerometerSetting.setOnPreferenceChangeListener(prefChangeListener);
        timeLoggingIntervalSetting.setOnPreferenceChangeListener(prefChangeListener);
        timeLabelIntervalSetting.setOnPreferenceChangeListener(prefChangeListener);
        sensorSamplingDelaySetting.setOnPreferenceChangeListener(prefChangeListener);
    }
}
