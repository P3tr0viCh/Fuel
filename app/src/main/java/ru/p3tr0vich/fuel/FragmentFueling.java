package ru.p3tr0vich.fuel;

import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.melnykov.fab.FloatingActionButton;
import com.pnikosis.materialishprogress.ProgressWheel;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;

import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.List;

public class FragmentFueling extends FragmentBase implements
        LoaderManager.LoaderCallbacks<Cursor>, FuelingAdapter.OnFuelingRecordsChangeListener {

    public static final String TAG = "FragmentFueling";

    private static final String KEY_FILTER_MODE = "KEY_FILTER_MODE";
    private static final String KEY_FILTER_DATE_FROM = "KEY_FILTER_DATE_FROM";
    private static final String KEY_FILTER_DATE_TO = "KEY_FILTER_DATE_TO";

    private static final int LOADER_LIST_ID = 0;

    private Toolbar mToolbarDates;
    private View mToolbarShadow;

    private RelativeLayout mLayoutMain;
    private LinearLayout mLayoutTotal;

    private boolean mToolbarDatesVisible;
    private boolean mLayoutTotalVisible;

    private DatabaseHelper.Filter mFilter;

    private FuelingAdapter mFuelingAdapter;

    private Button mBtnDateFrom;
    private Button mBtnDateTo;

    private RecyclerView mRecyclerViewFueling;

    private ProgressWheel mProgressWheelFueling;
    private TextView mTextNoRecords;

    private FloatingActionButton mFloatingActionButton;

    private CalcTotalTask mCalcTotalTask;

    private final Handler mHandler = new Handler();

    private TextView mTextAverage;
    private TextView mTextCostSum;

    private long mIdForScroll = -1;

    private Snackbar mSnackbar = null;
    private final Snackbar.Callback snackBarCallback = new Snackbar.Callback() {
        // Workaround for bug

        @Override
        public void onDismissed(Snackbar snackbar, int event) {
            if (event == DISMISS_EVENT_SWIPE)
                mFloatingActionButton.toggle(true, true, true);
        }
    };

    private FuelingRecord deletedFuelingRecord = null;

    private final View.OnClickListener undoClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            UtilsLog.d(TAG, "undoClickListener", deletedFuelingRecord.toString());

            insertRecord(deletedFuelingRecord);

            deletedFuelingRecord = null;
        }
    };

    private OnFilterChangeListener mOnFilterChangeListener;
    private OnRecordChangeListener mOnRecordChangeListener;

    @Override
    public int getFragmentId() {
        return R.id.action_fueling;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFilter = new DatabaseHelper.Filter();

        if (savedInstanceState == null) {
            UtilsLog.d(TAG, "onCreate", "savedInstanceState == null");

            mFilter.mode = DatabaseHelper.Filter.MODE_CURRENT_YEAR;

            mFilter.dateFrom = PreferenceManagerFuel.getFilterDateFrom();
            mFilter.dateTo = PreferenceManagerFuel.getFilterDateTo();
        } else {
            UtilsLog.d(TAG, "onCreate", "savedInstanceState != null");

            switch (savedInstanceState.getInt(KEY_FILTER_MODE, DatabaseHelper.Filter.MODE_ALL)) {
                case DatabaseHelper.Filter.MODE_CURRENT_YEAR:
                    mFilter.mode = DatabaseHelper.Filter.MODE_CURRENT_YEAR;
                    break;
                case DatabaseHelper.Filter.MODE_YEAR:
                    mFilter.mode = DatabaseHelper.Filter.MODE_YEAR;
                    break;
                case DatabaseHelper.Filter.MODE_DATES:
                    mFilter.mode = DatabaseHelper.Filter.MODE_DATES;
                    break;
                default:
                    mFilter.mode = DatabaseHelper.Filter.MODE_ALL;
            }

            mFilter.dateFrom = savedInstanceState.getLong(KEY_FILTER_DATE_FROM);
            mFilter.dateTo = savedInstanceState.getLong(KEY_FILTER_DATE_TO);
        }
    }

    @SuppressLint("InflateParams")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        UtilsLog.d(TAG, "onCreateView");

        final boolean isPhone = Utils.isPhone();

        View view = inflater.inflate(R.layout.fragment_fueling, container, false);

        mToolbarDates = (Toolbar) view.findViewById(R.id.toolbarDates);
        mToolbarShadow = view.findViewById(R.id.toolbarShadow);

        mLayoutMain = (RelativeLayout) view.findViewById(R.id.layoutMain);
        mLayoutTotal = (LinearLayout) view.findViewById(R.id.layoutTotal);

        mRecyclerViewFueling = (RecyclerView) view.findViewById(R.id.recyclerViewFueling);
        mRecyclerViewFueling.setHasFixedSize(true);
        mRecyclerViewFueling.setItemAnimator(new DefaultItemAnimator());
        mRecyclerViewFueling.addItemDecoration(
                new DividerItemDecoration(getActivity(), isPhone ? -1 : FuelingAdapter.TYPE_FOOTER));
        mRecyclerViewFueling.setLayoutManager(new LinearLayoutManager(ApplicationFuel.getContext()));
        mRecyclerViewFueling.setAdapter(mFuelingAdapter = new FuelingAdapter(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doPopup(v);
            }
        }, this, isPhone, !isPhone));

        if (isPhone)
            mRecyclerViewFueling.addOnScrollListener(new OnRecyclerViewScrollListener(
                    getResources().getDimensionPixelOffset(R.dimen.recycler_view_scroll_threshold)) {

                @Override
                void onScrollUp() {
                    setTotalAndFabVisible(false);
                }

                @Override
                void onScrollDown() {
                    if (mSnackbar == null)
                        setTotalAndFabVisible(true);
                    else if (!mSnackbar.isShown())
                        setTotalAndFabVisible(true);
                }
            });

        mProgressWheelFueling = (ProgressWheel) view.findViewById(R.id.progressWheelFueling);
        mTextNoRecords = (TextView) view.findViewById(R.id.tvNoRecords);

        mTextAverage = (TextView) view.findViewById(R.id.tvAverage);
        mTextCostSum = (TextView) view.findViewById(R.id.tvCostSum);

        mFloatingActionButton = (FloatingActionButton) view.findViewById(R.id.floatingActionButton);
        mFloatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mOnRecordChangeListener.onRecordChange(Const.RECORD_ACTION_ADD, null);
            }
        });
        mFloatingActionButton.setScaleX(0.0f);
        mFloatingActionButton.setScaleY(0.0f);

        mBtnDateFrom = (Button) view.findViewById(R.id.btnDateFrom);
        mBtnDateFrom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDateDialog(true);
            }
        });
        mBtnDateFrom.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                doPopupDate(v, true);
                return true;
            }
        });

        mBtnDateTo = (Button) view.findViewById(R.id.btnDateTo);
        mBtnDateTo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDateDialog(false);
            }
        });
        mBtnDateTo.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                doPopupDate(v, false);
                return true;
            }
        });

        mToolbarDatesVisible = true;
        setToolbarDatesVisible(mFilter.mode == DatabaseHelper.Filter.MODE_DATES, false);

        mLayoutTotalVisible = true;

        updateFilterDateButtons(true, mFilter.dateFrom);
        updateFilterDateButtons(false, mFilter.dateTo);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        UtilsLog.d(TAG, "onActivityCreated", "savedInstanceState " +
                (savedInstanceState == null ? "=" : "!") + "= null");

        doSetFilterMode(mFilter.mode);

        getLoaderManager().initLoader(LOADER_LIST_ID, null, this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(KEY_FILTER_MODE, mFilter.mode);
        outState.putLong(KEY_FILTER_DATE_FROM, mFilter.dateFrom);
        outState.putLong(KEY_FILTER_DATE_TO, mFilter.dateTo);
    }

    @Override
    public void onDestroy() {
        mHandler.removeCallbacks(mRunnableShowNoRecords);
        mHandler.removeCallbacks(mRunnableShowProgressWheelFueling);

        calcTotalTaskCancel();

        PreferenceManagerFuel.putFilterDate(mFilter.dateFrom, mFilter.dateTo);

        super.onDestroy();
    }

    private void doSetFilterMode(@DatabaseHelper.Filter.Mode int filterMode) {
        mFilter.mode = filterMode;

        mOnFilterChangeListener.onFilterChange(filterMode);
    }

    public void setFilterMode(@DatabaseHelper.Filter.Mode int filterMode) {
        if (mFilter.mode != filterMode) {

            setToolbarDatesVisible(filterMode == DatabaseHelper.Filter.MODE_DATES, true);

            doSetFilterMode(filterMode);

            getLoaderManager().restartLoader(LOADER_LIST_ID, null, this);

            setTotalAndFabVisible(true);
        }
    }

    public void updateList(long id) {
        mIdForScroll = id;
        getLoaderManager().getLoader(LOADER_LIST_ID).forceLoad();
    }

    private void checkDateTime(@NonNull FuelingRecord fuelingRecord) {
        switch (mFilter.mode) {
            case DatabaseHelper.Filter.MODE_ALL:
                return;
            case DatabaseHelper.Filter.MODE_CURRENT_YEAR:
                if (UtilsDate.getCalendarInstance(fuelingRecord.getDateTime()).get(Calendar.YEAR) ==
                        UtilsDate.getCurrentYear()) return;

                break;
            case DatabaseHelper.Filter.MODE_DATES:
                final long dateTime = fuelingRecord.getDateTime();

                if (dateTime >= mFilter.dateFrom && dateTime <= mFilter.dateTo) return;

                break;
            case DatabaseHelper.Filter.MODE_YEAR:
                break;
        }

        setFilterMode(DatabaseHelper.Filter.MODE_ALL);
    }

    public void insertRecord(@NonNull FuelingRecord fuelingRecord) {
        checkDateTime(fuelingRecord);

        if (ContentProviderFuel.insertRecord(getContext(), fuelingRecord) == -1)
            Utils.toast(R.string.message_error_insert_record);
    }

    public void updateRecord(@NonNull FuelingRecord fuelingRecord) {
        checkDateTime(fuelingRecord);

        if (ContentProviderFuel.updateRecord(getContext(), fuelingRecord) == 0)
            Utils.toast(R.string.message_error_update_record);
    }

    private boolean markRecordAsDeleted(@NonNull FuelingRecord fuelingRecord) {
        final long id = fuelingRecord.getId();

        if (ContentProviderFuel.markRecordAsDeleted(getContext(), id) > 0)
            return true;
        else {
            Utils.toast(R.string.message_error_delete_record);

            return false;
        }
    }

    private LinearLayoutManager getRecyclerViewLayoutManager() {
        return (LinearLayoutManager) mRecyclerViewFueling.getLayoutManager();
    }

    private boolean isItemVisible(int position) {
        final int firstVisibleItem = getRecyclerViewLayoutManager()
                .findFirstCompletelyVisibleItemPosition();
        final int lastVisibleItem = getRecyclerViewLayoutManager()
                .findLastCompletelyVisibleItemPosition();

        return firstVisibleItem != RecyclerView.NO_POSITION &&
                lastVisibleItem != RecyclerView.NO_POSITION &&
                position > firstVisibleItem && position < lastVisibleItem;
    }

    private void scrollToPosition(int position) {
        if (position < 0) return;

        if (mFuelingAdapter.isShowHeader() && position == FuelingAdapter.HEADER_POSITION + 1)
            position = FuelingAdapter.HEADER_POSITION;


        if (isItemVisible(position)) return;

        getRecyclerViewLayoutManager().scrollToPositionWithOffset(position, 0);
    }

    private void setFilterDate(final long dateFrom, final long dateTo) {
        mFilter.dateFrom = dateFrom;
        mFilter.dateTo = dateTo;
        getLoaderManager().restartLoader(LOADER_LIST_ID, null, this);
    }

    private void setFilterDate(final boolean setDateFrom, final long date) {
        if (setDateFrom) mFilter.dateFrom = date;
        else mFilter.dateTo = date;
        getLoaderManager().restartLoader(LOADER_LIST_ID, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_LIST_ID:
                return new FuelingCursorLoader(getContext(), mFilter);
            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        UtilsLog.d(TAG, "onLoadFinished");

        mFuelingAdapter.setShowYear(mFilter.mode != DatabaseHelper.Filter.MODE_CURRENT_YEAR);

        mFuelingAdapter.swapCursor(data);

        setFabVisible(true); // TODO: неудачное место для вызова.
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mFuelingAdapter.swapCursor(null);
    }

    private void scrollToId() {
        if (mIdForScroll == -1) return;

        scrollToPosition(mFuelingAdapter.findPositionById(mIdForScroll));

        mIdForScroll = -1;
    }

    private void calcTotalTaskCancel() {
        if (mCalcTotalTask != null) mCalcTotalTask.cancel(false);
    }

    @Override
    public void OnFuelingRecordsChange(@NonNull List<FuelingRecord> fuelingRecords) {
        mHandler.removeCallbacks(mRunnableShowNoRecords);

        calcTotalTaskCancel();

        scrollToId();

        mCalcTotalTask = new CalcTotalTask(this, fuelingRecords);

        mCalcTotalTask.execute();

        if (fuelingRecords.isEmpty())
            mHandler.postDelayed(mRunnableShowNoRecords, Const.DELAYED_TIME_SHOW_NO_RECORDS);
        else
            mTextNoRecords.setVisibility(View.GONE);
    }

    private final Runnable mRunnableShowNoRecords = new Runnable() {
        @Override
        public void run() {
            Utils.setViewVisibleAnimate(mTextNoRecords, true);
        }
    };

    private void doPopup(final View v) {
        PopupMenu popupMenu = new PopupMenu(getActivity(), v);
        popupMenu.inflate(R.menu.menu_fueling);

        Object menuHelper = null;
        try {
            Field fMenuHelper = PopupMenu.class.getDeclaredField("mPopup");
            fMenuHelper.setAccessible(true);
            menuHelper = fMenuHelper.get(popupMenu);
            menuHelper.getClass().getDeclaredMethod("setForceShowIcon", boolean.class).invoke(menuHelper, true);
        } catch (Exception e) {
            //
        }

        final long id = (long) v.getTag();

        popupMenu.setOnMenuItemClickListener(
                new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        FuelingRecord fuelingRecord =
                                ContentProviderFuel.getFuelingRecord(getContext(), id);

                        if (fuelingRecord == null) {
                            UtilsLog.d(TAG, "onMenuItemClick",
                                    "ContentProviderFuel.getFuelingRecord() == null");
                            updateList(-1);
                            return true;
                        }

                        switch (item.getItemId()) {
                            case R.id.action_fueling_update:
                                mOnRecordChangeListener.onRecordChange(Const.RECORD_ACTION_UPDATE, fuelingRecord);
                                return true;
                            case R.id.action_fueling_delete:
                                deletedFuelingRecord = fuelingRecord;

                                if (markRecordAsDeleted(fuelingRecord)) {
                                    mSnackbar = Snackbar
                                            .make(mLayoutMain, R.string.message_record_deleted,
                                                    Snackbar.LENGTH_LONG)
                                            .setAction(R.string.dialog_btn_cancel, undoClickListener)
                                            .setCallback(snackBarCallback);
                                    mSnackbar.show();
                                }

                                return true;
                            default:
                                return false;
                        }
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

    public void setLoading(boolean loading) {
        mHandler.removeCallbacks(mRunnableShowProgressWheelFueling);
        if (loading) {
            mHandler.postDelayed(mRunnableShowProgressWheelFueling,
                    Const.DELAYED_TIME_SHOW_PROGRESS_WHEEL);
        } else
            Utils.setViewVisibleAnimate(mProgressWheelFueling, false);
    }

    private final Runnable mRunnableShowProgressWheelFueling = new Runnable() {
        @Override
        public void run() {
            Utils.setViewVisibleAnimate(mProgressWheelFueling, true);
        }
    };

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mOnFilterChangeListener = (OnFilterChangeListener) context;
            mOnRecordChangeListener = (OnRecordChangeListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() +
                    " must implement OnFilterChangeListener, OnRecordChangeListener");
        }
    }

    private void setToolbarDatesVisible(final boolean visible, final boolean animate) {
        if (mToolbarDatesVisible == visible) return;

        mToolbarDatesVisible = visible;

        final int toolbarDatesTopHidden =
                -getResources().getDimensionPixelSize(R.dimen.toolbar_height); // minus!
        final int toolbarShadowHeight =
                getResources().getDimensionPixelSize(R.dimen.toolbar_shadow_height);

        if (animate) {
            final ValueAnimator valueAnimatorShadowShow = ValueAnimator.ofInt(0, toolbarShadowHeight);
            valueAnimatorShadowShow
                    .setDuration(Const.ANIMATION_DURATION_TOOLBAR_SHADOW)
                    .addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        public void onAnimationUpdate(ValueAnimator animation) {
                            Utils.setViewHeight(mToolbarShadow,
                                    (Integer) animation.getAnimatedValue());
                        }
                    });

            final ValueAnimator valueAnimatorShadowHide = ValueAnimator.ofInt(toolbarShadowHeight, 0);
            valueAnimatorShadowHide
                    .setDuration(Const.ANIMATION_DURATION_TOOLBAR_SHADOW)
                    .addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        public void onAnimationUpdate(ValueAnimator animation) {
                            Utils.setViewHeight(mToolbarShadow, (Integer) animation.getAnimatedValue());
                        }
                    });

            final ValueAnimator valueAnimatorToolbar = ValueAnimator.ofInt(
                    visible ? toolbarDatesTopHidden : 0, visible ? 0 : toolbarDatesTopHidden);
            valueAnimatorToolbar
                    .setDuration(Const.ANIMATION_DURATION_TOOLBAR)
                    .addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        public void onAnimationUpdate(ValueAnimator animation) {
                            Utils.setViewTopMargin(mToolbarDates,
                                    (Integer) animation.getAnimatedValue());
                        }
                    });

            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playSequentially(valueAnimatorShadowShow,
                    valueAnimatorToolbar, valueAnimatorShadowHide);
            animatorSet.start();
        } else
            Utils.setViewTopMargin(mToolbarDates, visible ? 0 : toolbarDatesTopHidden);
    }

    private void setLayoutTotalVisible(final boolean visible) {
        if (mLayoutTotalVisible == visible) return;

        mLayoutTotalVisible = visible;

        final ValueAnimator valueAnimator = ValueAnimator.ofInt(
                (int) mLayoutTotal.getTranslationY(), visible ? 0 : mLayoutTotal.getHeight());
        valueAnimator
                .setDuration(visible ?
                        Const.ANIMATION_DURATION_LAYOUT_TOTAL_SHOW :
                        Const.ANIMATION_DURATION_LAYOUT_TOTAL_HIDE)
                .addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    public void onAnimationUpdate(ValueAnimator animation) {
                        int translationY = (Integer) animation.getAnimatedValue();

                        mLayoutTotal.setTranslationY(translationY);
                        Utils.setViewTopMargin(mLayoutTotal, -translationY);
                    }
                });
        valueAnimator.start();
    }

    private void setTotalAndFabVisible(boolean visible) {
        // FIXME: есть баг при количестве записей позволяющем сделать прокрутку вверх
        // при показанном лайоте, что приводит к его скрытию, после чего прокрутка вниз невозможна,
        // так как записи начинают влезать по высоте.
        // Три записи в альбомном режиме с показанным тулбаром выбора периода

        mFloatingActionButton.toggle(visible, true);
        setLayoutTotalVisible(visible);
    }

    public void setFabVisible(boolean visible) {
//        UtilsLog.d(TAG, "setFabVisible", "visible == " + visible);
        final float value = visible ? 1.0f : 0.0f;
        mFloatingActionButton.animate().scaleX(value).scaleY(value);
    }

    private void updateFilterDateButtons(final boolean dateFrom, final long date) {
        (dateFrom ? mBtnDateFrom : mBtnDateTo)
                .setText(UtilsFormat.dateTimeToString(date, true, Utils.isPhoneInPortrait()));
    }

    private void setPopupFilterDate(final boolean setDateFrom, final int menuId) {
        switch (menuId) {
            case R.id.action_dates_start_of_year:
            case R.id.action_dates_end_of_year:
                final Calendar calendar =
                        UtilsDate.getCalendarInstance(setDateFrom ? mFilter.dateFrom : mFilter.dateTo);

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

                final long date = calendar.getTimeInMillis();

                updateFilterDateButtons(setDateFrom, date);
                setFilterDate(setDateFrom, date);
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
                        calendarFrom.setTimeInMillis(setDateFrom ? mFilter.dateFrom : mFilter.dateTo);

                        year = calendarFrom.get(Calendar.YEAR);
                        break;
                    case R.id.action_dates_curr_year:
                    case R.id.action_dates_prev_year:
                        year = UtilsDate.getCurrentYear();
                        if (menuId == R.id.action_dates_prev_year) year--;
                }

                switch (menuId) {
                    case R.id.action_dates_winter:
                        // TODO: добавить выбор -- зима начала года и зима конца года
                        calendarFrom.set(year - 1, Calendar.DECEMBER, 1);
                        calendarTo.set(Calendar.YEAR, year);
                        calendarTo.set(Calendar.MONTH, Calendar.FEBRUARY);
                        calendarTo.set(Calendar.DAY_OF_MONTH,
                                calendarTo.getActualMaximum(Calendar.DAY_OF_MONTH));
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

                UtilsDate.setStartOfDay(calendarFrom);
                UtilsDate.setEndOfDay(calendarTo);

                final long dateFrom = calendarFrom.getTimeInMillis();
                final long dateTo = calendarTo.getTimeInMillis();

                updateFilterDateButtons(true, dateFrom);
                updateFilterDateButtons(false, dateTo);

                setFilterDate(dateFrom, dateTo);
        }
    }

    private void doPopupDate(final View v, final boolean dateFrom) {
        PopupMenu popupMenu = new PopupMenu(getActivity(), v);
        popupMenu.inflate(R.menu.menu_dates);

        Object menuHelper = null;
        try {
            Field fMenuHelper = PopupMenu.class.getDeclaredField("mPopup");
            fMenuHelper.setAccessible(true);
            menuHelper = fMenuHelper.get(popupMenu);
        } catch (Exception e) {
            //
        }

        popupMenu.setOnMenuItemClickListener(
                new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        setPopupFilterDate(dateFrom, item.getItemId());
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

    private void showDateDialog(final boolean dateFrom) {
        final Calendar calendar =
                UtilsDate.getCalendarInstance(dateFrom ? mFilter.dateFrom : mFilter.dateTo);

        DatePickerDialog.newInstance(
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {
                        calendar.set(year, monthOfYear, dayOfMonth);

                        final long date = calendar.getTimeInMillis();

                        updateFilterDateButtons(dateFrom, date);

                        setFilterDate(dateFrom, date);
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        ).show(getFragmentManager(), null);
    }

    public interface OnFilterChangeListener {
        // to Toolbar Spinner
        void onFilterChange(@DatabaseHelper.Filter.Mode int filterMode);
    }

    public interface OnRecordChangeListener {
        void onRecordChange(@Const.RecordAction int recordAction, FuelingRecord fuelingRecord);
    }

    static class FuelingCursorLoader extends CursorLoader {

        private final DatabaseHelper.Filter mFilter;

        public FuelingCursorLoader(Context context, DatabaseHelper.Filter filter) {
            super(context);
            mFilter = filter;
        }

        @Override
        public Cursor loadInBackground() {
            UtilsLog.d(TAG, "FuelingCursorLoader", "loadInBackground");

            BroadcastReceiverLoading.send(getContext(), true);

            try {
                return ContentProviderFuel.getAll(getContext(), mFilter);
            } finally {
                BroadcastReceiverLoading.send(getContext(), false);
            }
        }
    }

    static class CalcTotalResult {
        final float average;
        final float costSum;

        CalcTotalResult(float a, float s) {
            average = a;
            costSum = s;
        }
    }

    static class CalcTotalTask extends AsyncTask<Void, Void, CalcTotalResult> {

        private final List<FuelingRecord> mFuelingRecords;
        private FragmentFueling mFragmentFueling;

        CalcTotalTask(FragmentFueling fragmentFueling, List<FuelingRecord> fuelingRecords) {
            mFragmentFueling = fragmentFueling;
            mFuelingRecords = fuelingRecords;
        }

        @Override
        protected CalcTotalResult doInBackground(Void... params) {
            float costSum = 0, volumeSum = 0;
            float volume, total, firstTotal = 0, lastTotal = 0;

            FuelingRecord fuelingRecord;

            boolean completeData = true; // Во всех записях указаны объём заправки и текущий пробег

            final int size = mFuelingRecords.size();

            for (int i = 0; i < size; i++) {

                if (isCancelled()) return null;

                fuelingRecord = mFuelingRecords.get(i);

                costSum += fuelingRecord.getCost();

                if (completeData) {
                    volume = fuelingRecord.getVolume();
                    total = fuelingRecord.getTotal();

                    if (volume == 0 || total == 0) completeData = false;
                    else {
                        // Сортировка записей по дате в обратном порядке
                        // 0 -- последняя заправка
                        // Последний (i == 0) объём заправки не нужен -- неизвестно, сколько на ней будет пробег,
                        // в volumeSum не включается
                        if (i == 0) lastTotal = total;
                        else {
                            volumeSum += volume;
                            if (i == size - 1) firstTotal = total;
                        }
                    }
                }
            }

            float average;

            if (completeData)
                average = volumeSum != 0 ? (volumeSum / (lastTotal - firstTotal)) * 100 : 0;
            else {
                average = 0;
                int averageCount = 0;

                for (int i = 0; i < size - 1; i++) {

                    if (isCancelled()) return null;

                    lastTotal = mFuelingRecords.get(i).getTotal();
                    if (lastTotal != 0) {
                        volume = mFuelingRecords.get(i + 1).getVolume();
                        total = mFuelingRecords.get(i + 1).getTotal();

                        if (volume != 0 && total != 0) {
                            average += (volume / (lastTotal - total)) * 100;
                            averageCount++;
                        }
                    }
                }

                average = averageCount != 0 ? average / averageCount : 0;
            }

            return new CalcTotalResult(average, costSum);
        }

        @Override
        protected void onPostExecute(CalcTotalResult calcTotalResult) {
            if (calcTotalResult != null) {
                mFragmentFueling.mTextAverage.setText(UtilsFormat.floatToString(calcTotalResult.average));
                mFragmentFueling.mTextCostSum.setText(UtilsFormat.floatToString(calcTotalResult.costSum));
            }

            mFragmentFueling = null;
        }
    }
}