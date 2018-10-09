package ru.p3tr0vich.fuel;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import ru.p3tr0vich.fuel.helpers.ConnectivityHelper;
import ru.p3tr0vich.fuel.helpers.PreferencesHelper;
import ru.p3tr0vich.fuel.observers.DatabaseObserver;
import ru.p3tr0vich.fuel.observers.PreferencesObserver;
import ru.p3tr0vich.fuel.utils.UtilsLog;

/**
 * Сервис, инициализирующий наблюдатели ({@link android.database.ContentObserver})
 * за изменениями в базе данных и в настройках.
 * Также, используется для запуска синхронизации.
 *
 * @see DatabaseObserver
 * @see PreferencesObserver
 */
public class ContentObserverService extends Service {

    private static final String TAG = "ContentObserverService";

    private static final boolean LOG_ENABLED = false;

    private static final String EXTRA_NAME_SYNC = "EXTRA_NAME_SYNC";
    private static final String EXTRA_NAME_START_IF_ACTIVE = "EXTRA_NAME_START_IF_ACTIVE";
    private static final String EXTRA_NAME_WITH_DELAY = "EXTRA_NAME_WITH_DELAY";

    private static final String EXTRA_NAME_PENDING = "EXTRA_NAME_PENDING";
    private static final String EXTRA_NAME_RESULT = "EXTRA_NAME_RESULT";

    private static final long START_SYNC_DELAY = 10000;

    private final Handler mHandler = new Handler();

    /**
     * Объекты синхронизации.
     *
     * @see #SYNC_NONE
     * @see #SYNC_ALL
     * @see #SYNC_DATABASE
     * @see #SYNC_PREFERENCES
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({SYNC_NONE, SYNC_ALL, SYNC_DATABASE, SYNC_PREFERENCES})
    public @interface Sync {
    }

    /**
     * Синхронизация не выполняется.
     */
    private static final int SYNC_NONE = -1;

    /**
     * Синхронизировать все объекты.
     */
    public static final int SYNC_ALL = 0;

    /**
     * Синхронизировать только базу данных.
     */
    public static final int SYNC_DATABASE = 1;

    /**
     * Синхронизировать только настройки.
     */
    public static final int SYNC_PREFERENCES = 2;

    /**
     * Результат запуска синхронизации.
     *
     * @see #RESULT_REQUEST_DONE
     * @see #RESULT_SYNC_DISABLED
     * @see #RESULT_SYNC_ACTIVE
     * @see #RESULT_TOKEN_EMPTY
     * @see #RESULT_INTERNET_DISCONNECTED
     * @see #RESULT_SYNC_DELAYED_REQUEST
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({RESULT_REQUEST_DONE, RESULT_SYNC_DISABLED, RESULT_SYNC_ACTIVE, RESULT_TOKEN_EMPTY,
            RESULT_INTERNET_DISCONNECTED, RESULT_SYNC_DELAYED_REQUEST})
    public @interface Result {
    }

    /**
     * Синхронизация запущена.
     */
    public static final int RESULT_REQUEST_DONE = -1;

    /**
     * Синхронизация отключена в настройках приложения.
     */
    public static final int RESULT_SYNC_DISABLED = 0;

    /**
     * Синхронизация выполняется.
     */
    public static final int RESULT_SYNC_ACTIVE = 1;

    /**
     * Необходима авторизация.
     */
    public static final int RESULT_TOKEN_EMPTY = 2;

    /**
     * Нет доступа к сети Интернет.
     */
    public static final int RESULT_INTERNET_DISCONNECTED = 3;

    /**
     * Синхронизация будет запущена после задержки.
     */
    public static final int RESULT_SYNC_DELAYED_REQUEST = 4;

    private DatabaseObserver mDatabaseObserver;
    private PreferencesObserver mPreferencesObserver;

    public ContentObserverService() {
    }

