package ru.p3tr0vich.fuel;

import android.support.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class SyncLocal {

    private final SyncFiles mSyncFiles;

    SyncLocal(@NonNull SyncFiles syncFiles) {
        mSyncFiles = syncFiles;
    }

    public int getRevision() {
        File fileRevision = mSyncFiles.getLocalFilePreferencesRevision();

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

    public void loadPreferences(@NonNull List<String> preferences) throws IOException {
        File filePreferences = mSyncFiles.getLocalFilePreferences();

        FileIO.checkExists(filePreferences);
        if (!filePreferences.exists() || !filePreferences.isFile()) {
            throw new IOException("SyncLocal -- loadPreferences: " + filePreferences.toString() +
                    " not exists or not is a file");
        }

        FileIO.read(filePreferences, preferences);
    }

    public void savePreferences(@NonNull List<String> preferences) throws IOException {
        File dirPreferences = mSyncFiles.getLocalDirPreferences();

        FileIO.makeDir(dirPreferences);

        File filePreferences = mSyncFiles.getLocalFilePreferences();

        FileIO.createFile(filePreferences);

        FileIO.write(filePreferences, preferences);
    }

    public void saveRevision(int revision) throws IOException {
        File dirPreferences = mSyncFiles.getLocalDirPreferences();

        FileIO.makeDir(dirPreferences);

        File fileRevision = mSyncFiles.getLocalFilePreferencesRevision();

        FileIO.createFile(fileRevision);

        List<String> strings = new ArrayList<>();
        strings.add(String.valueOf(revision));

        FileIO.write(fileRevision, strings);
    }
}