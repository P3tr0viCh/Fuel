package ru.p3tr0vich.fuel;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.AxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.utils.ViewPortHandler;

import java.util.ArrayList;
import java.util.Calendar;

import ru.p3tr0vich.fuel.helpers.ContentProviderHelper;
import ru.p3tr0vich.fuel.helpers.DatabaseHelper;
import ru.p3tr0vich.fuel.models.ChartCostModel;
import ru.p3tr0vich.fuel.utils.Utils;
import ru.p3tr0vich.fuel.utils.UtilsDate;
import ru.p3tr0vich.fuel.utils.UtilsFormat;

public class FragmentChartCost extends FragmentBase implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final int YEARS_CURSOR_LOADER_ID = 0;
    private static final int CHART_CURSOR_LOADER_ID = 1;

    private TabLayout mTabLayout;
    private BarChart mChart;
    private TextView mTextNoRecords;
    private TextView mTextMedian;
    private TextView mTextSum;

    private final int[] mColors = new int[]{R.color.chart_winter, R.color.chart_winter,
            R.color.chart_spring, R.color.chart_spring, R.color.chart_spring,
            R.color.chart_summer, R.color.chart_summer, R.color.chart_summer,
            R.color.chart_autumn, R.color.chart_autumn, R.color.chart_autumn,
            R.color.chart_winter};

    private boolean mUpdateYearsInProcess = true;

    private ChartCostModel mChartCostModel;

    private static class Months {
        public static final int COUNT = 12;

        private final String[] mMonths = new String[COUNT];

        public Months(@NonNull Context context) {
            Calendar calendar = Calendar.getInstance();
            boolean abbrev = Utils.isPhoneInPortrait();

            for (int month = Calendar.JANUARY; month <= Calendar.DECEMBER; month++)
                mMonths[month] = UtilsDate.getMonthName(context, calendar, month, abbrev);
        }

        @NonNull
        public String getMonth(int month) {
            return month >= Calendar.JANUARY && month <= Calendar.DECEMBER ? mMonths[month] : "";
        }
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
        return mChartCostModel.getYears() != null ? mChartCostModel.getYears()[index] : -1;
    }

    private int getPositionForYear(int year) {
        if (mChartCostModel.getYears() == null) return -1;

        for (int i = 0; i < mChartCostModel.getYears().length; i++)
            if (mChartCostModel.getYears()[i] == year)
                return i;
        return -1;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            mChartCostModel = new ChartCostModel();

            getLoaderManager().initLoader(YEARS_CURSOR_LOADER_ID, null, this);
        } else
            mChartCostModel = savedInstanceState.getParcelable(ChartCostModel.NAME);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(ChartCostModel.NAME, mChartCostModel);
    }

    @Override
    public void onDestroy() {
        mHandler.removeCallbacks(mRunnableShowNoRecords);
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_chart_cost, container, false);

        mTabLayout = (TabLayout) view.findViewById(R.id.tab_layout);

        mTextNoRecords = (TextView) view.findViewById(R.id.text_no_records);
        mTextNoRecords.setVisibility(View.GONE);

        mTextMedian = (TextView) view.findViewById(R.id.text_median);
        mTextSum = (TextView) view.findViewById(R.id.text_sum);

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

        XAxis xAxis = mChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setTextSize(8f);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(Months.COUNT);
        xAxis.setAxisMinValue(-1f);
        xAxis.setAxisMaxValue(Months.COUNT);
        xAxis.setValueFormatter(mMonthFormatter);

        YAxis yAxis = mChart.getAxisLeft();
        yAxis.setEnabled(false);
        yAxis.setAxisMinValue(0f);
        yAxis.setDrawLimitLinesBehindData(true);

        mChart.getAxisRight().setEnabled(false);
        mChart.getLegend().setEnabled(false);

        mChart.setHardwareAccelerationEnabled(true);

        if (savedInstanceState != null) {
            updateYears();
            updateChart();
        }

        mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
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
            if (mChartCostModel.getYears() == null || mChartCostModel.getYears().length == 0)
                return;

            for (int year : mChartCostModel.getYears())
                mTabLayout.addTab(mTabLayout.newTab().setText(String.valueOf(year)));

            int position = getPositionForYear(mChartCostModel.getYear());
            if (position == -1) {
                position = mChartCostModel.getYears().length - 1;
                mChartCostModel.setYear(mChartCostModel.getYears()[position]);
            }

            selectTab(mTabLayout.getTabAt(position));
        } finally {
            mUpdateYearsInProcess = false;
        }
    }

    private void setYear(int year) {
        if (year == mChartCostModel.getYear()) return;

        mChartCostModel.setYear(year);

        getLoaderManager().restartLoader(CHART_CURSOR_LOADER_ID, null, this);
    }

    private static class ChartCursorLoader extends CursorLoader {

        private final int mYear;

        ChartCursorLoader(Context context, int year) {
            super(context);
            mYear = year;
        }

        @Override
        public Cursor loadInBackground() {
            return ContentProviderHelper.getSumByMonthsForYear(getContext(), mYear);
        }
    }

    private static class YearsCursorLoader extends CursorLoader {

        YearsCursorLoader(Context context) {
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
                return new ChartCursorLoader(getContext(), mChartCostModel.getYear());
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

                mChartCostModel.setSums(null);
                float[] sums = mChartCostModel.getSums();

                if (data.getCount() > 0) {

                    if (data.moveToFirst()) do {
                        sum = data.getFloat(DatabaseHelper.TableFueling.COST_SUM_INDEX);
                        month = data.getString(DatabaseHelper.TableFueling.MONTH_INDEX);

                        sums[Integer.parseInt(month) - 1] = sum;
                    } while (data.moveToNext());

                    mChartCostModel.setSums(sums);
                }

                updateChart();

                break;
            case YEARS_CURSOR_LOADER_ID:
                int count = data.getCount();

                if (count > 0) {
                    int[] years = new int[count];

                    int i = 0;

                    if (data.moveToFirst()) do {
                        years[i] = data.getInt(DatabaseHelper.TableFueling.YEAR_INDEX);

                        i++;
                    } while (data.moveToNext());

                    mChartCostModel.setYears(years);

                    updateYears();

                    getLoaderManager().initLoader(CHART_CURSOR_LOADER_ID, null, this);
                } else {
                    mChartCostModel.setYears(null);

                    updateChart();
                }
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

    private static final class MonthFormatter implements AxisValueFormatter {

        private final Months mMonths;

        public MonthFormatter(@NonNull Context context) {
            mMonths = new Months(context);
        }

        @Override
        public String getFormattedValue(float value, AxisBase axis) {
            return mMonths.getMonth((int) value);
        }

        @Override
        public int getDecimalDigits() {
            return 0;
        }
    }

    private final FloatFormatter mFloatFormatter = new FloatFormatter();
    private final MonthFormatter mMonthFormatter = new MonthFormatter(getContext());

    private void updateChart() {
        mHandler.removeCallbacks(mRunnableShowNoRecords);

        if (mChartCostModel.hasData()) {
            mTextNoRecords.setVisibility(View.GONE);

            ArrayList<BarEntry> sumsEntries = new ArrayList<>();

            float[] sums = mChartCostModel.getSums();
            for (int i = 0; i < sums.length; i++)
                sumsEntries.add(new BarEntry(i, sums[i]));

            BarDataSet sumsSet = new BarDataSet(sumsEntries, "");
            sumsSet.setColors(mColors, getContext());
            sumsSet.setAxisDependency(YAxis.AxisDependency.LEFT);

            ArrayList<IBarDataSet> dataSets = new ArrayList<>();
            dataSets.add(sumsSet);

            BarData sumsData = new BarData(dataSets);
            sumsData.setValueFormatter(mFloatFormatter);
            sumsData.setValueTextSize(8f);
            sumsData.setBarWidth(0.8f);

            mChart.setData(sumsData);

            mChart.getAxisLeft().removeAllLimitLines();

            if (mChartCostModel.getMedian() > 0 && mChartCostModel.isSumsNotEquals()) {
                LimitLine medianLine = new LimitLine(mChartCostModel.getMedian());
                medianLine.setLineColor(Utils.getColor(R.color.chart_median));
                medianLine.setLineWidth(Utils.isPhone() ? 0.2f : 0.5f);
                medianLine.enableDashedLine(8f, 2f, 0f);

                mChart.getAxisLeft().addLimitLine(medianLine);
            }

            int duration = Utils.getInteger(R.integer.animation_chart);
            mChart.animateXY(duration, duration);
        } else {
            mChart.clear();

            mHandler.postDelayed(mRunnableShowNoRecords, Utils.getInteger(R.integer.delayed_time_show_no_records));
        }

        updateTotal();
    }

    private void updateTotal() {
        UtilsFormat.floatToTextView(mTextMedian, mChartCostModel.getMedian(), true);
        UtilsFormat.floatToTextView(mTextSum, mChartCostModel.getSum(), true);
    }

    private final Handler mHandler = new Handler();

    private final Runnable mRunnableShowNoRecords = new Runnable() {
        @Override
        public void run() {
            Utils.setViewVisibleAnimate(mTextNoRecords, true);
        }
    };
}