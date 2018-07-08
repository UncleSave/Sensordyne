package unclesave.example.com.test2;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.Toast;

public class AboutMeFragment extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder aboutMeDialog = new AlertDialog.Builder(getActivity());
        aboutMeDialog.setTitle("About Me");
        aboutMeDialog.setMessage("This app is just a prototype");
        aboutMeDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(getActivity(), "Thanks for checking!", Toast.LENGTH_SHORT).show();
            }
        });
        return aboutMeDialog.create();
    }
}
