package ru.p3tr0vich.fuel.fragments

import android.content.Context
import android.database.Cursor
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.github.mikephil.charting.formatter.IValueFormatter
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.github.mikephil.charting.utils.ViewPortHandler
import com.google.android.material.tabs.TabLayout
import ru.p3tr0vich.fuel.R
import ru.p3tr0vich.fuel.factories.FragmentFactory
import ru.p3tr0vich.fuel.helpers.ContentResolverHelper
import ru.p3tr0vich.fuel.listeners.OnSwipeTouchListener
import ru.p3tr0vich.fuel.models.ChartCostModel
import ru.p3tr0vich.fuel.models.DatabaseModel
import ru.p3tr0vich.fuel.utils.Utils
import ru.p3tr0vich.fuel.utils.UtilsDate
import ru.p3tr0vich.fuel.utils.UtilsFormat
import java.util.*

class FragmentChartCost : FragmentBase(FragmentFactory.Ids.CHART_COST), LoaderManager.LoaderCallbacks<Cursor> {

    private var tabLayout: TabLayout? = null
    private lateinit var chart: BarChart
    private var textNoRecords: TextView? = null
    private var textMedian: TextView? = null
    private var textSum: TextView? = null

    private val colors = intArrayOf(
            R.color.chart_winter, R.color.chart_winter,
            R.color.chart_spring, R.color.chart_spring, R.color.chart_spring,
            R.color.chart_summer, R.color.chart_summer, R.color.chart_summer,
            R.color.chart_autumn, R.color.chart_autumn, R.color.chart_autumn,
            R.color.chart_winter)

    private var updateYearsInProcess = true

    private var chartCostModel: ChartCostModel? = null

    private lateinit var months: Months

    override val titleId: Int
        get() = R.string.title_chart_cost

    override val subtitleId: Int
        get() = R.string.title_chart_cost_subtitle

    private val onSwipeTouchListener = object : OnSwipeTouchListener(context) {
        override fun onSwipeRight() {
            val position = tabLayout!!.selectedTabPosition

            if (position != 0) {
                selectTab(tabLayout!!.getTabAt(position - 1))
            }
        }

        override fun onSwipeLeft() {
            val position = tabLayout!!.selectedTabPosition

            if (position != tabLayout!!.tabCount - 1) {
                selectTab(tabLayout!!.getTabAt(position + 1))
            }
        }

        override fun onSwipeTop() {}

        override fun onSwipeBottom() {}
    }

    private val floatFormatter = FloatFormatter()
    private val monthFormatter = MonthFormatter()

    private val handler = Handler()

    private val runnableShowNoRecords = Runnable { Utils.setViewVisibleAnimate(textNoRecords!!, true) }

    private class Months(context: Context) {

        private val months = Array(COUNT) { "" }

        init {
            for (month in Calendar.JANUARY..Calendar.DECEMBER)
                months[month] = UtilsDate.getMonthName(context, Calendar.getInstance(), month, Utils.isPhoneInPortrait)
        }

        fun getMonth(month: Int): String {
            return if (month >= Calendar.JANUARY && month <= Calendar.DECEMBER) months[month] else ""
        }

        companion object {
            const val COUNT = 12
        }
    }

    private fun getYearFromPosition(index: Int): Int {
        return chartCostModel!!.years?.get(index) ?: -1
    }

    private fun getPositionForYear(year: Int): Int {
        if (chartCostModel!!.years == null) return -1

        for (i in 0 until chartCostModel!!.years!!.size) {
            if (chartCostModel!!.years!![i] == year) {
                return i
            }
        }

        return -1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        months = Months(context!!)

        if (savedInstanceState == null) {
            chartCostModel = ChartCostModel()

            loaderManager.initLoader(YEARS_CURSOR_LOADER_ID, null, this)
        } else
            chartCostModel = savedInstanceState.getParcelable(ChartCostModel.NAME)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(ChartCostModel.NAME, chartCostModel)
    }

    override fun onDestroy() {
        handler.removeCallbacks(runnableShowNoRecords)

        super.onDestroy()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_chart_cost, container, false)

        tabLayout = view.findViewById(R.id.tab_layout)

        textNoRecords = view.findViewById(R.id.text_no_records)
        textNoRecords!!.visibility = View.GONE

        textMedian = view.findViewById(R.id.text_median)
        textSum = view.findViewById(R.id.text_sum)

        chart = view.findViewById(R.id.chart)

        chart.description.text = ""

        chart.setNoDataText("")

        chart.setDrawBarShadow(false)
        chart.setTouchEnabled(false)
        chart.setScaleEnabled(false)
        chart.isDoubleTapToZoomEnabled = false
        chart.setPinchZoom(false)
        chart.setDrawGridBackground(false)
        chart.setDrawValueAboveBar(true)

        with(chart.xAxis) {
            position = XAxis.XAxisPosition.BOTTOM
            setDrawAxisLine(true)
            axisLineWidth = 1f
            setDrawGridLines(false)
            textSize = 8f
            granularity = 1f
            labelCount = Months.COUNT
            axisMinimum = -0.5f
            axisMaximum = Months.COUNT - 0.5f
            valueFormatter = monthFormatter
            setDrawLimitLinesBehindData(true)
        }

        with(chart.axisLeft) {
            isEnabled = true
            setDrawAxisLine(true)
            setDrawGridLines(false)
            axisMinimum = -1f
            setDrawLabels(false)
            setDrawLimitLinesBehindData(true)
        }

        chart.axisRight.isEnabled = false
        chart.legend.isEnabled = false

        chart.setHardwareAccelerationEnabled(true)

        if (savedInstanceState != null) {
            updateYears()
            updateChart()
        }

