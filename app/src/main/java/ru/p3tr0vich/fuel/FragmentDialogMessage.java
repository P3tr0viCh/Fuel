package ru.p3tr0vich.fuel;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.widget.TextView;

public class FragmentDialogMessage extends DialogFragment {

    private static final String TAG = "FragmentDialogMessage";

    private static final String TITLE = "title";
    private static final String MESSAGE = "message";

    private static FragmentDialogMessage getInstance(@NonNull String title, @NonNull String message) {
        Bundle args = new Bundle();
        args.putString(TITLE, title);
        args.putString(MESSAGE, message);

        FragmentDialogMessage dialogMessage = new FragmentDialogMessage();
        dialogMessage.setArguments(args);

        return dialogMessage;
    }

    public static void show(@NonNull FragmentActivity parent,
                            @NonNull String title,
                            @NonNull String message) {
        getInstance(title, message).show(parent.getSupportFragmentManager(), TAG);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle arguments = getArguments();

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        @SuppressLint("InflateParams")
        TextView customTitle = (TextView) getActivity().getLayoutInflater()
                .inflate(R.layout.apptheme_dialog_title, null, false);

        customTitle.setText(arguments.getString(TITLE));

        builder.setCustomTitle(customTitle);

        builder.setMessage(arguments.getString(MESSAGE));

        builder.setPositiveButton(R.string.dialog_btn_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dismiss();
                    }
                });

        return builder.setCancelable(true).create();
    }
}
