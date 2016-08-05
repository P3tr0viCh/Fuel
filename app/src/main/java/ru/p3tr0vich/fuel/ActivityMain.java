package ru.p3tr0vich.fuel;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SyncStatusObserver;
import android.content.res.Configuration;
import android.os.Bundle;
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

import ru.p3tr0vich.fuel.helpers.ContactsHelper;
import ru.p3tr0vich.fuel.helpers.DatabaseHelper;
import ru.p3tr0vich.fuel.helpers.PreferencesHelper;
import ru.p3tr0vich.fuel.models.FuelingRecord;
import ru.p3tr0vich.fuel.utils.Utils;
import ru.p3tr0vich.fuel.utils.UtilsDate;
import ru.p3tr0vich.fuel.utils.UtilsLog;

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

    private static final int REQUEST_CODE_REQUEST_SYNC = 400;

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

    private BroadcastReceiverLoading mBroadcastReceiverLoading;
    private BroadcastReceiverDatabaseChanged mBroadcastReceiverDatabaseChanged;

    private PreferencesHelper mPreferencesHelper;

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
    private @interface FragmentId {
    }

    private static final int FRAGMENT_FUELING_ID = 0;
    private static final int FRAGMENT_CALC_ID = 1;
    private static final int FRAGMENT_CHART_COST_ID = 2;
    private static final int FRAGMENT_BACKUP_ID = 3;
    private static final int FRAGMENT_PREFERENCES_ID = 4;
    private static final int FRAGMENT_ABOUT_ID = 5;

    @Nullable
    private Fragment findFragmentByTag(@Nullable String fragmentTag) {
        return fragmentTag != null ?
                getSupportFragmentManager().findFragmentByTag(fragmentTag) : null;
    }

    @NonNull
    private FragmentInterface getCurrentFragment() {
        return (FragmentInterface) getSupportFragmentManager().findFragmentById(R.id.content_frame);

    }

    @Nullable
    private FragmentFueling getFragmentFueling() {
        return (FragmentFueling) getSupportFragmentManager().findFragmentByTag(FragmentFueling.TAG);
    }

    @Nullable
    private FragmentPreferences getFragmentPreferences() {
        return (FragmentPreferences) getSupportFragmentManager().findFragmentByTag(FragmentPreferences.TAG);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        UtilsLog.d(TAG, "onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initToolbar();
        initToolbarSpinner();
        initDrawer();

        mSyncAccount = new SyncAccount(this);

        mPreferencesHelper = PreferencesHelper.getInstance(this);

        initAnimationSync();
        initSyncViews();

        updateSyncStatus();

        initLoadingStatusReceiver();
        initDatabaseChangedReceiver();

        mSyncMonitor = ContentResolver.addStatusChangeListener(
                ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE, this);

        if (savedInstanceState == null) {
            mCurrentFragmentId = FRAGMENT_FUELING_ID;
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.content_frame,
                            FragmentFueling.newInstance(FRAGMENT_FUELING_ID),
                            FragmentFueling.TAG)
                    .setTransition(FragmentTransaction.TRANSIT_NONE)
                    .commit();

            ContentObserverService.requestSync(this);
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
        mToolbarMain = (Toolbar) findViewById(R.id.toolbar_main);
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

        mToolbarSpinner.setDropDownVerticalOffset(-Utils.getSupportActionBarSize(this)); // minus!

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
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
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

        mNavigationView = (NavigationView) findViewById(R.id.drawer_navigation_view);
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
        mAnimationSync.setDuration(Utils.getInteger(R.integer.animation_duration_sync));
        mAnimationSync.setRepeatCount(Animation.INFINITE);
    }

    private void initSyncViews() {
        mImgSync = (ImageView) findViewById(R.id.image_sync);

        mBtnSync = (TextView) findViewById(R.id.btn_sync);
        mBtnSync.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ContentObserverService.requestSync(ActivityMain.this,
                        createPendingResult(REQUEST_CODE_REQUEST_SYNC, new Intent(), 0));
            }
        });
    }

    // TODO: move to FragmentFueling
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

    private void initDatabaseChangedReceiver() {
        mBroadcastReceiverDatabaseChanged = new BroadcastReceiverDatabaseChanged() {
            @Override
            public void onReceive(long id) {
                FragmentFueling fragmentFueling = getFragmentFueling();
                if (fragmentFueling != null) fragmentFueling.updateList(id);
            }
        };
        mBroadcastReceiverDatabaseChanged.register(this);
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
                        mPreferencesHelper.keys.sync);

                fragment.setArguments(bundle);

                mOpenPreferenceSync = false;
            }

            fragmentManager.beginTransaction()
                    .replace(R.id.content_frame, fragment, fragmentTag)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
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

        mBroadcastReceiverDatabaseChanged.unregister(this);
        mBroadcastReceiverLoading.unregister(this);

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

        switch (requestCode) {
            case REQUEST_CODE_ACTIVITY_MAP_DISTANCE:
                FragmentCalc fragmentCalc = (FragmentCalc) findFragmentByTag(FragmentCalc.TAG);

                if (fragmentCalc != null)
                    fragmentCalc.setDistance(ActivityYandexMap.getDistance(data));

                break;
            case REQUEST_CODE_ACTIVITY_MAP_CENTER:
                ActivityYandexMap.MapCenter mapCenter = ActivityYandexMap.getMapCenter(data);

                mPreferencesHelper.putMapCenter(mapCenter.text,
                        mapCenter.latitude, mapCenter.longitude);

                break;
            case REQUEST_CODE_DIALOG_YANDEX_AUTH:
                Utils.openUrl(this, SyncYandexDisk.URL.AUTH, null);

                break;
            case REQUEST_CODE_ACTIVITY_CONTACTS:
                String SMSAddress = ContactsHelper.getPhoneNumber(this, data);

                if (SMSAddress != null)
                    mPreferencesHelper.putSMSAddress(SMSAddress);

                break;
            case REQUEST_CODE_REQUEST_SYNC:
                if (data != null)
                    switch (ContentObserverService.getResult(data)) {
                        case ContentObserverService.RESULT_INTERNET_DISCONNECTED:
                            FragmentDialogMessage.show(this,
                                    null, getString(R.string.message_error_no_internet));

                            break;
                        case ContentObserverService.RESULT_TOKEN_EMPTY:
                            showDialogNeedAuth();

                            break;
                        case ContentObserverService.RESULT_SYNC_DISABLED:
                            mClickedMenuId = R.id.action_preferences;
                            mOpenPreferenceSync = true;

                            if (mDrawerLayout.isDrawerOpen(GravityCompat.START))
                                mDrawerLayout.closeDrawer(GravityCompat.START);
                            else
                                selectItem(mClickedMenuId);

                            break;
                        case ContentObserverService.RESULT_REQUEST_DONE:
                        case ContentObserverService.RESULT_SYNC_ACTIVE:
                        case ContentObserverService.RESULT_SYNC_DELAYED_REQUEST:
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
            case DatabaseHelper.Filter.MODE_YEAR:
            case DatabaseHelper.Filter.MODE_DATES:
                return 1;
            case DatabaseHelper.Filter.MODE_ALL:
                return 2;
            case DatabaseHelper.Filter.MODE_CURRENT_YEAR:
            case DatabaseHelper.Filter.MODE_TWO_LAST_RECORDS:
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
        anim.setDuration(Utils.getInteger(R.integer.animation_duration_drawer_toggle));
        anim.start();
    }

    private void updateSyncStatus() {
        final String text;
        final int imgId;
        final boolean syncActive = mSyncAccount.isSyncActive();

        if (syncActive) {
            text = getString(R.string.sync_in_process);
            imgId = R.drawable.ic_sync;
        } else {
            if (mPreferencesHelper.isSyncEnabled()) {
                if (mSyncAccount.isYandexDiskTokenEmpty()) {
                    text = getString(R.string.sync_no_token);
                    imgId = R.drawable.ic_sync_off;
                } else {
                    if (mPreferencesHelper.getLastSyncHasError()) {
                        text = getString(R.string.sync_error);
                        imgId = R.drawable.ic_sync_alert;
                    } else {
                        final long dateTime = mPreferencesHelper.getLastSyncDateTime();

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
    public void onPreferenceSyncEnabledChanged(boolean enabled) {
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
            case ActivityYandexMap.MAP_TYPE_DISTANCE:
                requestCode = REQUEST_CODE_ACTIVITY_MAP_DISTANCE;
                break;
            case ActivityYandexMap.MAP_TYPE_CENTER:
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

    @Override
    public void onPreferenceSMSTextPatternClick() {
        ActivityDialog.start(this, ActivityDialog.DIALOG_SMS_TEXT_PATTERN);
    }
}