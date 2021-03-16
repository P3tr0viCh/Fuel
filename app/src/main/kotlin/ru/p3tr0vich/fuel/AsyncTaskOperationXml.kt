package ru.p3tr0vich.fuel

import android.annotation.SuppressLint
import android.content.Context
import android.os.AsyncTask
import ru.p3tr0vich.fuel.fragments.FragmentDialogProgress
import ru.p3tr0vich.fuel.helpers.ContentResolverHelper
import ru.p3tr0vich.fuel.helpers.DatabaseBackupXmlHelper
import ru.p3tr0vich.fuel.models.FuelingRecord
import ru.p3tr0vich.fuel.utils.UtilsLog
import java.util.*

@SuppressLint("StaticFieldLeak")
internal class AsyncTaskOperationXml(val context: Context,
                                     private val databaseBackupXmlHelper: DatabaseBackupXmlHelper,
                                     private val doSave: Boolean) : AsyncTask<Void, Void, Int>() {

    var fragmentDialogProgress: FragmentDialogProgress? = null
        set(value) {
            UtilsLog.d(TAG, "fragmentDialogProgress == $fragmentDialogProgress")
            field = value
        }

    override fun doInBackground(vararg params: Void): Int {
        val fuelingRecordList: List<FuelingRecord>

        @DatabaseBackupXmlHelper.BackupResult
        val result: Int

        if (doSave) {
            fuelingRecordList = ContentResolverHelper.getAllRecordsList(context)

            result = databaseBackupXmlHelper.save(fuelingRecordList)
        } else {
            fuelingRecordList = ArrayList()

            result = databaseBackupXmlHelper.load(fuelingRecordList)

            if (result == DatabaseBackupXmlHelper.RESULT_LOAD_OK) {
                ContentResolverHelper.swapRecords(context, fuelingRecordList)
            }
        }

        return result
    }

    override fun onPostExecute(result: Int?) {
        UtilsLog.d(TAG, "onPostExecute",
                "fragmentDialogProgress == $fragmentDialogProgress")

        fragmentDialogProgress?.stopTask(result!!)
    }

    companion object {
        private const val TAG = "AsyncTaskOperationXml"
    }
}