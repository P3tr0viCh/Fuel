package ru.p3tr0vich.fuel;

import android.content.Context;
import android.support.annotation.NonNull;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.List;

class CacheSyncHelper {

    private static final String DIR_SYNC = "sync";
    private static final String DIR_PREFERENCES = "preferences";
    private static final String FILE_PREFERENCES_REVISION = "REVISION";
    private static final String FILE_PREFERENCES = "PREFERENCES";

    private final Context mContext;

    CacheSyncHelper(Context context) {
        mContext = context;
    }

    private File getDirPreferences() {
        return new File(mContext.getCacheDir() + File.separator + DIR_SYNC +
                File.separator + DIR_PREFERENCES);
    }

    public int getRevision() {
        File fileRevision = new File(getDirPreferences(), FILE_PREFERENCES_REVISION);

        if (!fileRevision.exists() || !fileRevision.isFile()) {
            Functions.logD("CacheSyncHelper -- getRevision: fileRevision not exists");
            return -1;
        }

        FileInputStream fileInputStream;
        try {
            fileInputStream = new FileInputStream(fileRevision);
        } catch (FileNotFoundException e) {
            return -1;
        }

        InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);

        try {
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            String string = bufferedReader.readLine();

            bufferedReader.close();

            inputStreamReader.close();

            fileInputStream.close();

            return Integer.decode(string);
        } catch (IOException e) {
            Functions.logD("CacheSyncHelper -- getRevision: IOException");
            return -1;
        }
    }

    public boolean loadPreferences(@NonNull List<String> preferences) {
        File filePreferences = new File(getDirPreferences(), FILE_PREFERENCES);

        if (!filePreferences.exists() || !filePreferences.isFile()) {
            Functions.logD("CacheSyncHelper -- getRevision: fileRevision not exists");
            return false;
        }

        FileInputStream fileInputStream;
        try {
            fileInputStream = new FileInputStream(filePreferences);
        } catch (FileNotFoundException e) {
            return false;
        }

        InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);

        try {
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            String string = bufferedReader.readLine();
            while (string != null) {
                preferences.add(string);
                string = bufferedReader.readLine();
            }

            bufferedReader.close();

            inputStreamReader.close();

            fileInputStream.close();

            return true;
        } catch (IOException e) {
            Functions.logD("CacheSyncHelper -- loadPreferences: IOException");
            return false;
        }
    }

    public boolean savePreferences(@NonNull List<String> preferences) {
        File dirPreferences = getDirPreferences();

        if (!dirPreferences.exists()) if (!dirPreferences.mkdirs()) {
            Functions.logD("CacheSyncHelper -- savePreferences: can not create DIR_PREFERENCES");
            return false;
        }

        File filePreferences = new File(dirPreferences, FILE_PREFERENCES);

        try {
            if (!filePreferences.createNewFile()) if (!filePreferences.isFile()) {
                Functions.logD("CacheSyncHelper -- savePreferences: can not create FILE_PREFERENCES");
                return false;
            }
        } catch (IOException e) {
            return false;
        }

        FileOutputStream fileOutputStream;
        try {
            fileOutputStream = new FileOutputStream(filePreferences);
        } catch (FileNotFoundException e) {
            return false;
        }

        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);

        BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);

        try {
            for (String value : preferences) {
                bufferedWriter.write(value);
                bufferedWriter.newLine();
            }

            bufferedWriter.close();

            outputStreamWriter.close();

            fileOutputStream.close();

            return true;
        } catch (IOException e) {
            Functions.logD("CacheSyncHelper -- savePreferences: IOException");
            return false;
        }
    }

    public boolean saveRevision(int revision) {
        File dirPreferences = getDirPreferences();

        if (!dirPreferences.exists()) if (!dirPreferences.mkdirs()) {
            Functions.logD("CacheSyncHelper -- saveRevision: can not create DIR_PREFERENCES");
            return false;
        }

        File filePreferences = new File(dirPreferences, FILE_PREFERENCES_REVISION);

        try {
            if (!filePreferences.createNewFile()) if (!filePreferences.isFile()) {
                Functions.logD("CacheSyncHelper -- saveRevision: can not create FILE_PREFERENCES_REVISION");
                return false;
            }
        } catch (IOException e) {
            return false;
        }

        FileOutputStream fileOutputStream;
        try {
            fileOutputStream = new FileOutputStream(filePreferences);
        } catch (FileNotFoundException e) {
            return false;
        }

        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);

        BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);

        try {
            bufferedWriter.write(String.valueOf(revision));

            bufferedWriter.close();

            outputStreamWriter.close();

            fileOutputStream.close();

            return true;
        } catch (IOException e) {
            Functions.logD("CacheSyncHelper -- saveRevision: IOException");
            return false;
        }
    }
}