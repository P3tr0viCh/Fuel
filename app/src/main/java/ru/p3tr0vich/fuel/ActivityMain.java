package ru.p3tr0vich.fuel;
// TODO: change color of selected item in spinners and popup menu

import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Spinner;

import java.lang.reflect.Method;
import java.util.Date;

public class ActivityMain extends AppCompatActivity implements
        FragmentFueling.FilterChangeListener,
        FragmentFueling.RecordChangeListener {

    private static final String ACTION_LOADING = "ru.p3tr0vich.fuel.ACTION_LOADING";
    private static final String EXTRA_LOADING = "ru.p3tr0vich.fuel.EXTRA_LOADING";

    private Spinner mToolbarSpinner;
    private Toolbar mToolbarMainDates;
    private View mToolbarShadow;

    private BroadcastReceiver mLoadingStatusReceiver;

    private boolean mAbbrevMonth;
    private boolean mDateFromClicked;
    private boolean mToolbarMainDatesVisible;

    private FragmentFueling getFragmentFueling() {
        return (FragmentFueling) getFragmentManager().findFragmentById(R.id.fragmentFueling);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAbbrevMonth = getResources().getDimension(R.dimen.abbrev_month) != 0;
        Functions.sApplicationContext = this.getApplicationContext();

        Toolbar toolbarMain = (Toolbar) findViewById(R.id.toolbarMain);
        setSupportActionBar(toolbarMain);

        mToolbarMainDates = (Toolbar) findViewById(R.id.toolbarMainDates);
        mToolbarShadow = findViewById(R.id.toolbarShadow);

        //noinspection ConstantConditions
        mToolbarSpinner = new AppCompatSpinner(getSupportActionBar().getThemedContext());

        Functions.addSpinnerInToolbar(getSupportActionBar(), mToolbarSpinner, toolbarMain,
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        Log.d(Const.LOG_TAG, "ActivityMain -- onItemSelected");

                        FuelingDBHelper.FilterMode filterMode = Functions.positionToFilterMode(position);

                        setToolbarDatesVisible(filterMode == FuelingDBHelper.FilterMode.DATES, true);

                        getFragmentFueling().setFilterMode(filterMode);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });

        FuelingDBHelper.Filter filter = getFragmentFueling().getFilter();

        setToolbarDatesVisible(filter.filterMode == FuelingDBHelper.FilterMode.DATES, false);

        updateFilterDate(true, filter.dateFrom);
        updateFilterDate(false, filter.dateTo);

        mLoadingStatusReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                getFragmentFueling().setProgressBarVisible(intent.getBooleanExtra(EXTRA_LOADING, false));
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(mLoadingStatusReceiver,
                new IntentFilter(ACTION_LOADING));

        findViewById(R.id.btnDateFrom).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDateDialog(true);
            }
        });

        findViewById(R.id.btnDateTo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDateDialog(false);
            }
        });

        Log.d(Const.LOG_TAG, "ActivityMain -- onCreate");
    }

    private void setToolbarDatesVisible(final boolean visible, final boolean animate) {
        if (mToolbarMainDatesVisible == visible) return;

        mToolbarMainDatesVisible = visible;

        final RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mToolbarMainDates.getLayoutParams();

        if (animate) {
            final ValueAnimator valueAnimatorShadowShow = ValueAnimator.ofInt(0, getResources().getDimensionPixelSize(R.dimen.toolbar_shadow_height));
            valueAnimatorShadowShow
                    .setDuration(Const.ANIMATION_DURATION_TOOLBAR_SHADOW)
                    .addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        public void onAnimationUpdate(ValueAnimator animation) {
                            Functions.setViewHeight(mToolbarShadow, (Integer) animation.getAnimatedValue());
                        }
                    });

            final ValueAnimator valueAnimatorShadowHide = ValueAnimator.ofInt(getResources().getDimensionPixelSize(R.dimen.toolbar_shadow_height), 0);
            valueAnimatorShadowHide
                    .setDuration(Const.ANIMATION_DURATION_TOOLBAR_SHADOW)
                    .addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        public void onAnimationUpdate(ValueAnimator animation) {
                            Functions.setViewHeight(mToolbarShadow, (Integer) animation.getAnimatedValue());
                        }
                    });

            final ValueAnimator valueAnimatorToolbar = ValueAnimator.ofInt(
                    visible ? 0 : getResources().getDimensionPixelSize(R.dimen.toolbar_height),
                    visible ? getResources().getDimensionPixelSize(R.dimen.toolbar_height) : 0);
            valueAnimatorToolbar
                    .setDuration(Const.ANIMATION_DURATION_TOOLBAR)
                    .addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        public void onAnimationUpdate(ValueAnimator animation) {
                            Functions.setViewTopMargin(mToolbarMainDates, layoutParams, (Integer) animation.getAnimatedValue());
                        }
                    });

            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playSequentially(valueAnimatorShadowShow, valueAnimatorToolbar, valueAnimatorShadowHide);
            animatorSet.start();
        } else
            Functions.setViewTopMargin(mToolbarMainDates, layoutParams, visible ? getResources().getDimensionPixelSize(R.dimen.toolbar_height) : 0);
    }

    private void showDateDialog(boolean dateFrom) {
        mDateFromClicked = dateFrom;

        FuelingDBHelper.Filter filter = getFragmentFueling().getFilter();

        FragmentDialogDate.show(this, mDateFromClicked ? filter.dateFrom : filter.dateTo);
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
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    protected boolean onPrepareOptionsPanel(View view, Menu menu) {
        try {
            Method declaredMethod = menu.getClass().getDeclaredMethod("setOptionalIconsVisible", boolean.class);
            declaredMethod.setAccessible(true);
            declaredMethod.invoke(menu, true);
        } catch (Exception e) {
            //
        }
        return super.onPrepareOptionsPanel(view, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_calc:
                ActivityCalc.start(this);
                return true;
            case R.id.action_backup:
                ActivityBackup.start(this);
                return true;
            case R.id.action_settings:
                ActivityPreference.start(this);
                return true;
            case R.id.action_about:
                FragmentDialogAbout.show(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
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
                Const.RecordAction action = ActivityFuelingRecordChange.getAction(data);

                fuelingRecord = ActivityFuelingRecordChange.getFuelingRecord(data);

                switch (action) {
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
            case ActivityBackup.REQUEST_CODE:
                fragmentFueling.updateAfterChange();
                break;
            case FragmentDialogDate.REQUEST_CODE:
                Date date = FragmentDialogDate.getDate(data);
                if (mDateFromClicked) fragmentFueling.setFilterDateFrom(date);
                else fragmentFueling.setFilterDateTo(date);
                updateFilterDate(mDateFromClicked, date);
        }
    }


    private void updateFilterDate(boolean dateFrom, Date date) {
        int buttonId = dateFrom ? R.id.btnDateFrom : R.id.btnDateTo;
        Button button = (Button) findViewById(buttonId);
        button.setText(Functions.dateToString(date, true, mAbbrevMonth));
    }

    @Override
    public void onFilterChange(FuelingDBHelper.FilterMode filterMode) {
        Log.d(Const.LOG_TAG, "ActivityMain -- onFilterChange");

        int position = Functions.filterModeToPosition(filterMode);

        if (position != mToolbarSpinner.getSelectedItemPosition())
            mToolbarSpinner.setSelection(position);
    }
}
