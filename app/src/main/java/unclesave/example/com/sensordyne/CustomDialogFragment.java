package unclesave.example.com.sensordyne;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

// Helper class which creates customized dialog fragment
public class CustomDialogFragment extends DialogFragment {

    private int dialogCode;
    private String message;
    private String positiveButtonText;
    private String negativeButtonText;
    private String title;
    private AlertDialog alertDialog;

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
            message = "You have denied some permissions previously, please enable it in setting." +
                    " After you are done, please restart the application.";
            positiveButtonText = "Open Settings";
            setCancelable(false);
        } else if (dialogCode == 101) {
            title = "Permission denied";
            message = "You need to give permission.";
            positiveButtonText = "Ok";
        } else if (dialogCode == 102) {
            title = "No internet access";
            message = "Please enable wifi or mobile data. Some features might not be working.";
            positiveButtonText = "Close";
        } else if (dialogCode == 103) {
            title = "Sensor manager missing";
            message = "Cannot start sensor manager.";
            positiveButtonText = "Close";
        } else if (dialogCode == 200) {
            title = "Sensor/Sensor manager missing";
            message = "Cannot start sensor manager or either one of the sensors is missing:" +
                    " gyroscope, accelerometer, magnetometer, gravmeter";
            positiveButtonText = "Close";
        } else if (dialogCode == 400) {
            title = "Missing bucket";
            message = "Cannot access Amazon S3 bucket";
            positiveButtonText = "Close";
        } else if (dialogCode == 401) {
            title = "Share with who?";
        } else;
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setTitle(this.title);
        if (message != null)
            alertDialogBuilder.setMessage(this.message);
        if (positiveButtonText != null) {
            alertDialogBuilder.setPositiveButton(positiveButtonText,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            if (dialogCode == 1) ;
                            else if (dialogCode == 100) {
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package", getActivity().getPackageName(), null);
                                intent.setData(uri);
                                startActivityForResult(intent, 100);
                                ((MainActivity) getActivity()).getPermissionAccessHandler().responseToNoPermission();
                            } else if (dialogCode == 101) {
                                ((MainActivity) getActivity()).getPermissionAccessHandler().responseToNoPermission();
                            }
                            else if (dialogCode == 102);
                            else if (dialogCode == 103)
                                getActivity().finish();
                            else if (dialogCode == 200)
                                getActivity().finish();
                            else if (dialogCode == 400)
                                getActivity().finish();
                            else;
                        }
                    });
        }
        if (negativeButtonText != null) {
            alertDialogBuilder.setNegativeButton(negativeButtonText,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                        }
                    });
        }
        // Create unique dialog fragment for sharing Android ID
        if (dialogCode == 401) {
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            final View popupInputDialogView = layoutInflater.inflate(R.layout.popup_input_dialog, null);
            alertDialogBuilder.setView(popupInputDialogView);
            Button saveShareButton = popupInputDialogView.findViewById(R.id.save_share_button);
            Button cancelShareButton = popupInputDialogView.findViewById(R.id.cancel_share_button);
            final EditText targetAndroidIDInput = popupInputDialogView.findViewById(R.id.target_id_input);
            saveShareButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String targetAndroidID = targetAndroidIDInput.getText().toString();
                    if (!targetAndroidID.equals("")) {
                        ((ManageFileActivity) getActivity()).shareFileFunc(targetAndroidID);
                        alertDialog.cancel();
                    } else {
                        Toast.makeText(getActivity(), "Empty file name", Toast.LENGTH_SHORT).show();
                        alertDialog.cancel();
                    }
                }
            });
            cancelShareButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    alertDialog.cancel();
                }
            });

        }
        alertDialog = alertDialogBuilder.create();
        alertDialog.setCanceledOnTouchOutside(false);
        return alertDialog;
    }

    // This is DialogFragment, not Dialog
    @Override
    public void onCancel(DialogInterface dialog) {
        if (dialogCode != 401)
            getActivity().finish();
    }
}
