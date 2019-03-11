package unclesave.example.com.test2;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

public class CheckSensorActivity extends AppCompatActivity {

    ArrayList<String> sensorNames = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_sensor);
        // initiate a ListView
        ListView listView = findViewById(R.id.listView);

        SensorManager sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        List<Sensor> sensorList = sensorManager.getSensorList(Sensor.TYPE_ALL);
        StringBuilder sensorText = new StringBuilder();
        for (Sensor currentSensor: sensorList)
            sensorNames.add(currentSensor.getName());
        // set the adapter to fill the data in ListView
        CheckSensorAdapter customAdapter = new CheckSensorAdapter(getApplicationContext(), sensorNames);
        listView.setAdapter(customAdapter);
    }


}
