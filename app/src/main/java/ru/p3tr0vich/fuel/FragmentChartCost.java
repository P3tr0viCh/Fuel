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
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.utils.ValueFormatter;

import java.util.ArrayList;
import java.util.Calendar;

public class FragmentChartCost extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {

//    private static final String KEY_FILTER_YEAR = "KEY_FILTER_MODE";

    private FuelingDBHelper.Filter mFilter;

    private BarChart mChart;
    private ArrayList<String> mMonths = new ArrayList<>();

    private float[] mSums;

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
        mChart.setPinchZoom(false);
        mChart.setDrawGridBackground(false);
        mChart.setDrawValueAboveBar(true);
        mChart.setDrawHighlightArrow(false);

        XAxis xAxis = mChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setSpaceBetweenLabels(0);
        xAxis.setTextSize(8f);

        YAxis leftAxis = mChart.getAxisLeft();
        leftAxis.setLabelCount(10);
        leftAxis.setValueFormatter(new FloatFormatter());
        leftAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        leftAxis.setSpaceTop(5f);

        mChart.getAxisRight().setEnabled(false);

        mChart.getLegend().setEnabled(false);

        updateMonths();

        updateChart();

        getLoaderManager().initLoader(0, null, this);

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
        Functions.LogD("FragmentChartCost -- setYear: year == " + year);
        mFilter.year = Integer.parseInt(year);
        getLoaderManager().restartLoader(0, null, this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSums = new float[12];
        for (int i = 0; i < 12; i++) mSums[i] = 0;

        mFilter = new FuelingDBHelper.Filter();
        mFilter.year = Functions.getCurrentYear();
        mFilter.filterMode = FuelingDBHelper.FilterMode.YEAR;

/*        if (savedInstanceState == null) {
            Log.d(Const.LOG_TAG, "FragmentFueling -- onCreate: savedInstanceState == null");

            SharedPreferences sPref = PreferenceManager.getDefaultSharedPreferences(getActivity());

            mFilter.filterMode = FuelingDBHelper.FilterMode.CURRENT_YEAR;
            mFilter.dateFrom = Functions.sqliteToDate(
                    sPref.getString(getString(R.string.pref_filter_date_from),
                            Functions.dateToSQLite(new Date())));
            mFilter.dateTo = Functions.sqliteToDate(
                    sPref.getString(getString(R.string.pref_filter_date_to),
                            Functions.dateToSQLite(new Date())));

        } else {
            Log.d(Const.LOG_TAG, "FragmentFueling -- onCreate: savedInstanceState != null");

            mFilter.filterMode = (FuelingDBHelper.FilterMode) savedInstanceState.getSerializable(KEY_FILTER_MODE);
            mFilter.dateFrom = (Date) savedInstanceState.getSerializable(KEY_FILTER_DATE_FROM);
            mFilter.dateTo = (Date) savedInstanceState.getSerializable(KEY_FILTER_DATE_TO);
        }*/
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

/*        outState.putSerializable(KEY_FILTER_MODE, mFilter.filterMode);
        outState.putSerializable(KEY_FILTER_DATE_FROM, mFilter.dateFrom);
        outState.putSerializable(KEY_FILTER_DATE_TO, mFilter.dateTo);

        Log.d(Const.LOG_TAG, "FragmentFueling -- onSaveInstanceState");*/
    }

    static class ChartCursorLoader extends CursorLoader {

        private final Context mContext;
        private final FuelingDBHelper.Filter mFilter;

        public ChartCursorLoader(Context context, FuelingDBHelper.Filter filter) {
            super(context);
            mContext = context;
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

    public void updateChart() {
        ArrayList<BarEntry> yValues = new ArrayList<>();

        for (int i = 0; i < 12; i++) yValues.add(new BarEntry(mSums[i], i));

        BarDataSet set = new BarDataSet(yValues, "");
        set.setBarSpacePercent(35f);
        set.setColor(getResources().getColor(R.color.primary));
        set.setHighLightColor(getResources().getColor(R.color.primary_dark));

        ArrayList<BarDataSet> dataSets = new ArrayList<>();
        dataSets.add(set);

        BarData data = new BarData(mMonths, dataSets);
        data.setValueFormatter(new FloatFormatter());
        data.setValueTextSize(8f);

        mChart.setData(data);

        mChart.animateXY(Const.ANIMATION_CHART, Const.ANIMATION_CHART);
    }
}
