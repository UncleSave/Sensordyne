package unclesave.example.com.sensordyne;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.util.ArrayList;

// A helper class that checks for permission and internet access
public class PermissionAccessHandler {
    private Activity activity;
    public static final int PERMISSION_ALL = 1;
    private CustomDialogFragment permissionDialog;
    private CustomDialogFragment permissionSettingsDialog;
    private CustomDialogFragment internetAccessDialog;

    public PermissionAccessHandler(Activity activity) {
        this.activity = activity;
        this.permissionDialog = null;
        this.permissionSettingsDialog = null;
        this.internetAccessDialog = null;
    }

    public CustomDialogFragment getPermissionDialog() {
        return permissionDialog;
    }

    public void setPermissionDialog(CustomDialogFragment permissionDialog) {
        this.permissionDialog = permissionDialog;
    }

    public CustomDialogFragment getPermissionSettingsDialog() {
        return permissionSettingsDialog;
    }

    public void setPermissionSettingsDialog(CustomDialogFragment permissionSettingsDialog) {
        this.permissionSettingsDialog = permissionSettingsDialog;
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
            if (ContextCompat.checkSelfPermission(this.activity, Manifest.permission.INTERNET)
                    != PackageManager.PERMISSION_GRANTED)
                permissionsNeeded.add(Manifest.permission.INTERNET);
            if (ContextCompat.checkSelfPermission(this.activity, Manifest.permission.ACCESS_WIFI_STATE)
                    != PackageManager.PERMISSION_GRANTED)
                permissionsNeeded.add(Manifest.permission.ACCESS_WIFI_STATE);
            if (ContextCompat.checkSelfPermission(this.activity, Manifest.permission.ACCESS_NETWORK_STATE)
                    != PackageManager.PERMISSION_GRANTED)
                permissionsNeeded.add(Manifest.permission.ACCESS_NETWORK_STATE);
            if (ContextCompat.checkSelfPermission(this.activity, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this.activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(this.activity, Manifest.permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED)
                    permissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
                else
                    permissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
            if (ContextCompat.checkSelfPermission(this.activity, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED)
                permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
            if (permissionsNeeded.size() != 0)
                ActivityCompat.requestPermissions(this.activity,
                        convertArrayListStringToStringOfArray(permissionsNeeded),
                        PERMISSION_ALL);
        }
    }

    // Checks if internet access is available
    public boolean isInternetAccessAvailable() {
        // Checks whether wifi or mobile data is connected
        ConnectivityManager cm =
                (ConnectivityManager) this.activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork;
        if (cm != null)
            activeNetwork = cm.getActiveNetworkInfo();
        else {
            Log.e("Network error", "Cannot get network info");
            return false;
        }
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    public void responseToNoInternetAccess() {
        internetAccessDialog = CustomDialogFragment.newInstance(102);
        internetAccessDialog.show(this.activity.getFragmentManager(), null);
        internetAccessDialog = null;
    }
}