    @Override
    public void onCreate() {
        if (LOG_ENABLED) UtilsLog.d(TAG, "onCreate");

        super.onCreate();

        mDatabaseObserver = new DatabaseObserver();
        mDatabaseObserver.register(this);

        mPreferencesObserver = new PreferencesObserver();
        mPreferencesObserver.register(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (LOG_ENABLED) UtilsLog.d(TAG, "onStartCommand");

        if (intent != null) {
            @Sync
            int sync = intent.getIntExtra(EXTRA_NAME_SYNC, SYNC_NONE);

            boolean startIfActive = intent.getBooleanExtra(EXTRA_NAME_START_IF_ACTIVE, false);

            if (sync != SYNC_NONE) {
                // Отмена предыдущей задачи.
                mHandler.removeCallbacks(mRunnable);

                PendingIntent pendingIntent = intent.getParcelableExtra(EXTRA_NAME_PENDING);

                // Запуск после задержки.
                if (intent.getBooleanExtra(EXTRA_NAME_WITH_DELAY, false)) {
                    if (LOG_ENABLED) UtilsLog.d(TAG, "requestSync", "delayed start");

                    @Sync
                    int runnableSync;

                    switch (sync) {
                        case SYNC_ALL:
                            runnableSync = SYNC_ALL;

                            break;
                        case SYNC_DATABASE:
                        case SYNC_PREFERENCES:
                            // Если синхронизация с задержкой уже запущена,
                            // то проверяются её объекты.
                            // Например, если уже была запущена синхронизация настроек (SYNC_PREFERENCES)
                            // и новая задача запрашивает синхронизацию БД (SYNC_DATABASE),
                            // то новая синхронизация будет синхронизировать настройки и БД (SYNC_ALL).
                            runnableSync = mRunnable.sync;

                            if (runnableSync != SYNC_ALL && runnableSync != sync) {
                                if (runnableSync == SYNC_NONE)
                                    runnableSync = sync;
                                else
                                    runnableSync = SYNC_ALL;
                            }

                            break;
                        //noinspection ConstantConditions
                        case SYNC_NONE:
                        default:
                            runnableSync = SYNC_NONE;
                    }

                    mRunnable.sync = runnableSync;
                    mRunnable.startIfActive = startIfActive;
                    mRunnable.pendingIntent = pendingIntent;

                    mHandler.postDelayed(mRunnable, START_SYNC_DELAY);

                    sendResult(pendingIntent, RESULT_SYNC_DELAYED_REQUEST);
                } else {
                    // Запуск синхронизации без задержки.
                    int result = performRequestSync(sync, startIfActive);

                    sendResult(pendingIntent, result);
                }
            } else {
                if (LOG_ENABLED) UtilsLog.d(TAG, "requestSync", "sync == SYNC_NONE");
            }
        } else {
            if (LOG_ENABLED) UtilsLog.d(TAG, "requestSync", "intent == null");
        }

        return super.onStartCommand(intent, flags, startId);
    }

    private final RequestSyncRunnable mRunnable = new RequestSyncRunnable();

    private class RequestSyncRunnable implements Runnable {
        @Sync
        public int sync = SYNC_NONE;

        public boolean startIfActive = false;

        public PendingIntent pendingIntent;

        @Override
        public void run() {
            int result = performRequestSync(sync, startIfActive);

            sendResult(pendingIntent, result);

            sync = SYNC_NONE;
        }
    }

    /**
     * Запускает синхронизацию.
     *
     * @param sync          Объекты синхронизации ({@link Sync}).
     * @param startIfActive Запускать синхронизацию, даже если она уже запущена.
     * @return Результат запуска ({@link Result}).
     */
    @Result
    private int performRequestSync(@Sync int sync, boolean startIfActive) {
        if (!PreferencesHelper.getInstance(this).isSyncEnabled()) {
            if (LOG_ENABLED) UtilsLog.d(TAG, "requestSync", "sync disabled");
            return RESULT_SYNC_DISABLED;
        }

        if (ConnectivityHelper.getConnectedState(this) == ConnectivityHelper.DISCONNECTED) {
            if (LOG_ENABLED) UtilsLog.d(TAG, "requestSync", "Internet disconnected");
            return RESULT_INTERNET_DISCONNECTED;
        }

        SyncAccount syncAccount = new SyncAccount(this);

        if (syncAccount.isYandexDiskTokenEmpty()) {
            if (LOG_ENABLED) UtilsLog.d(TAG, "requestSync", "Yandex.Disk token empty");
            return RESULT_TOKEN_EMPTY;
        }

        if (syncAccount.isSyncActive() && !startIfActive) {
            if (LOG_ENABLED) UtilsLog.d(TAG, "requestSync", "sync active");
            return RESULT_SYNC_ACTIVE;
        }

        Bundle extras = new Bundle();

        extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        extras.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);

        boolean syncDatabase = sync == SYNC_ALL || sync == SYNC_DATABASE;
        boolean syncPreferences = sync == SYNC_ALL || sync == SYNC_PREFERENCES;

        extras.putBoolean(SyncAdapter.SYNC_DATABASE, syncDatabase);
        extras.putBoolean(SyncAdapter.SYNC_PREFERENCES, syncPreferences);

        ContentResolver.requestSync(syncAccount.getAccount(), syncAccount.getAuthority(), extras);

        if (LOG_ENABLED) UtilsLog.d(TAG, "requestSync", "request done");

        return RESULT_REQUEST_DONE;
    }

