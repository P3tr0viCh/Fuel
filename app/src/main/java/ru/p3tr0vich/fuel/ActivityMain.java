package ru.p3tr0vich.fuel;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
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
        FragmentFueling.OnFilterChangeListener,
        FragmentFueling.OnRecordChangeListener,
        FragmentInterface.OnFragmentChangeListener,
        FragmentPreference.OnPreferenceScreenChangeListener,
        FragmentPreference.OnPreferenceSyncEnabledChangeListener,
        FragmentBackup.OnDataLoadedFromBackupListener {

    private static final String ACTION_LOADING = "ru.p3tr0vich.fuel.ACTION_LOADING";
    private static final String EXTRA_LOADING = "ru.p3tr0vich.fuel.EXTRA_LOADING";

    private static final String KEY_CURRENT_FRAGMENT_ID = "KEY_CURRENT_FRAGMENT_ID";

    private Toolbar mToolbarMain;
    private Spinner mToolbarSpinner;

    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private NavigationView mNavigationView;

    private ImageView mImgSync;
    private Animation mAnimationSync;
    private TextView mBtnSync;

    private BroadcastReceiver mLoadingStatusReceiver;

    private int mCurrentFragmentId, mClickedMenuId;

    private Fragment findFragmentByTag(String fragmentTag) {
        return fragmentTag != null ?
                getSupportFragmentManager().findFragmentByTag(fragmentTag) : null;
    }

    private FragmentFueling getFragmentFueling() {
        return (FragmentFueling) getSupportFragmentManager().findFragmentByTag(FragmentFueling.TAG);
    }

    public static Intent getLoadingBroadcast(boolean startLoading) {
        return new Intent(ACTION_LOADING).putExtra(EXTRA_LOADING, startLoading);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Functions.sApplicationContext = getApplicationContext();

        FuelingPreferenceManager.init(Functions.sApplicationContext);

        Functions.logD("**************** ActivityMain -- onCreate ****************");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mToolbarMain = (Toolbar) findViewById(R.id.toolbarMain);
        setSupportActionBar(mToolbarMain);
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

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
                ((FragmentPreference) findFragmentByTag(FragmentPreference.TAG)).goToRootScreen();
            }
        });

        mNavigationView = (NavigationView) findViewById(R.id.drawerNavigationView);
        mNavigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {
                mClickedMenuId = menuItem.getItemId();
                if (mCurrentFragmentId == R.id.action_settings && mCurrentFragmentId != mClickedMenuId)
                    mDrawerToggle.setDrawerIndicatorEnabled(true);

                mDrawerLayout.closeDrawer(GravityCompat.START);
                return true;
            }
        });

        mAnimationSync = new RotateAnimation(360.0f, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        mAnimationSync.setInterpolator(new LinearInterpolator());
        mAnimationSync.setDuration(Const.ANIMATION_DURATION_SYNC);
        mAnimationSync.setRepeatCount(Animation.INFINITE);

        mImgSync = (ImageView) findViewById(R.id.imgSync);

        mBtnSync = (TextView) findViewById(R.id.btnSync);
        mBtnSync.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (FuelingPreferenceManager.isSyncEnabled())
                    startSync(true);
                else {
                    mClickedMenuId = R.id.action_settings; // TODO: open sync screen
                    mDrawerLayout.closeDrawer(GravityCompat.START);
                }
            }
        });
        updateSyncStatus();

        //noinspection ConstantConditions
        mToolbarSpinner = new AppCompatSpinner(getSupportActionBar().getThemedContext());

        Functions.addSpinnerInToolbar(getSupportActionBar(), mToolbarMain, mToolbarSpinner,
                ArrayAdapter.createFromResource(this, R.array.filter_dates, R.layout.toolbar_spinner_item),
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        Functions.logD("ActivityMain -- onItemSelected");

                        FragmentFueling fragmentFueling = getFragmentFueling();
                        if (fragmentFueling != null && fragmentFueling.isVisible())
                            fragmentFueling.setFilterMode(positionToFilterMode(position));
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });

        mLoadingStatusReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                FragmentFueling fragmentFueling = getFragmentFueling();

                if (fragmentFueling != null)
                    fragmentFueling.setProgressBarVisible(intent.getBooleanExtra(EXTRA_LOADING, false));
                else
                    Functions.logD("ActivityMain -- BroadcastReceiver.onReceive: fragmentFueling == null");
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(mLoadingStatusReceiver,
                new IntentFilter(ACTION_LOADING));

        if (savedInstanceState == null) {
            mCurrentFragmentId = R.id.action_fueling;
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.contentFrame, new FragmentFueling(), FragmentFueling.TAG)
                    .setTransition(FragmentTransaction.TRANSIT_NONE)
                    .commit();
        } else {
            mCurrentFragmentId = savedInstanceState.getInt(KEY_CURRENT_FRAGMENT_ID);
            if (mCurrentFragmentId == R.id.action_settings)
                mDrawerToggle.setDrawerIndicatorEnabled(
                        ((FragmentPreference) findFragmentByTag(FragmentPreference.TAG)).isInRoot());
        }

        startSync(savedInstanceState == null);
