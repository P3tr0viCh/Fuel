package ru.p3tr0vich.fuel

import android.app.Activity
import android.app.PendingIntent
import android.app.Service
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.support.annotation.IntDef
import ru.p3tr0vich.fuel.helpers.ConnectivityHelper
import ru.p3tr0vich.fuel.helpers.PreferencesHelper
import ru.p3tr0vich.fuel.observers.DatabaseObserver
import ru.p3tr0vich.fuel.observers.PreferencesObserver
import ru.p3tr0vich.fuel.sync.SyncAccount
import ru.p3tr0vich.fuel.sync.SyncAdapter
import ru.p3tr0vich.fuel.utils.UtilsLog


/**
 * Сервис, инициализирующий наблюдатели ([android.database.ContentObserver])
 * за изменениями в базе данных и в настройках.
 * Также, используется для запуска синхронизации.
 *
 * @see DatabaseObserver
 *
 * @see PreferencesObserver
 */
class ContentObserverService : Service() {

    private val handler = Handler()

    private var databaseObserver: DatabaseObserver? = null
    private var preferencesObserver: PreferencesObserver? = null

    private val runnable = RequestSyncRunnable()

    /**
     * Объекты синхронизации.
     *
     * @see .SYNC_NONE
     * @see .SYNC_ALL
     * @see .SYNC_DATABASE
     * @see .SYNC_PREFERENCES
     */
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(SYNC_NONE, SYNC_ALL, SYNC_DATABASE, SYNC_PREFERENCES)
    annotation class Sync

    /**
     * Результат запуска синхронизации.
     *
     * @see .RESULT_REQUEST_DONE
     * @see .RESULT_SYNC_DISABLED
     * @see .RESULT_SYNC_ACTIVE
     * @see .RESULT_TOKEN_EMPTY
     * @see .RESULT_INTERNET_DISCONNECTED
     * @see .RESULT_SYNC_DELAYED_REQUEST
     */
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(RESULT_REQUEST_DONE, RESULT_SYNC_DISABLED, RESULT_SYNC_ACTIVE, RESULT_TOKEN_EMPTY, RESULT_INTERNET_DISCONNECTED, RESULT_SYNC_DELAYED_REQUEST)
    annotation class Result

