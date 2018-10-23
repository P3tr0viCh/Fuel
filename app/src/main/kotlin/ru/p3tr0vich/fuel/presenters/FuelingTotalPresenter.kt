package ru.p3tr0vich.fuel.presenters

import ru.p3tr0vich.fuel.models.FuelingRecord
import ru.p3tr0vich.fuel.models.FuelingTotalModel
import ru.p3tr0vich.fuel.views.FuelingTotalView

class FuelingTotalPresenter(private val fuelingTotalView: FuelingTotalView) {
    private val fuelingTotalModel = FuelingTotalModel()

    init {
        fuelingTotalModel.onChangeListener = object : FuelingTotalModel.OnChangeListener {
            override fun onChange() {
                fuelingTotalView.setAverage(fuelingTotalModel.average)
                fuelingTotalView.setCostSum(fuelingTotalModel.costSum)
            }
        }
    }

    fun onFuelingRecordsChanged(fuelingRecords: List<FuelingRecord>?) {
        fuelingTotalModel.setFuelingRecords(fuelingRecords)
    }

    fun onLastFuelingRecordsChanged(fuelingRecords: List<FuelingRecord>?) {
        fuelingTotalModel.setLastRecords(fuelingRecords)

        fuelingTotalView.setLastConsumption(fuelingTotalModel.lastConsumption)
        fuelingTotalView.setEstimatedMileage(fuelingTotalModel.estimatedMileage)
        fuelingTotalView.setEstimatedTotal(fuelingTotalModel.estimatedTotal)
    }

    fun onDestroy() {
        fuelingTotalModel.destroy()
    }
}