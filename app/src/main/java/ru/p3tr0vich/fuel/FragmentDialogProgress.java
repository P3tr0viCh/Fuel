package ru.p3tr0vich.fuel;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.TextView;

import ru.p3tr0vich.fuel.helpers.DatabaseBackupXmlHelper;
import ru.p3tr0vich.fuel.utils.UtilsLog;

public class FragmentDialogProgress extends DialogFragment {

    public static final String TAG = "FragmentDialogProgress";

    private static final String MESSAGE = "message";
    private static final String EXTRA_XML_RESULT = "EXTRA_XML_RESULT";

    private AsyncTaskOperationXml mAsyncTaskOperationXml;

    public static void show(@NonNull Fragment parent, int requestCode,
                            @NonNull DatabaseBackupXmlHelper databaseBackupXmlHelper,
                            boolean doSave) {
        FragmentDialogProgress fragmentDialogProgress = new FragmentDialogProgress();

        Context context = parent.getContext();

        assert context != null;

        fragmentDialogProgress.setTask(new AsyncTaskOperationXml(context, databaseBackupXmlHelper, doSave));
        fragmentDialogProgress.setTargetFragment(parent, requestCode);

        Bundle args = new Bundle();
        args.putString(MESSAGE, parent.getString(doSave ? R.string.message_progress_save : R.string.message_progress_load));
        fragmentDialogProgress.setArguments(args);

        FragmentManager fragmentManager = parent.getFragmentManager();

        assert fragmentManager != null;

        fragmentDialogProgress.show(fragmentManager, TAG);
    }

    @DatabaseBackupXmlHelper.BackupResult
    public static int getResult(Intent data) {
        return DatabaseBackupXmlHelper.intToResult(data.getIntExtra(EXTRA_XML_RESULT, -1));
    }

    private void setTask(AsyncTaskOperationXml asyncTaskOperationXml) {
        UtilsLog.d(TAG, "setTask");

        mAsyncTaskOperationXml = asyncTaskOperationXml;
        mAsyncTaskOperationXml.setFragmentDialogProgress(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);
        setRetainInstance(true);

        UtilsLog.d(TAG, "onCreate");

        if (mAsyncTaskOperationXml != null) mAsyncTaskOperationXml.execute();
    }

    @Override
    public void onResume() {
        super.onResume();

        UtilsLog.d(TAG, "onResume",
                "mAsyncTaskOperationXml " + (mAsyncTaskOperationXml == null ? "=" : "!") + "= null");

        if (mAsyncTaskOperationXml == null) dismiss();
    }

    @Override
    public void dismiss() {
        UtilsLog.d(TAG, "dismiss");
        super.dismiss();
    }

    @Override
    public void onDestroyView() {
        if (getDialog() != null && getRetainInstance())
            getDialog().setDismissMessage(null);
        super.onDestroyView();
    }

    void stopTask(@DatabaseBackupXmlHelper.BackupResult int result) {
        UtilsLog.d(TAG, "stopTask",
                "getTargetFragment() " + (getTargetFragment() == null ? "=" : "!") + "= null");

        if (isResumed()) {
            UtilsLog.d(TAG, "stopTask", "isResumed()");
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
        UtilsLog.d(TAG, "onCreateDialog");

        Activity activity = getActivity();

        assert activity != null;

        @SuppressLint("InflateParams")
        View rootView = activity.getLayoutInflater().inflate(R.layout.dialog_progress, null, false);

        Bundle bundle = getArguments();

        assert bundle != null;

        ((TextView) rootView.findViewById(R.id.text_message)).setText(bundle.getString(MESSAGE));

        return new AlertDialog.Builder(activity).setView(rootView).create();
    }
}