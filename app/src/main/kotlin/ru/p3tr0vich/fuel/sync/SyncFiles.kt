package ru.p3tr0vich.fuel.sync

import android.content.Context
import ru.p3tr0vich.fuel.utils.Utils
import java.io.File

internal class SyncFiles(context: Context) {

    val localDirPreferences: File
    val localFilePreferences: File
    val localFilePreferencesRevision: File

    val serverDirPreferences: File
    val serverFilePreferences: File
    val serverFilePreferencesRevision: File

    val localDirDatabase: File
    val localFileDatabase: File
    val localFileDatabaseRevision: File

    val serverDirDatabase: File
    val serverFileDatabaseRevision: File

    init {
        val isDebuggable = Utils.isDebuggable

        val syncDir = File(context.cacheDir, DIR_SYNC)

        localDirDatabase = File(syncDir, DIR_DATABASE)
        localDirPreferences = File(syncDir, DIR_PREFERENCES)

        localFileDatabase = File(localDirDatabase, FILE_DATABASE)
        localFileDatabaseRevision = File(localDirDatabase, FILE_DATABASE_REVISION)

        serverDirDatabase = File(DIR_DATABASE + if (isDebuggable) DIR_DEBUG else "")
        serverFileDatabaseRevision = File(serverDirDatabase, FILE_DATABASE_REVISION)

        localFilePreferences = File(localDirPreferences, FILE_PREFERENCES)
        localFilePreferencesRevision = File(localDirPreferences, FILE_PREFERENCES_REVISION)

        serverDirPreferences = File(DIR_PREFERENCES + if (isDebuggable) DIR_DEBUG else "")
        serverFilePreferences = File(serverDirPreferences, FILE_PREFERENCES)
        serverFilePreferencesRevision = File(serverDirPreferences, FILE_PREFERENCES_REVISION)
    }

    fun getServerFileDatabase(revision: Int): File {
        return File(serverDirDatabase, revision.toString())
    }

    companion object {
        private const val DIR_SYNC = "sync"

        private const val DIR_PREFERENCES = "preferences"
        private const val FILE_PREFERENCES = "PREFERENCES"
        private const val FILE_PREFERENCES_REVISION = "REVISION"

        private const val DIR_DATABASE = "database"
        private const val FILE_DATABASE = "DATABASE"
        private const val FILE_DATABASE_REVISION = "REVISION"

        private const val DIR_DEBUG = "_debug"
    }
}