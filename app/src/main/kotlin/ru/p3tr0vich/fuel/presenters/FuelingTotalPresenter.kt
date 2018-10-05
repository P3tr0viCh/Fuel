package ru.p3tr0vich.fuel.presenters

import ru.p3tr0vich.fuel.models.FuelingRecord
import ru.p3tr0vich.fuel.models.FuelingTotalModel
import ru.p3tr0vich.fuel.views.FuelingTotalView

class FuelingTotalPresenter(private val mFuelingTotalView: FuelingTotalView) {
    private val mFuelingTotalModel: FuelingTotalModel = FuelingTotalModel()

    init {
        mFuelingTotalModel.onChangeListener = object : FuelingTotalModel.OnChangeListener {
            override fun onChange() {
                mFuelingTotalView.setAverage(mFuelingTotalModel.average)
                mFuelingTotalView.setCostSum(mFuelingTotalModel.costSum)
            }
        }
    }

    fun onFuelingRecordsChanged(fuelingRecords: List<FuelingRecord>?) {
        mFuelingTotalModel.setFuelingRecords(fuelingRecords)
    }

    fun onLastFuelingRecordsChanged(fuelingRecords: List<FuelingRecord>?) {
        mFuelingTotalModel.setLastRecords(fuelingRecords)

        mFuelingTotalView.setLastConsumption(mFuelingTotalModel.lastConsumption)
        mFuelingTotalView.setEstimatedMileage(mFuelingTotalModel.estimatedMileage)
        mFuelingTotalView.setEstimatedTotal(mFuelingTotalModel.estimatedTotal)
    }

    fun onDestroy() {
        mFuelingTotalModel.destroy()
    }
}