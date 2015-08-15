package ru.p3tr0vich.fuel;
// TODO: Скакать при изменении если не видно

import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.pnikosis.materialishprogress.ProgressWheel;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class FragmentFueling extends Fragment implements
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
    private FuelingCursorAdapter mFuelingCursorAdapter;

    private Button mBtnDateFrom;
    private Button mBtnDateTo;

    private ListView mListViewFueling;

    private View mListViewHeader;
    private View mListViewFooter;

    private ProgressWheel mProgressWheelFueling;

    private TextView mTextAverage;
    private TextView mTextCostSum;

    private long mSelectedId = 0; // TODO: HACK

    private FilterChangeListener mFilterChangeListener;
    private RecordChangeListener mRecordChangeListener;
    private OnFragmentChangedListener mOnFragmentChangedListener;

    @SuppressLint("InflateParams")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Functions.LogD("FragmentFueling -- onCreateView");

        View view = inflater.inflate(R.layout.fragment_fueling, container, false);

        mToolbarDates = (Toolbar) view.findViewById(R.id.toolbarDates);
        mToolbarShadow = view.findViewById(R.id.toolbarShadow);

        mListViewFueling = (ListView) view.findViewById(R.id.listViewFueling);

        mListViewHeader = inflater.inflate(R.layout.fueling_listview_header, null, false);
        mListViewFooter = inflater.inflate(R.layout.fueling_listview_footer, null, false);

        mProgressWheelFueling = (ProgressWheel) view.findViewById(R.id.progressWheelFueling);

        mTextAverage = (TextView) view.findViewById(R.id.tvAverage);
        mTextCostSum = (TextView) view.findViewById(R.id.tvCostSum);

        view.findViewById(R.id.floatingActionButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRecordChangeListener.onRecordChange(Const.RecordAction.ADD, null);
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

        mOnFragmentChangedListener.onFragmentChanged(R.id.action_fueling);

        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFilter = new FuelingDBHelper.Filter();

        if (savedInstanceState == null) {
            Functions.LogD("FragmentFueling -- onCreate: savedInstanceState == null");

            SharedPreferences sPref = PreferenceManager.getDefaultSharedPreferences(getActivity());

            mFilter.filterMode = FuelingDBHelper.FilterMode.CURRENT_YEAR;
            mFilter.dateFrom = Functions.sqliteToDate(
                    sPref.getString(getString(R.string.pref_filter_date_from),
                            Functions.dateToSQLite(new Date())));
            mFilter.dateTo = Functions.sqliteToDate(
                    sPref.getString(getString(R.string.pref_filter_date_to),
                            Functions.dateToSQLite(new Date())));

        } else {
            Functions.LogD("FragmentFueling -- onCreate: savedInstanceState != null");

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

        Functions.LogD("FragmentFueling -- onSaveInstanceState");
    }

    @Override
    public void onDestroy() {
        Functions.LogD("FragmentFueling -- onDestroy");

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

        Functions.LogD("FragmentFueling -- onActivityCreated");

        db = new FuelingDBHelper();
        String[] from = {
                FuelingDBHelper.COLUMN_DATETIME,
                FuelingDBHelper.COLUMN_COST,
                FuelingDBHelper.COLUMN_VOLUME,
                FuelingDBHelper.COLUMN_TOTAL};
        int[] to = {R.id.tvDate, R.id.tvCost, R.id.tvVolume, R.id.tvDate};

        mFuelingCursorAdapter = new FuelingCursorAdapter(getActivity(), from, to, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doPopup(v);
            }
        });

        mListViewFueling.addHeaderView(mListViewHeader, null, false);
        mListViewFueling.addFooterView(mListViewFooter, null, false);

        mListViewFueling.setAdapter(mFuelingCursorAdapter);

        doSetFilterMode(mFilter.filterMode);

        getLoaderManager().initLoader(LOADER_LIST_ID, null, this);
    }

    private void doSetFilterMode(FuelingDBHelper.FilterMode filterMode) {
        mFilter.filterMode = filterMode;
        mFuelingCursorAdapter.showYear = (filterMode != FuelingDBHelper.FilterMode.CURRENT_YEAR);

        mFilterChangeListener.onFilterChange(filterMode);
    }

    public boolean setFilterMode(FuelingDBHelper.FilterMode filterMode) {
        Functions.LogD("FragmentFueling -- setFilterMode: new FilterMode == " + filterMode +
                ", current FilterMode == " + mFilter.filterMode);
        if (mFilter.filterMode != filterMode) {

            setToolbarDatesVisible(filterMode == FuelingDBHelper.FilterMode.DATES, true);

            doSetFilterMode(filterMode);

            getLoaderManager().restartLoader(LOADER_LIST_ID, null, this);

            return false;
        } else return true;
    }

    private void checkFilterMode(FuelingRecord fuelingRecord) {
        // Вызов после добавления или изменения записи.
        // Если год в дате записи не текущий, то нужно установить фильтр -> "все записи".
        // Если год в записи текущий, то needUpdate == true.
        // Если год не текущий, то вызывается needUpdate == setFilterMode.
        // В setFilterMode проверяется текущий фильтр. Если он уже "все записи", то needUpdate == true,
        // иначе needUpdate == false и вызывается рестарт лоадер.
        // Если needUpdate == true вызывается форс лоад.

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(Functions.sqliteToDate(fuelingRecord.getSQLiteDate()));

        boolean needUpdate = calendar.get(Calendar.YEAR) == Functions.getCurrentYear() ||
                setFilterMode(FuelingDBHelper.FilterMode.ALL);

        if (needUpdate) updateAfterChange();
    }

    public void setFilterDate(final Date dateFrom, final Date dateTo) {
        mFilter.dateFrom = dateFrom;
        mFilter.dateTo = dateTo;
        getLoaderManager().restartLoader(LOADER_LIST_ID, null, this);
    }

    public void setFilterDate(final boolean setDateFrom, final Date date) {
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
            Functions.LogD("FragmentFueling -- loadInBackground");

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
                return new FuelingCursorLoader(getActivity().getApplicationContext(), mFilter);
            default:
                return null;
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
                calcTotalDataList.add(new CalcTotalData(mCursor.getFloat(columnCost), mCursor.getFloat(columnVolume), mCursor.getFloat(columnTotal)));
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

            Functions.LogD("FragmentFueling -- CalcTotalTask: onPostExecute");
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mFuelingCursorAdapter.swapCursor(data);
        selectItemById(mSelectedId);

        Functions.LogD("FragmentFueling -- onLoadFinished");

        new CalcTotalTask(data).execute(); // TODO: cancel
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mFuelingCursorAdapter.swapCursor(null);
    }

    public void updateAfterChange() {
        getLoaderManager().getLoader(LOADER_LIST_ID).forceLoad();
    }

    public void addRecord(FuelingRecord fuelingRecord) {
        mSelectedId = db.insertRecord(fuelingRecord);
        if (mSelectedId > 0) checkFilterMode(fuelingRecord);
    }

    public void updateRecord(FuelingRecord fuelingRecord) {
        if (db.updateRecord(fuelingRecord) > 0) checkFilterMode(fuelingRecord);
    }

    public void deleteRecord(FuelingRecord fuelingRecord) {
        if (db.deleteRecord(fuelingRecord) > 0)
            updateAfterChange();
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
                                mRecordChangeListener.onRecordChange(Const.RecordAction.UPDATE, fuelingRecord);
                                return true;
                            case R.id.action_fueling_delete:
                                mRecordChangeListener.onRecordChange(Const.RecordAction.DELETE, fuelingRecord);
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

    private void selectItemById(long id) {
        if (mSelectedId == 0) return;
        for (int i = 0; i < mListViewFueling.getCount(); i++) {
            if (mListViewFueling.getItemIdAtPosition(i) == id) {
                mListViewFueling.setSelection(i);
                break;
            }
        }
        mSelectedId = 0;
    }

    public void setProgressBarVisible(boolean visible) {
        Functions.setProgressWheelVisible(mProgressWheelFueling, visible);
    }

    public interface FilterChangeListener {
        // to Toolbar Spinner
        void onFilterChange(FuelingDBHelper.FilterMode filterMode);
    }

    public interface RecordChangeListener {
        void onRecordChange(Const.RecordAction recordAction, FuelingRecord fuelingRecord);
    }

    public FuelingDBHelper.Filter getFilter() {
        return mFilter;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mFilterChangeListener = (FilterChangeListener) activity;
            mRecordChangeListener = (RecordChangeListener) activity;
            mOnFragmentChangedListener = (OnFragmentChangedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() +
                    " must implement FilterChangeListener, RecordChangeListener, OnFragmentChangedListener");
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
