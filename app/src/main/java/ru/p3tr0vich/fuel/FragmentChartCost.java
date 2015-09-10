package ru.p3tr0vich.fuel;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.utils.ValueFormatter;

import java.util.ArrayList;
import java.util.Calendar;

public class FragmentChartCost extends FragmentFuel implements
        LoaderManager.LoaderCallbacks<Cursor> {

    public static final String TAG = "FragmentChartCost";

    private static final String KEY_FILTER_YEAR = "KEY_FILTER_YEAR";
    private static final String KEY_YEARS = "KEY_YEARS";
    private static final String KEY_SUMS = "KEY_SUMS";
    private static final String KEY_IS_DATA = "KEY_IS_DATA";

    private FuelingDBHelper.Filter mFilter;

    private BarChart mChart;
    private TabLayout mTabLayout;

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
        if (savedInstanceState == null)
            Functions.logD("FragmentChartCost -- onCreateView: savedInstanceState == null");
        else
            Functions.logD("FragmentChartCost -- onCreateView: savedInstanceState != null");

        View view = inflater.inflate(R.layout.fragment_chart_cost, container, false);

        mTabLayout = (TabLayout) view.findViewById(R.id.tabLayout);

        mChart = (BarChart) view.findViewById(R.id.chart);

        mChart.setDescription("");
        mChart.setNoDataText(getString(R.string.chart_no_data));

        mChart.setDrawBarShadow(false);
        mChart.setTouchEnabled(false);
        mChart.setScaleEnabled(false);
        mChart.setDoubleTapToZoomEnabled(false);
        mChart.setHighlightEnabled(false);
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
                Functions.logD("FragmentChartCost -- onTabSelected");
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
        Functions.logD("FragmentChartCost -- updateYears, mFilter.year = " + mFilter.year);

        mUpdateYearInProcess = true;
        for (int year : mYears)
            mTabLayout.addTab(mTabLayout.newTab().setText(String.valueOf(year)));

        int position = getPositionForYear(mFilter.year);
        if (position != -1) selectTab(mTabLayout.getTabAt(position));

        mUpdateYearInProcess = false;
    }

    private String getMonth(Calendar calendar, int month) {
        calendar.set(Calendar.MONTH, month);
        int flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NO_YEAR | DateUtils.FORMAT_NO_MONTH_DAY;
        if (Functions.isPhoneInPortrait()) flags |= DateUtils.FORMAT_ABBREV_MONTH;

        return DateUtils.formatDateTime(getActivity(), calendar.getTimeInMillis(), flags);
    }

    private void updateMonths() {
        Calendar calendar = Calendar.getInstance();
        for (int i = 0; i < 12; i++) mMonths[i] = getMonth(calendar, i);
    }

    private void setYear(int year) {
        Functions.logD("FragmentChartCost -- setYear: year == " + year + ", mFilter.year = " + mFilter.year);

        if (year == mFilter.year) return;

        mFilter.year = year;
        getLoaderManager().restartLoader(ChartCursorLoader.ID, null, this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFilter = new FuelingDBHelper.Filter();
        mFilter.filterMode = FuelingDBHelper.FilterMode.YEAR;

        if (savedInstanceState == null) {
            Functions.logD("FragmentChartCost -- onCreate: savedInstanceState == null");

            for (int i = 0; i < 12; i++) mSums[i] = 0;

            mFilter.year = Functions.getCurrentYear();

            getLoaderManager().initLoader(YearsCursorLoader.ID, null, this);
            getLoaderManager().initLoader(ChartCursorLoader.ID, null, this);
        } else {
            Functions.logD("FragmentChartCost -- onCreate: savedInstanceState != null");

            mFilter.year = savedInstanceState.getInt(KEY_FILTER_YEAR);
            mYears = savedInstanceState.getIntArray(KEY_YEARS);
            mSums = savedInstanceState.getFloatArray(KEY_SUMS);
            mIsData = savedInstanceState.getBoolean(KEY_IS_DATA);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(KEY_FILTER_YEAR, mFilter.year);
        outState.putIntArray(KEY_YEARS, mYears);
        outState.putFloatArray(KEY_SUMS, mSums);
        outState.putBoolean(KEY_IS_DATA, mIsData);

        Functions.logD("FragmentChartCost -- onSaveInstanceState");
    }

    static class ChartCursorLoader extends CursorLoader {

        private static final int ID = 0;

        private final FuelingDBHelper.Filter mFilter;

        public ChartCursorLoader(Context context, FuelingDBHelper.Filter filter) {
            super(context);
            mFilter = filter;
        }

        @Override
        public Cursor loadInBackground() {
            Functions.logD("FragmentChartCost -- ChartCursorLoader: loadInBackground");

            FuelingDBHelper dbHelper = new FuelingDBHelper();
            dbHelper.setFilter(mFilter);
            return dbHelper.getSumByMonthsForYear();
        }
    }

    static class YearsCursorLoader extends CursorLoader {

        private static final int ID = 1;

        public YearsCursorLoader(Context context) {
            super(context);
        }

        @Override
        public Cursor loadInBackground() {
            Functions.logD("FragmentChartCost -- YearsCursorLoader: loadInBackground");

            FuelingDBHelper dbHelper = new FuelingDBHelper();
            return dbHelper.getYears();
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case ChartCursorLoader.ID:
                return new ChartCursorLoader(Functions.sApplicationContext, mFilter);
            case YearsCursorLoader.ID:
                return new YearsCursorLoader(Functions.sApplicationContext);
            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        switch (loader.getId()) {
            case ChartCursorLoader.ID:
                Functions.logD("FragmentChartCost -- ChartCursorLoader: onLoadFinished");

                float sum;
                String month;

                for (int i = 0; i < 12; i++) mSums[i] = 0;

                mIsData = data.getCount() > 0;

                if (data.moveToFirst()) do {
                    sum = data.getFloat(0);
                    month = data.getString(1);

                    Functions.logD("FragmentChartCost -- SUM == " + sum + ", MONTH == " + month);

                    mSums[Integer.parseInt(month) - 1] = sum;
                } while (data.moveToNext());

                updateChart();

                break;
            case YearsCursorLoader.ID:
                Functions.logD("FragmentChartCost -- YearsCursorLoader: onLoadFinished");

                int i = 0, year, count = data.getCount();

                if (count > 0) {
                    mYears = new int[count + 1];

                    if (data.moveToFirst()) do {
                        year = data.getInt(0);

                        Functions.logD("FragmentChartCost -- YEAR == " + year);

                        mYears[i] = year;
                        i++;
                    } while (data.moveToNext());

                    mYears[count] = Functions.getCurrentYear();
                } else {
                    mYears = new int[1];
                    mYears[0] = Functions.getCurrentYear();
                }

                updateYears();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    private final class FloatFormatter implements ValueFormatter {

        @Override
        public String getFormattedValue(float value) {
            return Functions.floatToString(value, false);
        }
    }

    private void updateChart() {
        if (mIsData) {
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
        } else
            mChart.clear();
    }
}
