package ru.p3tr0vich.fuel.fragments

import android.content.Context
import android.database.Cursor
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.TabLayout
import android.support.v4.app.LoaderManager
import android.support.v4.content.CursorLoader
import android.support.v4.content.Loader
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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

    private var mTabLayout: TabLayout? = null
    private var mChart: BarChart? = null
    private var mTextNoRecords: TextView? = null
    private var mTextMedian: TextView? = null
    private var mTextSum: TextView? = null

    private val mColors = intArrayOf(R.color.chart_winter, R.color.chart_winter, R.color.chart_spring, R.color.chart_spring, R.color.chart_spring, R.color.chart_summer, R.color.chart_summer, R.color.chart_summer, R.color.chart_autumn, R.color.chart_autumn, R.color.chart_autumn, R.color.chart_winter)

    private var mUpdateYearsInProcess = true

    private var mChartCostModel: ChartCostModel? = null

    private lateinit var months: Months

    override val titleId: Int
        get() = R.string.title_chart_cost

    override val subtitleId: Int
        get() = R.string.title_chart_cost_subtitle

    private val mOnSwipeTouchListener = object : OnSwipeTouchListener(context) {
        override fun onSwipeRight() {
            val position = mTabLayout!!.selectedTabPosition
            if (position != 0) {
                selectTab(mTabLayout!!.getTabAt(position - 1))
            }
        }

        override fun onSwipeLeft() {
            val position = mTabLayout!!.selectedTabPosition
            if (position != mTabLayout!!.tabCount - 1) {
                selectTab(mTabLayout!!.getTabAt(position + 1))
            }
        }

        override fun onSwipeTop() {}

        override fun onSwipeBottom() {}
    }

    private val mFloatFormatter = FloatFormatter()
    private val mMonthFormatter = MonthFormatter()

    private val mHandler = Handler()

    private val mRunnableShowNoRecords = Runnable { Utils.setViewVisibleAnimate(mTextNoRecords!!, true) }

    private class Months(context: Context) {

        private val months = ArrayList<String>(COUNT)

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
        return if (mChartCostModel!!.years != null) mChartCostModel!!.years!![index] else -1
    }

    private fun getPositionForYear(year: Int): Int {
        if (mChartCostModel!!.years == null) return -1

        for (i in 0 until mChartCostModel!!.years!!.size)
            if (mChartCostModel!!.years!![i] == year)
                return i
        return -1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val context = context!!

        months = Months(context)

        if (savedInstanceState == null) {
            mChartCostModel = ChartCostModel()

            loaderManager.initLoader(YEARS_CURSOR_LOADER_ID, null, this)
        } else
            mChartCostModel = savedInstanceState.getParcelable(ChartCostModel.NAME)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(ChartCostModel.NAME, mChartCostModel)
    }

    override fun onDestroy() {
        mHandler.removeCallbacks(mRunnableShowNoRecords)
        super.onDestroy()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_chart_cost, container, false)

        mTabLayout = view.findViewById(R.id.tab_layout)

        mTextNoRecords = view.findViewById(R.id.text_no_records)
        mTextNoRecords!!.visibility = View.GONE

        mTextMedian = view.findViewById(R.id.text_median)
        mTextSum = view.findViewById(R.id.text_sum)

        mChart = view.findViewById(R.id.chart)

        mChart!!.description.text = ""

        mChart!!.setNoDataText("")

        mChart!!.setDrawBarShadow(false)
        mChart!!.setTouchEnabled(false)
        mChart!!.setScaleEnabled(false)
        mChart!!.isDoubleTapToZoomEnabled = false
        mChart!!.setPinchZoom(false)
        mChart!!.setDrawGridBackground(false)
        mChart!!.setDrawValueAboveBar(true)

        val xAxis = mChart!!.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawAxisLine(true)
        xAxis.axisLineWidth = 1f
        xAxis.setDrawGridLines(false)
        xAxis.textSize = 8f
        xAxis.granularity = 1f
        xAxis.labelCount = Months.COUNT
        xAxis.axisMinimum = -0.5f
        xAxis.axisMaximum = Months.COUNT - 0.5f
        xAxis.valueFormatter = mMonthFormatter
        xAxis.setDrawLimitLinesBehindData(true)

        val yAxis = mChart!!.axisLeft
        yAxis.isEnabled = true
        xAxis.setDrawAxisLine(true)
        xAxis.setDrawGridLines(false)
        yAxis.axisMinimum = -1f
        yAxis.setDrawLabels(false)
        yAxis.setDrawLimitLinesBehindData(true)

        mChart!!.axisRight.isEnabled = false
        mChart!!.legend.isEnabled = false

        mChart!!.setHardwareAccelerationEnabled(true)

        if (savedInstanceState != null) {
            updateYears()
            updateChart()
        }

        mTabLayout!!.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                if (!mUpdateYearsInProcess) {
                    setYear(getYearFromPosition(tab.position))
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}

            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        view.setOnTouchListener(mOnSwipeTouchListener)
        mChart!!.setOnTouchListener(mOnSwipeTouchListener)

        return view
    }

    private fun selectTab(tab: TabLayout.Tab?) {
        tab?.select()
    }

    private fun updateYears() {
        mUpdateYearsInProcess = true
        try {
            if (mChartCostModel!!.years == null || mChartCostModel!!.years!!.size == 0)
                return

            for (year in mChartCostModel!!.years!!) {
                mTabLayout!!.addTab(mTabLayout!!.newTab().setText(year.toString()))
            }

            var position = getPositionForYear(mChartCostModel!!.year)
            if (position == -1) {
                position = mChartCostModel!!.years!!.size - 1
                mChartCostModel!!.year = mChartCostModel!!.years!![position]
            }

            selectTab(mTabLayout!!.getTabAt(position))
        } finally {
            mUpdateYearsInProcess = false
        }
    }

    private fun setYear(year: Int) {
        if (year == mChartCostModel!!.year) return

        mChartCostModel!!.year = year

        loaderManager.restartLoader(CHART_CURSOR_LOADER_ID, null, this)
    }

    private class ChartCursorLoader internal constructor(context: Context, private val mYear: Int) : CursorLoader(context) {

        override fun loadInBackground(): Cursor? {
            return ContentResolverHelper.getSumByMonthsForYear(context, mYear)
        }
    }

    private class YearsCursorLoader internal constructor(context: Context) : CursorLoader(context) {

        override fun loadInBackground(): Cursor? {
            return ContentResolverHelper.getYears(context)
        }
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        return when (id) {
            CHART_CURSOR_LOADER_ID -> ChartCursorLoader(context!!, mChartCostModel!!.year)
            YEARS_CURSOR_LOADER_ID -> YearsCursorLoader(context!!)
            else -> throw IllegalArgumentException("Wrong Loader ID")
        }
    }

    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor) {
        when (loader.id) {
            CHART_CURSOR_LOADER_ID -> {
                var sum: Float
                var month: String

                mChartCostModel!!.clear()
                val sums = mChartCostModel!!.sums

                if (data.count > 0) {
                    if (data.moveToFirst())
                        do {
                            sum = data.getFloat(DatabaseModel.TableFueling.Columns.COST_SUM_INDEX)
                            month = data.getString(DatabaseModel.TableFueling.Columns.MONTH_INDEX)

                            sums[Integer.parseInt(month) - 1] = sum
                        } while (data.moveToNext())

                    mChartCostModel!!.sums = sums
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

                    mChartCostModel!!.years = years

                    updateYears()

                    loaderManager.initLoader(CHART_CURSOR_LOADER_ID, null, this)
                } else {
                    mChartCostModel!!.years = null

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
        mHandler.removeCallbacks(mRunnableShowNoRecords)

        if (mChartCostModel!!.hasData) {
            mTextNoRecords!!.visibility = View.GONE

            val sumsEntries = ArrayList<BarEntry>()

            val sums = mChartCostModel!!.sums
            var sum: Float
            for (i in sums.indices) {
                sum = sums[i]

                sum = round(sum)

                sumsEntries.add(BarEntry(i.toFloat(), sum))
            }

            val sumsSet = BarDataSet(sumsEntries, "")
            sumsSet.setColors(mColors, context)
            sumsSet.axisDependency = YAxis.AxisDependency.LEFT

            val dataSets = ArrayList<IBarDataSet>()
            dataSets.add(sumsSet)

            val sumsData = BarData(dataSets)
            sumsData.setValueFormatter(mFloatFormatter)
            sumsData.setValueTextSize(8f)
            sumsData.barWidth = 0.8f

            mChart!!.data = sumsData

            mChart!!.axisLeft.removeAllLimitLines()

            if (mChartCostModel!!.median > 0 && mChartCostModel!!.isSumsNotEquals) {
                val medianLine = LimitLine(mChartCostModel!!.median)
                medianLine.lineColor = Utils.getColor(R.color.chart_median)
                medianLine.lineWidth = 0.5f
                medianLine.enableDashedLine(8f, 2f, 0f)

                mChart!!.axisLeft.addLimitLine(medianLine)
            }

            val duration = Utils.getInteger(R.integer.animation_chart)
            mChart!!.animateXY(duration, duration)
        } else {
            mChart!!.clear()

            mHandler.postDelayed(mRunnableShowNoRecords, Utils.getInteger(R.integer.delayed_time_show_no_records).toLong())
        }

        updateTotal()
    }

    private fun updateTotal() {
        UtilsFormat.floatToTextView(mTextMedian!!, round(mChartCostModel!!.median), true)
        UtilsFormat.floatToTextView(mTextSum!!, round(mChartCostModel!!.sum), true)
    }

    companion object {

        private val YEARS_CURSOR_LOADER_ID = 0
        private val CHART_CURSOR_LOADER_ID = 1
    }
}