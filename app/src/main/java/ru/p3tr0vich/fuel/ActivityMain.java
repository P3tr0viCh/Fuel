package ru.p3tr0vich.fuel;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.content.SyncStatusObserver;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ActivityMain extends AppCompatActivity implements
        SyncStatusObserver,
        FragmentFueling.OnFilterChangeListener,
        FragmentFueling.OnRecordChangeListener,
        FragmentCalc.OnCalcDistanceButtonClickListener,
        FragmentInterface.OnFragmentChangeListener,
        FragmentPreferences.OnPreferenceScreenChangeListener,
        FragmentPreferences.OnPreferenceSyncEnabledChangeListener,
        FragmentPreferences.OnPreferenceClickListener {

    private static final String TAG = "ActivityMain";

    private static final String KEY_CURRENT_FRAGMENT_ID = "KEY_CURRENT_FRAGMENT_ID";

    private static final int REQUEST_CODE_ACTIVITY_MAP_DISTANCE = 100;
    private static final int REQUEST_CODE_ACTIVITY_MAP_CENTER = 101;

    private static final int REQUEST_CODE_DIALOG_YANDEX_AUTH = 200;

    private static final int REQUEST_CODE_ACTIVITY_CONTACTS = 300;

    private Toolbar mToolbarMain;
    private Spinner mToolbarSpinner;

    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private NavigationView mNavigationView;

    private SyncAccount mSyncAccount;
    private Object mSyncMonitor;

    private ImageView mImgSync;
    private Animation mAnimationSync;
    private TextView mBtnSync;

    private boolean mDatabaseChanged, mPreferencesChanged;
    private TimerSync mTimerSync;

    private BroadcastReceiverLoading mBroadcastReceiverLoading;
    private BroadcastReceiverSync mBroadcastReceiverSync;

    private PreferencesObserver mPreferencesObserver;
    private DatabaseObserver mDatabaseObserver;

    @FragmentId
    private int mCurrentFragmentId;
    private int mClickedMenuId = -1;
    private boolean mOpenPreferenceSync = false;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({FRAGMENT_FUELING_ID,
            FRAGMENT_CALC_ID,
            FRAGMENT_CHART_COST_ID,
            FRAGMENT_BACKUP_ID,
            FRAGMENT_PREFERENCES_ID,
            FRAGMENT_ABOUT_ID})
    public @interface FragmentId {
    }

    private static final int FRAGMENT_FUELING_ID = 0;
    private static final int FRAGMENT_CALC_ID = 1;
    private static final int FRAGMENT_CHART_COST_ID = 2;
    private static final int FRAGMENT_BACKUP_ID = 3;
    private static final int FRAGMENT_PREFERENCES_ID = 4;
    private static final int FRAGMENT_ABOUT_ID = 5;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({START_SYNC_APP_STARTED,
            START_SYNC_BUTTON_CLICKED,
            START_SYNC_TOKEN_CHANGED,
            START_SYNC_PREFERENCES_CHANGED,
            START_SYNC_DATABASE_CHANGED,
            START_SYNC_CHANGED,
            START_SYNC_ACTIVITY_DESTROY})
    public @interface StartSync {
    }

    private static final int START_SYNC_APP_STARTED = 0;
    private static final int START_SYNC_BUTTON_CLICKED = 1;
    private static final int START_SYNC_TOKEN_CHANGED = 2;
    private static final int START_SYNC_PREFERENCES_CHANGED = 3;
    private static final int START_SYNC_DATABASE_CHANGED = 4;
    private static final int START_SYNC_CHANGED = 5;
    private static final int START_SYNC_ACTIVITY_DESTROY = 6;

    @Nullable
    private Fragment findFragmentByTag(@Nullable String fragmentTag) {
        return fragmentTag != null ?
                getSupportFragmentManager().findFragmentByTag(fragmentTag) : null;
    }

    @NonNull
    private FragmentInterface getCurrentFragment() {
        return (FragmentInterface) getSupportFragmentManager().findFragmentById(R.id.contentFrame);

    }

    @Nullable
    private FragmentFueling getFragmentFueling() {
        return (FragmentFueling) getSupportFragmentManager().findFragmentByTag(FragmentFueling.TAG);
    }

    @Nullable
    private FragmentPreferences getFragmentPreferences() {
        return (FragmentPreferences) getSupportFragmentManager().findFragmentByTag(FragmentPreferences.TAG);
    }

    //TODO: delete
    void checkMessage(boolean expectedResult, String message, Pattern pattern) {
        Matcher matcher = pattern.matcher(message);

        boolean find = matcher.find();
        String value = find && matcher.groupCount() > 0 ? matcher.group(1) : null;

        UtilsLog.d(TAG, "temp", (expectedResult == find ? "OK" : "FAIL") +
                " -- message == " + message + ", find == " + find + ", value == " + value);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        UtilsLog.d(TAG, "**************** onCreate ****************");

        final String PATTERN_FLOAT = "([-]?\\\\d*[.,]?\\\\d+)";

        String patternStr = "\\\\(яяя|ююю) \\@ \\(xxx\\) @yyy \\(zz[z]+";
        UtilsLog.d(TAG, "temp", "patternStr == " + patternStr);

        // Экранирование всех символов, кроме буквенно-цифровых,
        // разделителей (пробелов, переводов строки и т.п.)
        // символов '(', '|', ')', '@' и символа экранирования '\'.
        // Пример: "яяя (xxx[zzz]111\|/) qqq" -> "яяя (xxx\[zzz\]111\|\/) qqq".
        // RegEx: "([^\w\s@\(\|\)\\])". Replace: "\$1".
        patternStr = patternStr.replaceAll("([^\\w\\s@\\(\\|\\)\\\\])", "\\\\$1");

        // Замена групп символов-разделителей.
        // Пример: "яяя xxx      zzz" -> "яяя.*?xxx.*?zzz".
        // RegEx: "[\s]+". Replace: ".*?".
        patternStr = patternStr.replaceAll("[\\s]+", ".*?");

        // Замена двойных символов '\' на символ с кодом 0 (��).
        // Пример: "@ \@ \\@ \\\@ \\\\@" -> "@ \@ ��@ ��\@ ����@".
        // RegEx: "[\\][\\]". Replace: "\00".
        patternStr = patternStr.replaceAll("[\\\\][\\\\]", "\00");

        // Установка всех групп в незахватывающие, то есть
        // добавление к символу '(' (открывающаяся скобка, начало группы) строки "?:",
        // если символ '(' не экранирован, то есть перед ним не стоит символ '\'.
        // В итоговом выражении должна быть только одна захватывающая группа,
        // в которой будет содержаться вещественное число.
        // Пример: "(xxx\(yyy(zzz\(\(qqq" -> "(?:xxx\(yyy(?:zzz\(\(qqq".
        // RegEx: "(?<![\\])[(]". Replace: "(?:".
        patternStr = patternStr.replaceAll("(?<![\\\\])[(]", "(?:");

        // Замена символа '@' (собака) на регулярное выражение поиска вещественного числа,
        // если символ '@' не экранирован, то есть перед ним не стоит символ '\'.
        // Пример: "@xxx\@yyy@zzz\@\@qqq" -> "PATTERN_FLOATxxx\@yyyPATTERN_FLOATzzz\@\@qqq",
        // где PATTERN_FLOAT -- регулярное выражение поиска вещественного числа.
        // RegEx: "(?<![\\])[@]". Replace: PATTERN_FLOAT == "([-]?\d*[.,]?\d+)".
        patternStr = patternStr.replaceAll("(?<![\\\\])[@]", PATTERN_FLOAT);

        // Обратная замена символов с кодом 0 на двойные символы '\'.
        // Пример: "@ \@ ��@ ��\@ ����@" -> "@ \@ \\@ \\\@ \\\\@".
        // RegEx: "[\00]". Replace: "\\\\".
        patternStr = patternStr.replaceAll("[\\00]", "\\\\\\\\");

        UtilsLog.d(TAG, "temp", "pattern == " + patternStr);
        UtilsLog.d(TAG, "temp");

        Pattern pattern = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE);

        checkMessage(true, "\\яяя @ (xxx) 123.3yyy (zz[z]+", pattern);
        checkMessage(true, "\\ююю @ (xxx) 123.3yyy (zz[z]+", pattern);
        checkMessage(false, "\\ююю @ xxx 123.3yyy (zz[z]+", pattern);
        checkMessage(false, "\\эээ @ (xxx) 123.3yyy (zz[z]+", pattern);

        checkMessage(true, "\\яяя @ (xxx) 123.3yyy (zz[z]+", pattern);
        checkMessage(false, "\\яяя @ (xxx) yyy (zz[z]+", pattern);
        checkMessage(false, "\\яяя @ (xxx) 123.yyy (zz[z]+", pattern);
        checkMessage(true, "\\яяя1234 @ (xxx) 123.3yyy (zz[z]+", pattern);
        checkMessage(false, "ggg \\яяя @ xxx 123.3yyy (zz[z]+", pattern);
        checkMessage(false, "яяя @ (xxx) 123.3yyy (zz[z]+ kkk", pattern);
        checkMessage(true, "ggg \\яяя @ (xxx) 123.3yyy (zz[z]+ jjj", pattern);
        checkMessage(false, "\\яяя (xxx) 123.3yyy (zz[z]+", pattern);
        checkMessage(true, "qwq \\яяяe232 @ (xxx)ccc 123.3yyy (zz[z]+", pattern);
        checkMessage(true, "\\яяя @ (xxx) .3yyy (zz[z]+", pattern);
        checkMessage(false, "яяя @ (xxx) 123.3 yyy (zz[z]+", pattern);
        checkMessage(false, " @ (xxx) 123.3yyy (zz[z]+", pattern);
        checkMessage(false, "яяя @ (xxx) 123.3yyy", pattern);

        UtilsLog.d(TAG, "temp");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initToolbar();
        initToolbarSpinner();
        initDrawer();

        mSyncAccount = new SyncAccount(this);

        initAnimationSync();
        initSyncViews();

        updateSyncStatus();

        initLoadingStatusReceiver();
        initStartSyncReceiver();

        initPreferencesObserver();
        initDatabaseObserver();

        mSyncMonitor = ContentResolver.addStatusChangeListener(
                ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE, this);

        if (savedInstanceState == null) {
            mCurrentFragmentId = FRAGMENT_FUELING_ID;
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.contentFrame,
                            FragmentFueling.newInstance(FRAGMENT_FUELING_ID),
                            FragmentFueling.TAG)
                    .setTransition(FragmentTransaction.TRANSIT_NONE)
                    .commit();

            startSync(START_SYNC_APP_STARTED);
        } else {
            mCurrentFragmentId = intToFragmentId(savedInstanceState.getInt(KEY_CURRENT_FRAGMENT_ID));
            if (mCurrentFragmentId == FRAGMENT_PREFERENCES_ID) {
                FragmentPreferences fragmentPreferences = getFragmentPreferences();
                if (fragmentPreferences != null)
                    mDrawerToggle.setDrawerIndicatorEnabled(fragmentPreferences.isInRoot());
            }
        }
    }

    private void initToolbar() {
        mToolbarMain = (Toolbar) findViewById(R.id.toolbarMain);
        setSupportActionBar(mToolbarMain);
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void initToolbarSpinner() {
        ActionBar actionBar = getSupportActionBar();

        //noinspection ConstantConditions
        mToolbarSpinner = new AppCompatSpinner(actionBar.getThemedContext());

        Utils.setBackgroundTint(mToolbarSpinner, R.color.toolbar_title_text, R.color.primary_light);

        actionBar.setDisplayShowTitleEnabled(false);

        ArrayAdapter adapter = ArrayAdapter.createFromResource(this, R.array.filter_dates, R.layout.toolbar_spinner_item);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);

        mToolbarSpinner.setAdapter(adapter);

        mToolbarSpinner.setDropDownVerticalOffset(-getResources().getDimensionPixelOffset(R.dimen.toolbar_height)); // minus

        mToolbarMain.addView(mToolbarSpinner);

        mToolbarSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                FragmentFueling fragmentFueling = getFragmentFueling();
                if (fragmentFueling != null && fragmentFueling.isVisible())
                    fragmentFueling.setFilterMode(positionToFilterMode(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void initDrawer() {
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);
        mDrawerToggle = new ActionBarDrawerToggle(this,
                mDrawerLayout, mToolbarMain, R.string.app_name, R.string.app_name) {

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);

                FragmentFueling fragmentFueling = getFragmentFueling();
                if (fragmentFueling != null && fragmentFueling.isVisible())
                    fragmentFueling.setFabVisible(false);

                Utils.hideKeyboard(ActivityMain.this);

                updateSyncStatus();
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);

                FragmentFueling fragmentFueling = getFragmentFueling();
                if (fragmentFueling != null && fragmentFueling.isVisible())
                    fragmentFueling.setFabVisible(true);

                selectItem(mClickedMenuId);
            }
        };
        mDrawerLayout.addDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();
        mDrawerToggle.setToolbarNavigationClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getCurrentFragment().onBackPressed();
            }
        });

        mNavigationView = (NavigationView) findViewById(R.id.drawerNavigationView);
        mNavigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {
                mClickedMenuId = menuItem.getItemId();
                mOpenPreferenceSync = false;
                // Если текущий фрагмент -- настройки, может отображаться стрелка влево.
                // Если нажат другой пункт меню, показывается значок меню.
                if (mCurrentFragmentId == FRAGMENT_PREFERENCES_ID &&
                        mCurrentFragmentId != menuIdToFragmentId(mClickedMenuId))
                    mDrawerToggle.setDrawerIndicatorEnabled(true);

                mDrawerLayout.closeDrawer(GravityCompat.START);
                return true;
            }
        });
    }

    private void initAnimationSync() {
        mAnimationSync = new RotateAnimation(360.0f, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        mAnimationSync.setInterpolator(new LinearInterpolator());
        mAnimationSync.setDuration(Const.ANIMATION_DURATION_SYNC);
        mAnimationSync.setRepeatCount(Animation.INFINITE);
    }

    private void initSyncViews() {
        mImgSync = (ImageView) findViewById(R.id.imgSync);

        mBtnSync = (TextView) findViewById(R.id.btnSync);
        mBtnSync.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSync(START_SYNC_BUTTON_CLICKED);
            }
        });
    }

    private void initLoadingStatusReceiver() {
        mBroadcastReceiverLoading = new BroadcastReceiverLoading() {

            @Override
            public void onReceive(boolean loading) {
                FragmentFueling fragmentFueling = getFragmentFueling();

                if (fragmentFueling != null)
                    fragmentFueling.setLoading(loading);
                else
                    UtilsLog.d(TAG, "mBroadcastReceiverLoading.onReceive", "fragmentFueling == null");
            }
        };
        mBroadcastReceiverLoading.register(this);
    }

    private void initStartSyncReceiver() {
        mBroadcastReceiverSync = new BroadcastReceiverSync() {

            @Override
            public void onReceive(boolean databaseChanged,
                                  boolean preferencesChanged, boolean tokenChanged) {
                if (tokenChanged)
                    startSync(START_SYNC_TOKEN_CHANGED);
                else {
                    if (databaseChanged && preferencesChanged)
                        startSync(START_SYNC_CHANGED);
                    else if (databaseChanged)
                        startSync(START_SYNC_DATABASE_CHANGED);
                    else if (preferencesChanged)
                        startSync(START_SYNC_PREFERENCES_CHANGED);
                }
            }
        };
        mBroadcastReceiverSync.register(this);
    }

    private void startTimerSync() {
        TimerSync.stop(mTimerSync);

        if (PreferencesHelper.isSyncEnabled())
            mTimerSync = TimerSync.start(mDatabaseChanged, mPreferencesChanged);
    }

    private class PreferencesObserver extends ContentObserver {
        public PreferencesObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            onChange(selfChange, null);
        }

        @Override
        public void onChange(boolean selfChange, Uri changeUri) {
            UtilsLog.d(TAG, "PreferencesObserver", "onChange: selfChange == " + selfChange +
                    ", changeUri == " + changeUri);

            mPreferencesChanged = true;
            startTimerSync();
        }
    }

    private void initPreferencesObserver() {
        mPreferencesObserver = new PreferencesObserver(new Handler());
        getContentResolver().registerContentObserver(ContentProviderHelper.URI_PREFERENCES,
                false, mPreferencesObserver);
    }

    private class DatabaseObserver extends ContentObserver {
        public DatabaseObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            onChange(selfChange, null);
        }

        @Override
        public void onChange(boolean selfChange, Uri changeUri) {
            UtilsLog.d(TAG, "DatabaseObserver", "onChange: changeUri == " + changeUri);

            long id = -1;

            if (changeUri != null)
                switch (ContentProviderHelper.uriMatch(changeUri)) {
                    case ContentProviderHelper.DATABASE_ITEM:
                        id = ContentUris.parseId(changeUri);
                    case ContentProviderHelper.DATABASE:
                        mDatabaseChanged = true;
                        startTimerSync();
                        break;
                    case ContentProviderHelper.DATABASE_SYNC:
                        break;
                    default:
                        return;
                }

            FragmentFueling fragmentFueling = getFragmentFueling();
            if (fragmentFueling != null) fragmentFueling.updateList(id);
        }
    }

    private void initDatabaseObserver() {
        mDatabaseObserver = new DatabaseObserver(new Handler());
        getContentResolver().registerContentObserver(ContentProviderHelper.URI_DATABASE,
                true, mDatabaseObserver);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(KEY_CURRENT_FRAGMENT_ID, mCurrentFragmentId);
    }

    @Nullable
    private Fragment getFragmentNewInstance(@Nullable String fragmentTag) {
        if (fragmentTag == null) return null;
        if (findFragmentByTag(fragmentTag) != null) return null;

        switch (fragmentTag) {
            case FragmentCalc.TAG:
                return FragmentCalc.newInstance(FRAGMENT_CALC_ID);
            case FragmentChartCost.TAG:
                return FragmentChartCost.newInstance(FRAGMENT_CHART_COST_ID);
            case FragmentPreferences.TAG:
                return FragmentPreferences.newInstance(FRAGMENT_PREFERENCES_ID);
            case FragmentBackup.TAG:
                return FragmentBackup.newInstance(FRAGMENT_BACKUP_ID);
            case FragmentAbout.TAG:
                return FragmentAbout.newInstance(FRAGMENT_ABOUT_ID);
            default:
                return null;
        }
    }

    @FragmentId
    private int menuIdToFragmentId(int menuId) {
        switch (menuId) {
            case R.id.action_fueling:
                return FRAGMENT_FUELING_ID;
            case R.id.action_calc:
                return FRAGMENT_CALC_ID;
            case R.id.action_chart_cost:
                return FRAGMENT_CHART_COST_ID;
            case R.id.action_preferences:
                return FRAGMENT_PREFERENCES_ID;
            case R.id.action_backup:
                return FRAGMENT_BACKUP_ID;
            case R.id.action_about:
                return FRAGMENT_ABOUT_ID;
            default:
                throw new IllegalArgumentException("Bad menu id == " + menuId);
        }
    }

    private int fragmentIdToMenuId(@FragmentId int fragmentId) {
        switch (fragmentId) {
            case FRAGMENT_ABOUT_ID:
                return R.id.action_about;
            case FRAGMENT_BACKUP_ID:
                return R.id.action_backup;
            case FRAGMENT_CALC_ID:
                return R.id.action_calc;
            case FRAGMENT_CHART_COST_ID:
                return R.id.action_chart_cost;
            case FRAGMENT_FUELING_ID:
                return R.id.action_fueling;
            case FRAGMENT_PREFERENCES_ID:
                return R.id.action_preferences;
            default:
                return -1;
        }
    }

    private void selectItem(int menuId) {
        if (menuId == -1) return;

        mClickedMenuId = -1;

        int fragmentId = menuIdToFragmentId(menuId);

        if (mCurrentFragmentId == fragmentId) {
            if (mCurrentFragmentId == FRAGMENT_PREFERENCES_ID) {
                FragmentPreferences fragmentPreferences = getFragmentPreferences();
                if (fragmentPreferences != null) {
                    if (mOpenPreferenceSync)
                        fragmentPreferences.goToSyncScreen();
                    else
                        fragmentPreferences.goToRootScreen();
                }
            }
            return;
        }

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentFueling fragmentFueling = getFragmentFueling();

        if (fragmentFueling != null)
            if (!fragmentFueling.isVisible()) fragmentManager.popBackStack();

        String fragmentTag = null;

        switch (fragmentId) {
            case FRAGMENT_ABOUT_ID:
                fragmentTag = FragmentAbout.TAG;
                break;
            case FRAGMENT_BACKUP_ID:
                fragmentTag = FragmentBackup.TAG;
                break;
            case FRAGMENT_CALC_ID:
                fragmentTag = FragmentCalc.TAG;
                break;
            case FRAGMENT_CHART_COST_ID:
                fragmentTag = FragmentChartCost.TAG;
                break;
            case FRAGMENT_FUELING_ID:
                break;
            case FRAGMENT_PREFERENCES_ID:
                fragmentTag = FragmentPreferences.TAG;
                break;
        }

        Fragment fragment = getFragmentNewInstance(fragmentTag);

        if (fragment != null) {
            if (mOpenPreferenceSync) {
                Bundle bundle = fragment.getArguments();
                if (bundle == null) bundle = new Bundle();

                bundle.putString(FragmentPreferences.KEY_PREFERENCE_SCREEN,
                        PreferencesHelper.PREF_SYNC);

                fragment.setArguments(bundle);

                mOpenPreferenceSync = false;
            }

            fragmentManager.beginTransaction()
                    .replace(R.id.contentFrame, fragment, fragmentTag)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .addToBackStack(null)
                    .commit();
        }
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerVisible(GravityCompat.START))
            mDrawerLayout.closeDrawer(GravityCompat.START);
        else {
            FragmentInterface fragment = getCurrentFragment();

            if (fragment.onBackPressed()) return;

            if (getSupportFragmentManager().getBackStackEntryCount() != 0)
                getSupportFragmentManager().popBackStack();
            else super.onBackPressed();
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onDestroy() {
        ContentResolver.removeStatusChangeListener(mSyncMonitor);

        getContentResolver().unregisterContentObserver(mDatabaseObserver);
        getContentResolver().unregisterContentObserver(mPreferencesObserver);

        mBroadcastReceiverSync.unregister(this);
        mBroadcastReceiverLoading.unregister(this);

        startSync(START_SYNC_ACTIVITY_DESTROY);

        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    @Override
    public void onRecordChange(@Nullable FuelingRecord fuelingRecord) {
        startActivity(ActivityFuelingRecordChange.getIntentForStart(this, fuelingRecord));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) return;

        Fragment fragment;

        switch (requestCode) {
            case REQUEST_CODE_ACTIVITY_MAP_CENTER:
                fragment = findFragmentByTag(FragmentCalc.TAG);

                if (fragment != null)
                    ((FragmentCalc) fragment).setDistance(ActivityYandexMap.getDistance(data));

                break;
            case REQUEST_CODE_ACTIVITY_MAP_DISTANCE:
                ActivityYandexMap.MapCenter mapCenter = ActivityYandexMap.getMapCenter(data);

                PreferencesHelper.putMapCenter(mapCenter.text,
                        mapCenter.latitude, mapCenter.longitude);

                fragment = getFragmentPreferences();
                if (fragment != null) ((FragmentPreferences) fragment).updateMapCenter();

                break;
            case REQUEST_CODE_DIALOG_YANDEX_AUTH:
                Utils.openUrl(this, SyncYandexDisk.URL.AUTH, null);

                break;
            case REQUEST_CODE_ACTIVITY_CONTACTS:
                String SMSAddress = ContactsHelper.getPhoneNumber(this, data);

                if (SMSAddress != null) {
                    PreferencesHelper.putSMSAddress(SMSAddress);

                    fragment = getFragmentPreferences();
                    if (fragment != null) ((FragmentPreferences) fragment).updateSMSAddress();
                }
        }
    }

    @DatabaseHelper.Filter.Mode
    private static int positionToFilterMode(int position) {
        switch (position) {
            case 0:
                return DatabaseHelper.Filter.MODE_CURRENT_YEAR;
            case 1:
                return DatabaseHelper.Filter.MODE_DATES;
            default:
                return DatabaseHelper.Filter.MODE_ALL;
        }
    }

    private static int filterModeToPosition(@DatabaseHelper.Filter.Mode int filterMode) {
        switch (filterMode) {
            case DatabaseHelper.Filter.MODE_CURRENT_YEAR:
                return 0;
            case DatabaseHelper.Filter.MODE_YEAR:
            case DatabaseHelper.Filter.MODE_DATES:
                return 1;
            case DatabaseHelper.Filter.MODE_ALL:
                return 2;
            default:
                return 0;
        }
    }

    @FragmentId
    private static int intToFragmentId(int id) {
        return id;
    }

    @Override
    public void onFilterChange(@DatabaseHelper.Filter.Mode int filterMode) {
        int position = filterModeToPosition(filterMode);

        if (position != mToolbarSpinner.getSelectedItemPosition())
            mToolbarSpinner.setSelection(position);
    }

    @Override
    public void onFragmentChange(FragmentInterface fragment) {
        mCurrentFragmentId = intToFragmentId(fragment.getFragmentId());

        setTitle(fragment.getTitle());
        setSubtitle(fragment.getSubtitle());

        mToolbarSpinner.setVisibility(mCurrentFragmentId == FRAGMENT_FUELING_ID ? View.VISIBLE : View.GONE);

        mNavigationView.getMenu().findItem(fragmentIdToMenuId(mCurrentFragmentId)).setChecked(true);
    }

    @Override
    public void setTitle(CharSequence title) {
        ActionBar actionBar = getSupportActionBar();

        if (actionBar == null) return;

        if (title != null) {
            actionBar.setTitle(title);
            actionBar.setDisplayShowTitleEnabled(true);
        } else {
            actionBar.setTitle(null);
            actionBar.setDisplayShowTitleEnabled(false);
        }
    }

    private void setSubtitle(@Nullable String subtitle) {
        mToolbarMain.setSubtitle(subtitle);
    }

    @Override
    public void onPreferenceScreenChanged(@NonNull CharSequence title, boolean isInRoot) {
        setTitle(title);
        toggleDrawer(mDrawerToggle, mDrawerLayout, !isInRoot);
    }

    private void toggleDrawer(final ActionBarDrawerToggle actionBarDrawerToggle, final DrawerLayout drawerLayout,
                              final boolean showArrow) {
        if (mDrawerToggle == null || mDrawerToggle.isDrawerIndicatorEnabled() != showArrow) return;

        float start, end;

        if (showArrow) {
            start = 0f;
            end = 1f;
        } else {
            start = 1f;
            end = 0f;
        }

        ValueAnimator anim = ValueAnimator.ofFloat(start, end);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                actionBarDrawerToggle.onDrawerSlide(drawerLayout, (Float) valueAnimator.getAnimatedValue());
            }
        });
        anim.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                if (!showArrow) mDrawerToggle.setDrawerIndicatorEnabled(true);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (showArrow) mDrawerToggle.setDrawerIndicatorEnabled(false);
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        anim.setInterpolator(new DecelerateInterpolator());
        anim.setDuration(Const.ANIMATION_DURATION_DRAWER_TOGGLE);
        anim.start();
    }

    private void startSync(@StartSync int startSync) {
        mDatabaseChanged = false;
        mPreferencesChanged = false;

        boolean showDialogs = false;
        boolean startIfSyncActive = false;
        boolean syncDatabase = false;
        boolean syncPreferences = false;

        TimerSync.stop(mTimerSync);

        switch (startSync) {
            case START_SYNC_APP_STARTED:
                UtilsLog.d(TAG, "startSync", "START_SYNC_APP_STARTED");
                syncDatabase = true;
                syncPreferences = true;
                break;
            case START_SYNC_BUTTON_CLICKED:
                UtilsLog.d(TAG, "startSync", "START_SYNC_BUTTON_CLICKED");
                showDialogs = true;
                syncDatabase = true;
                syncPreferences = true;
                break;
            case START_SYNC_TOKEN_CHANGED:
                UtilsLog.d(TAG, "startSync", "START_SYNC_TOKEN_CHANGED");
                syncDatabase = true;
                syncPreferences = true;
                break;
            case START_SYNC_PREFERENCES_CHANGED:
                UtilsLog.d(TAG, "startSync", "START_SYNC_PREFERENCES_CHANGED");
                startIfSyncActive = true;
                syncDatabase = false;
                syncPreferences = true;
                break;
            case START_SYNC_DATABASE_CHANGED:
                UtilsLog.d(TAG, "startSync", "START_SYNC_DATABASE_CHANGED");
                startIfSyncActive = true;
                syncDatabase = true;
                syncPreferences = false;
                break;
            case START_SYNC_CHANGED:
                UtilsLog.d(TAG, "startSync", "START_SYNC_CHANGED");
                startIfSyncActive = true;
                syncDatabase = true;
                syncPreferences = true;
                break;
            case START_SYNC_ACTIVITY_DESTROY:
                UtilsLog.d(TAG, "startSync", "START_SYNC_ACTIVITY_DESTROY");
                startIfSyncActive = true;
                syncDatabase = true;
                syncPreferences = true;
        }

        if (!PreferencesHelper.isSyncEnabled()) {
            UtilsLog.d(TAG, "startSync", "sync disabled");

            if (showDialogs) {
                mClickedMenuId = R.id.action_preferences;
                mOpenPreferenceSync = true;

                if (mDrawerLayout.isDrawerOpen(GravityCompat.START))
                    mDrawerLayout.closeDrawer(GravityCompat.START);
                else
                    selectItem(mClickedMenuId);
            }

            return;
        }

        if (mSyncAccount.isSyncActive() && !startIfSyncActive) {
            UtilsLog.d(TAG, "startSync", "sync active");

            return;
        }

        if (mSyncAccount.isYandexDiskTokenEmpty()) {
            UtilsLog.d(TAG, "startSync", "Yandex.Disk token empty");

            if (showDialogs) showDialogNeedAuth();

            return;
        }

        if (ConnectivityHelper.getConnectedState(this) == ConnectivityHelper.DISCONNECTED) {
            UtilsLog.d(TAG, "startSync", "Internet disconnected");

            if (showDialogs) FragmentDialogMessage.show(ActivityMain.this,
                    null,
                    getString(R.string.message_error_no_internet));

            return;
        }

        Bundle extras = new Bundle();

        extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        extras.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);

        extras.putBoolean(SyncAdapter.SYNC_DATABASE, syncDatabase);
        extras.putBoolean(SyncAdapter.SYNC_PREFERENCES, syncPreferences);

        ContentResolver.requestSync(mSyncAccount.getAccount(), mSyncAccount.getAuthority(), extras);
    }

    private void updateSyncStatus() {
        final String text;
        final int imgId;
        final boolean syncActive = mSyncAccount.isSyncActive();

        if (syncActive) {
            text = getString(R.string.sync_in_process);
            imgId = R.drawable.ic_sync;
        } else {
            if (PreferencesHelper.isSyncEnabled()) {
                if (mSyncAccount.isYandexDiskTokenEmpty()) {
                    text = getString(R.string.sync_no_token);
                    imgId = R.drawable.ic_sync_off;
                } else {
                    if (PreferencesHelper.getLastSyncHasError()) {
                        text = getString(R.string.sync_error);
                        imgId = R.drawable.ic_sync_alert;
                    } else {
                        final long dateTime = PreferencesHelper.getLastSyncDateTime();

                        text = dateTime != PreferencesHelper.SYNC_NONE ?
                                getString(R.string.sync_done,
                                        UtilsDate.getRelativeDateTime(this, dateTime)) :
                                getString(R.string.sync_not_performed);
                        imgId = R.drawable.ic_sync;
                    }
                }
            } else {
                text = getString(R.string.sync_disabled);
                imgId = R.drawable.ic_sync_off;
            }
        }

        mBtnSync.setText(text);
        mImgSync.setImageResource(imgId);

        if (syncActive) mImgSync.startAnimation(mAnimationSync);
        else mImgSync.clearAnimation();
    }

    @Override
    public void onPreferenceSyncEnabledChanged(final boolean enabled) {
        mSyncAccount.setIsSyncable(enabled);

        updateSyncStatus();

        if (enabled && mSyncAccount.isYandexDiskTokenEmpty())
            showDialogNeedAuth();
    }

    @Override
    public void onStatusChanged(int which) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateSyncStatus();
            }
        });
    }

    private void showDialogNeedAuth() {
        FragmentDialogQuestion.show(this, REQUEST_CODE_DIALOG_YANDEX_AUTH,
                R.string.dialog_caption_auth,
                R.string.message_dialog_auth,
                R.string.dialog_btn_agree, R.string.dialog_btn_disagree);
    }

    private void startYandexMap(@ActivityYandexMap.MapType int mapType) {
        int requestCode;

        switch (mapType) {
            case ActivityYandexMap.MAP_TYPE_CENTER:
                requestCode = REQUEST_CODE_ACTIVITY_MAP_DISTANCE;
                break;
            case ActivityYandexMap.MAP_TYPE_DISTANCE:
                requestCode = REQUEST_CODE_ACTIVITY_MAP_CENTER;
                break;
            default:
                return;
        }

        ActivityYandexMap.start(this, mapType, requestCode);
    }

    @Override
    public void onCalcDistanceButtonClick() {
        startYandexMap(ActivityYandexMap.MAP_TYPE_DISTANCE);
    }

    @Override
    public void onPreferenceMapCenterClick() {
        startYandexMap(ActivityYandexMap.MAP_TYPE_CENTER);
    }

    @Override
    public void onPreferenceSyncYandexDiskClick() {
        Utils.openUrl(this, SyncYandexDisk.URL.WWW, getString(R.string.message_error_yandex_disk_browser_open));
    }

    @Override
    public void onPreferenceSMSAddressClick() {
        Intent intent = ContactsHelper.getIntent();

        if (intent.resolveActivity(getPackageManager()) != null)
            startActivityForResult(intent, REQUEST_CODE_ACTIVITY_CONTACTS);
        else
            Utils.toast(R.string.message_error_no_contacts_activity);
    }
}