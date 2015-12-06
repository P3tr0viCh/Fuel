package ru.p3tr0vich.fuel;

import android.content.Context;
import android.support.annotation.NonNull;

import java.io.File;

class SyncFiles {

    private static final String DIR_SYNC = "sync";
    private static final String DIR_PREFERENCES = "preferences";
    private static final String FILE_PREFERENCES_REVISION = "REVISION";
    private static final String FILE_PREFERENCES = "PREFERENCES";

    private final Context mContext;

    SyncFiles(@NonNull Context context) {
        mContext = context;
    }

    public File getDirPreferences() {
        return new File(mContext.getCacheDir() + File.separator + DIR_SYNC +
                File.separator + DIR_PREFERENCES);
    }

    public File getFilePreferencesRevision() {
        return new File(getDirPreferences(), FILE_PREFERENCES_REVISION);
    }

    public File getFilePreferences() {
        return new File(getDirPreferences(), FILE_PREFERENCES);
    }
}
