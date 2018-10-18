package ru.p3tr0vich.fuel.sync

import ru.p3tr0vich.fuel.utils.UtilsFileIO
import ru.p3tr0vich.fuel.utils.UtilsLog
import java.io.File
import java.io.IOException
import java.util.*

internal class SyncLocal(private val syncFiles: SyncFiles) {

    val preferencesRevision: Int
        get() = getRevision(syncFiles.localFilePreferencesRevision)

    val databaseRevision: Int
        get() = getRevision(syncFiles.localFileDatabaseRevision)

    @Throws(IOException::class)
    private fun load(file: File, strings: MutableList<String>) {
        UtilsFileIO.checkExists(file)
        UtilsFileIO.read(file, strings)
    }

    @Throws(IOException::class)
    private fun save(file: File, strings: List<String>) {
        UtilsFileIO.createFile(file)
        UtilsFileIO.write(file, strings)
    }

    private fun getRevision(fileRevision: File): Int {
        val strings = ArrayList<String>()

        return try {
            load(fileRevision, strings)

            Integer.decode(strings[0])
        } catch (e: Exception) {
            UtilsLog.d(TAG, "getRevision", "addAccountExplicitly == false")
            -1
        }
    }

    @Throws(IOException::class)
    fun loadPreferences(preferences: MutableList<String>) {
        load(syncFiles.localFilePreferences, preferences)
    }

    @Throws(IOException::class)
    fun loadDatabase(syncRecords: MutableList<String>) {
        load(syncFiles.localFileDatabase, syncRecords)
    }

    @Throws(IOException::class)
    fun savePreferences(preferences: List<String>) {
        save(syncFiles.localFilePreferences, preferences)
    }

    @Throws(IOException::class)
    fun saveDatabase(syncRecords: List<String>) {
        save(syncFiles.localFileDatabase, syncRecords)
    }

    @Throws(IOException::class)
    private fun saveRevision(fileRevision: File, revision: Int) {
        val strings = ArrayList<String>()

        strings.add(revision.toString())

        save(fileRevision, strings)
    }

    @Throws(IOException::class)
    fun savePreferencesRevision(revision: Int) {
        saveRevision(syncFiles.localFilePreferencesRevision, revision)
    }

    @Throws(IOException::class)
    fun saveDatabaseRevision(revision: Int) {
        saveRevision(syncFiles.localFileDatabaseRevision, revision)
    }

    @Throws(IOException::class)
    fun makeDirs() {
        UtilsFileIO.makeDir(syncFiles.localDirDatabase)
        UtilsFileIO.makeDir(syncFiles.localDirPreferences)
    }

    @Throws(IOException::class)
    fun deleteFiles() {
        UtilsFileIO.deleteFile(syncFiles.localFilePreferences)
        UtilsFileIO.deleteFile(syncFiles.localFilePreferencesRevision)

        UtilsFileIO.deleteFile(syncFiles.localFileDatabase)
        UtilsFileIO.deleteFile(syncFiles.localFileDatabaseRevision)
    }

    companion object {
        private const val TAG = "SyncLocal"
    }
}