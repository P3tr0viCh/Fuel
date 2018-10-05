package ru.p3tr0vich.fuel.views

import ru.p3tr0vich.fuel.models.FuelingRecord
import ru.p3tr0vich.fuel.presenters.FuelingTotalPresenter

abstract class FuelingTotalViewBase : FuelingTotalView {

    @Suppress("LeakingThis")
    private val mFuelingTotalPresenter: FuelingTotalPresenter = FuelingTotalPresenter(this)

    override fun onFuelingRecordsChanged(fuelingRecords: List<FuelingRecord>?) {
        mFuelingTotalPresenter.onFuelingRecordsChanged(fuelingRecords)
    }

    override fun onLastFuelingRecordsChanged(fuelingRecords: List<FuelingRecord>?) {
        mFuelingTotalPresenter.onLastFuelingRecordsChanged(fuelingRecords)
    }

    override fun destroy() {
        mFuelingTotalPresenter.onDestroy()
    }
}