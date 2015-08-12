package ru.p3tr0vich.fuel;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
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

public class FragmentChartCost extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final String KEY_FILTER_YEAR = "KEY_FILTER_YEAR";

    private FuelingDBHelper.Filter mFilter;

    private BarChart mChart;
    private final ArrayList<String> mMonths = new ArrayList<>();

    private float[] mSums;
    private final int[] mColors = new int[]{R.color.chart_winter, R.color.chart_winter,
            R.color.chart_spring, R.color.chart_spring, R.color.chart_spring,
            R.color.chart_summer, R.color.chart_summer, R.color.chart_summer,
            R.color.chart_autumn, R.color.chart_autumn, R.color.chart_autumn,
            R.color.chart_winter};

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chart_cost, container, false);

        mChart = (BarChart) view.findViewById(R.id.chart);

        mChart.setDescription("");
        mChart.setNoDataText(getString(R.string.chart_no_data));

        mChart.setDrawBarShadow(false);
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

        updateMonths();

        return view;
    }

    private String getMonth(Calendar calendar, int month) {
        calendar.set(Calendar.MONTH, month);
        int flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NO_YEAR | DateUtils.FORMAT_NO_MONTH_DAY;
        if (Functions.isPhoneInPortrait()) flags |= DateUtils.FORMAT_ABBREV_MONTH;

        return DateUtils.formatDateTime(getActivity(), calendar.getTimeInMillis(), flags);
    }

    private void updateMonths() {
        Calendar calendar = Calendar.getInstance();
        for (int i = 0; i < 12; i++) mMonths.add(getMonth(calendar, i));
    }

    public void setYear(String year) {
        int intYear = Integer.parseInt(year);

        Functions.LogD("FragmentChartCost -- setYear: year == " + year + ", mFilter.year = " + mFilter.year);

        if (intYear == mFilter.year) return;

        mFilter.year = intYear;
        getLoaderManager().restartLoader(0, null, this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSums = new float[12];
        for (int i = 0; i < 12; i++) mSums[i] = 0;

        mFilter = new FuelingDBHelper.Filter();
        mFilter.filterMode = FuelingDBHelper.FilterMode.YEAR;

        if (savedInstanceState == null) {
            Functions.LogD("FragmentChartCost -- onCreate: savedInstanceState == null");

            mFilter.year = Functions.getCurrentYear();
            getLoaderManager().initLoader(0, null, this);
        } else {
            Functions.LogD("FragmentChartCost -- onCreate: savedInstanceState != null");

            mFilter.year = savedInstanceState.getInt(KEY_FILTER_YEAR);
            getLoaderManager().restartLoader(0, null, this);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(KEY_FILTER_YEAR, mFilter.year);

        Functions.LogD("FragmentChartCost -- onSaveInstanceState");
    }

    static class ChartCursorLoader extends CursorLoader {

        private final FuelingDBHelper.Filter mFilter;

        public ChartCursorLoader(Context context, FuelingDBHelper.Filter filter) {
            super(context);
            mFilter = filter;
        }

        @Override
        public Cursor loadInBackground() {
            Functions.LogD("FragmentChartCost -- loadInBackground");

            FuelingDBHelper dbHelper = new FuelingDBHelper();
            dbHelper.setFilter(mFilter);
            return dbHelper.getSumByMonthsForYear();
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new ChartCursorLoader(Functions.sApplicationContext, mFilter);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Functions.LogD("FragmentChartCost -- onLoadFinished");

        for (int i = 0; i < 12; i++) mSums[i] = 0;

        if (data.moveToFirst()) do {
            float sum = data.getFloat(0);
            String month = data.getString(1);
            Functions.LogD("FragmentChartCost -- SUM == " + sum + ", MONTH == " + month);

            mSums[Integer.parseInt(month) - 1] = sum;
        } while (data.moveToNext());

        updateChart();
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
    }
}
