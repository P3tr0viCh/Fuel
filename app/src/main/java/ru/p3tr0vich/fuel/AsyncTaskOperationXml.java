package ru.p3tr0vich.fuel;

import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.List;

class AsyncTaskOperationXml extends AsyncTask<Void, Void, Integer> {

    private static final String TAG = "AsyncTaskOperationXml";

    private FragmentDialogProgress mFragmentDialogProgress;
    private final DatabaseBackupXmlHelper mDatabaseBackupXmlHelper;

    private final boolean mDoSave;

    AsyncTaskOperationXml(DatabaseBackupXmlHelper databaseBackupXmlHelper, boolean doSave) {
        mDoSave = doSave;
        mDatabaseBackupXmlHelper = new DatabaseBackupXmlHelper(databaseBackupXmlHelper);
    }

    void setFragmentDialogProgress(FragmentDialogProgress fragmentDialogProgress) {
        UtilsLog.d(TAG, "setFragmentDialogProgress");
        mFragmentDialogProgress = fragmentDialogProgress;
    }

    @Override
    protected Integer doInBackground(Void... params) {
        List<FuelingRecord> fuelingRecordList;

        @DatabaseBackupXmlHelper.BackupResult
        int result;

        FuelingDBHelper dbHelper = new FuelingDBHelper();

        if (mDoSave) {
            fuelingRecordList = dbHelper.getAllRecords();

            result = mDatabaseBackupXmlHelper.save(fuelingRecordList);
        } else {
            fuelingRecordList = new ArrayList<>();

            result = mDatabaseBackupXmlHelper.load(fuelingRecordList);

            if (result == DatabaseBackupXmlHelper.RESULT_LOAD_OK)
                dbHelper.swapRecords(fuelingRecordList);
        }

        return result;
    }

    @Override
    protected void onPostExecute(Integer result) {
        UtilsLog.d(TAG, "onPostExecute", "mFragmentDialogProgress != null " +
                Boolean.toString(mFragmentDialogProgress != null));

        if (mFragmentDialogProgress != null) mFragmentDialogProgress.stopTask(result);
    }
}