    /**
     * Отправляет результат в PendingIntent.
     *
     * @param pendingIntent Получатель результата.
     * @param resultCode    Результат ({@link Result}).
     */
    private void sendResult(@Nullable PendingIntent pendingIntent, @Result int resultCode) {
        if (pendingIntent != null) {
            Intent result = new Intent().putExtra(EXTRA_NAME_RESULT, resultCode);

            try {
                pendingIntent.send(this, Activity.RESULT_OK, result);
            } catch (PendingIntent.CanceledException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Извлекает результат запуска синхронизации из интента.
     *
     * @param data Интент.
     * @return Результат запуска синхронизации ({@link Result}).
     */
    @Result
    public static int getResult(@Nullable Intent data) {
        @Result
        int result = data != null ? data.getIntExtra(EXTRA_NAME_RESULT, RESULT_REQUEST_DONE) : RESULT_REQUEST_DONE;
        return result;
    }

    @Override
    public void onDestroy() {
        if (LOG_ENABLED) UtilsLog.d(TAG, "onDestroy");

        mPreferencesObserver.unregister(this);
        mDatabaseObserver.unregister(this);

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Запускает сервис.
     *
     * @param context Контекст.
     */
    public static void start(@NonNull Context context) {
        UtilsLog.d(TAG, "start");
        try {
            context.startService(new Intent(context, ContentObserverService.class));
        } catch (Exception e) {
            UtilsLog.d(TAG, "start", "exception = " + e.getMessage());
        }
    }

    /**
     * Запускает синхронизацию.
     *
     * @param context           Контекст.
     * @param sync              Объекты синхронизации ({@link Sync}).
     * @param startIfSyncActive Запускать синхронизацию, даже если она уже запущена.
     * @param withDelay         Запустить синхронизацию после задержки
     * @param pendingIntent     Результат запуска.
     */
    public static void requestSync(@NonNull Context context, @Sync int sync,
                                   boolean startIfSyncActive, boolean withDelay,
                                   @Nullable PendingIntent pendingIntent) {
        Intent intent = new Intent(context, ContentObserverService.class)
                .putExtra(EXTRA_NAME_SYNC, sync)
                .putExtra(EXTRA_NAME_START_IF_ACTIVE, startIfSyncActive)
                .putExtra(EXTRA_NAME_WITH_DELAY, withDelay);

        if (pendingIntent != null)
            intent.putExtra(EXTRA_NAME_PENDING, pendingIntent);

        context.startService(intent);
    }

    /**
     * Запускает синхронизацию всех объектов без задержки и без возврата результата,
     * только если синхронизация уже не запущена.
     *
     * @param context Контекст.
     */
    public static void requestSync(@NonNull Context context) {
        requestSync(context, SYNC_ALL, false, false, null);
    }

    /**
     * Запускает синхронизацию всех объектов без задержки с возвратом результата,
     * только если синхронизация уже не запущена.
     *
     * @param context       Контекст.
     * @param pendingIntent Результат запуска.
     */
    public static void requestSync(@NonNull Context context, @NonNull PendingIntent pendingIntent) {
        requestSync(context, SYNC_ALL, false, false, pendingIntent);
    }
}