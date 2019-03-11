package unclesave.example.com.test2;

import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

public class DeviceInfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_info);

        TextView deviceInfo = findViewById(R.id.device_info);
        String deviceName = Build.DEVICE;
        deviceInfo.setText("Device Name: " + deviceName);
        String deviceBuild = Build.BRAND;
        deviceInfo.append("\nDevice Brand: " + deviceBuild);
        String deviceRelease = Build.VERSION.RELEASE;
        deviceInfo.append("\nDevice Version: " + deviceRelease);
        String androidID = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ANDROID_ID);
        deviceInfo.append("\nDevice Android ID: " + androidID);
    }
}
