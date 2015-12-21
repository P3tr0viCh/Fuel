package ru.p3tr0vich.fuel;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SyncStatusObserver;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
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
import android.text.TextUtils;
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

public class ActivityMain extends AppCompatActivity implements
        SyncStatusObserver,
        FragmentFueling.OnFilterChangeListener,
        FragmentFueling.OnRecordChangeListener,
        FragmentInterface.OnFragmentChangeListener,
        FragmentPreference.OnPreferenceScreenChangeListener,
        FragmentPreference.OnPreferenceSyncEnabledChangeListener,
        FuelingPreferenceManager.OnPreferencesChangedListener,
        FragmentBackup.OnDataLoadedFromBackupListener {

    private static final String ACTION_LOADING = "ru.p3tr0vich.fuel.ACTION_LOADING";
    private static final String EXTRA_LOADING = "ru.p3tr0vich.fuel.EXTRA_LOADING";

    private static final String ACTION_START_SYNC = "ru.p3tr0vich.fuel.ACTION_START_SYNC";

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

    private TimerPreferenceChanged mTimerPreferenceChanged;

    private BroadcastReceiver mLoadingStatusReceiver;
    private BroadcastReceiver mStartSyncReceiver;

    private int mCurrentFragmentId, mClickedMenuId;
    private boolean mOpenPreferenceSync = false;

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

    @NonNull
    private static Intent getLoadingBroadcast(boolean startLoading) {
        return new Intent(ACTION_LOADING).putExtra(EXTRA_LOADING, startLoading);
    }

    @NonNull
    private static Intent getStartSyncBroadcast() {
        return new Intent(ACTION_START_SYNC);
    }

    public static void sendLoadingBroadcast(boolean startLoading) {
        LocalBroadcastManager.getInstance(ApplicationFuel.getContext())
                .sendBroadcast(getLoadingBroadcast(startLoading));
    }

    public static void sendStartSyncBroadcast() {
        LocalBroadcastManager.getInstance(ApplicationFuel.getContext()).sendBroadcast(getStartSyncBroadcast());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Functions.logD("**************** ActivityMain -- onCreate ****************");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initToolbar();
        initToolbarSpinner();
        initDrawer();

        mSyncAccount = new SyncAccount(ApplicationFuel.getContext());

//        mSyncAccount.setYandexDiskToken(null);

        initAnimationSync();
        initSyncViews();

        updateSyncStatus();

        initLoadingStatusReceiver();
        initStartSyncReceiver();

        mSyncMonitor = ContentResolver.addStatusChangeListener(
                ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE, this);

        if (savedInstanceState == null) {
            mCurrentFragmentId = R.id.action_fueling;
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.contentFrame, new FragmentFueling(), FragmentFueling.TAG)
                    .setTransition(FragmentTransaction.TRANSIT_NONE)
                    .commit();

            startSync(false);
        } else {
            mCurrentFragmentId = savedInstanceState.getInt(KEY_CURRENT_FRAGMENT_ID);
            if (mCurrentFragmentId == R.id.action_settings) {
                FragmentPreference fragmentPreference = getFragmentPreference();
                if (fragmentPreference != null)
                    mDrawerToggle.setDrawerIndicatorEnabled(fragmentPreference.isInRoot());
            }
        }

        FuelingPreferenceManager.registerOnPreferencesChangedListener(this);
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

        Functions.addSpinnerInToolbar(getSupportActionBar(), mToolbarMain, mToolbarSpinner,
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
                Functions.hideKeyboard(ActivityMain.this);
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
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
                if (mCurrentFragmentId == R.id.action_settings && mCurrentFragmentId != mClickedMenuId)
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
                startSync(true);
            }
        });
    }

    private void initLoadingStatusReceiver() {
        mLoadingStatusReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                FragmentFueling fragmentFueling = getFragmentFueling();

                if (fragmentFueling != null)
                    fragmentFueling.setProgressBarVisible(intent.getBooleanExtra(EXTRA_LOADING, false));
                else
                    Functions.logD("ActivityMain -- mLoadingStatusReceiver.onReceive: fragmentFueling == null");
            }
        };
        LocalBroadcastManager.getInstance(getApplicationContext())
                .registerReceiver(mLoadingStatusReceiver, new IntentFilter(ACTION_LOADING));
    }

    private void initStartSyncReceiver() {
        mStartSyncReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                startSync(false);
            }
        };
        LocalBroadcastManager.getInstance(getApplicationContext())
                .registerReceiver(mStartSyncReceiver, new IntentFilter(ACTION_START_SYNC));
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
        if (mCurrentFragmentId == menuId) {
            FragmentPreference fragmentPreference = getFragmentPreference();
            if (fragmentPreference != null) {
                if (mOpenPreferenceSync)
                    fragmentPreference.goToSyncScreen();
                else
                    fragmentPreference.goToRootScreen();
                return;
            }
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
        FuelingPreferenceManager.registerOnPreferencesChangedListener(null);

        ContentResolver.removeStatusChangeListener(mSyncMonitor);

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mStartSyncReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mLoadingStatusReceiver);
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
                        fragmentFueling.addRecord(fuelingRecord);
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
                Functions.logD("ActivityMain -- onActivityResult: ActivityYandexMap.REQUEST_CODE_MAP_CENTER");
                ActivityYandexMap.MapCenter mapCenter = ActivityYandexMap.getMapCenter(data);

                FuelingPreferenceManager.putMapCenter(mapCenter.text,
                        mapCenter.latitude, mapCenter.longitude);
                FragmentPreference fragmentPreference = getFragmentPreference();
                if (fragmentPreference != null) fragmentPreference.updateMapCenter();
                break;
            case FragmentDialogQuestion.REQUEST_CODE:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(SyncYandexDisk.AUTH_URL)));
        }
    }

    @FuelingDBHelper.FilterMode
    private static int positionToFilterMode(int position) {
        switch (position) {
            case 0:
                return FuelingDBHelper.FILTER_MODE_CURRENT_YEAR;
            case 1:
                return FuelingDBHelper.FILTER_MODE_DATES;
            default:
                return FuelingDBHelper.FILTER_MODE_ALL;
        }
    }

    private static int filterModeToPosition(@FuelingDBHelper.FilterMode int filterMode) {
        switch (filterMode) {
            case FuelingDBHelper.FILTER_MODE_CURRENT_YEAR:
                return 0;
            case FuelingDBHelper.FILTER_MODE_YEAR:
            case FuelingDBHelper.FILTER_MODE_DATES:
                return 1;
            case FuelingDBHelper.FILTER_MODE_ALL:
                return 2;
            default:
                return 0;
        }
    }

    @Override
    public void onFilterChange(@FuelingDBHelper.FilterMode int filterMode) {
        int position = filterModeToPosition(filterMode);

        if (position != mToolbarSpinner.getSelectedItemPosition())
            mToolbarSpinner.setSelection(position);
    }

    @Override
    public void onDataLoadedFromBackup() {
        FragmentFueling fragmentFueling = getFragmentFueling();
        if (fragmentFueling != null) fragmentFueling.forceLoad();
    }

    @Override
    public void onFragmentChange(FragmentInterface fragment) {
        mCurrentFragmentId = mClickedMenuId = fragment.getFragmentId();

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

    // TODO: start on preference change
    private void startSync(boolean showDialogs) {
        Functions.logD("ActivityMain -- startSync");

        if (true) return;

        if (FuelingPreferenceManager.isSyncEnabled()) {
            if (!mSyncAccount.isSyncActive()) {
                if (!mSyncAccount.isYandexDiskTokenEmpty()) {
                    if (Functions.isInternetConnected()) {

                        Bundle extras = new Bundle();
                        extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
                        extras.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);

                        ContentResolver.requestSync(mSyncAccount.getAccount(), mSyncAccount.getAuthority(), extras);
                    } else {
                        Functions.logD("ActivityMain -- startSync: Internet disconnected");
                        if (showDialogs) FragmentDialogMessage.show(ActivityMain.this,
                                getString(R.string.title_message_error),
                                getString(R.string.message_error_no_internet));
                    }
                } else {
                    Functions.logD("ActivityMain -- startSync: Yandex.Disk token empty");
                    if (showDialogs) showDialogNeedAuth();
                }
            } else
                Functions.logD("ActivityMain -- startSync: sync active");
        } else {
            Functions.logD("ActivityMain -- startSync: sync disabled");
            if (showDialogs) {
                mClickedMenuId = R.id.action_settings;
                mOpenPreferenceSync = true;

                if (mDrawerLayout.isDrawerOpen(GravityCompat.START))
                    mDrawerLayout.closeDrawer(GravityCompat.START);
                else
                    selectItem(mClickedMenuId);
            }
        }
    }

    private void updateSyncStatus() {
        String text;
        int imgId;
        boolean syncActive = mSyncAccount.isSyncActive();

        if (syncActive) {
            text = getString(R.string.sync_in_process);
            imgId = R.mipmap.ic_sync_grey600_24dp;
        } else {
            if (FuelingPreferenceManager.isSyncEnabled()) {
                if (mSyncAccount.isYandexDiskTokenEmpty()) {
                    text = getString(R.string.sync_no_token);
                    imgId = R.mipmap.ic_sync_off_grey600_24dp;
                } else {
                    String lastSync = FuelingPreferenceManager.getLastSync();

                    if (TextUtils.isEmpty(lastSync)) {
                        text = getString(R.string.sync_not_performed);
                        imgId = R.mipmap.ic_sync_grey600_24dp;
                    } else if (lastSync.equals(FuelingPreferenceManager.SYNC_ERROR)) {
                        text = getString(R.string.sync_error);
                        imgId = R.mipmap.ic_sync_alert_grey600_24dp;
                    } else {
                        text = getString(R.string.sync_done, lastSync);
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

    @Override
    public void onPreferencesChanged() {
        if (mTimerPreferenceChanged != null) mTimerPreferenceChanged.cancel();

        mTimerPreferenceChanged = TimerPreferenceChanged.start();
    }
}