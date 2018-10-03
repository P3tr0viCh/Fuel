package ru.p3tr0vich.fuel.views

import ru.p3tr0vich.fuel.models.FuelingRecord

interface FuelingTotalView {
    fun setAverage(value: Float)

    fun setCostSum(value: Float)

    fun setLastConsumption(value: Float)

    fun setEstimatedMileage(value: Float)

    fun setEstimatedTotal(value: Float)

    fun onFuelingRecordsChanged(fuelingRecords: List<FuelingRecord>?)

    fun onLastFuelingRecordsChanged(fuelingRecords: List<FuelingRecord>?)

    fun destroy()
}