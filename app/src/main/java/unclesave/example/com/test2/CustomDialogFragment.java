package unclesave.example.com.test2;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;

public class CustomDialogFragment extends DialogFragment {

    private int dialogCode;
    private String message;
    private String positiveButtonText;
    private String title;

    static CustomDialogFragment newInstance(int dialogCode){
        CustomDialogFragment frag = new CustomDialogFragment();
        Bundle args = new Bundle();
        args.putInt("dialogCode", dialogCode);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dialogCode = getArguments().getInt("dialogCode");
        if (dialogCode == 1) {
            title = "About me";
            message = "This is just a prototype";
            positiveButtonText = "Close";
        } else if (dialogCode == 100) {
            title = "Permission denied again";
            message = "You have denied some permissions previously, please enable it in setting.";
            positiveButtonText = "Open Settings";
        } else if (dialogCode == 101) {
            title = "Permission denied";
            message = "You need to give permission.";
            positiveButtonText = "Ok";
        } else if (dialogCode == 102) {
            title = "No internet access";
            message = "Please enable wifi or mobile data.";
            positiveButtonText = "Close";
        } else if (dialogCode == 103) {
            title = "Sensor manager missing";
            message = "Cannot start sensor manager.";
            positiveButtonText = "Close";
        } else;
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        final MainActivity mainActivity = ((MainActivity)getActivity());
        alertDialogBuilder.setTitle(this.title);
        alertDialogBuilder.setMessage(this.message);
        alertDialogBuilder.setPositiveButton(positiveButtonText,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        if (dialogCode == 1);
                        else if (dialogCode == 100) {
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package", getActivity().getPackageName(), null);
                            intent.setData(uri);
                            startActivityForResult(intent, 100);
                        }
                        else if (dialogCode == 101)
                            mainActivity.responseToNoPermission();
                        else if (dialogCode == 102)
                            mainActivity.finish();
                        else if (dialogCode == 103)
                            mainActivity.finish();
                        else;
                    }
        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.setCanceledOnTouchOutside(false);
        return alertDialog;
    }

    // This is DialogFragment, not Dialog
    @Override
    public void onCancel(DialogInterface dialog) {
        getActivity().finish();
    }
}