//        FuelingPreferenceManager.putRevision(-1);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(KEY_CURRENT_FRAGMENT_ID, mCurrentFragmentId);
    }

    private Fragment getFragmentNewInstance(String fragmentTag) {
        if (fragmentTag == null) return null;
        if (findFragmentByTag(fragmentTag) == null)
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
        else return null;
    }

    private void selectItem(int menuId) {
        if (mCurrentFragmentId == menuId) return;

        String fragmentTag = null;

        Fragment fragment;
        FragmentManager fragmentManager = getSupportFragmentManager();

        if (!getFragmentFueling().isVisible()) fragmentManager.popBackStack();

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

        if (fragment != null)
            fragmentManager.beginTransaction()
                    .replace(R.id.contentFrame, fragment, fragmentTag)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .addToBackStack(null)
                    .commit();
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerVisible(GravityCompat.START))
            mDrawerLayout.closeDrawer(GravityCompat.START);
        else {
            if (mCurrentFragmentId == R.id.action_settings) {
                if (((FragmentPreference) findFragmentByTag(FragmentPreference.TAG)).goToRootScreen())
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
        if (requestCode == ServiceSync.REQUEST_CODE) {
            if (resultCode == ServiceSync.STATUS_START)
                Functions.logD("ActivityMain -- onActivityResult: resultCode == STATUS_START");
            else if (resultCode == ServiceSync.STATUS_FINISH)
                Functions.logD("ActivityMain -- onActivityResult: resultCode == STATUS_FINISH");
            updateSyncStatus();
            return;
        }

        if (resultCode != RESULT_OK) return;

        FuelingRecord fuelingRecord;
        FragmentFueling fragmentFueling = getFragmentFueling();

        switch (requestCode) {
            case ActivityFuelingRecordChange.REQUEST_CODE:
                fuelingRecord = new FuelingRecord(data);

                switch (ActivityFuelingRecordChange.getAction(data)) {
                    case Const.RECORD_ACTION_ADD:
                        fragmentFueling.addRecord(fuelingRecord);
                        break;
                    case Const.RECORD_ACTION_UPDATE:
                        fragmentFueling.updateRecord(fuelingRecord);
                        break;
                }
                break;
            case ActivityYandexMap.REQUEST_CODE_DISTANCE:
                ((FragmentCalc) findFragmentByTag(FragmentCalc.TAG))
                        .setDistance(ActivityYandexMap.getDistance(data));
                break;
            case ActivityYandexMap.REQUEST_CODE_MAP_CENTER:
                Functions.logD("ActivityMain -- onActivityResult: ActivityYandexMap.REQUEST_CODE_MAP_CENTER");
                ActivityYandexMap.MapCenter mapCenter = ActivityYandexMap.getMapCenter(data);

                FuelingPreferenceManager.putMapCenter(mapCenter.text,
                        mapCenter.latitude, mapCenter.longitude);

                ((FragmentPreference) findFragmentByTag(FragmentPreference.TAG)).updateMapCenter();
                break;
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
            case FuelingDBHelper.FILTER_MODE_DATES:
                return 1;
            default:
                return 2;
        }
    }

    @Override
    public void onFilterChange(@FuelingDBHelper.FilterMode int filterMode) {
        Functions.logD("ActivityMain -- onFilterChange");

        int position = filterModeToPosition(filterMode);

        if (position != mToolbarSpinner.getSelectedItemPosition())
            mToolbarSpinner.setSelection(position);
    }

    @Override
    public void onDataLoadedFromBackup() {
        getFragmentFueling().forceLoad();
    }

    @Override
    public void onFragmentChange(int fragmentId) {
        Functions.logD("ActivityMain -- onFragmentChange");

        mCurrentFragmentId = mClickedMenuId = fragmentId;

        int titleId, subtitleId = -1;
        boolean showSpinner = false;

        switch (fragmentId) {
            case R.id.action_calc:
                titleId = R.string.title_calc;
                break;
            case R.id.action_chart_cost:
                titleId = R.string.title_chart_cost;
                subtitleId = R.string.title_chart_cost_subtitle;
                break;
            case R.id.action_settings:
                titleId = ((FragmentPreference) findFragmentByTag(FragmentPreference.TAG)).getTitleId();
                break;
            case R.id.action_backup:
                titleId = R.string.title_backup;
                break;
            case R.id.action_about:
                titleId = R.string.title_about;
                break;
            default:
                titleId = -1;
                showSpinner = true;
        }

        setTitle(titleId);
        setSubtitle(subtitleId);
        mToolbarSpinner.setVisibility(showSpinner ? View.VISIBLE : View.GONE);

        mNavigationView.getMenu().findItem(fragmentId).setChecked(true);
    }

    @Override
    public void setTitle(int resId) {
        if (resId != -1) setTitle(getString(resId));
        else setTitle(null);
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

    private void setSubtitle(int resId) {
        if (resId != -1) mToolbarMain.setSubtitle(resId);
        else mToolbarMain.setSubtitle(null);
    }

    @Override
    public void OnPreferenceScreenChanged(CharSequence title) {
        Functions.logD("ActivityMain -- OnPreferenceScreenChanged: title == " + title);

        setTitle(title == null ? getString(R.string.title_prefs) : title);
        toggleDrawer(mDrawerToggle, mDrawerLayout, title != null);
    }

    private void toggleDrawer(final ActionBarDrawerToggle actionBarDrawerToggle, final DrawerLayout drawerLayout,
                              final boolean showArrow) {
        if (mDrawerToggle.isDrawerIndicatorEnabled() != showArrow) return;

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

    private void startSync(boolean start) {
        // Вызывается всегда при старте приложения
        // для обновления PendingIntent
        // Синхронизация запускается только при запуске приложения
        // start == (savedInstanceState == null)

        Functions.logD("ActivityMain -- startSync");

        if (FuelingPreferenceManager.isSyncEnabled())
            startService(new Intent(this, ServiceSync.class)
                    .putExtra(ServiceSync.EXTRA_START, start)
                    .putExtra(ServiceSync.EXTRA_PENDING,
                            createPendingResult(ServiceSync.REQUEST_CODE, new Intent(), 0)));
    }

    private void updateSyncStatus() {
        String status;

        if (ServiceSync.isSyncInProcess()) {
            status = getString(R.string.sync_in_process);

            mImgSync.setImageResource(R.drawable.ic_sync_grey600_24dp);

            mImgSync.startAnimation(mAnimationSync);
        } else {
            mImgSync.clearAnimation();

            if (FuelingPreferenceManager.isSyncEnabled()) {
                if (ServiceSync.isErrorInProcess()) {
                    status = getString(R.string.sync_error);

                    mImgSync.setImageResource(R.drawable.ic_sync_alert_grey600_24dp);
                } else {
                    status = FuelingPreferenceManager.getLastSync();

                    status = !status.isEmpty() ?
                            getString(R.string.sync_done, status) :
                            getString(R.string.sync_not_performed);

                    mImgSync.setImageResource(R.drawable.ic_sync_grey600_24dp);
                }
            } else {
                status = getString(R.string.sync_disabled);

                mImgSync.setImageResource(R.drawable.ic_sync_off_grey600_24dp);
            }
        }

        mBtnSync.setText(status);
    }

    @Override
    public void OnPreferenceSyncEnabledChanged() {
        updateSyncStatus();
    }
}