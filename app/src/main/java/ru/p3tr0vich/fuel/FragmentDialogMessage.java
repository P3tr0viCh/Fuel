package ru.p3tr0vich.fuel;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;

public class FragmentDialogMessage extends DialogFragment {

    private static final String TAG = "DialogMessage";

    private static final String TITLE = "title";
    private static final String MESSAGE = "message";

    private static FragmentDialogMessage getInstance(String title, String message) {
        Bundle args = new Bundle();
        args.putString(TITLE, title);
        args.putString(MESSAGE, message);

        FragmentDialogMessage dialogMessage = new FragmentDialogMessage();
        dialogMessage.setArguments(args);

        return dialogMessage;
    }

    public static void show(FragmentActivity parent, String title, String message) {
        getInstance(title, message).show(parent.getSupportFragmentManager(), TAG);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle arguments = getArguments();
        return new AlertDialog.Builder(getActivity())
                .setTitle(arguments.getString(TITLE))
                .setMessage(arguments.getString(MESSAGE))

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
