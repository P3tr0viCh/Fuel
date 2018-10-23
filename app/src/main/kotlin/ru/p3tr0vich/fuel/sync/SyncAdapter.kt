package ru.p3tr0vich.fuel.sync

import android.accounts.Account
import android.content.*
import android.nfc.FormatException
import android.os.Bundle
import android.os.RemoteException
import android.text.TextUtils
import com.yandex.disk.rest.exceptions.ServerException
import com.yandex.disk.rest.exceptions.ServerIOException
import com.yandex.disk.rest.exceptions.http.HttpCodeException
import ru.p3tr0vich.fuel.helpers.ContentProviderHelper
import ru.p3tr0vich.fuel.utils.UtilsLog
import java.io.IOException
import java.util.*

internal class SyncAdapter(context: Context) : AbstractThreadedSyncAdapter(context, true) {

    private var syncProviderDatabase: SyncProviderDatabase? = null
    private var syncProviderPreferences: SyncProviderPreferences? = null

    private var syncAccount: SyncAccount? = null

    private var syncLocal: SyncLocal? = null
    private var syncYandexDisk: SyncYandexDisk? = null

    override fun onPerformSync(account: Account, extras: Bundle, authority: String,
                               provider: ContentProviderClient, syncResult: SyncResult) {
        if (LOG_ENABLED) {
            UtilsLog.d(TAG, "onPerformSync", "start")
        }

        try { // finally
            syncProviderDatabase = SyncProviderDatabase(provider)
            syncProviderPreferences = SyncProviderPreferences(context, provider)

            syncAccount = SyncAccount(context)

            val yandexDiskToken = syncAccount!!.yandexDiskToken

            if (TextUtils.isEmpty(yandexDiskToken)) {
                syncResult.stats.numAuthExceptions++

                if (LOG_ENABLED) {
                    UtilsLog.d(TAG, "onPerformSync", "error  == empty Yandex.Disk token")
                }

                return
            }

            val syncFiles = SyncFiles(context)

            syncLocal = SyncLocal(syncFiles)

            syncYandexDisk = SyncYandexDisk(syncFiles, yandexDiskToken!!)

            try { // catch
                syncLocal!!.makeDirs()

                try {
                    if (extras.getBoolean(SYNC_DATABASE, true)) {
                        syncDatabase()
                    }

                    if (extras.getBoolean(SYNC_PREFERENCES, true)) {
                        syncPreferences()
                    }
                } finally {
                    try {
                        syncLocal!!.deleteFiles()
                    } catch (e: IOException) {
                        handleException(e, syncResult)
                    }

                }
            } catch (e: Exception) {
                handleException(e, syncResult)
            }
        } finally {
            try {
                syncProviderPreferences!!.putLastSync(System.currentTimeMillis(), syncResult.hasError())
            } catch (e: RemoteException) {
                handleException(e, syncResult)
            }

            if (LOG_ENABLED || syncResult.hasError()) {
                UtilsLog.d(TAG, "onPerformSync", "finish" + if (syncResult.hasError()) ", errors == " + syncResult.toString() else ", all ok")
            }

            if (extras.getBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, false)) {
                syncResult.clear()
            }

            syncYandexDisk = null
            syncLocal = null
            syncAccount = null
            syncProviderPreferences = null
            syncProviderDatabase = null
        }
    }

    private fun handleException(e: Exception, syncResult: SyncResult) {
        when (e) {
            is RemoteException -> syncResult.databaseError = true
            is IOException -> syncResult.stats.numIoExceptions++
            is FormatException -> syncResult.stats.numParseExceptions++
            is HttpCodeException -> {
                if (e.code == SyncYandexDisk.HTTP_CODE_UNAUTHORIZED) {
                    syncResult.stats.numAuthExceptions++
                    syncAccount!!.yandexDiskToken = null
                } else {
                    syncResult.stats.numIoExceptions++
                }
            }
            is ServerIOException -> syncResult.stats.numIoExceptions++
            is ServerException -> syncResult.stats.numIoExceptions++
            else -> syncResult.databaseError = true
        }

        UtilsLog.d(TAG, "handleException", "error  == $e")
    }

    @Throws(IOException::class, ServerException::class, RemoteException::class, FormatException::class)
    private fun syncPreferences() {
        if (LOG_ENABLED) {
            UtilsLog.d(TAG, "syncPreferences", "start")
        }

        try {
            // Получить файл с номером ревизии с сервера и сохранить в папку кэша.
            // Если файла нет, игнорировать.
            syncYandexDisk!!.loadPreferencesRevision()

            if (LOG_ENABLED) {
                UtilsLog.d(TAG, "syncPreferences", "syncYandexDisk.loadPreferencesRevision() OK")
            }

            val serverRevision = syncLocal!!.preferencesRevision
            // serverRevision == -1, если синхронизация не производилась
            // или файлы синхронизации отсутствуют на сервере.

            var localRevision = syncProviderPreferences!!.preferencesRevision
            // localRevision == -1, если программа запускается первый раз.

            val isChanged = syncProviderPreferences!!.isChanged

            if (LOG_ENABLED) {
                UtilsLog.d(TAG, "syncPreferences",
                        "serverRevision == $serverRevision, localRevision == $localRevision , preference changed == $isChanged")
            }

            if (localRevision < serverRevision) {
                // Синхронизация уже выполнялась на другом устройстве.
                // Текущие изменения теряются.

                if (LOG_ENABLED) {
                    UtilsLog.d(TAG, "syncPreferences", "localRevision < serverRevision")
                }

                syncPreferencesLoad(serverRevision)
            } else {
                if (localRevision > serverRevision) {
                    // Файлы синхронизации были удалены
                    // (localRevision > -1 > serverRevision == -1).

                    if (LOG_ENABLED) {
                        UtilsLog.d(TAG, "syncPreferences", "localRevision > serverRevision")
                    }

                    syncPreferencesSave(localRevision)
                } else
                /* localRevision == serverRevision */ {
                    // 1. Сихронизация выполняется в первый раз
                    // (localRevision == -1, serverRevision == -1, changed == true).
                    // 2. Настройки синхронизированы.
                    // Если настройки были изменены, сохранить их на сервер.

                    if (LOG_ENABLED) {
                        UtilsLog.d(TAG, "syncPreferences", "localRevision == serverRevision")
                    }

                    if (isChanged) {
                        localRevision++

                        syncPreferencesSave(localRevision)
                    }
                }
            }
        } finally {
            if (LOG_ENABLED) {
                UtilsLog.d(TAG, "syncPreferences", "finish")
            }
        }
    }

    @Throws(IOException::class, RemoteException::class, FormatException::class, ServerException::class)
    private fun syncPreferencesSave(revision: Int) {
        // 1) Сохранить настройки в файл в папке кэша.
        // 2) Сохранить номер ревизии в файл в папке кэша.
        // 3) Передать файл настроек из папки кэша на сервер.
        // 4) Передать файл с номером ревизии из папки кэша на сервер.
        // 5) Сохранить флаг изменения настроек и номер ревизии в настройках.

        if (LOG_ENABLED) {
            UtilsLog.d(TAG, "syncPreferencesSave", "start")
        }

        val preferences = syncProviderPreferences!!.preferences
        if (LOG_ENABLED) {
            UtilsLog.d(TAG, "syncPreferencesSave", "syncProviderPreferences.preferences OK")
        }

        syncLocal!!.savePreferences(preferences)
        if (LOG_ENABLED) {
            UtilsLog.d(TAG, "syncPreferencesSave", "syncLocal.savePreferences() OK")
        }

        syncLocal!!.savePreferencesRevision(revision)
        if (LOG_ENABLED) {
            UtilsLog.d(TAG, "syncPreferencesSave", "syncLocal.savePreferencesRevision() OK")
        }

        syncYandexDisk!!.makeDirs()
        if (LOG_ENABLED) {
            UtilsLog.d(TAG, "syncPreferencesSave", "syncYandexDisk.makeDirs() OK")
        }

        syncYandexDisk!!.savePreferences()
        if (LOG_ENABLED) {
            UtilsLog.d(TAG, "syncPreferencesSave", "syncYandexDisk.savePreferences() OK")
        }

        syncYandexDisk!!.savePreferencesRevision()
        if (LOG_ENABLED) {
            UtilsLog.d(TAG, "syncPreferencesSave", "syncYandexDisk.savePreferencesRevision() OK")
        }

        syncProviderPreferences!!.isChanged = false
        syncProviderPreferences!!.preferencesRevision = revision

        if (LOG_ENABLED) {
            UtilsLog.d(TAG, "syncPreferencesSave", "finish")
        }
    }

    @Throws(IOException::class, RemoteException::class, ServerException::class)
    private fun syncPreferencesLoad(revision: Int) {
        // 1) Получить файл настроек с сервера и сохранить в папку кэша.
        // 2) Прочитать значения из файла в папке кэша.
        // 3) Сохранить полученные значения в настройках.
        // 4) Сохранить флаг изменения настроек и номер ревизии в настройках.

        if (LOG_ENABLED) {
            UtilsLog.d(TAG, "syncPreferencesLoad", "start")
        }

        syncYandexDisk!!.loadPreferences()
        if (LOG_ENABLED) {
            UtilsLog.d(TAG, "syncPreferencesLoad", "syncYandexDisk.loadPreferences() OK")
        }

        val preferences = ArrayList<String>()

        syncLocal!!.loadPreferences(preferences)
        if (LOG_ENABLED) {
            UtilsLog.d(TAG, "syncPreferencesLoad", "syncLocal.loadPreferences() OK")
        }

        syncProviderPreferences!!.preferences = preferences
        if (LOG_ENABLED) {
            UtilsLog.d(TAG, "syncPreferencesLoad", "syncProviderPreferences.setPreferences() OK")
        }

        syncProviderPreferences!!.isChanged = false
        syncProviderPreferences!!.preferencesRevision = revision

        if (LOG_ENABLED) {
            UtilsLog.d(TAG, "syncPreferencesLoad", "finish")
        }
    }

    @Throws(IOException::class, ServerException::class, RemoteException::class, FormatException::class)
    private fun syncDatabase() {
        if (LOG_ENABLED) {
            UtilsLog.d(TAG, "syncDatabase", "start")
        }

        try {
            val localRevision = syncProviderPreferences!!.databaseRevision
            // localRevision == -1, если программа запускается первый раз.

            // Получить файл с номером ревизии с сервера и сохранить в папку кэша.
            // Если файла нет, игнорировать.
            syncYandexDisk!!.loadDatabaseRevision()
            if (LOG_ENABLED) {
                UtilsLog.d(TAG, "syncDatabase", "syncYandexDisk.loadDatabaseRevision() OK")
            }

            val serverRevision = syncLocal!!.databaseRevision
            // serverRevision == -1, если синхронизация не производилась
            // или файлы синхронизации отсутствуют на сервере.

            if (LOG_ENABLED) {
                UtilsLog.d(TAG, "syncDatabase", "serverRevision == $serverRevision, localRevision == $localRevision")
            }

            // Проверить, что данные на этом устройстве были загружены из резервной копии.
            val isFullSyncToServer = syncProviderPreferences!!.isDatabaseFullSync

            //            syncDatabaseSave(localRevision, false);
            //            syncDatabaseLoad(-1, serverRevision);

            if (isFullSyncToServer) {
                if (LOG_ENABLED) {
                    UtilsLog.d(TAG, "syncDatabase", "isFullSyncToServer == true")
                }

                syncDatabaseFullSave(serverRevision)
            } else {
                if (localRevision < serverRevision) {
                    // Синхронизация уже выполнялась на другом устройстве.
                    // Загрузить записи с сервера.
                    // Если есть изменённые или удалённые записи, сохранить их на сервер.

                    if (LOG_ENABLED) {
                        UtilsLog.d(TAG, "syncDatabase", "localRevision < serverRevision")
                    }

                    syncDatabaseLoad(localRevision, serverRevision)

                    syncDatabaseSave(serverRevision, false, false)
                } else {
                    if (localRevision > serverRevision) {
                        // Файлы синхронизации были удалены (localRevision > -1 > serverRevision == -1).
                        // Сохранить все записи на сервер.

                        if (LOG_ENABLED) {
                            UtilsLog.d(TAG, "syncDatabase", "localRevision > serverRevision")
                        }

                        syncDatabaseSave(localRevision, true, false)
                    } else
                    /* localRevision == serverRevision */ {
                        // 1. Сихронизация выполняется в первый раз
                        // (localRevision == -1, serverRevision == -1).
                        // Сохранить все записи на сервер.
                        // 2. БД синхронизирована.
                        // Если есть изменённые или удалённые записи, сохранить их на сервер.

                        if (LOG_ENABLED) {
                            UtilsLog.d(TAG, "syncDatabase", "localRevision == serverRevision")
                        }

                        syncDatabaseSave(localRevision, serverRevision == -1, false)
                    }
                }
            }
        } finally {
            if (LOG_ENABLED) {
                UtilsLog.d(TAG, "syncDatabase", "finish")
            }
        }
    }

    @Throws(IOException::class, ServerException::class, RemoteException::class)
    private fun syncDatabaseFullSave(revision: Int) {
        // 1) Удалить все файлы с сервера.
        // 2) Сохранить полную копию БД с признаком полного обновления БД на сервер.
        // 3) Удалить признак загрузки из настроек.

        if (LOG_ENABLED) {
            UtilsLog.d(TAG, "syncDatabaseFullSave", "start")
        }

        syncYandexDisk!!.deleteDirDatabase()
        if (LOG_ENABLED) {
            UtilsLog.d(TAG, "syncDatabaseFullSave", "syncYandexDisk.deleteDirDatabase() OK")
        }

        syncDatabaseSave(revision, true, true)

        syncProviderPreferences!!.isDatabaseFullSync = false

        if (LOG_ENABLED) {
            UtilsLog.d(TAG, "syncDatabaseFullSave", "finish")
        }
    }

    @Throws(IOException::class, RemoteException::class, ServerException::class)
    private fun syncDatabaseSave(revision: Int, saveAllRecords: Boolean, addDeleteAll: Boolean) {
        var rev = revision
        // 1) Сохранить БД в файл в папке кэша.
        // 1.1) Если "getAllRecords", выбрать все записи, иначе выбрать изменённые и удалённые записи.
        // 1.2) Если "addDeleteAll", добавить признак очистки БД перед добавлением записей с сервера.
        // 1.3) Если записи есть, увеличить номер ревизии на единицу и сохранить БД на сервер.
        // 2) Сохранить номер ревизии в файл в папке кэша.
        // 3) Передать файл БД из папки кэша на сервер.
        // 4) Передать файл с номером ревизии из папки кэша на сервер.
        // 5) Удалить записи, отмеченные как удалённые.
        // 6) Отметить изменённые записи как не изменённые.
        // 7) Сохранить номер ревизии БД в настройках.

        if (LOG_ENABLED) {
            UtilsLog.d(TAG, "syncDatabaseSave", "start")
        }

        val syncRecords = syncProviderDatabase!!.getSyncRecords(saveAllRecords, addDeleteAll)
        if (LOG_ENABLED) {
            UtilsLog.d(TAG, "syncDatabaseSave",
                    "syncProviderDatabase.getSyncRecords(saveAllRecords == $saveAllRecords, addDeleteAll == $addDeleteAll) OK")
        }

        val size = syncRecords.size

        if (LOG_ENABLED) {
            UtilsLog.d(TAG, "syncDatabaseSave", "records count == $size")
        }

        //        for (String record : syncRecords) if (LOG_ENABLED) UtilsLog.d(TAG, record);

        if (size != 0) {
            rev++

            syncLocal!!.saveDatabase(syncRecords)
            if (LOG_ENABLED) {
                UtilsLog.d(TAG, "syncDatabaseSave", "syncLocal.saveDatabase() OK")
            }

            syncLocal!!.saveDatabaseRevision(rev)
            if (LOG_ENABLED) {
                UtilsLog.d(TAG, "syncDatabaseSave", "syncLocal.saveDatabaseRevision() OK")
            }

            syncYandexDisk!!.makeDirs()
            if (LOG_ENABLED) {
                UtilsLog.d(TAG, "syncDatabaseSave", "syncYandexDisk.makeDirs() OK")
            }

            syncYandexDisk!!.saveDatabase(rev)
            if (LOG_ENABLED) {
                UtilsLog.d(TAG, "syncDatabaseSave", "syncYandexDisk.saveDatabase() OK")
            }

            syncYandexDisk!!.saveDatabaseRevision()
            if (LOG_ENABLED) {
                UtilsLog.d(TAG, "syncDatabaseSave", "syncYandexDisk.saveDatabaseRevision() OK")
            }

            syncProviderDatabase!!.syncDeletedRecords()
            if (LOG_ENABLED) {
                UtilsLog.d(TAG, "syncDatabaseSave", "syncProviderDatabase.syncDeletedRecords() OK")
            }

            syncProviderDatabase!!.syncChangedRecords()
            if (LOG_ENABLED) {
                UtilsLog.d(TAG, "syncDatabaseSave", "syncProviderDatabase.syncChangedRecords() OK")
            }

            syncProviderPreferences!!.databaseRevision = rev
        }

        if (LOG_ENABLED) {
            UtilsLog.d(TAG, "syncDatabaseSave", "finish")
        }
    }

    @Throws(IOException::class, ServerException::class, RemoteException::class, FormatException::class)
    private fun syncDatabaseLoad(localRevision: Int, serverRevision: Int) {
        // 1) Получить файлы БД с сервера и сохранить в папку кэша.
        // 2) Прочитать записи из файлов в папке кэша.
        // 3) Сохранить полученные значения в БД.
        // 4) Сохранить номер ревизии БД в настройках.

        if (LOG_ENABLED) {
            UtilsLog.d(TAG, "syncDatabaseLoad", "start")
        }

        val syncRecords = ArrayList<String>()

        var loadResult: Boolean

        for (revision in localRevision + 1..serverRevision) {
            loadResult = syncYandexDisk!!.loadDatabase(revision)
            if (LOG_ENABLED) {
                UtilsLog.d(TAG, "syncDatabaseLoad", "syncYandexDisk.loadDatabase(revision == $revision , loadResult == $loadResult) OK")
            }

            if (loadResult) {
                syncLocal!!.loadDatabase(syncRecords)
                if (LOG_ENABLED) {
                    UtilsLog.d(TAG, "syncDatabaseLoad", "syncLocal.loadDatabase() OK")
                }
            }
        }

        //        for (String record : syncRecords) if (LOG_ENABLED) UtilsLog.d(TAG, record);

        syncProviderDatabase!!.updateDatabase(syncRecords)
        if (LOG_ENABLED) {
            UtilsLog.d(TAG, "syncDatabaseLoad", "syncProviderDatabase.updateDatabase() OK")
        }

        syncProviderPreferences!!.databaseRevision = serverRevision

        context.contentResolver.notifyChange(ContentProviderHelper.URI_DATABASE_SYNC, null, false)

        if (LOG_ENABLED) {
            UtilsLog.d(TAG, "syncDatabaseLoad", "finish")
        }
    }

    companion object {
        private const val TAG = "SyncAdapter"

        private var LOG_ENABLED = false

        const val SYNC_DATABASE = "SYNC_DATABASE"
        const val SYNC_PREFERENCES = "SYNC_PREFERENCES"
    }
}