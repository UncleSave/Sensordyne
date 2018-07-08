package unclesave.example.com.test2;

import android.os.Build;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class DeviceInfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_info);

        TextView deviceInfo = findViewById(R.id.deviceinfo);
        String deviceName = Build.DEVICE;
        deviceInfo.setText("Device Name: " + deviceName);
        String deviceBuild = Build.BRAND;
        deviceInfo.append("\nDevice Brand: " + deviceBuild);
        String deviceRelease = Build.VERSION.RELEASE;
        deviceInfo.append("\nDevice Version: " + deviceRelease);
        /*switch (Build.VERSION.RELEASE) {
            case "6.0":
            case "6.0.1":
            case "7.0":
            case "7.1":
            case "7.1.1":
            case "7.1.2":
            case "8.0":
            case "8.1":
                break;
            default:
                WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                WifiInfo wInfo = wifiManager.getConnectionInfo();
                String deviceMac = wInfo.getMacAddress();
                deviceInfo.append("\nDevice Mac Address: " + deviceMac);
                break;
        }*/
        String androidID = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ANDROID_ID);
        deviceInfo.append("\nDevice Android ID: " + androidID);
    }
}
