package ru.p3tr0vich.fuel.utils;

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

public class UtilsFileIO {

    public static void read(@NonNull File file, @NonNull List<String> strings) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(file);

        InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);

        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

        String string = bufferedReader.readLine();
        while (string != null) {
            strings.add(string);
            string = bufferedReader.readLine();
        }

        bufferedReader.close();

        inputStreamReader.close();

        fileInputStream.close();
    }

    public static void write(@NonNull File file, @NonNull List<String> strings) throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(file);

        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);

        BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);

        for (String s : strings) {
            bufferedWriter.write(s);
            bufferedWriter.newLine();
        }

        bufferedWriter.close();

        outputStreamWriter.close();

        fileOutputStream.close();
    }

    public static void makeDir(@NonNull File dir) throws IOException {
        if (dir.exists()) return;

        if (!dir.mkdirs())
            throw new IOException("UtilsFileIO -- makeDir: can not create dir " + dir.toString());
    }

    public static void deleteFile(@NonNull File file) throws IOException {
        if (!file.exists()) return;

        if (!file.delete())
            throw new IOException("UtilsFileIO -- deleteFile: can not delete file " + file.toString());
    }

    public static void createFile(@NonNull File file) throws IOException {
        if (file.exists()) {
            if (!file.isFile())
                throw new FileNotFoundException("UtilsFileIO -- createFile: " + file.toString()
                        + " exists and is not a file");
        } else
            //noinspection ResultOfMethodCallIgnored
            file.createNewFile();
    }

    public static void checkExists(@NonNull File file) throws FileNotFoundException {
        if (!file.exists() || !file.isFile())
            throw new FileNotFoundException("UtilsFileIO -- checkExists: " + file.toString() +
                    " not exists or not is a file");
    }
}