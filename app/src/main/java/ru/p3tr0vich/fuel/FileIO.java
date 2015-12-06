package ru.p3tr0vich.fuel;

import android.support.annotation.NonNull;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.List;

class FileIO {

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
}
