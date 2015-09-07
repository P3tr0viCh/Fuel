package ru.p3tr0vich.fuel;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
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
import android.view.animation.DecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

public class ActivityMain extends AppCompatActivity implements
        FragmentFueling.OnFilterChangeListener,
        FragmentFueling.OnRecordChangeListener,
        FragmentFuel.OnFragmentChangedListener,
        FragmentPreference.OnPreferenceScreenChangedListener,
        FragmentBackup.OnDataLoadedFromBackupListener {

    private static final String ACTION_LOADING = "ru.p3tr0vich.fuel.ACTION_LOADING";
    private static final String EXTRA_LOADING = "ru.p3tr0vich.fuel.EXTRA_LOADING";

    private Toolbar mToolbarMain;
    private Spinner mToolbarSpinner;

    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private NavigationView mNavigationView;

    private BroadcastReceiver mLoadingStatusReceiver;

    private int mCurrentFragmentId, mClickedMenuId;

    private Fragment findFragmentByTag(String fragmentTag) {
        return fragmentTag != null ?
                getFragmentManager().findFragmentByTag(fragmentTag) : null;
    }

    private FragmentFueling getFragmentFueling() {
        return (FragmentFueling) getFragmentManager().findFragmentByTag(FragmentFueling.TAG);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Functions.sApplicationContext = getApplicationContext();

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
                Functions.logD("ActivityMain -- mDrawerToggle: onClick");
                FragmentPreference fragmentPreference = (FragmentPreference) findFragmentByTag(FragmentPreference.TAG);
                if (fragmentPreference.goToRootScreen()) {
                    setTitle(R.string.title_prefs);
                    toggleDrawer(mDrawerToggle, mDrawerLayout, false);
                }
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
                getFragmentFueling().setProgressBarVisible(intent.getBooleanExtra(EXTRA_LOADING, false));
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(mLoadingStatusReceiver,
                new IntentFilter(ACTION_LOADING));

        if (savedInstanceState == null) {
            mCurrentFragmentId = R.id.action_fueling;
            getFragmentManager().beginTransaction()
                    .add(R.id.contentFrame, new FragmentFueling(), FragmentFueling.TAG)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit();
        }
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
        FragmentManager fragmentManager = getFragmentManager();

        if (!getFragmentFueling().isVisible()) fragmentManager.popBackStack();

        switch (menuId) {
            case R.id.action_calc:
                fragmentTag = FragmentCalc.TAG;
                break;
            case R.id.action_chart_cost:
                fragmentTag = FragmentChartCost.TAG;
                break;
            case R.id.action_settings:
//                ActivityPreference.start(this);
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
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .addToBackStack(null)
                    .commit();
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerVisible(GravityCompat.START))
            mDrawerLayout.closeDrawer(GravityCompat.START);
        else if (mCurrentFragmentId == R.id.action_settings) {
            FragmentPreference fragmentPreference = (FragmentPreference) findFragmentByTag(FragmentPreference.TAG);
            if (fragmentPreference.goToRootScreen()) {
                setTitle(R.string.title_prefs);
                toggleDrawer(mDrawerToggle, mDrawerLayout, false);
                return;
            }
        }
        if (getFragmentManager().getBackStackEntryCount() != 0)
            getFragmentManager().popBackStack();
        else super.onBackPressed();
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

    public static Intent getLoadingBroadcast(boolean startLoading) {
        return new Intent(ACTION_LOADING).putExtra(EXTRA_LOADING, startLoading);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    @Override
    public void onRecordChange(Const.RecordAction recordAction, FuelingRecord fuelingRecord) {
        if (recordAction != Const.RecordAction.DELETE)
            // ADD, UPDATE
            ActivityFuelingRecordChange.start(this, recordAction, fuelingRecord);
        else
            FragmentDialogDeleteRecord.show(this, fuelingRecord);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) return;

        FuelingRecord fuelingRecord;
        FragmentFueling fragmentFueling = getFragmentFueling();

        switch (requestCode) {
            case ActivityFuelingRecordChange.REQUEST_CODE:
                fuelingRecord = ActivityFuelingRecordChange.getFuelingRecord(data);
                switch (ActivityFuelingRecordChange.getAction(data)) {
                    case ADD:
                        fragmentFueling.addRecord(fuelingRecord);
                        break;
                    case UPDATE:
                        fragmentFueling.updateRecord(fuelingRecord);
                        break;
                }
                break;
            case FragmentDialogDeleteRecord.REQUEST_CODE:
                fuelingRecord = FragmentDialogDeleteRecord.getFuelingRecord(data);
                fragmentFueling.deleteRecord(fuelingRecord);
                break;
            case ActivityYandexMap.REQUEST_CODE:
                ((FragmentCalc) findFragmentByTag(FragmentCalc.TAG))
                        .setDistance(ActivityYandexMap.getDistance(data));
                break;
        }
    }

    private static FuelingDBHelper.FilterMode positionToFilterMode(int position) {
        switch (position) {
            case 0:
                return FuelingDBHelper.FilterMode.CURRENT_YEAR;
            case 1:
                return FuelingDBHelper.FilterMode.DATES;
            default:
                return FuelingDBHelper.FilterMode.ALL;
        }
    }

    private static int filterModeToPosition(FuelingDBHelper.FilterMode filterMode) {
        switch (filterMode) {
            case CURRENT_YEAR:
                return 0;
            case DATES:
                return 1;
            default:
                return 2;
        }
    }

    @Override
    public void onFilterChange(FuelingDBHelper.FilterMode filterMode) {
        Functions.logD("ActivityMain -- onFilterChange");

        int position = filterModeToPosition(filterMode);

        if (position != mToolbarSpinner.getSelectedItemPosition())
            mToolbarSpinner.setSelection(position);
    }

    @Override
    public void onDataLoadedFromBackup() {
        getFragmentFueling().updateAfterChange();
    }

    @Override
    public void onFragmentChanged(int fragmentId) {
        Functions.logD("ActivityMain -- onFragmentChanged");

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
                titleId = R.string.title_prefs;
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

//        for (int i = 0; i < mNavigationView.getMenu().size(); i++)
//            mNavigationView.getMenu().getItem(i).setChecked(false);
        mNavigationView.getMenu().findItem(fragmentId).setChecked(true);
    }

    @Override
    public void setTitle(int resId) {
        ActionBar actionBar = getSupportActionBar();

        if (actionBar == null) return;

        if (resId != -1) {
            actionBar.setTitle(resId);
            actionBar.setDisplayShowTitleEnabled(true);
        } else {
            actionBar.setTitle(null);
            actionBar.setDisplayShowTitleEnabled(false);
        }
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
        setTitle(title);
        toggleDrawer(mDrawerToggle, mDrawerLayout, true);
    }

    private void toggleDrawer(final ActionBarDrawerToggle actionBarDrawerToggle, final DrawerLayout drawerLayout,
                              final boolean showArrow) {
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
                float slideOffset = (Float) valueAnimator.getAnimatedValue();
                actionBarDrawerToggle.onDrawerSlide(drawerLayout, slideOffset);
            }
        });
        anim.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                if (!showArrow)
                    mDrawerToggle.setDrawerIndicatorEnabled(true);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (showArrow)
                    mDrawerToggle.setDrawerIndicatorEnabled(false);
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
}