    override fun onCreate() {
        if (LOG_ENABLED) {
            UtilsLog.d(TAG, "onCreate")
        }

        super.onCreate()

        databaseObserver = DatabaseObserver()
        databaseObserver?.register(this)

        preferencesObserver = PreferencesObserver()
        preferencesObserver?.register(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (LOG_ENABLED) {
            UtilsLog.d(TAG, "onStartCommand")
        }

        if (intent == null) {
            if (LOG_ENABLED) {
                UtilsLog.d(TAG, "requestSync", "intent == null")
            }
        } else {
            @Sync
            val sync = intent.getIntExtra(EXTRA_NAME_SYNC, SYNC_NONE)

            val startIfActive = intent.getBooleanExtra(EXTRA_NAME_START_IF_ACTIVE, false)

            if (LOG_ENABLED) {
                UtilsLog.d(TAG, "requestSync", "sync == $sync")
            }

            if (sync != SYNC_NONE) {
                // Отмена предыдущей задачи.
                handler.removeCallbacks(runnable)

                val pendingIntent = intent.getParcelableExtra<PendingIntent>(EXTRA_NAME_PENDING)

                // Запуск после задержки.
                if (intent.getBooleanExtra(EXTRA_NAME_WITH_DELAY, false)) {
                    if (LOG_ENABLED) {
                        UtilsLog.d(TAG, "requestSync", "delayed start")
                    }

                    val runnableSync = when (sync) {
                        SYNC_ALL -> SYNC_ALL
                        SYNC_DATABASE, SYNC_PREFERENCES -> {
                            // Если синхронизация с задержкой уже запущена,
                            // то проверяются её объекты.
                            // Например, если уже была запущена синхронизация настроек (SYNC_PREFERENCES)
                            // и новая задача запрашивает синхронизацию БД (SYNC_DATABASE),
                            // то новая синхронизация будет синхронизировать настройки и БД (SYNC_ALL).

                            if (runnable.sync != SYNC_ALL && runnable.sync != sync) {
                                if (runnable.sync == SYNC_NONE)
                                    sync
                                else
                                    SYNC_ALL
                            } else
                                runnable.sync
                        }

                        SYNC_NONE -> SYNC_NONE
                        else -> SYNC_NONE
                    }

                    runnable.sync = runnableSync
                    runnable.startIfActive = startIfActive
                    runnable.pendingIntent = pendingIntent

                    handler.postDelayed(runnable, START_SYNC_DELAY)

                    sendResult(pendingIntent, RESULT_SYNC_DELAYED_REQUEST)
                } else {
                    // Запуск синхронизации без задержки.
                    val result = performRequestSync(sync, startIfActive)

                    sendResult(pendingIntent, result)
                }
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private inner class RequestSyncRunnable : Runnable {
        @Sync
        var sync = SYNC_NONE

        var startIfActive = false

        var pendingIntent: PendingIntent? = null

        override fun run() {
            val result = performRequestSync(sync, startIfActive)

            sendResult(pendingIntent, result)

            sync = SYNC_NONE
        }
    }

    /**
     * Запускает синхронизацию.
     *
     * @param sync Объекты синхронизации ([Sync]).
     * @param startIfActive Запускать синхронизацию, даже если она уже запущена.
     * @return Результат запуска ([Result]).
     */
    @Result
    private fun performRequestSync(@Sync sync: Int, startIfActive: Boolean): Int {
        if (!PreferencesHelper.getInstance(this).isSyncEnabled) {
            if (LOG_ENABLED) {
                UtilsLog.d(TAG, "requestSync", "sync disabled")
            }

            return RESULT_SYNC_DISABLED
        }

        if (ConnectivityHelper.getConnectedState(this) == ConnectivityHelper.DISCONNECTED) {
            if (LOG_ENABLED) {
                UtilsLog.d(TAG, "requestSync", "Internet disconnected")
            }

            return RESULT_INTERNET_DISCONNECTED
        }

        val syncAccount = SyncAccount(this)

        if (syncAccount.yandexDiskToken.isNullOrEmpty()) {
            if (LOG_ENABLED) {
                UtilsLog.d(TAG, "requestSync", "Yandex.Disk token empty")
            }

            return RESULT_TOKEN_EMPTY
        }

        if (syncAccount.isSyncActive && !startIfActive) {
            if (LOG_ENABLED) {
                UtilsLog.d(TAG, "requestSync", "sync active")
            }

            return RESULT_SYNC_ACTIVE
        }

        val extras = Bundle()

        extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true)
        extras.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true)

        val syncDatabase = sync == SYNC_ALL || sync == SYNC_DATABASE
        val syncPreferences = sync == SYNC_ALL || sync == SYNC_PREFERENCES

        extras.putBoolean(SyncAdapter.SYNC_DATABASE, syncDatabase)
        extras.putBoolean(SyncAdapter.SYNC_PREFERENCES, syncPreferences)

        ContentResolver.requestSync(syncAccount.account, syncAccount.authority, extras)

        if (LOG_ENABLED) {
            UtilsLog.d(TAG, "requestSync", "request done")
        }

        return RESULT_REQUEST_DONE
    }

    /**
     * Отправляет результат в PendingIntent.
     *
     * @param pendingIntent Получатель результата.
     * @param resultCode    Результат ([Result]).
     */
    private fun sendResult(pendingIntent: PendingIntent?, @Result resultCode: Int) {
        if (pendingIntent != null) {
            val result = Intent().putExtra(EXTRA_NAME_RESULT, resultCode)

            try {
                pendingIntent.send(this, Activity.RESULT_OK, result)
            } catch (e: PendingIntent.CanceledException) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        if (LOG_ENABLED) {
            UtilsLog.d(TAG, "onDestroy")
        }

        preferencesObserver?.unregister(this)
        databaseObserver?.unregister(this)

        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        // TODO: Return the communication channel to the service.
        throw UnsupportedOperationException("Not yet implemented")
    }

    companion object {
        private const val TAG = "ContentObserverService"

        private var LOG_ENABLED = false

        private const val EXTRA_NAME_SYNC = "EXTRA_NAME_SYNC"
        private const val EXTRA_NAME_START_IF_ACTIVE = "EXTRA_NAME_START_IF_ACTIVE"
        private const val EXTRA_NAME_WITH_DELAY = "EXTRA_NAME_WITH_DELAY"

        private const val EXTRA_NAME_PENDING = "EXTRA_NAME_PENDING"
        private const val EXTRA_NAME_RESULT = "EXTRA_NAME_RESULT"

        private const val START_SYNC_DELAY = 10000L

        /**
         * Синхронизация не выполняется.
         */
        private const val SYNC_NONE = -1

        /**
         * Синхронизировать все объекты.
         */
        const val SYNC_ALL = 0

        /**
         * Синхронизировать только базу данных.
         */
        const val SYNC_DATABASE = 1

        /**
         * Синхронизировать только настройки.
         */
        const val SYNC_PREFERENCES = 2

        /**
         * Синхронизация запущена.
         */
        const val RESULT_REQUEST_DONE = -1

        /**
         * Синхронизация отключена в настройках приложения.
         */
        const val RESULT_SYNC_DISABLED = 0

        /**
         * Синхронизация выполняется.
         */
        const val RESULT_SYNC_ACTIVE = 1

        /**
         * Необходима авторизация.
         */
        const val RESULT_TOKEN_EMPTY = 2

        /**
         * Нет доступа к сети Интернет.
         */
        const val RESULT_INTERNET_DISCONNECTED = 3

        /**
         * Синхронизация будет запущена после задержки.
         */
        const val RESULT_SYNC_DELAYED_REQUEST = 4

        /**
         * Извлекает результат запуска синхронизации из интента.
         *
         * @param data Интент.
         * @return Результат запуска синхронизации ([Result]).
         */
        @Result
        fun getResult(data: Intent?): Int {
            return data?.getIntExtra(EXTRA_NAME_RESULT, RESULT_REQUEST_DONE)
                    ?: RESULT_REQUEST_DONE
        }

        /**
         * Запускает сервис.
         *
         * @param context Контекст.
         */
        fun start(context: Context) {
            UtilsLog.d(TAG, "start")
            try {
                context.startService(Intent(context, ContentObserverService::class.java))
            } catch (e: Exception) {
                UtilsLog.d(TAG, "start", "exception = ${e.message}")
            }
        }

        /**
         * Запускает синхронизацию.
         *
         * @param context           Контекст.
         * @param sync              Объекты синхронизации ([Sync]).
         * @param startIfSyncActive Запускать синхронизацию, даже если она уже запущена.
         * @param withDelay         Запустить синхронизацию после задержки
         * @param pendingIntent     Результат запуска.
         */
        fun requestSync(context: Context, @Sync sync: Int = SYNC_ALL,
                        startIfSyncActive: Boolean = false, withDelay: Boolean = false,
                        pendingIntent: PendingIntent? = null) {
            val intent = Intent(context, ContentObserverService::class.java)
                    .putExtra(EXTRA_NAME_SYNC, sync)
                    .putExtra(EXTRA_NAME_START_IF_ACTIVE, startIfSyncActive)
                    .putExtra(EXTRA_NAME_WITH_DELAY, withDelay)

            if (pendingIntent != null) {
                intent.putExtra(EXTRA_NAME_PENDING, pendingIntent)
            }

            context.startService(intent)
        }

        /**
         * Запускает синхронизацию всех объектов без задержки с возвратом результата,
         * только если синхронизация уже не запущена.
         *
         * @param context       Контекст.
         * @param pendingIntent Результат запуска.
         */
        fun requestSync(context: Context, pendingIntent: PendingIntent) {
            requestSync(context, SYNC_ALL, false, false, pendingIntent)
        }

        /**
         * Запускает синхронизацию всех объектов без задержки и без возврата результата,
         * только если синхронизация уже не запущена.
         *
         * @param context Контекст.
         */
        fun requestSync(context: Context) {
            requestSync(context, SYNC_ALL, false, false, null)
        }
    }
}