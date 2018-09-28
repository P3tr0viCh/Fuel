package ru.p3tr0vich.fuel;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import ru.p3tr0vich.fuel.helpers.DatabaseBackupXmlHelper;
import ru.p3tr0vich.fuel.utils.Utils;
import ru.p3tr0vich.fuel.utils.UtilsLog;

import static android.support.v4.content.ContextCompat.checkSelfPermission;

public class FragmentBackup extends FragmentBase {

    private static final String TAG = "FragmentBackup";

    private static final int REQUEST_CODE_DIALOG_PROGRESS = 100;
    private static final int REQUEST_CODE_DIALOG_QUESTION = 200;

    private static final int REQUEST_CODE_PERMISSION_WRITE_EXTERNAL_STORAGE = 300;
    private static final int REQUEST_CODE_PERMISSION_READ_EXTERNAL_STORAGE = 301;

    private final DatabaseBackupXmlHelper mDatabaseBackupXmlHelper = new DatabaseBackupXmlHelper();

    @Override
    public int getTitleId() {
        return R.string.title_backup;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FragmentManager fragmentManager = getFragmentManager();

        assert fragmentManager != null;

        FragmentDialogProgress fragmentDialogProgress =
                (FragmentDialogProgress) fragmentManager.findFragmentByTag(FragmentDialogProgress.TAG);
        FragmentDialogQuestion fragmentDialogQuestion =
                (FragmentDialogQuestion) fragmentManager.findFragmentByTag(FragmentDialogQuestion.TAG);

        UtilsLog.d(TAG, "onCreate", "fragmentDialogProgress " +
                (fragmentDialogProgress == null ? "=" : "!") + "= null");

        if (fragmentDialogProgress != null)
            fragmentDialogProgress.setTargetFragment(this, REQUEST_CODE_DIALOG_PROGRESS);
        if (fragmentDialogQuestion != null)
            fragmentDialogQuestion.setTargetFragment(this, REQUEST_CODE_DIALOG_QUESTION);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        UtilsLog.d(TAG, "onCreateView");

        View view = inflater.inflate(R.layout.fragment_backup, container, false);

        ((TextView) view.findViewById(R.id.text_directory)).setText(
                mDatabaseBackupXmlHelper.getExternalDirectory().toString());
        ((TextView) view.findViewById(R.id.text_file)).setText(
                mDatabaseBackupXmlHelper.getFileName().toString());

        view.findViewById(R.id.btn_save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnSaveClick();
            }
        });

        view.findViewById(R.id.btn_load).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnLoadClick();
            }
        });

        return view;
    }

    private void startSave() {
        UtilsLog.d(TAG, "startSave");

        FragmentDialogProgress.show(this, REQUEST_CODE_DIALOG_PROGRESS, mDatabaseBackupXmlHelper, true);
    }

    private void startLoad() { // TODO: Сохранять старые в old?
        UtilsLog.d(TAG, "startLoad");

        FragmentDialogProgress.show(this, REQUEST_CODE_DIALOG_PROGRESS, mDatabaseBackupXmlHelper, false);
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

        if (result == DatabaseBackupXmlHelper.RESULT_SAVE_OK) {
            Utils.toast(R.string.message_save_file_ok);
        } else if (result == DatabaseBackupXmlHelper.RESULT_LOAD_OK) {
            Utils.toast(R.string.message_load_file_ok);

            preferencesHelper.putFullSync(true);
        } else {
            FragmentActivity activity = getActivity();

            assert activity != null;

            FragmentDialogMessage.show(activity, getString(R.string.title_message_error), resultMessage);
        }
    }

    private boolean checkPermission(final int permissionCode) {
        if (Build.VERSION.SDK_INT < 23) {
            return true;
        }

        Context context = getContext();

        assert context != null;

        final String permission;

        switch (permissionCode) {
            case REQUEST_CODE_PERMISSION_WRITE_EXTERNAL_STORAGE:
                permission = Manifest.permission.WRITE_EXTERNAL_STORAGE;
                break;
            case REQUEST_CODE_PERMISSION_READ_EXTERNAL_STORAGE:
                permission = Manifest.permission.READ_EXTERNAL_STORAGE;
                break;
            default:
                return false;
        }

        if (checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }

        requestPermissions(new String[]{permission}, permissionCode);

        return false;
    }

    private void btnSaveClick() {
        if (checkPermission(REQUEST_CODE_PERMISSION_WRITE_EXTERNAL_STORAGE)) {
            startSave();
        }
    }

    private void startLoadQuestion() {
        FragmentDialogQuestion.show(this, REQUEST_CODE_DIALOG_QUESTION,
                R.string.dialog_caption_load_from_xml,
                preferencesHelper.isSyncEnabled() ?
                        R.string.message_dialog_load_from_xml_sync :
                        R.string.message_dialog_load_from_xml, R.string.dialog_btn_load, R.string.dialog_btn_disagree);
    }

    private void btnLoadClick() {
        if (checkPermission(REQUEST_CODE_PERMISSION_READ_EXTERNAL_STORAGE)) {
            startLoadQuestion();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        UtilsLog.d(TAG, "onActivityResult");

        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        switch (requestCode) {
            case REQUEST_CODE_DIALOG_PROGRESS:
                stopOperationXml(FragmentDialogProgress.getResult(data));
                break;
            case REQUEST_CODE_DIALOG_QUESTION:
                startLoad();
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            switch (requestCode) {
                case REQUEST_CODE_PERMISSION_WRITE_EXTERNAL_STORAGE:
                    startSave();
                    break;
                case REQUEST_CODE_PERMISSION_READ_EXTERNAL_STORAGE:
                    startLoadQuestion();
            }
        } else {
            FragmentActivity activity = getActivity();

            assert activity != null;

            FragmentDialogMessage.show(activity, R.string.title_message_error, R.string.message_need_permission_to_storage);
        }
    }
}