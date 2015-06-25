package ru.p3tr0vich.fuel;

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
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Spinner;

import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Calendar;
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

        Button btnDateFrom = (Button) findViewById(R.id.btnDateFrom);
        btnDateFrom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDateDialog(true);
            }
        });
        btnDateFrom.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                doPopupDate(v, true);
                return true;
            }
        });

        Button btnDateTo = (Button) findViewById(R.id.btnDateTo);
        btnDateTo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDateDialog(false);
            }
        });
        btnDateTo.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                doPopupDate(v, false);
                return true;
            }
        });

        Log.d(Const.LOG_TAG, "**************** ActivityMain -- onCreate ****************");
    }

    private void doPopupDate(final View v, final boolean dateFrom) {
        PopupMenu popupMenu = new PopupMenu(this, v);
        popupMenu.inflate(R.menu.menu_dates);

        Object menuHelper = null;
        try {
            Field fMenuHelper = PopupMenu.class.getDeclaredField("mPopup");
            fMenuHelper.setAccessible(true);
            menuHelper = fMenuHelper.get(popupMenu);
//            menuHelper.getClass().getDeclaredMethod("setForceShowIcon", boolean.class).invoke(menuHelper, true);
        } catch (Exception e) {
            //
        }

        popupMenu.setOnMenuItemClickListener(
                new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        setFilterDate(dateFrom, item.getItemId());
                        return true;
                    }
                }
        );

        popupMenu.show();

        try {
            if (menuHelper == null) return;

            Field fListPopup = menuHelper.getClass().getDeclaredField("mPopup");
            fListPopup.setAccessible(true);
            Object listPopup = fListPopup.get(menuHelper);
            Class<?> listPopupClass = listPopup.getClass();

            // Magic number
            listPopupClass.getDeclaredMethod("setVerticalOffset", int.class).invoke(listPopup, -v.getHeight() - 8);

            listPopupClass.getDeclaredMethod("show").invoke(listPopup);
        } catch (Exception e) {
            //
        }
    }

    private void setFilterDate(final boolean setDateFrom, final int menuId) {
        FuelingDBHelper.Filter filter = getFragmentFueling().getFilter();

        switch (menuId) {
            case R.id.action_dates_start_of_year:
            case R.id.action_dates_end_of_year:
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(setDateFrom ? filter.dateFrom : filter.dateTo);

                switch (menuId) {
                    case R.id.action_dates_start_of_year:
                        calendar.set(Calendar.MONTH, Calendar.JANUARY);
                        calendar.set(Calendar.DAY_OF_MONTH, 1);
                        break;
                    case R.id.action_dates_end_of_year:
                        calendar.set(Calendar.MONTH, Calendar.DECEMBER);
                        calendar.set(Calendar.DAY_OF_MONTH, 31);
                        break;
                }

                Date date = calendar.getTime();

                updateFilterDate(setDateFrom, date);
                getFragmentFueling().setFilterDate(setDateFrom, date);
                break;
            case R.id.action_dates_winter:
            case R.id.action_dates_summer:
            case R.id.action_dates_curr_year:
            case R.id.action_dates_prev_year:
                Calendar calendarFrom = Calendar.getInstance();
                Calendar calendarTo = Calendar.getInstance();

                int year = 0;

                switch (menuId) {
                    case R.id.action_dates_winter:
                    case R.id.action_dates_summer:
                        calendarFrom.setTime(setDateFrom ? filter.dateFrom : filter.dateTo);

                        year = calendarFrom.get(Calendar.YEAR);
                        break;
                    case R.id.action_dates_curr_year:
                    case R.id.action_dates_prev_year:
                        year = Calendar.getInstance().get(Calendar.YEAR);
                        if (menuId == R.id.action_dates_prev_year) year--;
                }

                switch (menuId) {
                    case R.id.action_dates_winter:
                        calendarFrom.set(year - 1, Calendar.DECEMBER, 1);
                        calendarTo.set(Calendar.YEAR, year);
                        calendarTo.set(Calendar.MONTH, Calendar.FEBRUARY);
                        calendarTo.set(Calendar.DAY_OF_MONTH, calendarTo.getActualMaximum(Calendar.DAY_OF_MONTH));
                        break;
                    case R.id.action_dates_summer:
                        calendarFrom.set(year, Calendar.JUNE, 1);
                        calendarTo.set(year, Calendar.AUGUST, 31);
                        break;
                    case R.id.action_dates_curr_year:
                    case R.id.action_dates_prev_year:
                        calendarFrom.set(year, Calendar.JANUARY, 1);
                        calendarTo.set(year, Calendar.DECEMBER, 31);

                }

                Date dateFrom = calendarFrom.getTime();
                Date dateTo = calendarTo.getTime();

                updateFilterDate(true, dateFrom);
                updateFilterDate(false, dateTo);
                getFragmentFueling().setFilterDate(dateFrom, dateTo);
        }
    }

    private void setToolbarDatesVisible(final boolean visible, final boolean animate) {
        if (mToolbarMainDatesVisible == visible) return;

        mToolbarMainDatesVisible = visible;

        final int toolbarHeight = getResources().getDimensionPixelSize(R.dimen.toolbar_height);
        final int toolbarShadowHeight = getResources().getDimensionPixelSize(R.dimen.toolbar_shadow_height);
        final RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mToolbarMainDates.getLayoutParams();

        if (animate) {
            final ValueAnimator valueAnimatorShadowShow = ValueAnimator.ofInt(0, toolbarShadowHeight);
            valueAnimatorShadowShow
                    .setDuration(Const.ANIMATION_DURATION_TOOLBAR_SHADOW)
                    .addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        public void onAnimationUpdate(ValueAnimator animation) {
                            Functions.setViewHeight(mToolbarShadow, (Integer) animation.getAnimatedValue());
                        }
                    });

            final ValueAnimator valueAnimatorShadowHide = ValueAnimator.ofInt(toolbarShadowHeight, 0);
            valueAnimatorShadowHide
                    .setDuration(Const.ANIMATION_DURATION_TOOLBAR_SHADOW)
                    .addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        public void onAnimationUpdate(ValueAnimator animation) {
                            Functions.setViewHeight(mToolbarShadow, (Integer) animation.getAnimatedValue());
                        }
                    });

            final ValueAnimator valueAnimatorToolbar = ValueAnimator.ofInt(
                    visible ? 0 : toolbarHeight, visible ? toolbarHeight : 0);
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
            Functions.setViewTopMargin(mToolbarMainDates, layoutParams, visible ? toolbarHeight : 0);
    }

    private void showDateDialog(final boolean dateFrom) {
        mDateFromClicked = dateFrom;

        FuelingDBHelper.Filter filter = getFragmentFueling().getFilter();

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(mDateFromClicked ? filter.dateFrom : filter.dateTo);
        DatePickerDialog.newInstance(
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {
                        Calendar calendar = Calendar.getInstance();
                        calendar.set(year, monthOfYear, dayOfMonth);
                        Date date = calendar.getTime();

                        updateFilterDate(mDateFromClicked, date);
                        getFragmentFueling().setFilterDate(mDateFromClicked, date);
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        ).show(getFragmentManager(), null);
    }

    private void updateFilterDate(final boolean dateFrom, final Date date) {
        ((Button) findViewById(dateFrom ? R.id.btnDateFrom : R.id.btnDateTo))
                .setText(Functions.dateToString(date, true, mAbbrevMonth));
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
        }
    }

    @Override
    public void onFilterChange(FuelingDBHelper.FilterMode filterMode) {
        Log.d(Const.LOG_TAG, "ActivityMain -- onFilterChange");

        int position = Functions.filterModeToPosition(filterMode);

        if (position != mToolbarSpinner.getSelectedItemPosition())
            mToolbarSpinner.setSelection(position);
    }
}
