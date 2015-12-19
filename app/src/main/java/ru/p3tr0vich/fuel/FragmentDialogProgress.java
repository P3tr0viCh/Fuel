package ru.p3tr0vich.fuel;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.TextView;

public class FragmentDialogProgress extends DialogFragment {

    public static final String TAG = "DialogProgress";
    public static final int REQUEST_CODE = 7548;

    private static final String MESSAGE = "message";
    private static final String EXTRA_XML_RESULT = "EXTRA_XML_RESULT";

    private AsyncTaskOperationXml mAsyncTaskOperationXml;

    public static void show(Fragment parent, DatabaseBackupXmlHelper databaseBackupXmlHelper, boolean doSave) {
        FragmentDialogProgress fragmentDialogProgress = new FragmentDialogProgress();

        fragmentDialogProgress.setTask(new AsyncTaskOperationXml(databaseBackupXmlHelper, doSave));
        fragmentDialogProgress.setTargetFragment(parent, REQUEST_CODE);

        Bundle args = new Bundle();
        args.putString(MESSAGE, parent.getString(doSave ? R.string.message_progress_save : R.string.message_progress_load));
        fragmentDialogProgress.setArguments(args);

        fragmentDialogProgress.show(parent.getFragmentManager(), TAG);
    }

    @DatabaseBackupXmlHelper.BackupResult
    public static int getResult(Intent data) {
        return DatabaseBackupXmlHelper.intToResult(data.getIntExtra(EXTRA_XML_RESULT, -1));
    }

    private void setTask(AsyncTaskOperationXml asyncTaskOperationXml) {
        Functions.logD("FragmentDialogProgress -- setTask");

        mAsyncTaskOperationXml = asyncTaskOperationXml;
        mAsyncTaskOperationXml.setFragmentDialogProgress(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);
        setRetainInstance(true);

        Functions.logD("FragmentDialogProgress -- onCreate");

        if (mAsyncTaskOperationXml != null) mAsyncTaskOperationXml.execute();
    }

    @Override
    public void onResume() {
        super.onResume();

        Functions.logD("FragmentDialogProgress -- onResume: mOperationXml == null " + Boolean.toString(mAsyncTaskOperationXml == null));

        if (mAsyncTaskOperationXml == null) dismiss();
    }

    @Override
    public void dismiss() {
        Functions.logD("FragmentDialogProgress -- dismiss");
        super.dismiss();
    }

    @Override
    public void onDestroyView() {
        if (getDialog() != null && getRetainInstance())
            getDialog().setDismissMessage(null);
        super.onDestroyView();
    }

    void stopTask(@DatabaseBackupXmlHelper.BackupResult int result) {
        Functions.logD("FragmentDialogProgress -- stopTask: getTargetFragment() != null " + Boolean.toString(getTargetFragment() != null));

        if (isResumed()) {
            Functions.logD("FragmentDialogProgress -- stopTask: isResumed()");
            dismiss();
        }

        mAsyncTaskOperationXml = null;

        if (getTargetFragment() != null)
            getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK,
                    new Intent().putExtra(EXTRA_XML_RESULT, result));
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Functions.logD("FragmentDialogProgress -- onCreateDialog");

        @SuppressLint("InflateParams")
        View rootView = getActivity().getLayoutInflater().inflate(R.layout.dialog_progress, null, false);

        ((TextView) rootView.findViewById(R.id.textProgressMessage)).setText(getArguments().getString(MESSAGE));

        return new AlertDialog.Builder(getActivity()).setView(rootView).create();
    }
}
