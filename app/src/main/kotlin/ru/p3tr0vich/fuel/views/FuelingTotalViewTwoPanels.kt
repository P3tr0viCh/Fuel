package ru.p3tr0vich.fuel.views

import android.view.View
import android.widget.TextView

import ru.p3tr0vich.fuel.R
import ru.p3tr0vich.fuel.utils.UtilsFormat
import kotlin.math.roundToInt

class FuelingTotalViewTwoPanels(view: View) : FuelingTotalViewBase() {

    private val averageAndLastConsumptionCollapsed: TextView = view.findViewById(R.id.text_average_and_last_cons_collapsed)
    private val estimatedMileageAndTotalCollapsed: TextView = view.findViewById(R.id.text_estimated_mileage_and_total_collapsed)
    private val costSumCollapsed: TextView? = view.findViewById(R.id.text_cost_sum_collapsed)

    private val costSumExpanded: TextView = view.findViewById(R.id.text_cost_sum_expanded)
    private val averageExpanded: TextView = view.findViewById(R.id.text_average_expanded)
    private val lastConsumptionExpanded: TextView = view.findViewById(R.id.text_last_cons_expanded)
    private val estimatedMileageExpanded: TextView = view.findViewById(R.id.text_estimated_mileage_expanded)
    private val estimatedTotalExpanded: TextView = view.findViewById(R.id.text_estimated_total_expanded)

    private var average: String? = null
    private var costSum: String? = null
    private var lastConsumption: String? = null
    private var estimatedMileage: String? = null
    private var estimatedTotal: String? = null

    override fun setAverage(value: Float) {
        average = UtilsFormat.floatToString(value)

        averageAndLastConsumptionCollapsed.text = String.format("%s/%s", lastConsumption, average)

        averageExpanded.text = average
    }

    override fun setCostSum(value: Float) {
        costSum = UtilsFormat.floatToString(value)

        costSumCollapsed?.text = costSum

        costSumExpanded.text = costSum
    }

    override fun setLastConsumption(value: Float) {
        lastConsumption = UtilsFormat.floatToString(value)

        averageAndLastConsumptionCollapsed.text = String.format("%s/%s", lastConsumption, average)

        lastConsumptionExpanded.text = lastConsumption
    }

    override fun setEstimatedMileage(value: Float) {
        estimatedMileage = value.roundToInt().toString()

        estimatedMileageAndTotalCollapsed.text = String.format("%s/%s", estimatedMileage, estimatedTotal)

        estimatedMileageExpanded.text = estimatedMileage
    }

    override fun setEstimatedTotal(value: Float) {
        estimatedTotal = value.roundToInt().toString()

        estimatedMileageAndTotalCollapsed.text = String.format("%s/%s", estimatedMileage, estimatedTotal)

        estimatedTotalExpanded.text = estimatedTotal
    }
}