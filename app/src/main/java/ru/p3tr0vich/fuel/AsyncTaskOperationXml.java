package ru.p3tr0vich.fuel;

import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.List;

class AsyncTaskOperationXml extends AsyncTask<Void, Void, DatabaseBackupXmlHelper.Result> {

    private FragmentDialogProgress mFragmentDialogProgress;
    private final DatabaseBackupXmlHelper mDatabaseBackupXmlHelper;

    private final boolean mDoSave;

    AsyncTaskOperationXml(DatabaseBackupXmlHelper databaseBackupXmlHelper, boolean doSave) {
        mDoSave = doSave;
        mDatabaseBackupXmlHelper = new DatabaseBackupXmlHelper(databaseBackupXmlHelper);
    }

    void setFragmentDialogProgress(FragmentDialogProgress fragmentDialogProgress) {
        Functions.LogD("OperationXml -- setFragmentDialogProgress");
        mFragmentDialogProgress = fragmentDialogProgress;
    }

    @Override
    protected DatabaseBackupXmlHelper.Result doInBackground(Void... params) {
        List<FuelingRecord> fuelingRecordList;

        DatabaseBackupXmlHelper.Result result;

        FuelingDBHelper dbHelper = new FuelingDBHelper();

        if (mDoSave) {
            fuelingRecordList = dbHelper.getAllRecords();

            result = mDatabaseBackupXmlHelper.save(fuelingRecordList);
        } else {
            fuelingRecordList = new ArrayList<>();

            result = mDatabaseBackupXmlHelper.load(fuelingRecordList);

            if (result == DatabaseBackupXmlHelper.Result.RESULT_LOAD_OK)
                dbHelper.insertRecords(fuelingRecordList);
        }

        return result;
    }

    @Override
    protected void onPostExecute(DatabaseBackupXmlHelper.Result result) {
        Functions.LogD("OperationXml -- onPostExecute: mFragmentDialogProgress != null " + Boolean.toString(mFragmentDialogProgress != null));

        if (mFragmentDialogProgress != null) mFragmentDialogProgress.stopTask(result);
    }
}