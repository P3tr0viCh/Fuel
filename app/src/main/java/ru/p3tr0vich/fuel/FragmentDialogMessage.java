package ru.p3tr0vich.fuel;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;

public class FragmentDialogMessage extends DialogFragment {

    private static final String TAG = "DialogMessage";
    private static final String TITLE = "title";
    private static final String MESSAGE = "message";

    private static void showMessage(FragmentManager fragmentManager, String title, String message) {
        Bundle args = new Bundle();
        args.putString(FragmentDialogMessage.TITLE, title);
        args.putString(FragmentDialogMessage.MESSAGE, message);

        FragmentDialogMessage dialogMessage = new FragmentDialogMessage();
        dialogMessage.setArguments(args);
        dialogMessage.show(fragmentManager, TAG);
    }

    public static void showMessage(Fragment parent, String title, String message) {
        showMessage(parent.getFragmentManager(), title, message);
    }

    public static void showMessage(FragmentActivity parent, String title, String message) {
        showMessage(parent.getSupportFragmentManager(), title, message);
    }

    @NonNull
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
