package unclesave.example.com.sensordyne;

import android.app.ProgressDialog;
import android.content.Context;

// Helper class which creates customized progress dialog
public class CustomProgressDialog extends ProgressDialog {
    private String message;
    private int progressStyle;
    private boolean indeterminate;
    private boolean cancelable;
    private int progress;
    private int maxProgress;

    public CustomProgressDialog(Context context, String message, int progressStyle,
                                boolean indeterminate, boolean cancelable,
                                int progress, int maxProgress) {
        super(context);
        this.message = message;
        this.progressStyle = progressStyle;
        this.indeterminate = indeterminate;
        this.cancelable = cancelable;
        this.progress = progress;
        this.maxProgress = maxProgress;
        setupProgressDialog();
    }

    private void setupProgressDialog() {
        this.setMessage(message);
        this.setProgressStyle(progressStyle);
        this.setIndeterminate(indeterminate);
        this.setCancelable(cancelable);
        this.setProgress(progress);
        this.setMax(maxProgress);
    }

}
