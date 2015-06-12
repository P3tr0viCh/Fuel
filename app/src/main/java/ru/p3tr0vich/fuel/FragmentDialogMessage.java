package ru.p3tr0vich.fuel;


import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;

public class FragmentDialogMessage extends DialogFragment {

    private static final String DIALOG_TAG = "DialogMessage";
    private static final String TITLE = "title";
    private static final String MESSAGE = "message";

    private static void showMessage(FragmentManager fragmentManager, String title, String message) {
        Bundle args = new Bundle();
        args.putString(FragmentDialogMessage.TITLE, title);
        args.putString(FragmentDialogMessage.MESSAGE, message);

        FragmentDialogMessage dialogMessage = new FragmentDialogMessage();
        dialogMessage.setArguments(args);
        dialogMessage.show(fragmentManager, DIALOG_TAG);
    }

    public static void showMessage(Fragment parent, String title, String message) {
        showMessage(parent.getFragmentManager(), title, message);
    }

    public static void showMessage(Activity parent, String title, String message) {
        showMessage(parent.getFragmentManager(), title, message);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setTitle(getArguments().getString(TITLE))
                .setMessage(getArguments().getString(MESSAGE))

                .setPositiveButton(R.string.dialog_btn_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dismiss();
                    }
                })

                .setCancelable(true)
                .create();
    }
}
