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

        try {
            UtilsFileIO.checkExists(fileRevision);

            List<String> strings = new ArrayList<>();
            UtilsFileIO.read(fileRevision, strings);

            return Integer.decode(strings.get(0));
        } catch (Exception e) {
            return -1;
        }
    }

    public void loadPreferences(@NonNull List<String> preferences) throws IOException {
        File filePreferences = mSyncFiles.getLocalFilePreferences();

        UtilsFileIO.checkExists(filePreferences);

        UtilsFileIO.read(filePreferences, preferences);
    }

    public void savePreferences(@NonNull List<String> preferences) throws IOException {
        File filePreferences = mSyncFiles.getLocalFilePreferences();

        UtilsFileIO.createFile(filePreferences);

        UtilsFileIO.write(filePreferences, preferences);
    }

    public void saveRevision(int revision) throws IOException {
        File fileRevision = mSyncFiles.getLocalFilePreferencesRevision();

        UtilsFileIO.createFile(fileRevision);

        List<String> strings = new ArrayList<>();
        strings.add(String.valueOf(revision));

        UtilsFileIO.write(fileRevision, strings);
    }

    public void makeDirs() throws IOException {
        UtilsFileIO.makeDir(mSyncFiles.getLocalDirPreferences());
    }

    public void deleteFiles() throws IOException {
        UtilsFileIO.deleteFile(mSyncFiles.getLocalFilePreferencesRevision());
        UtilsFileIO.deleteFile(mSyncFiles.getLocalFilePreferences());
    }
}