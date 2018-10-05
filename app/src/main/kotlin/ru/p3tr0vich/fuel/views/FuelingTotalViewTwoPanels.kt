package ru.p3tr0vich.fuel.views

import android.view.View
import android.widget.TextView

import ru.p3tr0vich.fuel.R
import ru.p3tr0vich.fuel.utils.UtilsFormat

class FuelingTotalViewTwoPanels(view: View) : FuelingTotalViewBase() {

    private val mAverageAndLastConsumptionCollapsed: TextView = view.findViewById(R.id.text_average_and_last_cons_collapsed)
    private val mEstimatedMileageAndTotalCollapsed: TextView = view.findViewById(R.id.text_estimated_mileage_and_total_collapsed)
    private val mCostSumCollapsed: TextView? = view.findViewById(R.id.text_cost_sum_collapsed)

    private val mCostSumExpanded: TextView = view.findViewById(R.id.text_cost_sum_expanded)
    private val mAverageExpanded: TextView = view.findViewById(R.id.text_average_expanded)
    private val mLastConsumptionExpanded: TextView = view.findViewById(R.id.text_last_cons_expanded)
    private val mEstimatedMileageExpanded: TextView = view.findViewById(R.id.text_estimated_mileage_expanded)
    private val mEstimatedTotalExpanded: TextView = view.findViewById(R.id.text_estimated_total_expanded)

    private var mAverage: String? = null
    private var mCostSum: String? = null
    private var mLastConsumption: String? = null
    private var mEstimatedMileage: String? = null
    private var mEstimatedTotal: String? = null

    override fun setAverage(value: Float) {
        mAverage = UtilsFormat.floatToString(value)

        mAverageAndLastConsumptionCollapsed.text = String.format("%s/%s", mLastConsumption, mAverage)

        mAverageExpanded.text = mAverage
    }

    override fun setCostSum(value: Float) {
        mCostSum = UtilsFormat.floatToString(value)

        mCostSumCollapsed?.text = mCostSum

        mCostSumExpanded.text = mCostSum
    }

    override fun setLastConsumption(value: Float) {
        mLastConsumption = UtilsFormat.floatToString(value)

        mAverageAndLastConsumptionCollapsed.text = String.format("%s/%s", mLastConsumption, mAverage)

        mLastConsumptionExpanded.text = mLastConsumption
    }

    override fun setEstimatedMileage(value: Float) {
        mEstimatedMileage = Math.round(value).toString()

        mEstimatedMileageAndTotalCollapsed.text = String.format("%s/%s", mEstimatedMileage, mEstimatedTotal)

        mEstimatedMileageExpanded.text = mEstimatedMileage
    }

    override fun setEstimatedTotal(value: Float) {
        mEstimatedTotal = Math.round(value).toString()

        mEstimatedMileageAndTotalCollapsed.text = String.format("%s/%s", mEstimatedMileage, mEstimatedTotal)

        mEstimatedTotalExpanded.text = mEstimatedTotal
    }
}