package ru.p3tr0vich.fuel;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ViewPortHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

import ru.p3tr0vich.fuel.helpers.ContentProviderHelper;
import ru.p3tr0vich.fuel.helpers.DatabaseHelper;
import ru.p3tr0vich.fuel.utils.Utils;
import ru.p3tr0vich.fuel.utils.UtilsDate;
import ru.p3tr0vich.fuel.utils.UtilsFormat;

public class FragmentChartCost extends FragmentBase implements
        LoaderManager.LoaderCallbacks<Cursor> {

    public static final String TAG = "FragmentChartCost";

    private static final String KEY_FILTER_YEAR = "KEY_FILTER_YEAR";
    private static final String KEY_YEARS = "KEY_YEARS";
    private static final String KEY_SUMS = "KEY_SUMS";
    private static final String KEY_IS_DATA = "KEY_IS_DATA";

    private static final int YEARS_CURSOR_LOADER_ID = 0;
    private static final int CHART_CURSOR_LOADER_ID = 1;

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
    private boolean mUpdateYearsInProcess = true;

    @NonNull
    public static Fragment newInstance(int id) {
        return newInstance(id, new FragmentChartCost());
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
        return mYears != null ? mYears[index] : -1;
    }

    private int getPositionForYear(int year) {
        if (mYears == null) return -1;

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
        mChart.getAxisLeft().setDrawLimitLinesBehindData(true);

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
                if (!mUpdateYearsInProcess) setYear(getYearFromPosition(tab.getPosition()));
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        view.setOnTouchListener(mOnSwipeTouchListener);
        mChart.setOnTouchListener(mOnSwipeTouchListener);

        return view;
    }

    private void selectTab(@Nullable TabLayout.Tab tab) {
        if (tab != null) tab.select();
    }

    private final OnSwipeTouchListener mOnSwipeTouchListener = new OnSwipeTouchListener(getActivity()) {
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
    };

    private void updateYears() {
        mUpdateYearsInProcess = true;
        try {
            if (mYears == null || mYears.length == 0) return;

            for (int year : mYears)
                mTabLayout.addTab(mTabLayout.newTab().setText(String.valueOf(year)));

            int position = getPositionForYear(mYear);
            if (position == -1) {
                position = mYears.length - 1;
                mYear = mYears[position];
            }

            selectTab(mTabLayout.getTabAt(position));
        } finally {
            mUpdateYearsInProcess = false;
        }
    }

    private void updateMonths() {
        Context context = getContext();
        Calendar calendar = Calendar.getInstance();
        boolean abbrev = Utils.isPhoneInPortrait();

        for (int month = Calendar.JANUARY; month <= Calendar.DECEMBER; month++)
            mMonths[month] = UtilsDate.getMonthName(context, calendar, month, abbrev);
    }

    private void setYear(int year) {
        if (year == mYear) return;

        mYear = year;

        getLoaderManager().restartLoader(CHART_CURSOR_LOADER_ID, null, this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            for (int i = 0; i < 12; i++) mSums[i] = 0;

            mYear = UtilsDate.getCurrentYear();

            getLoaderManager().initLoader(YEARS_CURSOR_LOADER_ID, null, this);
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
        mHandler.removeCallbacks(mRunnableShowNoRecords);
        super.onDestroy();
    }

    static class ChartCursorLoader extends CursorLoader {

        private final int mYear;

        public ChartCursorLoader(Context context, int year) {
            super(context);
            mYear = year;
        }

        @Override
        public Cursor loadInBackground() {
            return ContentProviderHelper.getSumByMonthsForYear(getContext(), mYear);
        }
    }

    static class YearsCursorLoader extends CursorLoader {

        public YearsCursorLoader(Context context) {
            super(context);
        }

        @Override
        public Cursor loadInBackground() {
            return ContentProviderHelper.getYears(getContext());
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case CHART_CURSOR_LOADER_ID:
                return new ChartCursorLoader(getContext(), mYear);
            case YEARS_CURSOR_LOADER_ID:
                return new YearsCursorLoader(getContext());
            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        switch (loader.getId()) {
            case CHART_CURSOR_LOADER_ID:
                float sum;
                String month;

                for (int i = 0; i < 12; i++) mSums[i] = 0;

                mIsData = data.getCount() > 0;

                if (data.moveToFirst()) do {
                    sum = data.getFloat(DatabaseHelper.TableFueling.COST_SUM_INDEX);
                    month = data.getString(DatabaseHelper.TableFueling.MONTH_INDEX);

                    mSums[Integer.parseInt(month) - 1] = sum;
                } while (data.moveToNext());

                updateChart();

                break;
            case YEARS_CURSOR_LOADER_ID:
                int count = data.getCount();

                if (count > 0) {
                    mYears = new int[count];

                    int i = 0;

                    if (data.moveToFirst()) do {
                        mYears[i] = data.getInt(DatabaseHelper.TableFueling.YEAR_INDEX);

                        i++;
                    } while (data.moveToNext());

                    updateYears();

                    getLoaderManager().initLoader(CHART_CURSOR_LOADER_ID, null, this);
                } else
                    mYears = null;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    private static final class FloatFormatter implements ValueFormatter {

        @Override
        public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {
            return UtilsFormat.floatToString(value, false);
        }
    }

    private final FloatFormatter mFloatFormatter = new FloatFormatter();

    private float median(@NonNull float[] values) {
        int middle = values.length / 2;

        return values.length % 2 == 1 ? values[middle] : (values[middle - 1] + values[middle]) / 2f;
    }

    private void updateChart() {
        mHandler.removeCallbacks(mRunnableShowNoRecords);

        if (mIsData) {
            mTextNoRecords.setVisibility(View.GONE);

            ArrayList<BarEntry> sumsEntries = new ArrayList<>();

            for (int i = 0; i < 12; i++) sumsEntries.add(new BarEntry(mSums[i], i));

            BarDataSet sumsSet = new BarDataSet(sumsEntries, "");
            sumsSet.setBarSpacePercent(35f);
            sumsSet.setColors(mColors, getActivity());

            BarData sumsData = new BarData(mMonths);
            sumsData.addDataSet(sumsSet);
            sumsData.setValueFormatter(mFloatFormatter);
            sumsData.setValueTextSize(8f);

            mChart.setData(sumsData);

            mChart.getAxisLeft().removeAllLimitLines();

            int aboveZeroCount = 0;
            for (float value : mSums) if (value > 0) aboveZeroCount++;

            if (aboveZeroCount > 1) {
                float[] sortedSums = new float[aboveZeroCount];

                int i = 0;
                for (float value : mSums)
                    if (value > 0) {
                        sortedSums[i] = value;
                        i++;
                    }

                boolean valuesNotEquals = false;
                float value = sortedSums[0];
                for (int j = 1; j < sortedSums.length; j++)
                    if (sortedSums[j] != value) {
                        valuesNotEquals = true;
                        break;
                    }

                if (valuesNotEquals) {
                    Arrays.sort(sortedSums);

                    float median = median(sortedSums);

                    LimitLine medianLine = new LimitLine(median);
                    //noinspection deprecation
                    medianLine.setLineColor(getResources().getColor(R.color.chart_median));
                    medianLine.setLineWidth(0.5f); // 0.2 не выводится на планшете с апи 17
                    medianLine.enableDashedLine(8f, 2f, 0f);

                    mChart.getAxisLeft().addLimitLine(medianLine);
                }
            }

            int duration = Utils.getInteger(R.integer.animation_chart);
            mChart.animateXY(duration, duration);
        } else {
            mChart.clear();

            mHandler.postDelayed(mRunnableShowNoRecords, Utils.getInteger(R.integer.delayed_time_show_no_records));
        }
    }

    private final Handler mHandler = new Handler();

    private final Runnable mRunnableShowNoRecords = new Runnable() {
        @Override
        public void run() {
            Utils.setViewVisibleAnimate(mTextNoRecords, true);
        }
    };
}