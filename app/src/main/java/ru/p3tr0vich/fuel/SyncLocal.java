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

    private int getRevision(@NonNull File fileRevision) {
        try {
            UtilsFileIO.checkExists(fileRevision);

            List<String> strings = new ArrayList<>();
            UtilsFileIO.read(fileRevision, strings);

            return Integer.decode(strings.get(0));
        } catch (Exception e) {
            return -1;
        }
    }

    public int getPreferencesRevision() {
        return getRevision(mSyncFiles.getLocalFilePreferencesRevision());
    }

    public int getDatabaseRevision() {
        return getRevision(mSyncFiles.getLocalFileDatabaseRevision());
    }

    private void load(@NonNull File file, @NonNull List<String> strings) throws IOException {
        UtilsFileIO.checkExists(file);
        UtilsFileIO.read(file, strings);
    }

    public void loadPreferences(@NonNull List<String> preferences) throws IOException {
        load(mSyncFiles.getLocalFilePreferences(), preferences);
    }

    public void loadDatabase(@NonNull List<String> syncRecords) throws IOException {
        load(mSyncFiles.getLocalFileDatabase(), syncRecords);
    }

    private void save(@NonNull File file, @NonNull List<String> strings) throws IOException {
        UtilsFileIO.createFile(file);
        UtilsFileIO.write(file, strings);
    }

    public void savePreferences(@NonNull List<String> preferences) throws IOException {
        save(mSyncFiles.getLocalFilePreferences(), preferences);
    }

    public void saveDatabase(@NonNull List<String> syncRecords) throws IOException {
        save(mSyncFiles.getLocalFileDatabase(), syncRecords);
    }

    private void saveRevision(@NonNull File fileRevision, int revision) throws IOException {
        UtilsFileIO.createFile(fileRevision);

        List<String> strings = new ArrayList<>();
        strings.add(String.valueOf(revision));

        UtilsFileIO.write(fileRevision, strings);
    }

    public void savePreferencesRevision(int revision) throws IOException {
        saveRevision(mSyncFiles.getLocalFilePreferencesRevision(), revision);
    }

    public void saveDatabaseRevision(int revision) throws IOException {
        saveRevision(mSyncFiles.getLocalFileDatabaseRevision(), revision);
    }

    public void makeDirs() throws IOException {
        UtilsFileIO.makeDir(mSyncFiles.getLocalDirDatabase());
        UtilsFileIO.makeDir(mSyncFiles.getLocalDirPreferences());
    }

    public void deleteFiles() throws IOException {
        UtilsFileIO.deleteFile(mSyncFiles.getLocalFilePreferences());
        UtilsFileIO.deleteFile(mSyncFiles.getLocalFilePreferencesRevision());

        UtilsFileIO.deleteFile(mSyncFiles.getLocalFileDatabase());
        UtilsFileIO.deleteFile(mSyncFiles.getLocalFileDatabaseRevision());
    }

    public void deleteLocalFileDatabase() throws IOException {
        UtilsFileIO.deleteFile(mSyncFiles.getLocalFileDatabase());
    }
}