package ru.p3tr0vich.fuel;

import android.content.Context;
import android.support.annotation.NonNull;

import java.io.File;

class SyncFiles {

    private static final String DIR_SYNC = "sync";
    private static final String DIR_PREFERENCES = "preferences";
    private static final String FILE_PREFERENCES_REVISION = "REVISION";
    private static final String FILE_PREFERENCES = "PREFERENCES";

    private final File mLocalDirPreferences;
    private final File mLocalFilePreferences;
    private final File mLocalFilePreferencesRevision;

    SyncFiles(@NonNull Context context) {
        mLocalDirPreferences = new File(context.getCacheDir() + File.separator + DIR_SYNC +
                File.separator + DIR_PREFERENCES);
        mLocalFilePreferences = new File(mLocalDirPreferences, FILE_PREFERENCES);
        mLocalFilePreferencesRevision = new File(mLocalDirPreferences, FILE_PREFERENCES_REVISION);
    }

    public File getLocalDirPreferences() {
        return mLocalDirPreferences;
    }

    public File getLocalFilePreferences() {
        return mLocalFilePreferences;
    }

    public File getLocalFilePreferencesRevision() {
        return mLocalFilePreferencesRevision;
    }
}
