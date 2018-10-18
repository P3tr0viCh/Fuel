package ru.p3tr0vich.fuel.sync

import com.yandex.disk.rest.Credentials
import com.yandex.disk.rest.RestClient
import com.yandex.disk.rest.exceptions.ServerException
import com.yandex.disk.rest.exceptions.ServerIOException
import com.yandex.disk.rest.exceptions.http.HttpCodeException
import ru.p3tr0vich.fuel.BuildConfig
import ru.p3tr0vich.fuel.utils.UtilsFileIO
import ru.p3tr0vich.fuel.utils.UtilsLog
import java.io.File
import java.io.IOException

class SyncYandexDisk internal constructor(private val syncFiles: SyncFiles, token: String) {

    private val restClient = RestClient(Credentials("", token))

    object URL {
        const val WWW = "https://disk.yandex.ru/client/disk"

        const val AUTH = "https://oauth.yandex.ru/authorize?response_type=token&client_id=" + BuildConfig.YANDEX_DISK_CLIENT_ID
    }

    @Throws(IOException::class, ServerException::class)
    private fun save(serverFile: File, localFile: File) {
        val link = restClient.getUploadLink(APP_DIR + serverFile.path, true)

        restClient.uploadFile(link, false, localFile, null)
    }

    @Throws(IOException::class, ServerException::class)
    private fun load(serverFile: File, localFile: File) {
        UtilsFileIO.deleteFile(localFile)

        restClient.downloadFile(APP_DIR + serverFile.path, localFile, null)
    }

    @Throws(IOException::class, ServerIOException::class)
    private fun makeDir(dir: File) {
        try {
            restClient.makeFolder(APP_DIR + dir.name)
        } catch (e: HttpCodeException) {
            if (e.code != HTTP_CODE_DIR_EXISTS) throw e
        }
    }

    @Throws(IOException::class, ServerIOException::class)
    fun makeDirs() {
        makeDir(syncFiles.serverDirDatabase)
        makeDir(syncFiles.serverDirPreferences)
    }

    @Throws(IOException::class, ServerException::class)
    private fun delete(`object`: File) {
        try {
            val link = restClient.delete(APP_DIR + `object`.path, true)

            if ("GET" == link.method) {
                restClient.waitProgress(link) {
                    UtilsLog.d("SyncYandexDisk", "wait until deleting object...")
                }
            }
        } catch (e: HttpCodeException) {
            if (e.code != HTTP_CODE_RESOURCE_NOT_FOUND) throw e
        }
    }

    @Throws(IOException::class, ServerException::class)
    fun deleteDirDatabase() {
        delete(syncFiles.serverDirDatabase)
    }

    @Throws(IOException::class, ServerException::class)
    fun savePreferencesRevision() {
        save(syncFiles.serverFilePreferencesRevision,
                syncFiles.localFilePreferencesRevision)
    }

    @Throws(IOException::class, ServerException::class)
    fun saveDatabaseRevision() {
        save(syncFiles.serverFileDatabaseRevision,
                syncFiles.localFileDatabaseRevision)
    }

    @Throws(IOException::class, ServerException::class)
    private fun loadRevision(serverFile: File, localFile: File) {
        try {
            load(serverFile, localFile)
        } catch (e: HttpCodeException) {
            if (e.code != HTTP_CODE_RESOURCE_NOT_FOUND) throw e
        }
    }

    @Throws(IOException::class, ServerException::class)
    fun loadPreferencesRevision() {
        loadRevision(syncFiles.serverFilePreferencesRevision, syncFiles.localFilePreferencesRevision)
    }

    @Throws(IOException::class, ServerException::class)
    fun loadDatabaseRevision() {
        loadRevision(syncFiles.serverFileDatabaseRevision, syncFiles.localFileDatabaseRevision)
    }

    @Throws(IOException::class, ServerException::class)
    fun saveDatabase(revision: Int) {
        save(syncFiles.getServerFileDatabase(revision), syncFiles.localFileDatabase)
    }

    @Throws(IOException::class, ServerException::class)
    fun loadDatabase(revision: Int): Boolean {
        return try {
            load(syncFiles.getServerFileDatabase(revision), syncFiles.localFileDatabase)

            true
        } catch (e: HttpCodeException) {
            if (e.code != HTTP_CODE_RESOURCE_NOT_FOUND) throw e

            false
        }

    }

    @Throws(IOException::class, ServerException::class)
    fun savePreferences() {
        save(syncFiles.serverFilePreferences, syncFiles.localFilePreferences)
    }

    @Throws(IOException::class, ServerException::class)
    fun loadPreferences() {
        load(syncFiles.serverFilePreferences, syncFiles.localFilePreferences)
    }

    companion object {
        const val PATTERN_ACCESS_TOKEN = "access_token=(.*?)(&|$)"

        private const val APP_DIR = "app:/"

        const val HTTP_CODE_UNAUTHORIZED = 401
        private const val HTTP_CODE_RESOURCE_NOT_FOUND = 404
        private const val HTTP_CODE_DIR_EXISTS = 409
    }
}