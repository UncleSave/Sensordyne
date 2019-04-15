package unclesave.example.com.sensordyne;

import android.content.Context;
import android.support.v7.preference.EditTextPreference;
import android.util.AttributeSet;
import android.widget.Toast;

/* A helper class which slightly modifies the original EditTextPreference class
to allow the integer input */
public class IntEditTextPreference extends EditTextPreference {

    public IntEditTextPreference(Context context) {
        super(context);
    }

    public IntEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public IntEditTextPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected String getPersistedString(String defaultReturnValue) {
        return String.valueOf(getPersistedInt(-1));
    }

    @Override
    protected boolean persistString(String value) {
        // The data validation has to be done here, since the exception occurs in Integer.valueOf(value)
        int val;
        try {
            val = Integer.valueOf(value);
            if (this.getKey().equals("time_label_interval") && val < 2000) {
                Toast.makeText(this.getContext(), "Invalid value, fallback to default value", Toast.LENGTH_LONG).show();
                val = 4000;
                this.setText("4000");
            } else if (this.getKey().equals("time_logging_interval") && val < 10) {
                Toast.makeText(this.getContext(), "Invalid value, fallback to default value", Toast.LENGTH_LONG).show();
                val = 20;
                this.setText("20");
            } else if (this.getKey().equals("sensor_sampling_delay") && val < 0) {
                Toast.makeText(this.getContext(), "Invalid value, fallback to default value", Toast.LENGTH_LONG).show();
                val = 5000;
                this.setText("5000");
            }
        } catch (NumberFormatException ex) {
            if (value.equals(""))
                Toast.makeText(this.getContext(), "Empty input, fallback to default value", Toast.LENGTH_LONG).show();
            else
                Toast.makeText(this.getContext(), "Mismatch data types, fallback to default value", Toast.LENGTH_LONG).show();
            if (this.getKey().equals("time_label_interval")) {
                val = 4000;
                this.setText("4000");
            } else if (this.getKey().equals("time_logging_interval")) {
                val = 20;
                this.setText("20");
            } else {
                val = 5000;
                this.setText("5000");
            }
        }
        return persistInt(val);
    }
}