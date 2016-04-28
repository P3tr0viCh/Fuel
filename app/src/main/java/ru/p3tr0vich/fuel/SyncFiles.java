package ru.p3tr0vich.fuel;

import android.content.Context;
import android.support.annotation.NonNull;

import java.io.File;

import ru.p3tr0vich.fuel.utils.Utils;

class SyncFiles {

    private static final String DIR_SYNC = "sync";

    private static final String DIR_PREFERENCES = "preferences";
    private static final String FILE_PREFERENCES = "PREFERENCES";
    private static final String FILE_PREFERENCES_REVISION = "REVISION";

    private static final String DIR_DATABASE = "database";
    private static final String FILE_DATABASE = "DATABASE";
    private static final String FILE_DATABASE_REVISION = "REVISION";

    private static final String DIR_DEBUG = "_debug";

    private final File mLocalDirPreferences;
    private final File mLocalFilePreferences;
    private final File mLocalFilePreferencesRevision;

    private final File mServerDirPreferences;
    private final File mServerFilePreferences;
    private final File mServerFilePreferencesRevision;

    private final File mLocalDirDatabase;
    private final File mLocalFileDatabase;
    private final File mLocalFileDatabaseRevision;

    private final File mServerDirDatabase;
    private final File mServerFileDatabaseRevision;

    SyncFiles(@NonNull Context context) {
        final boolean isDebuggable = Utils.isDebuggable();

        File syncDir = new File(context.getCacheDir(), DIR_SYNC);

        mLocalDirDatabase = new File(syncDir, DIR_DATABASE);
        mLocalDirPreferences = new File(syncDir, DIR_PREFERENCES);

        mLocalFileDatabase = new File(mLocalDirDatabase, FILE_DATABASE);
        mLocalFileDatabaseRevision = new File(mLocalDirDatabase, FILE_DATABASE_REVISION);

        mServerDirDatabase = new File(DIR_DATABASE + (isDebuggable ? DIR_DEBUG : ""));
        mServerFileDatabaseRevision = new File(mServerDirDatabase, FILE_DATABASE_REVISION);

        mLocalFilePreferences = new File(mLocalDirPreferences, FILE_PREFERENCES);
        mLocalFilePreferencesRevision = new File(mLocalDirPreferences, FILE_PREFERENCES_REVISION);

        mServerDirPreferences = new File(DIR_PREFERENCES + (isDebuggable ? DIR_DEBUG : ""));
        mServerFilePreferences = new File(mServerDirPreferences, FILE_PREFERENCES);
        mServerFilePreferencesRevision = new File(mServerDirPreferences, FILE_PREFERENCES_REVISION);
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

    public File getServerDirPreferences() {
        return mServerDirPreferences;
    }

    public File getServerFilePreferences() {
        return mServerFilePreferences;
    }

    public File getServerFilePreferencesRevision() {
        return mServerFilePreferencesRevision;
    }

    public File getLocalDirDatabase() {
        return mLocalDirDatabase;
    }

    public File getLocalFileDatabase() {
        return mLocalFileDatabase;
    }

    public File getLocalFileDatabaseRevision() {
        return mLocalFileDatabaseRevision;
    }

    public File getServerDirDatabase() {
        return mServerDirDatabase;
    }

    public File getServerFileDatabase(int revision) {
        return new File(mServerDirDatabase, String.valueOf(revision));
    }

    public File getServerFileDatabaseRevision() {
        return mServerFileDatabaseRevision;
    }
}