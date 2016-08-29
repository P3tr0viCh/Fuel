package ru.p3tr0vich.fuel;

import android.content.Context;
import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.List;

import ru.p3tr0vich.fuel.helpers.ContentResolverHelper;
import ru.p3tr0vich.fuel.helpers.DatabaseBackupXmlHelper;
import ru.p3tr0vich.fuel.models.FuelingRecord;
import ru.p3tr0vich.fuel.utils.UtilsLog;

class AsyncTaskOperationXml extends AsyncTask<Void, Void, Integer> {

    private static final String TAG = "AsyncTaskOperationXml";

    private FragmentDialogProgress mFragmentDialogProgress;
    private final DatabaseBackupXmlHelper mDatabaseBackupXmlHelper;

    private final boolean mDoSave;

    private final Context mContext;

    AsyncTaskOperationXml(Context context, DatabaseBackupXmlHelper databaseBackupXmlHelper, boolean doSave) {
        mDoSave = doSave;
        mContext = context.getApplicationContext();
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

        if (mDoSave) {
            fuelingRecordList = ContentResolverHelper.getAllRecordsList(mContext);

            result = mDatabaseBackupXmlHelper.save(fuelingRecordList);
        } else {
            fuelingRecordList = new ArrayList<>();

            result = mDatabaseBackupXmlHelper.load(fuelingRecordList);

            if (result == DatabaseBackupXmlHelper.RESULT_LOAD_OK)
                ContentResolverHelper.swapRecords(mContext, fuelingRecordList);
        }

        return result;
    }

    @Override
    protected void onPostExecute(Integer result) {
        UtilsLog.d(TAG, "onPostExecute",
                "mFragmentDialogProgress " + (mFragmentDialogProgress == null ? "=" : "!") + "= null");

        if (mFragmentDialogProgress != null) mFragmentDialogProgress.stopTask(result);
    }
}