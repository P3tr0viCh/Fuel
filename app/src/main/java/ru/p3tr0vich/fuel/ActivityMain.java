package ru.p3tr0vich.fuel;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
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
import java.util.Calendar;

public class ActivityMain extends AppCompatActivity implements
        SyncStatusObserver,
        FragmentFueling.OnFilterChangeListener,
        FragmentFueling.OnRecordChangeListener,
        FragmentInterface.OnFragmentChangeListener,
        FragmentPreference.OnPreferenceScreenChangeListener,
        FragmentPreference.OnPreferenceSyncEnabledChangeListener {

    private static final String TAG = "ActivityMain";

    private static final String ACTION_LOADING = BuildConfig.APPLICATION_ID + ".ACTION_LOADING";
    private static final String EXTRA_LOADING = BuildConfig.APPLICATION_ID + ".EXTRA_LOADING";

    private static final String KEY_CURRENT_FRAGMENT_ID = "KEY_CURRENT_FRAGMENT_ID";

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

    private boolean mSyncDatabase, mSyncPreferences;
    private TimerSync mTimerSync;

    private BroadcastReceiver mLoadingStatusReceiver;
    private BroadcastReceiver mStartSyncReceiver;

    private PreferencesObserver mPreferencesObserver;
    private DatabaseObserver mDatabaseObserver;

    private int mCurrentFragmentId, mClickedMenuId = -1;
    private boolean mOpenPreferenceSync = false;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({START_SYNC_APP_STARTED, START_SYNC_BUTTON_CLICKED,
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

    @Nullable
    private FragmentFueling getFragmentFueling() {
        return (FragmentFueling) getSupportFragmentManager().findFragmentByTag(FragmentFueling.TAG);
    }

    @Nullable
    private FragmentPreference getFragmentPreference() {
        return (FragmentPreference) getSupportFragmentManager().findFragmentByTag(FragmentPreference.TAG);
    }

    public static void sendLoadingBroadcast(boolean startLoading) {
        LocalBroadcastManager.getInstance(ApplicationFuel.getContext())
                .sendBroadcast(new Intent(ACTION_LOADING).putExtra(EXTRA_LOADING, startLoading));
    }

    private void test(String s, Calendar calendar) {
        UtilsLog.d(TAG, s + ", " +
                DateUtils.formatDateTime(this, calendar.getTimeInMillis(),
                        DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME),
                UtilsFormat.getRelativeDateTime(this, calendar.getTimeInMillis()));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        UtilsLog.d(TAG, "**************** onCreate ****************");

        final Calendar calendar = Calendar.getInstance();

        final long now = System.currentTimeMillis();

        calendar.setTimeInMillis(now);
        calendar.add(Calendar.DAY_OF_YEAR, +1);
        test("+ 1 d", calendar);

        calendar.setTimeInMillis(now);
        calendar.add(Calendar.MONTH, +1);
        test("+ 1 m", calendar);

        calendar.setTimeInMillis(now);
        calendar.add(Calendar.YEAR, +1);
        test("+ 1 y", calendar);

        calendar.setTimeInMillis(now);
        calendar.add(Calendar.SECOND, 0);
        test("now", calendar);

        calendar.setTimeInMillis(now);
        calendar.add(Calendar.SECOND, -2);
        test("- 2 sec", calendar);

        calendar.setTimeInMillis(now);
        calendar.add(Calendar.SECOND, -9);
        test("- 9 sec", calendar);

        calendar.setTimeInMillis(now);
        calendar.add(Calendar.SECOND, -1);
        test("- 10 sec", calendar);

        calendar.setTimeInMillis(now);
        calendar.add(Calendar.SECOND, -15);
        test("- 15 sec", calendar);

        calendar.setTimeInMillis(now);
        calendar.add(Calendar.SECOND, -42);
        test("- 42 sec", calendar);

        calendar.setTimeInMillis(now);
        calendar.add(Calendar.MINUTE, -1);
        test("- 1 min", calendar);

        calendar.setTimeInMillis(now);
        calendar.add(Calendar.MINUTE, -2);
        test("- 2 min", calendar);

        calendar.setTimeInMillis(now);
        calendar.add(Calendar.MINUTE, -5);
        test("- 5 min", calendar);

        calendar.setTimeInMillis(now);
        calendar.add(Calendar.MINUTE, -10);
        test("- 10 min", calendar);

        calendar.setTimeInMillis(now);
        calendar.add(Calendar.MINUTE, -42);
        test("- 42 min", calendar);

        calendar.setTimeInMillis(now);
        calendar.add(Calendar.HOUR_OF_DAY, -1);
        test("- 1 hour", calendar);

        calendar.setTimeInMillis(now);
        calendar.add(Calendar.HOUR_OF_DAY, -1);
        calendar.add(Calendar.MINUTE, -42);
        test("- 1 hour 42 min", calendar);

        calendar.setTimeInMillis(now);
        calendar.add(Calendar.HOUR_OF_DAY, -3);
        test("- 3 h", calendar);

        calendar.setTimeInMillis(now);
        calendar.add(Calendar.HOUR_OF_DAY, -12);
        test("- 12 h", calendar);

        calendar.setTimeInMillis(now);
        calendar.add(Calendar.DAY_OF_YEAR, -1);
        test("- 1 d", calendar);

        calendar.setTimeInMillis(now);
        calendar.add(Calendar.DAY_OF_YEAR, -1);
        calendar.add(Calendar.HOUR_OF_DAY, -12);
        test("- 1 d 12 h", calendar);

        calendar.setTimeInMillis(now);
        calendar.add(Calendar.DAY_OF_YEAR, -2);
        test("- 2 d", calendar);

        calendar.setTimeInMillis(now);
        calendar.add(Calendar.DAY_OF_YEAR, -6);
        test("- 6 d", calendar);

        calendar.setTimeInMillis(now);
        calendar.add(Calendar.DAY_OF_YEAR, -6);
        calendar.add(Calendar.HOUR_OF_DAY, -12);
        test("- 6 d 12 h", calendar);

        calendar.setTimeInMillis(now);
        calendar.add(Calendar.DAY_OF_YEAR, -7);
        test("- 7 d", calendar);

        calendar.setTimeInMillis(now);
        calendar.add(Calendar.DAY_OF_YEAR, -8);
        test("- 8 d", calendar);

        calendar.setTimeInMillis(now);
        calendar.add(Calendar.DAY_OF_YEAR, -20);
        test("- 20 d", calendar);

        calendar.setTimeInMillis(now);
        calendar.add(Calendar.DAY_OF_YEAR, -200);
        test("- 200 d", calendar);

        calendar.setTimeInMillis(now);
        calendar.add(Calendar.YEAR, -1);
        test("- 1 y", calendar);

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
            mCurrentFragmentId = R.id.action_fueling;
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.contentFrame, new FragmentFueling(), FragmentFueling.TAG)
                    .setTransition(FragmentTransaction.TRANSIT_NONE)
                    .commit();

            startSync(START_SYNC_APP_STARTED);
        } else {
            mCurrentFragmentId = savedInstanceState.getInt(KEY_CURRENT_FRAGMENT_ID);
            if (mCurrentFragmentId == R.id.action_settings) {
                FragmentPreference fragmentPreference = getFragmentPreference();
                if (fragmentPreference != null)
                    mDrawerToggle.setDrawerIndicatorEnabled(fragmentPreference.isInRoot());
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
        //noinspection ConstantConditions
        mToolbarSpinner = new AppCompatSpinner(getSupportActionBar().getThemedContext());

        Utils.addSpinnerInToolbar(getSupportActionBar(), mToolbarMain, mToolbarSpinner,
                ArrayAdapter.createFromResource(this, R.array.filter_dates, R.layout.toolbar_spinner_item),
                new AdapterView.OnItemSelectedListener() {
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
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();
        mDrawerToggle.setToolbarNavigationClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentPreference fragmentPreference = getFragmentPreference();
                if (fragmentPreference != null) fragmentPreference.goToRootScreen();
            }
        });

        mNavigationView = (NavigationView) findViewById(R.id.drawerNavigationView);
        mNavigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {
                mClickedMenuId = menuItem.getItemId();
                mOpenPreferenceSync = false;
                // Если текущий фрагмент -- настройки, может отображаться стрелка влево.
                // Если нажат другой пункт меню, показвается значок меню.
                if (mCurrentFragmentId == FragmentPreference.ID && mCurrentFragmentId != mClickedMenuId)
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
        mLoadingStatusReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                FragmentFueling fragmentFueling = getFragmentFueling();

                if (fragmentFueling != null)
                    fragmentFueling.setLoading(intent.getBooleanExtra(EXTRA_LOADING, false));
                else
                    UtilsLog.d(TAG, "mLoadingStatusReceiver.onReceive", "fragmentFueling == null");
            }
        };
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(mLoadingStatusReceiver, new IntentFilter(ACTION_LOADING));
    }

    private void initStartSyncReceiver() {
        mStartSyncReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final boolean syncDatabase = TimerTaskSync.isSyncDatabase(intent);
                final boolean syncPreferences = TimerTaskSync.isSyncPreferences(intent);

                if (syncDatabase && syncPreferences)
                    startSync(START_SYNC_CHANGED);
                else if (syncDatabase)
                    startSync(START_SYNC_DATABASE_CHANGED);
                else if (syncPreferences)
                    startSync(START_SYNC_PREFERENCES_CHANGED);
            }
        };
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(mStartSyncReceiver, TimerTaskSync.getIntentFilter());
    }

    private void startTimerSync() {
        TimerSync.stop(mTimerSync);

        if (PreferenceManagerFuel.isSyncEnabled())
            mTimerSync = TimerSync.start(mSyncDatabase, mSyncPreferences);
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

            mSyncPreferences = true;
            startTimerSync();
        }
    }

    private void initPreferencesObserver() {
        mPreferencesObserver = new PreferencesObserver(new Handler());
        getContentResolver().registerContentObserver(ContentProviderFuel.URI_PREFERENCES,
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
                switch (ContentProviderFuel.sURIMatcher.match(changeUri)) {
                    case ContentProviderFuel.DATABASE_ITEM:
                        id = ContentUris.parseId(changeUri);
                    case ContentProviderFuel.DATABASE:
                        mSyncDatabase = true;
                        startTimerSync();
                        break;
                    case ContentProviderFuel.DATABASE_SYNC:
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
        getContentResolver().registerContentObserver(ContentProviderFuel.URI_DATABASE,
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
                return new FragmentCalc();
            case FragmentChartCost.TAG:
                return new FragmentChartCost();
            case FragmentPreference.TAG:
                return new FragmentPreference();
            case FragmentBackup.TAG:
                return new FragmentBackup();
            case FragmentAbout.TAG:
                return new FragmentAbout();
            default:
                return null;
        }
    }

    private void selectItem(int menuId) {
        if (menuId == -1) return;

        mClickedMenuId = -1;

        if (mCurrentFragmentId == menuId) {
            if (mCurrentFragmentId == FragmentPreference.ID) {
                FragmentPreference fragmentPreference = getFragmentPreference();
                if (fragmentPreference != null) {
                    if (mOpenPreferenceSync)
                        fragmentPreference.goToSyncScreen();
                    else
                        fragmentPreference.goToRootScreen();
                }
            }
            return;
        }

        String fragmentTag = null;

        Fragment fragment;
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentFueling fragmentFueling = getFragmentFueling();

        if (fragmentFueling != null)
            if (!fragmentFueling.isVisible()) fragmentManager.popBackStack();

        switch (menuId) {
            case R.id.action_calc:
                fragmentTag = FragmentCalc.TAG;
                break;
            case R.id.action_chart_cost:
                fragmentTag = FragmentChartCost.TAG;
                break;
            case R.id.action_settings:
                fragmentTag = FragmentPreference.TAG;
                break;
            case R.id.action_backup:
                fragmentTag = FragmentBackup.TAG;
                break;
            case R.id.action_about:
                fragmentTag = FragmentAbout.TAG;
        }

        fragment = getFragmentNewInstance(fragmentTag);

        if (fragment != null) {
            if (mOpenPreferenceSync) {
                Bundle bundle = new Bundle();

                bundle.putString(FragmentPreference.KEY_PREFERENCE_SCREEN,
                        getString(R.string.pref_sync_key));

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
            if (mCurrentFragmentId == R.id.action_settings) {
                FragmentPreference fragmentPreference = getFragmentPreference();
                if (fragmentPreference != null && fragmentPreference.goToRootScreen())
                    return;
            }
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

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mStartSyncReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mLoadingStatusReceiver);

        startSync(START_SYNC_ACTIVITY_DESTROY);

        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    @Override
    public void onRecordChange(@Const.RecordAction int recordAction, FuelingRecord fuelingRecord) {
        if (recordAction != Const.RECORD_ACTION_DELETE)
            // ADD, UPDATE
            ActivityFuelingRecordChange.start(this, recordAction, fuelingRecord);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) return;

        switch (requestCode) {
            case ActivityFuelingRecordChange.REQUEST_CODE:
                FragmentFueling fragmentFueling = getFragmentFueling();

                if (fragmentFueling == null) return;

                FuelingRecord fuelingRecord = new FuelingRecord(data);

                switch (ActivityFuelingRecordChange.getAction(data)) {
                    case Const.RECORD_ACTION_ADD:
                        fragmentFueling.insertRecord(fuelingRecord);
                        break;
                    case Const.RECORD_ACTION_UPDATE:
                        fragmentFueling.updateRecord(fuelingRecord);
                        break;
                    case Const.RECORD_ACTION_DELETE:
                        break;
                }
                break;
            case ActivityYandexMap.REQUEST_CODE_DISTANCE:
                FragmentCalc fragmentCalc = (FragmentCalc) findFragmentByTag(FragmentCalc.TAG);
                if (fragmentCalc != null)
                    fragmentCalc.setDistance(ActivityYandexMap.getDistance(data));
                break;
            case ActivityYandexMap.REQUEST_CODE_MAP_CENTER:
                ActivityYandexMap.MapCenter mapCenter = ActivityYandexMap.getMapCenter(data);

                PreferenceManagerFuel.putMapCenter(mapCenter.text,
                        mapCenter.latitude, mapCenter.longitude);
                FragmentPreference fragmentPreference = getFragmentPreference();
                if (fragmentPreference != null) fragmentPreference.updateMapCenter();
                break;
            case FragmentDialogQuestion.REQUEST_CODE:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(SyncYandexDisk.AUTH_URL)));
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

    @Override
    public void onFilterChange(@DatabaseHelper.Filter.Mode int filterMode) {
        int position = filterModeToPosition(filterMode);

        if (position != mToolbarSpinner.getSelectedItemPosition())
            mToolbarSpinner.setSelection(position);
    }

    @Override
    public void onFragmentChange(FragmentInterface fragment) {
        mCurrentFragmentId = fragment.getFragmentId();

        setTitle(fragment.getTitle());
        setSubtitle(fragment.getSubtitle());

        boolean showSpinner = fragment instanceof FragmentFueling;

        mToolbarSpinner.setVisibility(showSpinner ? View.VISIBLE : View.GONE);

        mNavigationView.getMenu().findItem(mCurrentFragmentId).setChecked(true);
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
    public void OnPreferenceScreenChanged(@NonNull CharSequence title, boolean isInRoot) {
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
        anim.setDuration(300);
        anim.start();
    }

    private void startSync(@StartSync int startSync) {
        mSyncDatabase = false;
        mSyncPreferences = false;

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

        if (PreferenceManagerFuel.isSyncEnabled()) {
            if (!mSyncAccount.isSyncActive() || startIfSyncActive) {
                if (!mSyncAccount.isYandexDiskTokenEmpty()) {
                    if (Utils.isInternetConnected()) {
                        /* ****** */

                        Bundle extras = new Bundle();

                        extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
                        extras.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);

                        extras.putBoolean(SyncAdapter.SYNC_DATABASE, syncDatabase);
                        extras.putBoolean(SyncAdapter.SYNC_PREFERENCES, syncPreferences);

                        ContentResolver.requestSync(mSyncAccount.getAccount(),
                                mSyncAccount.getAuthority(), extras);

                        /* ****** */
                    } else {
                        UtilsLog.d(TAG, "startSync", "Internet disconnected");
                        if (showDialogs) FragmentDialogMessage.show(ActivityMain.this,
                                getString(R.string.title_message_error),
                                getString(R.string.message_error_no_internet));
                    }
                } else {
                    UtilsLog.d(TAG, "startSync", "Yandex.Disk token empty");
                    if (showDialogs) showDialogNeedAuth();
                }
            } else
                UtilsLog.d(TAG, "startSync", "sync active");
        } else {
            UtilsLog.d(TAG, "startSync", "sync disabled");
            if (showDialogs) {
                mClickedMenuId = FragmentPreference.ID;
                mOpenPreferenceSync = true;

                if (mDrawerLayout.isDrawerOpen(GravityCompat.START))
                    mDrawerLayout.closeDrawer(GravityCompat.START);
                else
                    selectItem(mClickedMenuId);
            }
        }
    }

    private void updateSyncStatus() {
        final String text;
        final int imgId;
        final boolean syncActive = mSyncAccount.isSyncActive();

        if (syncActive) {
            text = getString(R.string.sync_in_process);
            imgId = R.mipmap.ic_sync_grey600_24dp;
        } else {
            if (PreferenceManagerFuel.isSyncEnabled()) {
                if (mSyncAccount.isYandexDiskTokenEmpty()) {
                    text = getString(R.string.sync_no_token);
                    imgId = R.mipmap.ic_sync_off_grey600_24dp;
                } else {
                    if (PreferenceManagerFuel.getLastSyncHasError()) {
                        text = getString(R.string.sync_error);
                        imgId = R.mipmap.ic_sync_alert_grey600_24dp;
                    } else {
                        final long dateTime = PreferenceManagerFuel.getLastSyncDateTime();

                        text = dateTime != PreferenceManagerFuel.SYNC_NONE ?
                                getString(R.string.sync_done,
                                        UtilsFormat.getRelativeDateTime(this, dateTime)) :
                                getString(R.string.sync_not_performed);
                        imgId = R.mipmap.ic_sync_grey600_24dp;
                    }
                }
            } else {
                text = getString(R.string.sync_disabled);
                imgId = R.mipmap.ic_sync_off_grey600_24dp;
            }
        }

        mBtnSync.setText(text);
        mImgSync.setImageResource(imgId);

        if (syncActive) mImgSync.startAnimation(mAnimationSync);
        else mImgSync.clearAnimation();
    }

    @Override
    public void OnPreferenceSyncEnabledChanged(final boolean enabled) {
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
        FragmentDialogQuestion.show(this, R.string.dialog_caption_auth,
                R.string.message_dialog_auth, R.string.dialog_btn_agree, R.string.dialog_btn_disagree);
    }
}