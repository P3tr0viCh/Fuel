package ru.p3tr0vich.fuel.views;

import android.support.annotation.Nullable;

import java.util.List;

import ru.p3tr0vich.fuel.models.FuelingRecord;
import ru.p3tr0vich.fuel.presenters.FuelingTotalPresenter;
import ru.p3tr0vich.fuel.utils.UtilsFormat;

public abstract class FuelingTotalViewBase implements FuelingTotalView {

    private final FuelingTotalPresenter mFuelingTotalPresenter;

    protected FuelingTotalViewBase() {
        mFuelingTotalPresenter = new FuelingTotalPresenter(this);
    }

    protected static String floatToString(float value, boolean round) {
        return UtilsFormat.floatToString(round ? Math.round(value) : value);
    }

    public void onFuelingRecordsChanged(@Nullable List<FuelingRecord> fuelingRecords) {
        mFuelingTotalPresenter.onFuelingRecordsChanged(fuelingRecords);
    }

    @Override
    public void onLastFuelingRecordsChanged(@Nullable List<FuelingRecord> fuelingRecords) {
        mFuelingTotalPresenter.onLastFuelingRecordsChanged(fuelingRecords);
    }

    public void destroy() {
        mFuelingTotalPresenter.onDestroy();
    }
}