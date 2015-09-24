package ru.p3tr0vich.fuel;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;

public class FragmentDialogQuestion extends DialogFragment {

    public static final int REQUEST_CODE = 9106;
    public static final String TAG = "DialogQuestion";

    private static final String TITLE = "title";
    private static final String MESSAGE = "message";
    private static final String POSITIVE_BUTTON_TEXT = "positive_button_text";

    public static void show(Fragment parent, String title, String message, String positiveButtonText) {
        Bundle args = new Bundle();
        args.putString(FragmentDialogQuestion.TITLE, title);
        args.putString(FragmentDialogQuestion.MESSAGE, message);
        args.putString(FragmentDialogQuestion.POSITIVE_BUTTON_TEXT, positiveButtonText);

        FragmentDialogQuestion dialogQuestion = new FragmentDialogQuestion();
        dialogQuestion.setArguments(args);
        dialogQuestion.setTargetFragment(parent, REQUEST_CODE);
        dialogQuestion.show(parent.getFragmentManager(), TAG);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setTitle(getArguments().getString(TITLE))
                .setMessage(getArguments().getString(MESSAGE))

                .setPositiveButton(getArguments().getString(POSITIVE_BUTTON_TEXT), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        getTargetFragment().onActivityResult(REQUEST_CODE, Activity.RESULT_OK, null);
                    }
                })

                .setNegativeButton(R.string.dialog_btn_cancel, null)
                .setCancelable(true)
                .create();
    }

}
