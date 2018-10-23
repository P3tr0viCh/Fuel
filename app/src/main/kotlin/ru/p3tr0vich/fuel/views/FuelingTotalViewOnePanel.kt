package ru.p3tr0vich.fuel.views

import android.view.View
import android.widget.TextView

import ru.p3tr0vich.fuel.R
import ru.p3tr0vich.fuel.utils.UtilsFormat

class FuelingTotalViewOnePanel(view: View) : FuelingTotalViewBase() {

    private val average: TextView = view.findViewById(R.id.text_average)
    private val costSum: TextView = view.findViewById(R.id.text_cost_sum)
    private val lastConsumption: TextView = view.findViewById(R.id.text_last_cons)
    private val estimatedMileage: TextView = view.findViewById(R.id.text_estimated_mileage)
    private val estimatedTotal: TextView = view.findViewById(R.id.text_estimated_total)

    override fun setAverage(value: Float) {
        average.text = UtilsFormat.floatToString(value)
    }

    override fun setCostSum(value: Float) {
        costSum.text = UtilsFormat.floatToString(value)
    }

    override fun setLastConsumption(value: Float) {
        lastConsumption.text = UtilsFormat.floatToString(value)
    }

    override fun setEstimatedMileage(value: Float) {
        estimatedMileage.text = Math.round(value).toString()
    }

    override fun setEstimatedTotal(value: Float) {
        estimatedTotal.text = Math.round(value).toString()
    }
}