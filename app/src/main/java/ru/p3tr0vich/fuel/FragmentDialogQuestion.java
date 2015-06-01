package ru.p3tr0vich.fuel;


import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;

public class FragmentDialogQuestion extends DialogFragment {

    public static final int REQUEST_CODE = 9106;
    public static final String DIALOG_TAG = "DialogQuestion";

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
        dialogQuestion.show(parent.getFragmentManager(), DIALOG_TAG);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle(getArguments().getString(TITLE));
        builder.setMessage(getArguments().getString(MESSAGE));

        builder.setPositiveButton(getArguments().getString(POSITIVE_BUTTON_TEXT), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                getTargetFragment().onActivityResult(REQUEST_CODE, Activity.RESULT_OK, null);
            }
        });

        builder.setNegativeButton(R.string.dialog_btn_cancel, null);

        builder.setCancelable(true);

        return builder.create();
    }

}
