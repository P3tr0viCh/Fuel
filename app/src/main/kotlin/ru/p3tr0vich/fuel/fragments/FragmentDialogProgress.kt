package ru.p3tr0vich.fuel.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.view.View
import android.widget.TextView
import ru.p3tr0vich.fuel.AsyncTaskOperationXml
import ru.p3tr0vich.fuel.R
import ru.p3tr0vich.fuel.helpers.DatabaseBackupXmlHelper
import ru.p3tr0vich.fuel.utils.UtilsLog

class FragmentDialogProgress : DialogFragment() {

    private var asyncTaskOperationXml: AsyncTaskOperationXml? = null
        set(value) {
            UtilsLog.d(TAG, "setTask == $value")

            field = value

            field?.fragmentDialogProgress = this
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isCancelable = false
        retainInstance = true

        UtilsLog.d(TAG, "onCreate", "asyncTaskOperationXml == $asyncTaskOperationXml")

        asyncTaskOperationXml?.execute()
    }

    override fun onResume() {
        super.onResume()

        UtilsLog.d(TAG, "onResume", "asyncTaskOperationXml == $asyncTaskOperationXml")

        if (asyncTaskOperationXml == null) {
            dismiss()
        }
    }

    override fun dismiss() {
        UtilsLog.d(TAG, "dismiss")

        super.dismiss()
    }

    override fun onDestroyView() {
        if (dialog != null && retainInstance) {
            dialog.setDismissMessage(null)
        }

        super.onDestroyView()
    }

    internal fun stopTask(@DatabaseBackupXmlHelper.BackupResult result: Int) {
        UtilsLog.d(TAG, "stopTask", "targetFragment == $targetFragment")

        if (isResumed) {
            UtilsLog.d(TAG, "stopTask", "isResumed")
            dismiss()
        }

        asyncTaskOperationXml = null

        targetFragment?.onActivityResult(targetRequestCode, Activity.RESULT_OK,
                Intent().putExtra(EXTRA_XML_RESULT, result))
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        UtilsLog.d(TAG, "onCreateDialog")

        @SuppressLint("InflateParams")
        val rootView = activity!!.layoutInflater.inflate(R.layout.dialog_progress, null, false)

        (rootView.findViewById<View>(R.id.text_message) as TextView).text = arguments?.getString(MESSAGE)

        return AlertDialog.Builder(activity!!).setView(rootView).create()
    }

    companion object {

        const val TAG = "FragmentDialogProgress"

        private const val MESSAGE = "message"
        private const val EXTRA_XML_RESULT = "EXTRA_XML_RESULT"

        fun show(parent: Fragment, requestCode: Int,
                 databaseBackupXmlHelper: DatabaseBackupXmlHelper,
                 doSave: Boolean) {
            val fragmentDialogProgress = FragmentDialogProgress()

            fragmentDialogProgress.asyncTaskOperationXml = AsyncTaskOperationXml(parent.context!!, databaseBackupXmlHelper, doSave)
            fragmentDialogProgress.setTargetFragment(parent, requestCode)

            val args = Bundle()
            args.putString(MESSAGE, parent.getString(if (doSave) R.string.message_progress_save else R.string.message_progress_load))
            fragmentDialogProgress.arguments = args

            fragmentDialogProgress.show(parent.fragmentManager!!, TAG)
        }

        @DatabaseBackupXmlHelper.BackupResult
        fun getResult(data: Intent): Int {
            return DatabaseBackupXmlHelper.intToResult(data.getIntExtra(EXTRA_XML_RESULT, -1))
        }
    }
}