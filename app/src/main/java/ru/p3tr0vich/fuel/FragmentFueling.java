package ru.p3tr0vich.fuel;

import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
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

import com.pnikosis.materialishprogress.ProgressWheel;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class FragmentFueling extends FragmentFuel implements
        LoaderManager.LoaderCallbacks<Cursor> {

    public static final String TAG = "FragmentFueling";

    private static final String KEY_FILTER_MODE = "KEY_FILTER_MODE";
    private static final String KEY_FILTER_DATE_FROM = "KEY_FILTER_DATE_FROM";
    private static final String KEY_FILTER_DATE_TO = "KEY_FILTER_DATE_TO";

    private static final int LOADER_LIST_ID = 0;

    private Toolbar mToolbarDates;
    private View mToolbarShadow;

    private boolean mDateFromClicked;
    private boolean mToolbarDatesVisible;

    private FuelingDBHelper db;
    private FuelingDBHelper.Filter mFilter;

    private FuelingAdapter mFuelingAdapter;

    private Button mBtnDateFrom;
    private Button mBtnDateTo;

    private RecyclerView mRecyclerViewFueling;

    private ProgressWheel mProgressWheelFueling;

    private TextView mTextAverage;
    private TextView mTextCostSum;

    private long mIdForScroll = -1;

    private OnFilterChangeListener mOnFilterChangeListener;
    private OnRecordChangeListener mOnRecordChangeListener;

    @Override
    public int getFragmentId() {
        return R.id.action_fueling;
    }

    @SuppressLint("InflateParams")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Functions.logD("FragmentFueling -- onCreateView");

        View view = inflater.inflate(R.layout.fragment_fueling, container, false);

        mToolbarDates = (Toolbar) view.findViewById(R.id.toolbarDates);
        mToolbarShadow = view.findViewById(R.id.toolbarShadow);

        mRecyclerViewFueling = (RecyclerView) view.findViewById(R.id.recyclerViewFueling);
        mRecyclerViewFueling.setHasFixedSize(true);
        mRecyclerViewFueling.setItemAnimator(new DefaultItemAnimator());
        mRecyclerViewFueling.addItemDecoration(new DividerItemDecoration(getActivity(), null));

        mProgressWheelFueling = (ProgressWheel) view.findViewById(R.id.progressWheelFueling);

        mTextAverage = (TextView) view.findViewById(R.id.tvAverage);
        mTextCostSum = (TextView) view.findViewById(R.id.tvCostSum);

        view.findViewById(R.id.floatingActionButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mOnRecordChangeListener.onRecordChange(Const.RecordAction.ADD, null);
            }
        });

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
        setToolbarDatesVisible(mFilter.filterMode == FuelingDBHelper.FilterMode.DATES, false);

        updateFilterDateButtons(true, mFilter.dateFrom);
        updateFilterDateButtons(false, mFilter.dateTo);

        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFilter = new FuelingDBHelper.Filter();

        if (savedInstanceState == null) {
            Functions.logD("FragmentFueling -- onCreate: savedInstanceState == null");

            SharedPreferences sPref = PreferenceManager.getDefaultSharedPreferences(getActivity());

            mFilter.filterMode = FuelingDBHelper.FilterMode.CURRENT_YEAR;
            mFilter.dateFrom = Functions.sqlDateToDate(
                    sPref.getString(getString(R.string.pref_filter_date_from),
                            Functions.dateToSQLite(new Date())));
            mFilter.dateTo = Functions.sqlDateToDate(
                    sPref.getString(getString(R.string.pref_filter_date_to),
                            Functions.dateToSQLite(new Date())));

        } else {
            Functions.logD("FragmentFueling -- onCreate: savedInstanceState != null");

            mFilter.filterMode = (FuelingDBHelper.FilterMode) savedInstanceState.getSerializable(KEY_FILTER_MODE);
            mFilter.dateFrom = (Date) savedInstanceState.getSerializable(KEY_FILTER_DATE_FROM);
            mFilter.dateTo = (Date) savedInstanceState.getSerializable(KEY_FILTER_DATE_TO);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable(KEY_FILTER_MODE, mFilter.filterMode);
        outState.putSerializable(KEY_FILTER_DATE_FROM, mFilter.dateFrom);
        outState.putSerializable(KEY_FILTER_DATE_TO, mFilter.dateTo);

        Functions.logD("FragmentFueling -- onSaveInstanceState");
    }

    @Override
    public void onDestroy() {
        Functions.logD("FragmentFueling -- onDestroy");

        PreferenceManager.getDefaultSharedPreferences(getActivity())
                .edit()
                .putString(getString(R.string.pref_filter_date_from), Functions.dateToSQLite(mFilter.dateFrom))
                .putString(getString(R.string.pref_filter_date_to), Functions.dateToSQLite(mFilter.dateTo))
                .apply();

        super.onDestroy();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Functions.logD("FragmentFueling -- onActivityCreated");

        db = new FuelingDBHelper();

        mRecyclerViewFueling.setLayoutManager(new LinearLayoutManager(getContext()));

        mFuelingAdapter = new FuelingAdapter(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doPopup(v);
            }
        });

        mRecyclerViewFueling.setAdapter(mFuelingAdapter);

        doSetFilterMode(mFilter.filterMode);

        getLoaderManager().initLoader(LOADER_LIST_ID, null, this);
    }

    private void doSetFilterMode(FuelingDBHelper.FilterMode filterMode) {
        mFilter.filterMode = filterMode;
//        mFuelingCursorAdapter.showYear = (filterMode != FuelingDBHelper.FilterMode.CURRENT_YEAR);

        mOnFilterChangeListener.onFilterChange(filterMode);
    }

    public boolean setFilterMode(FuelingDBHelper.FilterMode filterMode) {
        // Результат:
        // true - фильтр не изменился.
        // false - фильтр изменён и вызван рестарт лоадер (список полностью обновлён).

        Functions.logD("FragmentFueling -- setFilterMode: new FilterMode == " + filterMode +
                ", current FilterMode == " + mFilter.filterMode);
        if (mFilter.filterMode != filterMode) {

            setToolbarDatesVisible(filterMode == FuelingDBHelper.FilterMode.DATES, true);

            doSetFilterMode(filterMode);

            getLoaderManager().restartLoader(LOADER_LIST_ID, null, this);

            return false;
        } else return true;
    }

    private boolean needUpdateCurrentList(FuelingRecord fuelingRecord) {
        // Вызов после добавления или изменения записи.

        // Результат:
        // true - добавление или обновление записи в текущем списке.
        // false - список полностью обновлён, добавлять или изменять запись в списке не нужно.

        // Если установлен фильтр "все записи", то возвращается true,
        // иначе проверяется год в дате записи.
        // Если год в дате записи текущий,
        // то нужно установить фильтр -> "текущий год".
        // Если год в дате записи не текущий,
        // то нужно установить фильтр -> "все записи".

        // В setFilterMode проверяется текущий фильтр.
        // Если он уже необходимый, то setFilterMode возвращает true,
        // иначе в setFilterMode вызывается рестарт лоадер и возвращается false.

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(Functions.sqlDateToDate(fuelingRecord.getSQLiteDate()));

        return mFilter.filterMode == FuelingDBHelper.FilterMode.ALL ||
                setFilterMode(calendar.get(Calendar.YEAR) == Functions.getCurrentYear() ?
                        FuelingDBHelper.FilterMode.CURRENT_YEAR : FuelingDBHelper.FilterMode.ALL);
    }

    public void forceLoad() {
        getLoaderManager().getLoader(LOADER_LIST_ID).forceLoad();
    }

    public void addRecord(FuelingRecord fuelingRecord) {
        mIdForScroll = db.insertRecord(fuelingRecord);

        if (mIdForScroll > -1) {
            fuelingRecord.setId(mIdForScroll);

            if (needUpdateCurrentList(fuelingRecord)) {
                int position = mFuelingAdapter.addRecord(fuelingRecord);

                if (position == mFuelingAdapter.getFirstRecordPosition())
                    position = FuelingAdapter.HEADER_POSITION;

                scrollToPosition(position);
            }
        }
    }

    public void updateRecord(FuelingRecord fuelingRecord) {
        // TODO: Скакать при изменении если не видно

        if (db.updateRecord(fuelingRecord) > -1) {
            mIdForScroll = fuelingRecord.getId();

            if (needUpdateCurrentList(fuelingRecord)) {
                int position = mFuelingAdapter.updateRecord(fuelingRecord);

                if (position > -1) {
                    if (!isItemVisible(position))
                        scrollToPosition(position);
                }
            }
        }
    }

    private boolean isItemVisible(int position) {
        int firstVisibleItem = ((LinearLayoutManager) mRecyclerViewFueling.getLayoutManager())
                .findFirstCompletelyVisibleItemPosition();
        int lastVisibleItem = ((LinearLayoutManager) mRecyclerViewFueling.getLayoutManager())
                .findLastCompletelyVisibleItemPosition();

        return firstVisibleItem != RecyclerView.NO_POSITION &&
                lastVisibleItem != RecyclerView.NO_POSITION &&
                position > firstVisibleItem && position < lastVisibleItem;
    }

    private void scrollToPosition(int position) {
        mRecyclerViewFueling.scrollToPosition(position);
    }

    public boolean deleteRecord(FuelingRecord fuelingRecord) {
        boolean deleted = db.deleteRecord(fuelingRecord) > 0;
        if (deleted) mFuelingAdapter.deleteRecord(fuelingRecord);
        return deleted;
    }

    private void setFilterDate(final Date dateFrom, final Date dateTo) {
        mFilter.dateFrom = dateFrom;
        mFilter.dateTo = dateTo;
        getLoaderManager().restartLoader(LOADER_LIST_ID, null, this);
    }

    private void setFilterDate(final boolean setDateFrom, final Date date) {
        if (setDateFrom) mFilter.dateFrom = date;
        else mFilter.dateTo = date;
        getLoaderManager().restartLoader(LOADER_LIST_ID, null, this);
    }

    static class FuelingCursorLoader extends CursorLoader {

        private final Context mContext;
        private final FuelingDBHelper.Filter mFilter;

        public FuelingCursorLoader(Context context, FuelingDBHelper.Filter filter) {
            super(context);
            mContext = context;
            mFilter = filter;
        }

        @Override
        public Cursor loadInBackground() {
            Functions.logD("FragmentFueling -- loadInBackground");

            LocalBroadcastManager.getInstance(mContext).sendBroadcast(ActivityMain.getLoadingBroadcast(true));

            FuelingDBHelper dbHelper = new FuelingDBHelper();
            dbHelper.setFilter(mFilter);
            try {
                return dbHelper.getAllCursor();
            } finally {
                LocalBroadcastManager.getInstance(mContext).sendBroadcast(ActivityMain.getLoadingBroadcast(false));
            }
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_LIST_ID:
                return new FuelingCursorLoader(Functions.sApplicationContext, mFilter);
            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Functions.logD("FragmentFueling -- onLoadFinished");

        mFuelingAdapter.setShowYear(mFilter.filterMode != FuelingDBHelper.FilterMode.CURRENT_YEAR);

        mFuelingAdapter.swapCursor(data);

        scrollToId(mIdForScroll);
        mIdForScroll = -1;

        new CalcTotalTask(data).execute(); // TODO: cancel
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mFuelingAdapter.swapCursor(null);
    }

    private void scrollToId(long id) {
        if (id == -1) return;

        int position = mFuelingAdapter.positionOfRecordById(id);

        if (position > -1) {
            if (position == mFuelingAdapter.getFirstRecordPosition())
                position = FuelingAdapter.HEADER_POSITION;

            ((LinearLayoutManager) mRecyclerViewFueling.getLayoutManager())
                    .scrollToPositionWithOffset(position, 0);
        }
    }

    class CalcTotalData {
        final float cost;
        final float volume;
        final float total;

        CalcTotalData(float c, float v, float t) {
            cost = c;
            volume = v;
            total = t;
        }
    }

    class CalcTotalResult {
        final float average;
        final float costSum;

        CalcTotalResult(float a, float s) {
            average = a;
            costSum = s;
        }
    }

    class CalcTotalTask extends AsyncTask<Void, Void, CalcTotalResult> {

        final Cursor mCursor;
        final List<CalcTotalData> calcTotalDataList = new ArrayList<>();

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            final int columnCost = mCursor.getColumnIndex(FuelingDBHelper.COLUMN_COST);
            final int columnVolume = mCursor.getColumnIndex(FuelingDBHelper.COLUMN_VOLUME);
            final int columnTotal = mCursor.getColumnIndex(FuelingDBHelper.COLUMN_TOTAL);

            if (mCursor.moveToFirst()) do
                calcTotalDataList.add(new CalcTotalData(mCursor.getFloat(columnCost),
                        mCursor.getFloat(columnVolume), mCursor.getFloat(columnTotal)));
            while (mCursor.moveToNext());
        }

        CalcTotalTask(Cursor cursor) {
            mCursor = cursor;
        }

        @Override
        protected CalcTotalResult doInBackground(Void... params) {
            float costSum = 0, volumeSum = 0;
            float volume, total, firstTotal = 0, lastTotal = 0, average;

            int averageCount;

            boolean completeData = true; // Во всех записях указаны объём заправки и текущий пробег

            for (int i = 0; i < calcTotalDataList.size(); i++) {
                costSum += calcTotalDataList.get(i).cost;

                if (completeData) {
                    volume = calcTotalDataList.get(i).volume;
                    total = calcTotalDataList.get(i).total;

                    if (volume == 0 || total == 0) completeData = false;
                    else {
                        // Сортировка записей по дате в обратном порядке
                        // 0 -- последняя заправка
                        if (i == 0) lastTotal = total;
                        else {
                            // Последний (i == 0) объём заправки не нужен -- неизвестно, сколько на ней будет пробег,
                            // в volumeSum не включается
                            volumeSum += volume;
                            if (i == calcTotalDataList.size() - 1) firstTotal = total;
                        }
                    }
                }
            }

            if (completeData)
                average = volumeSum != 0 ? (volumeSum / (lastTotal - firstTotal)) * 100 : 0;
            else {
                average = 0;
                averageCount = 0;
                for (int i = 0; i < calcTotalDataList.size() - 1; i++) {
                    lastTotal = calcTotalDataList.get(i).total;
                    if (lastTotal != 0) {
                        volume = calcTotalDataList.get(i + 1).volume;
                        total = calcTotalDataList.get(i + 1).total;
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
            super.onPostExecute(calcTotalResult);

            mTextAverage.setText(Functions.floatToString(calcTotalResult.average));
            mTextCostSum.setText(Functions.floatToString(calcTotalResult.costSum));

            Functions.logD("FragmentFueling -- CalcTotalTask: onPostExecute");
        }
    }

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
                        FuelingRecord fuelingRecord = db.getFuelingRecord(id);
                        switch (item.getItemId()) {
                            case R.id.action_fueling_update:
                                mOnRecordChangeListener.onRecordChange(Const.RecordAction.UPDATE, fuelingRecord);
                                return true;
                            case R.id.action_fueling_delete:
                                mOnRecordChangeListener.onRecordChange(Const.RecordAction.DELETE, fuelingRecord);
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

    public void setProgressBarVisible(boolean visible) {
        Functions.setProgressWheelVisible(mProgressWheelFueling, visible);
    }

    public interface OnFilterChangeListener {
        // to Toolbar Spinner
        void onFilterChange(FuelingDBHelper.FilterMode filterMode);
    }

    public interface OnRecordChangeListener {
        void onRecordChange(Const.RecordAction recordAction, FuelingRecord fuelingRecord);
    }

    private FuelingDBHelper.Filter getFilter() {
        return mFilter;
    }

    @Override
    public void onAttach(Context context) {
        Functions.logD("FragmentFueling -- onAttach");
        super.onAttach(context);
        try {
            mOnFilterChangeListener = (OnFilterChangeListener) context;
            mOnRecordChangeListener = (OnRecordChangeListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() +
                    " must implement FilterChangeListener, RecordChangeListener");
        }
    }

    private void setToolbarDatesVisible(final boolean visible, final boolean animate) {
        if (mToolbarDatesVisible == visible) return;

        mToolbarDatesVisible = visible;

        final int toolbarDatesTopHidden =
                -getResources().getDimensionPixelSize(R.dimen.toolbar_height); // minus!
        final int toolbarShadowHeight =
                getResources().getDimensionPixelSize(R.dimen.toolbar_shadow_height);
        final RelativeLayout.LayoutParams layoutParams =
                (RelativeLayout.LayoutParams) mToolbarDates.getLayoutParams();

        if (animate) {
            final ValueAnimator valueAnimatorShadowShow = ValueAnimator.ofInt(0, toolbarShadowHeight);
            valueAnimatorShadowShow
                    .setDuration(Const.ANIMATION_DURATION_TOOLBAR_SHADOW)
                    .addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        public void onAnimationUpdate(ValueAnimator animation) {
                            Functions.setViewHeight(mToolbarShadow,
                                    (Integer) animation.getAnimatedValue());
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
                    visible ? toolbarDatesTopHidden : 0, visible ? 0 : toolbarDatesTopHidden);
            valueAnimatorToolbar
                    .setDuration(Const.ANIMATION_DURATION_TOOLBAR)
                    .addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        public void onAnimationUpdate(ValueAnimator animation) {
                            Functions.setViewTopMargin(mToolbarDates, layoutParams,
                                    (Integer) animation.getAnimatedValue());
                        }
                    });

            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playSequentially(valueAnimatorShadowShow,
                    valueAnimatorToolbar, valueAnimatorShadowHide);
            animatorSet.start();
        } else
            Functions.setViewTopMargin(mToolbarDates, layoutParams, visible ? 0 : toolbarDatesTopHidden);
    }

    private void updateFilterDateButtons(final boolean dateFrom, final Date date) {
        (dateFrom ? mBtnDateFrom : mBtnDateTo)
                .setText(Functions.dateToString(date, true, Functions.isPhoneInPortrait()));
    }

    private void setPopupFilterDate(final boolean setDateFrom, final int menuId) {
        FuelingDBHelper.Filter filter = getFilter();

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
        mDateFromClicked = dateFrom;

        FuelingDBHelper.Filter filter = getFilter();

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(mDateFromClicked ? filter.dateFrom : filter.dateTo);
        DatePickerDialog.newInstance(
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {
                        Calendar calendar = Calendar.getInstance();
                        calendar.set(year, monthOfYear, dayOfMonth);
                        Date date = calendar.getTime();

                        updateFilterDateButtons(mDateFromClicked, date);
                        setFilterDate(mDateFromClicked, date);
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        ).show(getFragmentManager(), null);
    }
}
