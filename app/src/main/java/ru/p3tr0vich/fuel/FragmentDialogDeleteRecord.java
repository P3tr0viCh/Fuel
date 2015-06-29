package ru.p3tr0vich.fuel;


import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;

public class FragmentDialogDeleteRecord extends DialogFragment {

    public static final int REQUEST_CODE = 4901;

    private static final String DIALOG_TAG = "DialogDeleteRecord";
    private static final String INTENT_EXTRA_DATA = "EXTRA_DATA";
    private static final String INTENT_EXTRA_ACTION = "EXTRA_RECORD_DELETE";

    public static void show(Activity parent, FuelingRecord fuelingRecord) {
        FragmentDialogDeleteRecord dialog = new FragmentDialogDeleteRecord();

        Bundle data = new Bundle();
        data.putParcelable(INTENT_EXTRA_ACTION, fuelingRecord);

        dialog.setArguments(data);
        dialog.show(parent.getFragmentManager(), DIALOG_TAG);
    }

    public static FuelingRecord getFuelingRecord(Intent data) {
        return data.getParcelableExtra(INTENT_EXTRA_DATA);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final FuelingRecord mFuelingRecord = getArguments().getParcelable(INTENT_EXTRA_ACTION);

        assert mFuelingRecord != null;

        return new AlertDialog.Builder(getActivity())
                .setMessage(getString(R.string.message_dialog_delete,
                        Functions.sqliteToString(mFuelingRecord.getSQLiteDate(), true)))

                .setPositiveButton(R.string.dialog_btn_delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        Intent intent = new Intent();
                        intent.putExtra(INTENT_EXTRA_DATA, mFuelingRecord);

                        ((ActivityMain) getActivity())
                                .onActivityResult(REQUEST_CODE, Activity.RESULT_OK, intent);
                    }
                })

                .setNegativeButton(R.string.dialog_btn_cancel, null)
                .setCancelable(true)

                .create();
    }

    @Override
    public void onResume() {
        Functions.setDialogWidth(getDialog(), 300);
        super.onResume();
    }
}