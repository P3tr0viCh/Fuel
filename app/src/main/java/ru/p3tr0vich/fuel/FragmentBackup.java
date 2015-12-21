package ru.p3tr0vich.fuel;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

public class FragmentBackup extends FragmentFuel {

    public static final String TAG = "FragmentBackup";

    private DatabaseBackupXmlHelper mDatabaseBackupXmlHelper;

    private OnDataLoadedFromBackupListener mOnDataLoadedFromBackupListener;

    @Override
    public int getFragmentId() {
        return R.id.action_backup;
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

        Functions.logD("FragmentBackup -- onCreate: fragmentDialogProgress != null " + Boolean.toString(fragmentDialogProgress != null));

        if (fragmentDialogProgress != null)
            fragmentDialogProgress.setTargetFragment(this, FragmentDialogProgress.REQUEST_CODE);
        if (fragmentDialogQuestion != null)
            fragmentDialogQuestion.setTargetFragment(this, FragmentDialogQuestion.REQUEST_CODE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Functions.logD("FragmentBackup -- onCreateView");

        View v = inflater.inflate(R.layout.fragment_backup, container, false);

        mDatabaseBackupXmlHelper = new DatabaseBackupXmlHelper();

        ((TextView) v.findViewById(R.id.textDirectory)).setText(mDatabaseBackupXmlHelper.getExternalDirectory().toString());
        ((TextView) v.findViewById(R.id.textFile)).setText(mDatabaseBackupXmlHelper.getFileName().toString());

        v.findViewById(R.id.btnSave).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveToXml();
            }
        });

        v.findViewById(R.id.btnLoad).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadFromXml();
            }
        });

        return v;
    }

    private void startOperationXml(boolean doSave) {
        Functions.logD("FragmentBackup -- startOperationXml");

        FragmentDialogProgress.show(this, mDatabaseBackupXmlHelper, doSave);
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

        Functions.logD("FragmentBackup -- stopOperationXml: " + resultMessage);

        if (result == DatabaseBackupXmlHelper.RESULT_SAVE_OK)
            Toast.makeText(getActivity(), R.string.message_save_file_ok, Toast.LENGTH_SHORT).show();
        else if (result == DatabaseBackupXmlHelper.RESULT_LOAD_OK) {
            Toast.makeText(getActivity(), R.string.message_load_file_ok, Toast.LENGTH_SHORT).show();
            mOnDataLoadedFromBackupListener.onDataLoadedFromBackup();
        } else
            FragmentDialogMessage.show(getActivity(), getString(R.string.title_message_error), resultMessage);
    }

    private void saveToXml() {
        startOperationXml(true);
    }

    private void loadFromXml() { // TODO: Сохранять старые в old?
        FragmentDialogQuestion.show(this, R.string.dialog_caption_load_from_xml,
                R.string.message_dialog_load_from_xml, R.string.dialog_btn_load, R.string.dialog_btn_disagree);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Functions.logD("FragmentBackup -- onActivityResult");

        if (resultCode != Activity.RESULT_OK) return;

        switch (requestCode) {
            case FragmentDialogProgress.REQUEST_CODE:
                stopOperationXml(FragmentDialogProgress.getResult(data));
                break;
            case FragmentDialogQuestion.REQUEST_CODE:
                startOperationXml(false);
        }
    }

    public interface OnDataLoadedFromBackupListener {
        void onDataLoadedFromBackup();
    }

    @Override
    public void onAttach(Context context) {
        Functions.logD("FragmentBackup -- onAttach");
        super.onAttach(context);
        try {
            mOnDataLoadedFromBackupListener = (OnDataLoadedFromBackupListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() +
                    " must implement OnDataLoadedFromBackupListener");
        }
    }

}
