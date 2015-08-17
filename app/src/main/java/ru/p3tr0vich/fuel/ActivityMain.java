package ru.p3tr0vich.fuel;

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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

public class ActivityMain extends AppCompatActivity implements
        FragmentFueling.OnFilterChangeListener,
        FragmentFueling.OnRecordChangeListener,
        FragmentFuel.OnFragmentChangedListener,
        FragmentBackup.OnDataLoadedFromBackupListener {

    private static final String ACTION_LOADING = "ru.p3tr0vich.fuel.ACTION_LOADING";
    private static final String EXTRA_LOADING = "ru.p3tr0vich.fuel.EXTRA_LOADING";

    private Toolbar mToolbarMain;
    private Spinner mToolbarSpinner;

    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private NavigationView mNavigationView;

    private BroadcastReceiver mLoadingStatusReceiver;

    private int mCurrentFragmentId;

    private FragmentFuel getFragmentFuel(String fragmentTag) {
        return fragmentTag != null ?
                (FragmentFuel) getFragmentManager().findFragmentByTag(fragmentTag) : null;
    }

    private FragmentFueling getFragmentFueling() {
        return (FragmentFueling) getFragmentManager().findFragmentByTag(FragmentFueling.TAG);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Functions.sApplicationContext = getApplicationContext();

        Functions.LogD("**************** ActivityMain -- onCreate ****************");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mToolbarMain = (Toolbar) findViewById(R.id.toolbarMain);
        setSupportActionBar(mToolbarMain);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);
        mDrawerToggle = new ActionBarDrawerToggle(this,
                mDrawerLayout, mToolbarMain, R.string.app_name, R.string.app_name);
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();

        mNavigationView = (NavigationView) findViewById(R.id.drawerNavigationView);
        mNavigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {
                selectItem(menuItem.getItemId());
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
                        Functions.LogD("ActivityMain -- onItemSelected");

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
                    .setTransition(FragmentTransaction.TRANSIT_NONE)
                    .commit();
        }
    }

    private Fragment getFragmentNewInstance(String fragmentTag) {
        if (getFragmentFuel(fragmentTag) == null)
            switch (fragmentTag) {
                case FragmentCalc.TAG:
                    return new FragmentCalc();
                case FragmentChartCost.TAG:
                    return new FragmentChartCost();
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
        if (mCurrentFragmentId == menuId) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
            return;
        }

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
                ActivityPreference.start(this);
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

        mDrawerLayout.closeDrawer(GravityCompat.START);
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerVisible(GravityCompat.START))
            mDrawerLayout.closeDrawer(GravityCompat.START);
        else if (getFragmentManager().getBackStackEntryCount() != 0)
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
                ((FragmentCalc) getFragmentFuel(FragmentCalc.TAG))
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
        Functions.LogD("ActivityMain -- onFilterChange");

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
        Functions.LogD("ActivityMain -- onFragmentChanged");

        mCurrentFragmentId = fragmentId;

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

        for (int i = 0; i < mNavigationView.getMenu().size(); i++)
            mNavigationView.getMenu().getItem(i).setChecked(false);
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

    private void setSubtitle(int resId) {
        if (resId != -1) mToolbarMain.setSubtitle(resId);
        else mToolbarMain.setSubtitle(null);
    }
}