package ru.p3tr0vich.fuel;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

public class FragmentDialogQuestion extends DialogFragment {

    public static final int REQUEST_CODE = 9106;
    public static final String TAG = "DialogQuestion";

    private static final String TITLE = "title";
    private static final String MESSAGE = "message";
    private static final String POSITIVE_BUTTON_TEXT = "positive_button_text";

    private static FragmentDialogQuestion getInstance(String title, String message, String positiveButtonText) {
        Bundle args = new Bundle();
        args.putString(TITLE, title);
        args.putString(MESSAGE, message);
        args.putString(POSITIVE_BUTTON_TEXT, positiveButtonText);

        FragmentDialogQuestion dialogQuestion = new FragmentDialogQuestion();
        dialogQuestion.setArguments(args);

        return dialogQuestion;
    }

    public static void show(Fragment parent, String title, String message, String positiveButtonText) {
        FragmentDialogQuestion dialogQuestion = getInstance(title, message, positiveButtonText);
        dialogQuestion.setTargetFragment(parent, REQUEST_CODE);
        dialogQuestion.show(parent.getFragmentManager(), TAG);
    }

    public static void show(AppCompatActivity parent, String title, String message, String positiveButtonText) {
        FragmentDialogQuestion dialogQuestion = getInstance(title, message, positiveButtonText);
        dialogQuestion.setTargetFragment(null, REQUEST_CODE);
        dialogQuestion.show(parent.getSupportFragmentManager(), TAG);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle arguments = getArguments();
        return new AlertDialog.Builder(getActivity())
                .setTitle(arguments.getString(TITLE))
                .setMessage(arguments.getString(MESSAGE))

                .setPositiveButton(arguments.getString(POSITIVE_BUTTON_TEXT), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        Fragment fragment = getTargetFragment();
                        if (fragment != null) fragment.onActivityResult(REQUEST_CODE, Activity.RESULT_OK, null);
                        else {
                            Activity activity = getActivity();
                            if (activity instanceof ActivityMain)
                                ((ActivityMain) activity).onActivityResult(REQUEST_CODE, Activity.RESULT_OK, null);
                        }
                    }
                })

                .setNegativeButton(R.string.dialog_btn_cancel, null)
                .setCancelable(true)
                .create();
    }

}
