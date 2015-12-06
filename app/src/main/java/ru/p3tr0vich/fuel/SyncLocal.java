package ru.p3tr0vich.fuel;

import android.support.annotation.NonNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

class SyncLocal {

    private final SyncFiles mSyncFiles;

    SyncLocal(@NonNull SyncFiles syncFiles) {
        mSyncFiles = syncFiles;
    }

    public int getRevision() {
        File fileRevision = mSyncFiles.getFilePreferencesRevision();

        if (!fileRevision.exists() || !fileRevision.isFile()) {
            Functions.logD("SyncLocal -- getRevision: fileRevision not exists");
            return -1;
        }

        List<String> strings = new ArrayList<>();
        try {
            FileIO.read(fileRevision, strings);

            return Integer.decode(strings.get(0));
        } catch (Exception e) {
            Functions.logD("SyncLocal -- getRevision: exception == " + e.toString());
            return -1;
        }
    }

    public boolean loadPreferences(@NonNull List<String> preferences) {
        File filePreferences = mSyncFiles.getFilePreferences();

        if (!filePreferences.exists() || !filePreferences.isFile()) {
            Functions.logD("SyncLocal -- loadPreferences: filePreferences not exists");
            return false;
        }

        try {
            FileIO.read(filePreferences, preferences);

            return true;
        } catch (Exception e) {
            Functions.logD("SyncLocal -- loadPreferences: exception == " + e.toString());
            return false;
        }
    }

    public boolean savePreferences(@NonNull List<String> preferences) {
        File dirPreferences = mSyncFiles.getDirPreferences();

        if (!dirPreferences.exists()) if (!dirPreferences.mkdirs()) {
            Functions.logD("SyncLocal -- savePreferences: can not create dirPreferences");
            return false;
        }

        File filePreferences = mSyncFiles.getFilePreferences();

        try {
            if (!filePreferences.createNewFile()) if (!filePreferences.isFile()) {
                Functions.logD("SyncLocal -- savePreferences: can not create filePreferences");
                return false;
            }

            FileIO.write(filePreferences, preferences);

            return true;
        } catch (Exception e) {
            Functions.logD("SyncLocal -- savePreferences: exception == " + e.toString());
            return false;
        }
    }

    public boolean saveRevision(int revision) {
        File dirPreferences = mSyncFiles.getDirPreferences();

        if (!dirPreferences.exists()) if (!dirPreferences.mkdirs()) {
            Functions.logD("SyncLocal -- saveRevision: can not create dirPreferences");
            return false;
        }

        File fileRevision = mSyncFiles.getFilePreferencesRevision();

        try {
            if (!fileRevision.createNewFile()) if (!fileRevision.isFile()) {
                Functions.logD("SyncLocal -- saveRevision: can not create fileRevision");
                return false;
            }

            List<String> strings = new ArrayList<>();
            strings.add(String.valueOf(revision));

            FileIO.write(fileRevision, strings);

            return true;
        } catch (Exception e) {
            Functions.logD("SyncLocal -- saveRevision: exception == " + e.toString());
            return false;
        }
    }
}