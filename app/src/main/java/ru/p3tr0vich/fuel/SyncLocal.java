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
            FileIOUtils.checkExists(fileRevision);

            List<String> strings = new ArrayList<>();
            FileIOUtils.read(fileRevision, strings);

            return Integer.decode(strings.get(0));
        } catch (Exception e) {
            return -1;
        }
    }

    public void loadPreferences(@NonNull List<String> preferences) throws IOException {
        File filePreferences = mSyncFiles.getLocalFilePreferences();

        FileIOUtils.checkExists(filePreferences);

        FileIOUtils.read(filePreferences, preferences);
    }

    public void savePreferences(@NonNull List<String> preferences) throws IOException {
        File filePreferences = mSyncFiles.getLocalFilePreferences();

        FileIOUtils.createFile(filePreferences);

        FileIOUtils.write(filePreferences, preferences);
    }

    public void saveRevision(int revision) throws IOException {
        File fileRevision = mSyncFiles.getLocalFilePreferencesRevision();

        FileIOUtils.createFile(fileRevision);

        List<String> strings = new ArrayList<>();
        strings.add(String.valueOf(revision));

        FileIOUtils.write(fileRevision, strings);
    }

    public void makeDirs() throws IOException {
        FileIOUtils.makeDir(mSyncFiles.getLocalDirPreferences());
    }

    public void deleteFiles() throws IOException {
        FileIOUtils.deleteFile(mSyncFiles.getLocalFilePreferencesRevision());
        FileIOUtils.deleteFile(mSyncFiles.getLocalFilePreferences());
    }
}