package ru.p3tr0vich.fuel;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.TabLayout;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ViewPortHandler;

import java.util.ArrayList;
import java.util.Calendar;

public class FragmentChartCost extends FragmentFuel implements
        LoaderManager.LoaderCallbacks<Cursor> {

    public static final String TAG = "FragmentChartCost";

    private static final String KEY_FILTER_YEAR = "KEY_FILTER_YEAR";
    private static final String KEY_YEARS = "KEY_YEARS";
    private static final String KEY_SUMS = "KEY_SUMS";
    private static final String KEY_IS_DATA = "KEY_IS_DATA";

    private int mYear;

    private TabLayout mTabLayout;
    private BarChart mChart;
    private TextView mTextNoRecords;

    private final String[] mMonths = new String[12];
    private int[] mYears;
    private float[] mSums = new float[12];

    private final int[] mColors = new int[]{R.color.chart_winter, R.color.chart_winter,
            R.color.chart_spring, R.color.chart_spring, R.color.chart_spring,
            R.color.chart_summer, R.color.chart_summer, R.color.chart_summer,
            R.color.chart_autumn, R.color.chart_autumn, R.color.chart_autumn,
            R.color.chart_winter};

    private boolean mIsData = false;
    private boolean mUpdateYearInProcess = true;

    @Override
    public int getFragmentId() {
        return R.id.action_chart_cost;
    }

    @Override
    public int getTitleId() {
        return R.string.title_chart_cost;
    }

    @Override
    public int getSubtitleId() {
        return R.string.title_chart_cost_subtitle;
    }

    private int getYearFromPosition(int index) {
        return mYears[index];
    }

    private int getPositionForYear(int year) {
        for (int i = 0; i < mYears.length; i++)
            if (mYears[i] == year)
                return i;
        return -1;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_chart_cost, container, false);

        mTabLayout = (TabLayout) view.findViewById(R.id.tabLayout);

        mTextNoRecords = (TextView) view.findViewById(R.id.tvNoRecords);

        mChart = (BarChart) view.findViewById(R.id.chart);

        mChart.setDescription("");
        mChart.setNoDataText("");

        mChart.setDrawBarShadow(false);
        mChart.setTouchEnabled(false);
        mChart.setScaleEnabled(false);
        mChart.setDoubleTapToZoomEnabled(false);
        mChart.setPinchZoom(false);
        mChart.setDrawGridBackground(false);
        mChart.setDrawValueAboveBar(true);
        mChart.setDrawHighlightArrow(false);

        XAxis xAxis = mChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setSpaceBetweenLabels(0);
        xAxis.setTextSize(8f);

        mChart.getAxisLeft().setEnabled(false);
        mChart.getAxisRight().setEnabled(false);
        mChart.getLegend().setEnabled(false);

        mChart.setHardwareAccelerationEnabled(true);

        updateMonths();
        if (savedInstanceState != null) {
            updateYears();
            updateChart();
        }

        mTabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (!mUpdateYearInProcess) setYear(getYearFromPosition(tab.getPosition()));
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        view.setOnTouchListener(onSwipeTouchListener);
        mChart.setOnTouchListener(onSwipeTouchListener);

        return view;
    }

    private void selectTab(TabLayout.Tab tab) {
        if (tab != null) tab.select();
    }

    private final OnSwipeTouchListener onSwipeTouchListener = new OnSwipeTouchListener(getActivity()) {
        @Override
        public void onSwipeRight() {
            int position = mTabLayout.getSelectedTabPosition();
            if (position != 0) selectTab(mTabLayout.getTabAt(position - 1));
        }

        @Override
        public void onSwipeLeft() {
            int position = mTabLayout.getSelectedTabPosition();
            if (position != mTabLayout.getTabCount() - 1)
                selectTab(mTabLayout.getTabAt(position + 1));
        }

        @Override
        public void onSwipeTop() {

        }

        @Override
        public void onSwipeBottom() {

        }

        public boolean onTouch(View v, MotionEvent event) {
            return gestureDetector.onTouchEvent(event);
        }
    };

    private void updateYears() {
        mUpdateYearInProcess = true;
        for (int year : mYears)
            mTabLayout.addTab(mTabLayout.newTab().setText(String.valueOf(year)));

        int position = getPositionForYear(mYear);
        if (position != -1) selectTab(mTabLayout.getTabAt(position));

        mUpdateYearInProcess = false;
    }

    private String getMonth(Calendar calendar, int month) {
        calendar.set(Calendar.MONTH, month);
        int flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NO_YEAR | DateUtils.FORMAT_NO_MONTH_DAY;
        if (Utils.isPhoneInPortrait()) flags |= DateUtils.FORMAT_ABBREV_MONTH;

        return DateUtils.formatDateTime(getActivity(), calendar.getTimeInMillis(), flags);
    }

    private void updateMonths() {
        Calendar calendar = Calendar.getInstance();
        for (int i = 0; i < 12; i++) mMonths[i] = getMonth(calendar, i);
    }

    private void setYear(int year) {
        if (year == mYear) return;

        mYear = year;
        getLoaderManager().restartLoader(ChartCursorLoader.ID, null, this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            for (int i = 0; i < 12; i++) mSums[i] = 0;

            mYear = UtilsDate.getCurrentYear();

            getLoaderManager().initLoader(YearsCursorLoader.ID, null, this);
            getLoaderManager().initLoader(ChartCursorLoader.ID, null, this);
        } else {
            mYear = savedInstanceState.getInt(KEY_FILTER_YEAR);
            mYears = savedInstanceState.getIntArray(KEY_YEARS);
            mSums = savedInstanceState.getFloatArray(KEY_SUMS);
            mIsData = savedInstanceState.getBoolean(KEY_IS_DATA);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(KEY_FILTER_YEAR, mYear);
        outState.putIntArray(KEY_YEARS, mYears);
        outState.putFloatArray(KEY_SUMS, mSums);
        outState.putBoolean(KEY_IS_DATA, mIsData);
    }

    @Override
    public void onDestroy() {
        mHandlerShowNoRecords.removeCallbacks(mRunnableShowNoRecords);
        super.onDestroy();
    }

    static class ChartCursorLoader extends CursorLoader {

        private static final int ID = 0;

        private final int mYear;

        public ChartCursorLoader(Context context, int year) {
            super(context);
            mYear = year;
        }

        @Override
        public Cursor loadInBackground() {
//            return new DatabaseHelper(getContext()).getSumByMonthsForYear(mYear);
            return ContentProviderFuel.getSumByMonthsForYear(getContext(), mYear);
        }
    }

    static class YearsCursorLoader extends CursorLoader {

        private static final int ID = 1;

        public YearsCursorLoader(Context context) {
            super(context);
        }

        @Override
        public Cursor loadInBackground() {
//            return new DatabaseHelper(getContext()).getYears();
            return ContentProviderFuel.getYears(getContext());
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case ChartCursorLoader.ID:
                return new ChartCursorLoader(getContext(), mYear);
            case YearsCursorLoader.ID:
                return new YearsCursorLoader(getContext());
            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        switch (loader.getId()) {
            case ChartCursorLoader.ID:
                float sum;
                String month;

                for (int i = 0; i < 12; i++) mSums[i] = 0;

                mIsData = data.getCount() > 0;

                if (data.moveToFirst()) do {
                    sum = data.getFloat(DatabaseHelper.COLUMN_COST_SUM_INDEX);
                    month = data.getString(DatabaseHelper.COLUMN_MONTH_INDEX);

                    mSums[Integer.parseInt(month) - 1] = sum;
                } while (data.moveToNext());

                updateChart();

                break;
            case YearsCursorLoader.ID:
                int i = 0, year, count = data.getCount();

                if (count > 0) {
                    mYears = new int[count + 1];

                    if (data.moveToFirst()) do {
                        year = data.getInt(DatabaseHelper.COLUMN_YEAR_INDEX);

                        mYears[i] = year;
                        i++;
                    } while (data.moveToNext());

                    mYears[count] = UtilsDate.getCurrentYear();
                } else {
                    mYears = new int[1];
                    mYears[0] = UtilsDate.getCurrentYear();
                }

                updateYears();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    private final class FloatFormatter implements ValueFormatter {

        @Override
        public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {
            return UtilsFormat.floatToString(value, false);
        }
    }

    private void updateChart() {
        mHandlerShowNoRecords.removeCallbacks(mRunnableShowNoRecords);

        if (mIsData) {
            mTextNoRecords.setVisibility(View.GONE);

            ArrayList<BarEntry> yValues = new ArrayList<>();

            for (int i = 0; i < 12; i++) yValues.add(new BarEntry(mSums[i], i));

            BarDataSet set = new BarDataSet(yValues, "");
            set.setBarSpacePercent(35f);
            set.setColors(mColors, getActivity());

            ArrayList<BarDataSet> dataSets = new ArrayList<>();
            dataSets.add(set);

            BarData data = new BarData(mMonths, dataSets);
            data.setValueFormatter(new FloatFormatter());
            data.setValueTextSize(8f);

            mChart.setData(data);

            mChart.animateXY(Const.ANIMATION_CHART, Const.ANIMATION_CHART);
        } else {
            mChart.clear();

            mHandlerShowNoRecords.postDelayed(mRunnableShowNoRecords,
                    Const.DELAYED_TIME_SHOW_NO_RECORDS);
        }
    }

    private final Handler mHandlerShowNoRecords = new Handler();

    private final Runnable mRunnableShowNoRecords = new Runnable() {
        @Override
        public void run() {
            Utils.setViewVisibleAnimate(mTextNoRecords, true);
        }
    };
}