package ru.p3tr0vich.fuel.fragments

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.support.v4.content.ContextCompat.checkSelfPermission
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import ru.p3tr0vich.fuel.R
import ru.p3tr0vich.fuel.factories.FragmentFactory
import ru.p3tr0vich.fuel.helpers.DatabaseBackupXmlHelper
import ru.p3tr0vich.fuel.utils.Utils
import ru.p3tr0vich.fuel.utils.UtilsLog

class FragmentBackup : FragmentBase(FragmentFactory.Ids.BACKUP) {

    private val databaseBackupXmlHelper = DatabaseBackupXmlHelper()

    override val titleId: Int
        get() = R.string.title_backup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val fragmentDialogProgress = fragmentManager?.findFragmentByTag(FragmentDialogProgress.TAG) as FragmentDialogProgress?
        val fragmentDialogQuestion = fragmentManager?.findFragmentByTag(FragmentDialogQuestion.TAG) as FragmentDialogQuestion?

        UtilsLog.d(TAG, "onCreate", "fragmentDialogProgress = $fragmentDialogProgress")

        fragmentDialogProgress?.setTargetFragment(this, REQUEST_CODE_DIALOG_PROGRESS)
        fragmentDialogQuestion?.setTargetFragment(this, REQUEST_CODE_DIALOG_QUESTION)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        UtilsLog.d(TAG, "onCreateView")

        with(inflater.inflate(R.layout.fragment_backup, container, false)) {
            (findViewById<View>(R.id.text_directory) as TextView).text =
                    databaseBackupXmlHelper.externalDirectory.toString()
            (findViewById<View>(R.id.text_file) as TextView).text =
                    databaseBackupXmlHelper.fileName.toString()

            findViewById<View>(R.id.btn_save).setOnClickListener { btnSaveClick() }

            findViewById<View>(R.id.btn_load).setOnClickListener { btnLoadClick() }

            return this
        }
    }

    private fun startSave() {
        UtilsLog.d(TAG, "startSave")

        FragmentDialogProgress.show(this, REQUEST_CODE_DIALOG_PROGRESS, databaseBackupXmlHelper, true)
    }

    private fun startLoad() { // TODO: Сохранять старые в old?
        UtilsLog.d(TAG, "startLoad")

        FragmentDialogProgress.show(this, REQUEST_CODE_DIALOG_PROGRESS, databaseBackupXmlHelper, false)
    }

    private fun stopOperationXml(@DatabaseBackupXmlHelper.BackupResult result: Int) {
        val resultMessage: String = when (result) {
            DatabaseBackupXmlHelper.RESULT_SAVE_OK -> "File '${databaseBackupXmlHelper.fileName}' save in '${databaseBackupXmlHelper.externalDirectory}' without errors"
            DatabaseBackupXmlHelper.RESULT_LOAD_OK -> "File '${databaseBackupXmlHelper.fileName}' load from '${databaseBackupXmlHelper.externalDirectory}' without errors"
            DatabaseBackupXmlHelper.RESULT_ERROR_MKDIRS -> getString(R.string.message_error_mkdirs, databaseBackupXmlHelper.externalDirectory)
            DatabaseBackupXmlHelper.RESULT_ERROR_CREATE_XML -> getString(R.string.message_error_create_xml)
            DatabaseBackupXmlHelper.RESULT_ERROR_CREATE_FILE -> getString(R.string.message_error_create_file,
                    databaseBackupXmlHelper.fileName,
                    databaseBackupXmlHelper.externalDirectory)
            DatabaseBackupXmlHelper.RESULT_ERROR_SAVE_FILE -> getString(R.string.message_error_save_file,
                    databaseBackupXmlHelper.fileName,
                    databaseBackupXmlHelper.externalDirectory)
            DatabaseBackupXmlHelper.RESULT_ERROR_DIR_NOT_EXISTS -> getString(R.string.message_error_dir_not_exists, databaseBackupXmlHelper.externalDirectory)
            DatabaseBackupXmlHelper.RESULT_ERROR_FILE_NOT_EXISTS -> getString(R.string.message_error_file_not_exists,
                    databaseBackupXmlHelper.fileName,
                    databaseBackupXmlHelper.externalDirectory)
            DatabaseBackupXmlHelper.RESULT_ERROR_READ_FILE -> getString(R.string.message_error_read_file)
            DatabaseBackupXmlHelper.RESULT_ERROR_PARSE_XML -> getString(R.string.message_error_parse_xml)
            else -> return
        }

        UtilsLog.d(TAG, "stopOperationXml: $resultMessage")

        when (result) {
            DatabaseBackupXmlHelper.RESULT_SAVE_OK -> {
                Utils.toast(R.string.message_save_file_ok)
            }
            DatabaseBackupXmlHelper.RESULT_LOAD_OK -> {
                Utils.toast(R.string.message_load_file_ok)

                preferencesHelper.isFullSync = true
            }
            else -> {
                FragmentDialogMessage.show(activity!!, getString(R.string.title_message_error), resultMessage)
            }
        }
    }

    private fun checkPermission(permissionCode: Int): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true
        }

        val permission: String = when (permissionCode) {
            REQUEST_CODE_PERMISSION_WRITE_EXTERNAL_STORAGE -> Manifest.permission.WRITE_EXTERNAL_STORAGE
            REQUEST_CODE_PERMISSION_READ_EXTERNAL_STORAGE -> Manifest.permission.READ_EXTERNAL_STORAGE
            else -> return false
        }

        if (checkSelfPermission(context!!, permission) == PackageManager.PERMISSION_GRANTED) {
            return true
        }

        requestPermissions(arrayOf(permission), permissionCode)

        return false
    }

    private fun btnSaveClick() {
        if (checkPermission(REQUEST_CODE_PERMISSION_WRITE_EXTERNAL_STORAGE)) {
            startSave()
        }
    }

    private fun startLoadQuestion() {
        FragmentDialogQuestion.show(this, REQUEST_CODE_DIALOG_QUESTION,
                R.string.dialog_caption_load_from_xml,
                if (preferencesHelper.isSyncEnabled)
                    R.string.message_dialog_load_from_xml_sync
                else
                    R.string.message_dialog_load_from_xml, R.string.dialog_btn_load, R.string.dialog_btn_disagree)
    }

    private fun btnLoadClick() {
        if (checkPermission(REQUEST_CODE_PERMISSION_READ_EXTERNAL_STORAGE)) {
            startLoadQuestion()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        UtilsLog.d(TAG, "onActivityResult")

        if (resultCode != Activity.RESULT_OK) {
            return
        }

        when (requestCode) {
            REQUEST_CODE_DIALOG_PROGRESS -> stopOperationXml(FragmentDialogProgress.getResult(data!!))
            REQUEST_CODE_DIALOG_QUESTION -> startLoad()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            when (requestCode) {
                REQUEST_CODE_PERMISSION_WRITE_EXTERNAL_STORAGE -> startSave()
                REQUEST_CODE_PERMISSION_READ_EXTERNAL_STORAGE -> startLoadQuestion()
            }
        } else {
            FragmentDialogMessage.show(activity!!, R.string.title_message_error, R.string.message_need_permission_to_storage)
        }
    }

    companion object {

        private const val TAG = "FragmentBackup"

        private const val REQUEST_CODE_DIALOG_PROGRESS = 100
        private const val REQUEST_CODE_DIALOG_QUESTION = 200

        private const val REQUEST_CODE_PERMISSION_WRITE_EXTERNAL_STORAGE = 300
        private const val REQUEST_CODE_PERMISSION_READ_EXTERNAL_STORAGE = 301
    }
}