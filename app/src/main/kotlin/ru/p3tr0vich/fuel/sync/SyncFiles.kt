package ru.p3tr0vich.fuel.sync

import android.content.Context
import ru.p3tr0vich.fuel.utils.Utils
import java.io.File

internal class SyncFiles(context: Context) {

    private val isDebuggable = Utils.isDebuggable

    private val syncDir = File(context.cacheDir, DIR_SYNC)

    val localDirDatabase = File(syncDir, DIR_DATABASE)
    val localFileDatabase = File(localDirDatabase, FILE_DATABASE)
    val localFileDatabaseRevision = File(localDirDatabase, FILE_DATABASE_REVISION)

    val localDirPreferences = File(syncDir, DIR_PREFERENCES)
    val localFilePreferences = File(localDirPreferences, FILE_PREFERENCES)
    val localFilePreferencesRevision = File(localDirPreferences, FILE_PREFERENCES_REVISION)

    val serverDirPreferences = File(DIR_PREFERENCES + if (isDebuggable) DIR_DEBUG else "")
    val serverFilePreferences = File(serverDirPreferences, FILE_PREFERENCES)
    val serverFilePreferencesRevision = File(serverDirPreferences, FILE_PREFERENCES_REVISION)

    val serverDirDatabase = File(DIR_DATABASE + if (isDebuggable) DIR_DEBUG else "")
    val serverFileDatabaseRevision = File(serverDirDatabase, FILE_DATABASE_REVISION)

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