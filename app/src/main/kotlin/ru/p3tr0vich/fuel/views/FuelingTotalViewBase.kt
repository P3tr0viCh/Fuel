package ru.p3tr0vich.fuel.views

import ru.p3tr0vich.fuel.models.FuelingRecord
import ru.p3tr0vich.fuel.presenters.FuelingTotalPresenter

abstract class FuelingTotalViewBase : FuelingTotalView {

    @Suppress("LeakingThis")
    private val fuelingTotalPresenter = FuelingTotalPresenter(this)

    override fun onFuelingRecordsChanged(fuelingRecords: List<FuelingRecord>?) {
        fuelingTotalPresenter.onFuelingRecordsChanged(fuelingRecords)
    }

    override fun onLastFuelingRecordsChanged(fuelingRecords: List<FuelingRecord>?) {
        fuelingTotalPresenter.onLastFuelingRecordsChanged(fuelingRecords)
    }

    override fun destroy() {
        fuelingTotalPresenter.onDestroy()
    }
}