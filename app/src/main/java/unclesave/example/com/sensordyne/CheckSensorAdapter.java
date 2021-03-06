package unclesave.example.com.sensordyne;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;

import java.util.ArrayList;

// Helper class which creates the list of sensors in CheckSensorActivity
public class CheckSensorAdapter extends BaseAdapter {
    private ArrayList<String> names;
    private Context context;
    private LayoutInflater layoutInflater;
    private String value;

    public CheckSensorAdapter(Context context, ArrayList<String> names) {
        this.context = context;
        this.names = names;
        layoutInflater = (LayoutInflater.from(context));
    }

    @Override
    public int getCount() {
        return names.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        view = layoutInflater.inflate(R.layout.sensor_list_view, null);
        final CheckedTextView simpleCheckedTextView = view.findViewById(R.id.sensorCheckedTextView);
        simpleCheckedTextView.setText(names.get(position));
        return view;
    }
}