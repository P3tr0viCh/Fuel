package ru.p3tr0vich.fuel.utils

import java.io.*

object UtilsFileIO {

    @JvmStatic
    @Throws(IOException::class)
    fun read(file: File, strings: MutableList<String>) {
        val fileInputStream = FileInputStream(file)

        val inputStreamReader = InputStreamReader(fileInputStream)

        val bufferedReader = BufferedReader(inputStreamReader)

        var s: String? = bufferedReader.readLine()
        while (s != null) {
            strings.add(s)
            s = bufferedReader.readLine()
        }

        bufferedReader.close()

        inputStreamReader.close()

        fileInputStream.close()
    }

    @JvmStatic
    @Throws(IOException::class)
    fun write(file: File, strings: List<String>) {
        val fileOutputStream = FileOutputStream(file)

        val outputStreamWriter = OutputStreamWriter(fileOutputStream)

        val bufferedWriter = BufferedWriter(outputStreamWriter)

        for (s in strings) {
            bufferedWriter.write(s)
            bufferedWriter.newLine()
        }

        bufferedWriter.close()

        outputStreamWriter.close()

        fileOutputStream.close()
    }

    @JvmStatic
    @Throws(IOException::class)
    fun makeDir(dir: File) {
        if (dir.exists()) {
            return
        }

        if (!dir.mkdirs()) {
            throw IOException("UtilsFileIO -- makeDir: can not create dir " + dir.toString())
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun deleteFile(file: File) {
        if (!file.exists()) {
            return
        }

        if (!file.delete()) {
            throw IOException("UtilsFileIO -- deleteFile: can not delete file " + file.toString())
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun createFile(file: File) {
        if (file.exists()) {
            if (!file.isFile) {
                throw FileNotFoundException("UtilsFileIO -- createFile: " + file.toString()
                        + " exists and is not a file")
            }
        } else {
            file.createNewFile()
        }
    }

    @JvmStatic
    @Throws(FileNotFoundException::class)
    fun checkExists(file: File) {
        if (!file.exists() || !file.isFile) {
            throw FileNotFoundException("UtilsFileIO -- checkExists: " + file.toString() +
                    " not exists or not is a file")
        }
    }
}