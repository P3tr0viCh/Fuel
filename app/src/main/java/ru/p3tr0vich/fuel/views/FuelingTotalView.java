package ru.p3tr0vich.fuel.views;

import android.support.annotation.Nullable;
import android.support.annotation.Size;

import java.util.List;

import ru.p3tr0vich.fuel.models.FuelingRecord;

public interface FuelingTotalView {
    void setAverage(float average);

    void setCostSum(float costSum);

    void setLastConsumption(float lastConsumption);

    void setEstimatedMileage(float estimatedMileage);

    void setEstimatedTotal(float estimatedTotal);

    void onFuelingRecordsChanged(@Nullable List<FuelingRecord> fuelingRecords);

    void onLastFuelingRecordsChanged(@Nullable List<FuelingRecord> fuelingRecords);

    void destroy();
}