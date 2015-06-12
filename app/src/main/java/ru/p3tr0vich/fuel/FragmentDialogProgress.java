package ru.p3tr0vich.fuel;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class FragmentDialogProgress extends DialogFragment {

    public static final String DIALOG_TAG = "DialogProgress";
    public static final int REQUEST_CODE = 7548;

    private static final String MESSAGE = "message";
    private static final String EXTRA_XML_RESULT = "EXTRA_XML_RESULT";

    private AsyncTaskOperationXml mAsyncTaskOperationXml;

    public static void show(Fragment parent, DatabaseBackupXmlHelper databaseBackupXmlHelper, boolean doSave) {
        FragmentDialogProgress fragmentDialogProgress = new FragmentDialogProgress();

        fragmentDialogProgress.setTask(new AsyncTaskOperationXml(databaseBackupXmlHelper, doSave));
        fragmentDialogProgress.setTargetFragment(parent, REQUEST_CODE);

        Bundle args = new Bundle();
        int message = doSave ? R.string.message_progress_save : R.string.message_progress_load;
        args.putString(MESSAGE, parent.getString(message));
        fragmentDialogProgress.setArguments(args);

        fragmentDialogProgress.show(parent.getFragmentManager(), DIALOG_TAG);
    }

    public static DatabaseBackupXmlHelper.Result getResult(Intent data) {
        return (DatabaseBackupXmlHelper.Result) data.getSerializableExtra(EXTRA_XML_RESULT);
    }

    private void setTask(AsyncTaskOperationXml asyncTaskOperationXml) {
        Log.d("XXX", "FragmentDialogProgress -- setTask");

        mAsyncTaskOperationXml = asyncTaskOperationXml;
        mAsyncTaskOperationXml.setFragmentDialogProgress(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);
        setRetainInstance(true);

        Log.d("XXX", "FragmentDialogProgress -- onCreate");

        if (mAsyncTaskOperationXml != null) mAsyncTaskOperationXml.execute();
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.d("XXX", "FragmentDialogProgress -- onResume: mOperationXml == null " + Boolean.toString(mAsyncTaskOperationXml == null));

        if (mAsyncTaskOperationXml == null) dismiss();
    }

    @Override
    public void dismiss() {
        Log.d("XXX", "FragmentDialogProgress -- dismiss");
        super.dismiss();
    }

    @Override
    public void onDestroyView() {
        if (getDialog() != null && getRetainInstance())
            getDialog().setDismissMessage(null);
        super.onDestroyView();
    }

    void stopTask(DatabaseBackupXmlHelper.Result result) {
        Log.d("XXX", "FragmentDialogProgress -- stopTask: getTargetFragment() != null " + Boolean.toString(getTargetFragment() != null));

        if (isResumed()) {
            Log.d("XXX", "FragmentDialogProgress -- stopTask: isResumed()");
            dismiss();
        }

        mAsyncTaskOperationXml = null;

        if (getTargetFragment() != null)
            getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK,
                    new Intent().putExtra(EXTRA_XML_RESULT, result));
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Log.d("XXX", "FragmentDialogProgress -- onCreateDialog");

        @SuppressLint("InflateParams")
        View rootView = getActivity().getLayoutInflater().inflate(R.layout.dialog_progress, null, false);

        ((TextView) rootView.findViewById(R.id.textProgressMessage)).setText(getArguments().getString(MESSAGE));

        return new AlertDialog.Builder(getActivity()).setView(rootView).create();
    }
}