        tabLayout!!.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                if (!updateYearsInProcess) {
                    setYear(getYearFromPosition(tab.position))
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}

            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        view.setOnTouchListener(onSwipeTouchListener)

        return view
    }

    private fun selectTab(tab: TabLayout.Tab?) {
        tab?.select()
    }

    private fun updateYears() {
        updateYearsInProcess = true
        try {
            if (chartCostModel!!.years?.isEmpty() == true) {
                return
            }

            for (year in chartCostModel!!.years!!) {
                tabLayout!!.addTab(tabLayout!!.newTab().setText(year.toString()))
            }

            var position = getPositionForYear(chartCostModel!!.year)
            if (position == -1) {
                position = chartCostModel!!.years!!.size - 1
                chartCostModel!!.year = chartCostModel!!.years!![position]
            }

            selectTab(tabLayout!!.getTabAt(position))
        } finally {
            updateYearsInProcess = false
        }
    }

    private fun setYear(year: Int) {
        if (year == chartCostModel!!.year) return

        chartCostModel!!.year = year

        loaderManager.restartLoader(CHART_CURSOR_LOADER_ID, null, this)
    }

    private class ChartCursorLoader(context: Context, private val year: Int) : CursorLoader(context) {

        override fun loadInBackground(): Cursor? {
            return ContentResolverHelper.getSumByMonthsForYear(context, year)
        }
    }

    private class YearsCursorLoader(context: Context) : CursorLoader(context) {

        override fun loadInBackground(): Cursor? {
            return ContentResolverHelper.getYears(context)
        }
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        return when (id) {
            CHART_CURSOR_LOADER_ID -> ChartCursorLoader(context!!, chartCostModel!!.year)
            YEARS_CURSOR_LOADER_ID -> YearsCursorLoader(context!!)
            else -> throw IllegalArgumentException("Wrong Loader ID")
        }
    }

    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor) {
        when (loader.id) {
            CHART_CURSOR_LOADER_ID -> {
                var sum: Float
                var month: String

                chartCostModel!!.clear()
                val sums = chartCostModel!!.sums

                if (data.count > 0) {
                    if (data.moveToFirst())
                        do {
                            sum = data.getFloat(DatabaseModel.TableFueling.Columns.COST_SUM_INDEX)
                            month = data.getString(DatabaseModel.TableFueling.Columns.MONTH_INDEX)

                            sums[Integer.parseInt(month) - 1] = sum
                        } while (data.moveToNext())

                    chartCostModel!!.sums = sums
                }

                updateChart()
            }
            YEARS_CURSOR_LOADER_ID -> {
                val count = data.count

                if (count > 0) {
                    val years = IntArray(count)

                    var i = 0

                    if (data.moveToFirst())
                        do {
                            years[i] = data.getInt(DatabaseModel.TableFueling.Columns.YEAR_INDEX)

                            i++
                        } while (data.moveToNext())

                    chartCostModel!!.years = years

                    updateYears()

                    loaderManager.initLoader(CHART_CURSOR_LOADER_ID, null, this)
                } else {
                    chartCostModel!!.years = null

                    updateChart()
                }
            }
        }
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {}

    private class FloatFormatter : IValueFormatter {

        override fun getFormattedValue(value: Float, entry: Entry, dataSetIndex: Int, viewPortHandler: ViewPortHandler): String {
            return UtilsFormat.floatToString(value, false)
        }
    }

    private inner class MonthFormatter : IAxisValueFormatter {

        override fun getFormattedValue(value: Float, axis: AxisBase): String {
            return months.getMonth(value.toInt())
        }
    }

    private fun round(f: Float): Float {
        return Math.round(f / 10f) * 10f
    }

    private fun updateChart() {
        handler.removeCallbacks(runnableShowNoRecords)

        if (chartCostModel!!.hasData) {
            textNoRecords!!.visibility = View.GONE

            val sumsEntries = ArrayList<BarEntry>()

            val sums = chartCostModel!!.sums

            var sum: Float

            for (i in sums.indices) {
                sum = sums[i]

                sum = round(sum)

                sumsEntries.add(BarEntry(i.toFloat(), sum))
            }

            val sumsSet = BarDataSet(sumsEntries, "")
            sumsSet.setColors(colors, context)
            sumsSet.axisDependency = YAxis.AxisDependency.LEFT

            val dataSets = ArrayList<IBarDataSet>()
            dataSets.add(sumsSet)

            with(BarData(dataSets)) {
                setValueFormatter(floatFormatter)
                setValueTextSize(8f)
                barWidth = 0.8f

                chart.data = this
            }

            chart.axisLeft.removeAllLimitLines()

            if (chartCostModel!!.median > 0 && chartCostModel!!.isSumsNotEquals) {
                with(LimitLine(chartCostModel!!.median)) {
                    lineColor = Utils.getColor(R.color.chart_median)
                    lineWidth = 0.5f
                    enableDashedLine(8f, 2f, 0f)

                    chart.axisLeft.addLimitLine(this)
                }
            }

            val duration = Utils.getInteger(R.integer.animation_chart)
            chart.animateXY(duration, duration)
        } else {
            chart.clear()

            handler.postDelayed(runnableShowNoRecords, Utils.getInteger(R.integer.delayed_time_show_no_records).toLong())
        }

        updateTotal()
    }

    private fun updateTotal() {
        UtilsFormat.floatToTextView(textMedian, round(chartCostModel!!.median), true)
        UtilsFormat.floatToTextView(textSum, round(chartCostModel!!.sum), true)
    }

    companion object {

        private const val YEARS_CURSOR_LOADER_ID = 0
        private const val CHART_CURSOR_LOADER_ID = 1
    }
}