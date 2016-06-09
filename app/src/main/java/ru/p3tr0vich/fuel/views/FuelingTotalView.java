package ru.p3tr0vich.fuel.views;

import android.support.annotation.Nullable;

import java.util.List;

import ru.p3tr0vich.fuel.models.FuelingRecord;

public interface FuelingTotalView {
    void setAverage(float average);

    void setCostSum(float costSum);

    void onFuelingRecordsChanged(@Nullable List<FuelingRecord> fuelingRecords);

    void destroy();
}