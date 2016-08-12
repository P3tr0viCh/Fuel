package ru.p3tr0vich.fuel;

import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.melnykov.fab.FloatingActionButton;
import com.pnikosis.materialishprogress.ProgressWheel;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;

import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.List;

import ru.p3tr0vich.fuel.factories.FuelingTotalViewFactory;
import ru.p3tr0vich.fuel.helpers.ContentProviderHelper;
import ru.p3tr0vich.fuel.helpers.DatabaseHelper;
import ru.p3tr0vich.fuel.models.FuelingRecord;
import ru.p3tr0vich.fuel.utils.Utils;
import ru.p3tr0vich.fuel.utils.UtilsDate;
import ru.p3tr0vich.fuel.utils.UtilsFormat;
import ru.p3tr0vich.fuel.utils.UtilsLog;
import ru.p3tr0vich.fuel.views.FuelingTotalView;

public class FragmentFueling extends FragmentBase implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = "FragmentFueling";

    private static final boolean LOG_ENABLED = false;

    private static final String KEY_FILTER_MODE = "KEY_FILTER_MODE";
    private static final String KEY_FILTER_DATE_FROM = "KEY_FILTER_DATE_FROM";
    private static final String KEY_FILTER_DATE_TO = "KEY_FILTER_DATE_TO";

    private static final int FUELING_CURSOR_LOADER_ID = 0;
    private static final int FUELING_TOTAL_CURSOR_LOADER_ID = 1;

    private Toolbar mToolbarDates;
    private View mToolbarShadow;

    private RelativeLayout mLayoutMain;
    private ViewGroup mTotalPanel;

    private boolean mToolbarDatesVisible;
    private boolean mTotalPanelVisible;

    private DatabaseHelper.Filter mFilter;

    private FuelingAdapter mFuelingAdapter;

    private Button mBtnDateFrom;
    private Button mBtnDateTo;

    private RecyclerView mRecyclerView;

    private ProgressWheel mProgressWheel;
    private TextView mTextNoRecords;

    private FloatingActionButton mFloatingActionButton;

    private final Handler mHandler = new Handler();

    private FuelingTotalView mFuelingTotalView;

    private long mIdForScroll = -1;

    private Snackbar mSnackbar = null;
    private final Snackbar.Callback mSnackBarCallback = new Snackbar.Callback() {
        @Override
        public void onDismissed(Snackbar snackbar, int event) {
            // Workaround for bug

            if (event == DISMISS_EVENT_SWIPE)
                mFloatingActionButton.toggle(true, true, true);
        }
    };

    private FuelingRecord mDeletedFuelingRecord = null;

    private final View.OnClickListener mUndoClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            ContentProviderHelper.insertRecord(getContext(), mDeletedFuelingRecord);

            mDeletedFuelingRecord = null;
        }
    };

    private OnFilterChangeListener mOnFilterChangeListener;
    private OnRecordChangeListener mOnRecordChangeListener;

    private static class AnimationDuration {
        public final int layoutTotalShow;
        public final int layoutTotalHide;
        public final int startDelayFab;

        public AnimationDuration() {
            startDelayFab = Utils.getInteger(R.integer.animation_start_delay_fab);
            layoutTotalShow = Utils.getInteger(R.integer.animation_duration_layout_total_show);
            layoutTotalHide = Utils.getInteger(R.integer.animation_duration_layout_total_hide);
        }
    }

    private AnimationDuration mAnimationDuration;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFilter = new DatabaseHelper.Filter();

        mAnimationDuration = new AnimationDuration();

        if (savedInstanceState == null) {
            if (LOG_ENABLED) UtilsLog.d(TAG, "onCreate", "savedInstanceState == null");

            mFilter.mode = DatabaseHelper.Filter.MODE_CURRENT_YEAR;

            mFilter.dateFrom = preferencesHelper.getFilterDateFrom();
            mFilter.dateTo = preferencesHelper.getFilterDateTo();
        } else {
            if (LOG_ENABLED) UtilsLog.d(TAG, "onCreate", "savedInstanceState != null");

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
        if (LOG_ENABLED) UtilsLog.d(TAG, "onCreateView");

        final boolean isPhone = Utils.isPhone();

        View view = inflater.inflate(R.layout.fragment_fueling, container, false);

        mFuelingTotalView = FuelingTotalViewFactory.getFuelingTotalView(view);

        mToolbarDates = (Toolbar) view.findViewById(R.id.toolbar_dates);
        mToolbarShadow = view.findViewById(R.id.view_toolbar_shadow);

        mLayoutMain = (RelativeLayout) view.findViewById(R.id.layout_main);

        mTotalPanel = isPhone ? (ViewGroup) view.findViewById(R.id.total_panel) : null;

        mRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());

        DividerItemDecorationFueling itemDecoration = new DividerItemDecorationFueling(getContext());
        if (!isPhone) itemDecoration.setFooterType(FuelingAdapter.TYPE_FOOTER);
        mRecyclerView.addItemDecoration(itemDecoration);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecyclerView.setAdapter(mFuelingAdapter = new FuelingAdapter(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doPopup(v);
            }
        }, isPhone, !isPhone));

        if (isPhone)
            mRecyclerView.addOnScrollListener(new OnRecyclerViewScrollListener(
                    getResources().getDimensionPixelOffset(R.dimen.recycler_view_scroll_threshold)) {

                @Override
                public void onScrollUp() {
                    setTotalAndFabVisible(false);
                }

                @Override
                public void onScrollDown() {
                    if (mSnackbar == null || !mSnackbar.isShown()) setTotalAndFabVisible(true);
                }
            });

        mProgressWheel = (ProgressWheel) view.findViewById(R.id.progress_wheel);
        mProgressWheel.setVisibility(View.GONE);
        mTextNoRecords = (TextView) view.findViewById(R.id.text_no_records);
        mTextNoRecords.setVisibility(View.GONE);

        mFloatingActionButton = (FloatingActionButton) view.findViewById(R.id.fab);
        mFloatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mOnRecordChangeListener.onRecordChange(null);
            }
        });
        mFloatingActionButton.setScaleX(0.0f);
        mFloatingActionButton.setScaleY(0.0f);

        mBtnDateFrom = (Button) view.findViewById(R.id.btn_date_from);
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

        mBtnDateTo = (Button) view.findViewById(R.id.btn_date_to);
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

        Utils.setBackgroundTint(mBtnDateFrom, R.color.toolbar_title_text, R.color.primary_light);
        Utils.setBackgroundTint(mBtnDateTo, R.color.toolbar_title_text, R.color.primary_light);

        mToolbarDatesVisible = true;
        setToolbarDatesVisible(mFilter.mode == DatabaseHelper.Filter.MODE_DATES, false);

        mTotalPanelVisible = true;

        updateFilterDateButtons(true, mFilter.dateFrom);
        updateFilterDateButtons(false, mFilter.dateTo);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (LOG_ENABLED) UtilsLog.d(TAG, "onActivityCreated", "savedInstanceState " +
                (savedInstanceState == null ? "=" : "!") + "= null");

        doSetFilterMode(mFilter.mode);

        getLoaderManager().initLoader(FUELING_CURSOR_LOADER_ID, null, this);
        getLoaderManager().initLoader(FUELING_TOTAL_CURSOR_LOADER_ID, null, this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(KEY_FILTER_MODE, mFilter.mode);
        outState.putLong(KEY_FILTER_DATE_FROM, mFilter.dateFrom);
        outState.putLong(KEY_FILTER_DATE_TO, mFilter.dateTo);
    }

    @Override
    public void onDestroyView() {
        mFuelingTotalView.destroy();

        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        mHandler.removeCallbacks(mRunnableShowNoRecords);
        mHandler.removeCallbacks(mRunnableShowProgressWheelFueling);

        preferencesHelper.putFilterDate(mFilter.dateFrom, mFilter.dateTo);

        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        setFabVisible(true);
    }

    @Override
    public void onPause() {
        setFabVisible(false);
        super.onPause();
    }

    private void doSetFilterMode(@DatabaseHelper.Filter.Mode int filterMode) {
        mFilter.mode = filterMode;

        mOnFilterChangeListener.onFilterChange(filterMode);
    }

    /**
     * Изменяет текущий фильтр и вызывает restartLoader в случае изменения.
     *
     * @param filterMode новый фильтр.
     * @return true, если фильтр изменился.
     */
    public boolean setFilterMode(@DatabaseHelper.Filter.Mode int filterMode) {
        if (mFilter.mode != filterMode) {

            setToolbarDatesVisible(filterMode == DatabaseHelper.Filter.MODE_DATES, true);

            doSetFilterMode(filterMode);

            getLoaderManager().restartLoader(FUELING_CURSOR_LOADER_ID, null, this);

            setTotalAndFabVisible(true);

            return true;
        }

        return false;
    }

    private boolean isDateTimeInCurrentYear(final long dateTime) {
        int year = UtilsDate.getCalendarInstance(dateTime).get(Calendar.YEAR);
        return year == UtilsDate.getCurrentYear();
    }

    /**
     * Проверяет, входит ли dateTime в фильтр записей mFilter.
     *
     * @param dateTime дата и время.
     * @return true, если dateTime входит в фильтр, можно вызвать forceLoad,
     * и false, если фильтр был изменён, при этом был вызван restartLoader.
     */
    private boolean checkDateTime(final long dateTime) {
        switch (mFilter.mode) {
            case DatabaseHelper.Filter.MODE_ALL:
                return true;

            case DatabaseHelper.Filter.MODE_CURRENT_YEAR:
                if (isDateTimeInCurrentYear(dateTime))
                    return true;

            case DatabaseHelper.Filter.MODE_DATES:
                if (dateTime >= mFilter.dateFrom && dateTime <= mFilter.dateTo)
                    return true;
                else if (isDateTimeInCurrentYear(dateTime))
                    return !setFilterMode(DatabaseHelper.Filter.MODE_CURRENT_YEAR); // not

            case DatabaseHelper.Filter.MODE_YEAR:
            case DatabaseHelper.Filter.MODE_TWO_LAST_RECORDS:
        }

        return !setFilterMode(DatabaseHelper.Filter.MODE_ALL); // not
    }

    public void updateList(long id) {
        mIdForScroll = id;

        boolean forceLoad = true;

        if (id != -1) {
            FuelingRecord fuelingRecord = ContentProviderHelper.getFuelingRecord(getContext(), id);

            if (fuelingRecord != null)
                forceLoad = checkDateTime(fuelingRecord.getDateTime());
        }

        if (forceLoad)
            getLoaderManager().getLoader(FUELING_CURSOR_LOADER_ID).forceLoad();

        getLoaderManager().getLoader(FUELING_TOTAL_CURSOR_LOADER_ID).forceLoad();
    }

    private boolean markRecordAsDeleted(@NonNull FuelingRecord fuelingRecord) {
        final long id = fuelingRecord.getId();

        if (ContentProviderHelper.markRecordAsDeleted(getContext(), id) > 0)
            return true;
        else {
            Utils.toast(R.string.message_error_delete_record);

            return false;
        }
    }

    private LinearLayoutManager getRecyclerViewLayoutManager() {
        return (LinearLayoutManager) mRecyclerView.getLayoutManager();
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
        getLoaderManager().restartLoader(FUELING_CURSOR_LOADER_ID, null, this);
    }

    private void setFilterDate(final boolean setDateFrom, final long date) {
        if (setDateFrom) mFilter.dateFrom = date;
        else mFilter.dateTo = date;
        getLoaderManager().restartLoader(FUELING_CURSOR_LOADER_ID, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case FUELING_CURSOR_LOADER_ID:
                return new FuelingCursorLoader(getContext(), mFilter);
            case FUELING_TOTAL_CURSOR_LOADER_ID:
                return new FuelingTotalCursorLoader(getContext());
            default:
                return null;
        }
    }

    private void swapRecords(@Nullable Cursor data) {
        mHandler.removeCallbacks(mRunnableShowNoRecords);

        List<FuelingRecord> records = DatabaseHelper.getFuelingRecords(data);

        if (records == null || records.isEmpty())
            mHandler.postDelayed(mRunnableShowNoRecords, Utils.getInteger(R.integer.delayed_time_show_no_records));
        else
            mTextNoRecords.setVisibility(View.GONE);

        mFuelingAdapter.setShowYear(mFilter.mode != DatabaseHelper.Filter.MODE_CURRENT_YEAR);
        mFuelingAdapter.swapRecords(records);

        if (mIdForScroll != -1) {
            scrollToPosition(mFuelingAdapter.findPositionById(mIdForScroll));
            mIdForScroll = -1;
        }

        mFuelingTotalView.onFuelingRecordsChanged(records);
    }

    private void updateTotal(@Nullable Cursor data) {
        List<FuelingRecord> records = DatabaseHelper.getFuelingRecords(data);
        mFuelingTotalView.onLastFuelingRecordsChanged(records);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (LOG_ENABLED) UtilsLog.d(TAG, "onLoadFinished");

        switch (loader.getId()) {
            case FUELING_CURSOR_LOADER_ID:
                swapRecords(data);
                break;
            case FUELING_TOTAL_CURSOR_LOADER_ID:
                updateTotal(data);
                break;
        }

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (LOG_ENABLED) UtilsLog.d(TAG, "onLoaderReset");

        switch (loader.getId()) {
            case FUELING_CURSOR_LOADER_ID:
                swapRecords(null);
                break;
            case FUELING_TOTAL_CURSOR_LOADER_ID:
                updateTotal(null);
                break;
        }
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
                                ContentProviderHelper.getFuelingRecord(getContext(), id);

                        if (fuelingRecord == null) {
                            UtilsLog.d(TAG, "onMenuItemClick",
                                    "ContentProviderHelper.getFuelingRecord() == null");
                            updateList(-1);
                            return true;
                        }

                        switch (item.getItemId()) {
                            case R.id.action_fueling_update:
                                mOnRecordChangeListener.onRecordChange(fuelingRecord);
                                return true;
                            case R.id.action_fueling_delete:
                                mDeletedFuelingRecord = fuelingRecord;

                                if (markRecordAsDeleted(fuelingRecord)) {
                                    mSnackbar = Snackbar
                                            .make(mLayoutMain, R.string.message_record_deleted,
                                                    Snackbar.LENGTH_LONG)
                                            .setAction(R.string.dialog_btn_cancel, mUndoClickListener)
                                            .setCallback(mSnackBarCallback);
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
                    Utils.getInteger(R.integer.delayed_time_show_progress_wheel));
        } else
            Utils.setViewVisibleAnimate(mProgressWheel, false);
    }

    private final Runnable mRunnableShowProgressWheelFueling = new Runnable() {
        @Override
        public void run() {
            Utils.setViewVisibleAnimate(mProgressWheel, true);
        }
    };

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mOnFilterChangeListener = (OnFilterChangeListener) context;
            mOnRecordChangeListener = (OnRecordChangeListener) context;
        } catch (ClassCastException e) {
            throw new ImplementException(context,
                    new Class[]{OnFilterChangeListener.class, OnRecordChangeListener.class});
        }
    }

    private void setToolbarDatesVisible(final boolean visible, final boolean animate) {
        if (mToolbarDatesVisible == visible) return;

        mToolbarDatesVisible = visible;

        final int toolbarDatesTopHidden = -Utils.getSupportActionBarSize(getContext()); // minus!
        final int toolbarShadowHeight =
                getResources().getDimensionPixelSize(R.dimen.toolbar_shadow_height);

        if (animate) {
            final ValueAnimator valueAnimatorShadowShow = ValueAnimator.ofInt(0, toolbarShadowHeight);
            valueAnimatorShadowShow
                    .setDuration(Utils.getInteger(R.integer.animation_duration_toolbar_shadow))
                    .addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        public void onAnimationUpdate(ValueAnimator animation) {
                            Utils.setViewHeight(mToolbarShadow, (int) animation.getAnimatedValue());
                        }
                    });

            final ValueAnimator valueAnimatorShadowHide = ValueAnimator.ofInt(toolbarShadowHeight, 0);
            valueAnimatorShadowHide
                    .setDuration(Utils.getInteger(R.integer.animation_duration_toolbar_shadow))
                    .addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        public void onAnimationUpdate(ValueAnimator animation) {
                            Utils.setViewHeight(mToolbarShadow, (int) animation.getAnimatedValue());
                        }
                    });

            final ValueAnimator valueAnimatorToolbar = ValueAnimator.ofInt(
                    visible ? toolbarDatesTopHidden : 0, visible ? 0 : toolbarDatesTopHidden);
            valueAnimatorToolbar
                    .setDuration(Utils.getInteger(R.integer.animation_duration_toolbar))
                    .addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        public void onAnimationUpdate(ValueAnimator animation) {
                            Utils.setViewTopMargin(mToolbarDates, (int) animation.getAnimatedValue());
                        }
                    });

            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playSequentially(valueAnimatorShadowShow,
                    valueAnimatorToolbar, valueAnimatorShadowHide);
            animatorSet.start();
        } else
            Utils.setViewTopMargin(mToolbarDates, visible ? 0 : toolbarDatesTopHidden);
    }

    private void setTotalPanelVisible(final boolean visible) {
        if (mTotalPanelVisible == visible) return;

        mTotalPanelVisible = visible;

        ValueAnimator valueAnimator = ValueAnimator.ofInt(
                (int) mTotalPanel.getTranslationY(), visible ? 0 : mTotalPanel.getHeight());
        valueAnimator
                .setDuration(visible ?
                        mAnimationDuration.layoutTotalShow :
                        mAnimationDuration.layoutTotalHide)
                .addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    int translationY;

                    public void onAnimationUpdate(ValueAnimator animation) {
                        translationY = (int) animation.getAnimatedValue();

                        mTotalPanel.setTranslationY(translationY);
                        Utils.setViewTopMargin(mTotalPanel, -translationY);
                    }
                });
        valueAnimator.start();
    }

    private void setTotalAndFabVisible(boolean visible) {
        mFloatingActionButton.toggle(visible, true);
        setTotalPanelVisible(visible);
    }

    public void setFabVisible(boolean visible) {
        final float value = visible ? 1.0f : 0.0f;
        mFloatingActionButton.animate()
                .setStartDelay(mAnimationDuration.startDelayFab).scaleX(value).scaleY(value);
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

    /**
     * Вызывается при изменении фильтра.
     * Используется в главной активности для обновления списка в тулбаре.
     */
    public interface OnFilterChangeListener {
        /**
         * @param filterMode текущий фильтр.
         */
        void onFilterChange(@DatabaseHelper.Filter.Mode int filterMode);
    }

    /**
     * Вызывается при добавлении или обновлении записи.
     * Используется в главной активности для вызова активности изменения записи.
     */
    public interface OnRecordChangeListener {
        /**
         * @param fuelingRecord если null -- добавляется новая запись,
         *                      иначе -- запись обновляется.
         */
        void onRecordChange(@Nullable FuelingRecord fuelingRecord);
    }

    private static class FuelingCursorLoader extends CursorLoader {

        private final DatabaseHelper.Filter mFilter;

        FuelingCursorLoader(Context context, DatabaseHelper.Filter filter) {
            super(context);
            mFilter = filter;
        }

        @Override
        public Cursor loadInBackground() {
            if (LOG_ENABLED) UtilsLog.d(TAG, "FuelingCursorLoader", "loadInBackground");

            BroadcastReceiverLoading.send(getContext(), true);

            try {
                return ContentProviderHelper.getAll(getContext(), mFilter);
            } finally {
                BroadcastReceiverLoading.send(getContext(), false);
            }
        }
    }

    private static class FuelingTotalCursorLoader extends CursorLoader {

        FuelingTotalCursorLoader(Context context) {
            super(context);
        }

        @Override
        public Cursor loadInBackground() {
            if (LOG_ENABLED) UtilsLog.d(TAG, "FuelingTotalCursorLoader", "loadInBackground");

            return ContentProviderHelper.getTwoLastRecords(getContext());
        }
    }
}