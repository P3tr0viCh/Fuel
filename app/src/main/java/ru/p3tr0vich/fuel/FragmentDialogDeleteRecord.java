package ru.p3tr0vich.fuel;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;

public class FragmentDialogDeleteRecord extends DialogFragment {

    public static final int REQUEST_CODE = 4901;

    private static final String TAG = "DialogDeleteRecord";

    public static void show(FragmentManager fragmentManager, FuelingRecord fuelingRecord) {
        FragmentDialogDeleteRecord dialog = new FragmentDialogDeleteRecord();

        dialog.setArguments(fuelingRecord.toBundle());
        dialog.show(fragmentManager, TAG);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final FuelingRecord fuelingRecord = new FuelingRecord(getArguments());

        return new AlertDialog.Builder(getActivity())
                .setMessage(getString(R.string.message_dialog_delete,
                        Functions.sqlDateToString(fuelingRecord.getSQLiteDate(), true)))

                .setPositiveButton(R.string.dialog_btn_delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        Intent intent = new Intent();
                        fuelingRecord.toIntent(intent);

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