package ru.p3tr0vich.fuel;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import ru.p3tr0vich.fuel.helpers.DatabaseBackupXmlHelper;
import ru.p3tr0vich.fuel.helpers.PreferencesHelper;
import ru.p3tr0vich.fuel.utils.Utils;
import ru.p3tr0vich.fuel.utils.UtilsLog;

public class FragmentBackup extends FragmentBase {

    public static final String TAG = "FragmentBackup";

    private static final int REQUEST_CODE_DIALOG_PROGRESS = 100;
    private static final int REQUEST_CODE_DIALOG_QUESTION = 200;

    private final DatabaseBackupXmlHelper mDatabaseBackupXmlHelper = new DatabaseBackupXmlHelper();

    @NonNull
    public static Fragment newInstance(int id) {
        return newInstance(id, new FragmentBackup());
    }

    @Override
    public int getTitleId() {
        return R.string.title_backup;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FragmentDialogProgress fragmentDialogProgress =
                (FragmentDialogProgress) getFragmentManager().findFragmentByTag(FragmentDialogProgress.TAG);
        FragmentDialogQuestion fragmentDialogQuestion =
                (FragmentDialogQuestion) getFragmentManager().findFragmentByTag(FragmentDialogQuestion.TAG);

        UtilsLog.d(TAG, "onCreate", "fragmentDialogProgress " +
                (fragmentDialogProgress == null ? "=" : "!") + "= null");

        if (fragmentDialogProgress != null)
            fragmentDialogProgress.setTargetFragment(this, REQUEST_CODE_DIALOG_PROGRESS);
        if (fragmentDialogQuestion != null)
            fragmentDialogQuestion.setTargetFragment(this, REQUEST_CODE_DIALOG_QUESTION);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        UtilsLog.d(TAG, "onCreateView");

        View view = inflater.inflate(R.layout.fragment_backup, container, false);

        ((TextView) view.findViewById(R.id.textDirectory)).setText(
                mDatabaseBackupXmlHelper.getExternalDirectory().toString());
        ((TextView) view.findViewById(R.id.textFile)).setText(
                mDatabaseBackupXmlHelper.getFileName().toString());

        view.findViewById(R.id.btnSave).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveToXml();
            }
        });

        view.findViewById(R.id.btnLoad).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadFromXml();
            }
        });

        return view;
    }

    private void startOperationXml(boolean doSave) {
        UtilsLog.d(TAG, "startOperationXml");

        FragmentDialogProgress.show(this, REQUEST_CODE_DIALOG_PROGRESS, mDatabaseBackupXmlHelper, doSave);
    }

    private void stopOperationXml(@DatabaseBackupXmlHelper.BackupResult int result) {
        String resultMessage;

        switch (result) {
            case DatabaseBackupXmlHelper.RESULT_SAVE_OK:
                resultMessage = "File '" + mDatabaseBackupXmlHelper.getFileName() + "' save in '" +
                        mDatabaseBackupXmlHelper.getExternalDirectory() + "' without errors";
                break;
            case DatabaseBackupXmlHelper.RESULT_LOAD_OK:
                resultMessage = "File '" + mDatabaseBackupXmlHelper.getFileName() + "' load from '" +
                        mDatabaseBackupXmlHelper.getExternalDirectory() + "' without errors";
                break;
            case DatabaseBackupXmlHelper.RESULT_ERROR_MKDIRS:
                resultMessage = getString(R.string.message_error_mkdirs, mDatabaseBackupXmlHelper.getExternalDirectory());
                break;
            case DatabaseBackupXmlHelper.RESULT_ERROR_CREATE_XML:
                resultMessage = getString(R.string.message_error_create_xml);
                break;
            case DatabaseBackupXmlHelper.RESULT_ERROR_CREATE_FILE:
                resultMessage = getString(R.string.message_error_create_file,
                        mDatabaseBackupXmlHelper.getFileName(),
                        mDatabaseBackupXmlHelper.getExternalDirectory());
                break;
            case DatabaseBackupXmlHelper.RESULT_ERROR_SAVE_FILE:
                resultMessage = getString(R.string.message_error_save_file,
                        mDatabaseBackupXmlHelper.getFileName(),
                        mDatabaseBackupXmlHelper.getExternalDirectory());
                break;
            case DatabaseBackupXmlHelper.RESULT_ERROR_DIR_NOT_EXISTS:
                resultMessage = getString(R.string.message_error_dir_not_exists, mDatabaseBackupXmlHelper.getExternalDirectory());
                break;
            case DatabaseBackupXmlHelper.RESULT_ERROR_FILE_NOT_EXISTS:
                resultMessage = getString(R.string.message_error_file_not_exists,
                        mDatabaseBackupXmlHelper.getFileName(),
                        mDatabaseBackupXmlHelper.getExternalDirectory());
                break;
            case DatabaseBackupXmlHelper.RESULT_ERROR_READ_FILE:
                resultMessage = getString(R.string.message_error_read_file);
                break;
            case DatabaseBackupXmlHelper.RESULT_ERROR_PARSE_XML:
                resultMessage = getString(R.string.message_error_parse_xml);
                break;
            default:
                return;
        }

        UtilsLog.d(TAG, "stopOperationXml: " + resultMessage);

        if (result == DatabaseBackupXmlHelper.RESULT_SAVE_OK)
            Utils.toast(R.string.message_save_file_ok);
        else if (result == DatabaseBackupXmlHelper.RESULT_LOAD_OK) {
            Utils.toast(R.string.message_load_file_ok);

            PreferencesHelper.putFullSync(true);
        } else
            FragmentDialogMessage.show(getActivity(), getString(R.string.title_message_error), resultMessage);
    }

    private void saveToXml() {
        startOperationXml(true);
    }

    private void loadFromXml() { // TODO: Сохранять старые в old?
        FragmentDialogQuestion.show(this, REQUEST_CODE_DIALOG_QUESTION,
                R.string.dialog_caption_load_from_xml,
                PreferencesHelper.isSyncEnabled() ?
                        R.string.message_dialog_load_from_xml_sync :
                        R.string.message_dialog_load_from_xml, R.string.dialog_btn_load, R.string.dialog_btn_disagree);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        UtilsLog.d(TAG, "onActivityResult");

        if (resultCode != Activity.RESULT_OK) return;

        switch (requestCode) {
            case REQUEST_CODE_DIALOG_PROGRESS:
                stopOperationXml(FragmentDialogProgress.getResult(data));
                break;
            case REQUEST_CODE_DIALOG_QUESTION:
                startOperationXml(false);
        }
    }
}