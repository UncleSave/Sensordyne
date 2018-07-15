package unclesave.example.com.test2;

import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, SensorEventListener {

    private SensorManager sensorManager;
    private Sensor proximity;
    private Sensor accelerometer;
    private Sensor gyroscope;
    private Sensor gravity;
    private Sensor linearAcceleration;
    private TextView proximityInfo;
    private TextView accelerometerInfo;
    private TextView gyroscopeInfo;
    private TextView gravityInfo;
    /*private double proximityVal;
    private double accelerometerVal[3];
    private double gyroscopeVal[3];
    private double gravityVal[3];
    private double linearAccelerationVal[3];*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        proximity = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        gravity = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        linearAcceleration = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        proximityInfo = findViewById(R.id.proximity_info);
        if (proximity == null)
            proximityInfo.setText("Proximity sensor cannot be found");
        accelerometerInfo = findViewById(R.id.accelerometer_info);
        if (accelerometer == null)
            accelerometerInfo.setText("Accelerometer cannot be found");
        gyroscopeInfo = findViewById(R.id.gyroscope_info);
        if (gyroscope == null)
            gyroscopeInfo.setText("Gyroscope sensor cannot be found");
        gravityInfo = findViewById(R.id.gravity_info);
        if (gravity == null)
            gravityInfo.setText("Gravity sensor cannot be found");
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (proximity != null) {
            sensorManager.registerListener(this, proximity,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (gyroscope != null) {
            sensorManager.registerListener(this, gyroscope,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (gravity != null) {
            sensorManager.registerListener(this, gravity,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (linearAcceleration != null) {
            sensorManager.registerListener(this, linearAcceleration,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id) {
            case R.id.action_settings: Intent getSettingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(getSettingsIntent);
                return true;
            case R.id.action_about: DialogFragment aboutMeFragment = new AboutMeFragment();
                aboutMeFragment.show(getFragmentManager(), "aboutMeDialog");
                return true;
            case R.id.action_exit: finish();
                return true;
            default: break;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_available_sensor) {
            Intent getAvailableSensorIntent = new Intent(this, CheckSensorActivity.class);
            startActivity(getAvailableSensorIntent);
            return true;
        } else if (id == R.id.nav_device_info) {
            Intent getDeviceInfoIntent = new Intent(this, DeviceInfoActivity.class);
            startActivity(getDeviceInfoIntent);
            return true;
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        int sensorType = event.sensor.getType();
        switch (sensorType) {
            case Sensor.TYPE_PROXIMITY:
                //proximityVal = event.values[0];
                proximityInfo.setText(getResources().getString(
                        R.string.proximity_text, event.values[0]));
                break;
            case Sensor.TYPE_ACCELEROMETER:
                /*
                // Isolate the force of gravity with the low-pass filter.
                gravityVal[0] = 0.8 * gravityVal[0] + (1 - 0.8) * event.values[0];
                gravityVal[1] = 0.8 * gravityVal[1] + (1 - 0.8) * event.values[1];
                gravityVal[2] = 0.8 * gravityVal[2] + (1 - 0.8) * event.values[2];
                accelerometerVal[0] = event.values[0];
                accelerometerVal[1] = event.values[1];
                accelerometerVal[2] = event.values[2];
                linearAccelerationVal[0] = event.values[0] - gravityVal[0];
                linearAccelerationVal[1] = event.values[1] - gravityVal[1];
                linearAccelerationVal[2] = event.values[2] - gravityVal[2]; */
                accelerometerInfo.setText(getResources().getString(
                        R.string.accelerometer_text, event.values[0],
                        event.values[1], event.values[2]));
                break;
            case Sensor.TYPE_GYROSCOPE:
                /*gyroscopeVal[0] = event.values[0];
                gyroscopeVal[1] = event.values[1];
                gyroscopeVal[2] = event.values[2];*/
                gyroscopeInfo.setText(getResources().getString(
                        R.string.gyroscope_text, event.values[0],
                        event.values[1], event.values[2]));
                break;
            case Sensor.TYPE_GRAVITY:
                /*gravityVal[0] = event.values[0];
                gravityVal[1] = event.values[1];
                gravityVal[2] = event.values[2];*/
                gravityInfo.setText(getResources().getString(
                        R.string.gravity_text, event.values[0],
                        event.values[1], event.values[2]));
                break;
            /*case Sensor.TYPE_LINEAR_ACCELERATION:
                linearAccelerationVal[0] = event.values[0];
                linearAccelerationVal[1] = event.values[1];
                linearAccelerationVal[2] = event.values[2];
                break;*/
            default:
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
