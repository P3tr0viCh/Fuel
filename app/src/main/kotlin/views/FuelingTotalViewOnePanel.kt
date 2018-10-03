package ru.p3tr0vich.fuel.views

import android.view.View
import android.widget.TextView

import ru.p3tr0vich.fuel.R
import ru.p3tr0vich.fuel.utils.UtilsFormat

class FuelingTotalViewOnePanel(view: View) : FuelingTotalViewBase() {

    private val mAverage: TextView = view.findViewById(R.id.text_average)
    private val mCostSum: TextView = view.findViewById(R.id.text_cost_sum)
    private val mLastConsumption: TextView = view.findViewById(R.id.text_last_cons)
    private val mEstimatedMileage: TextView = view.findViewById(R.id.text_estimated_mileage)
    private val mEstimatedTotal: TextView = view.findViewById(R.id.text_estimated_total)

    override fun setAverage(value: Float) {
        mAverage.text = UtilsFormat.floatToString(value)
    }

    override fun setCostSum(value: Float) {
        mCostSum.text = UtilsFormat.floatToString(value)
    }

    override fun setLastConsumption(value: Float) {
        mLastConsumption.text = UtilsFormat.floatToString(value)
    }

    override fun setEstimatedMileage(value: Float) {
        mEstimatedMileage.text = Math.round(value).toString()
    }

    override fun setEstimatedTotal(value: Float) {
        mEstimatedTotal.text = Math.round(value).toString()
    }
}