package ru.p3tr0vich.fuel;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

public class FragmentDialogQuestion extends DialogFragment {

    public static final String TAG = "FragmentDialogQuestion";

    private static final String TITLE = "title";
    private static final String MESSAGE = "message";
    private static final String POSITIVE_BUTTON_TEXT = "positive_button_text";
    private static final String NEGATIVE_BUTTON_TEXT = "negative_button_text";

    private static FragmentDialogQuestion newInstance(@Nullable @StringRes Integer titleId,
                                                      @NonNull @StringRes Integer messageId,
                                                      @Nullable @StringRes Integer positiveButtonTextId,
                                                      @Nullable @StringRes Integer negativeButtonTextId) {
        Bundle args = new Bundle();

        if (titleId != null) args.putInt(TITLE, titleId);
        args.putInt(MESSAGE, messageId);
        if (positiveButtonTextId != null) args.putInt(POSITIVE_BUTTON_TEXT, positiveButtonTextId);
        if (negativeButtonTextId != null) args.putInt(NEGATIVE_BUTTON_TEXT, negativeButtonTextId);

        FragmentDialogQuestion dialogQuestion = new FragmentDialogQuestion();
        dialogQuestion.setArguments(args);

        return dialogQuestion;
    }

    @SuppressWarnings("SameParameterValue")
    public static void show(@NonNull Fragment parent,
                            int requestCode,
                            @Nullable @StringRes Integer titleId,
                            @NonNull @StringRes Integer messageId,
                            @Nullable @StringRes Integer positiveButtonTextId,
                            @Nullable @StringRes Integer negativeButtonTextId) {
        FragmentDialogQuestion dialogQuestion = newInstance(titleId, messageId,
                positiveButtonTextId, negativeButtonTextId);

        dialogQuestion.setTargetFragment(parent, requestCode);
        dialogQuestion.show(parent.getFragmentManager(), TAG);
    }

    @SuppressWarnings("SameParameterValue")
    public static void show(@NonNull AppCompatActivity parent,
                            int requestCode,
                            @Nullable @StringRes Integer titleId,
                            @NonNull @StringRes Integer messageId,
                            @Nullable @StringRes Integer positiveButtonTextId,
                            @Nullable @StringRes Integer negativeButtonTextId) {
        FragmentDialogQuestion dialogQuestion = newInstance(titleId, messageId,
                positiveButtonTextId, negativeButtonTextId);

        dialogQuestion.setTargetFragment(null, requestCode);
        dialogQuestion.show(parent.getSupportFragmentManager(), TAG);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle arguments = getArguments();

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        @StringRes int textId;

        textId = arguments.getInt(TITLE);
        if (textId != 0) {
            @SuppressLint("InflateParams")
            TextView customTitle = (TextView) getActivity().getLayoutInflater()
                    .inflate(R.layout.apptheme_dialog_title, null, false);

            customTitle.setText(textId);

            builder.setCustomTitle(customTitle);
        }

        builder.setMessage(arguments.getInt(MESSAGE));

        textId = arguments.getInt(POSITIVE_BUTTON_TEXT);
        if (textId == 0) textId = R.string.dialog_btn_ok;

        builder.setPositiveButton(textId, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                Fragment fragment = getTargetFragment();
                if (fragment != null)
                    fragment.onActivityResult(getTargetRequestCode(),
                            Activity.RESULT_OK, null);
                else {
                    Activity activity = getActivity();
                    if (activity instanceof ActivityMain) // TODO: use listener?
                        ((ActivityMain) activity).onActivityResult(getTargetRequestCode(),
                                Activity.RESULT_OK, null);
                }
            }
        });

        textId = arguments.getInt(NEGATIVE_BUTTON_TEXT);
        if (textId == 0) textId = R.string.dialog_btn_cancel;

        builder.setNegativeButton(textId, null);

        return builder.setCancelable(true).create();
    }
}