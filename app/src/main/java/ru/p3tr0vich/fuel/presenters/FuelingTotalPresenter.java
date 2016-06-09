package ru.p3tr0vich.fuel.presenters;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;

import ru.p3tr0vich.fuel.models.FuelingRecord;
import ru.p3tr0vich.fuel.models.FuelingTotalModel;
import ru.p3tr0vich.fuel.views.FuelingTotalView;

public class FuelingTotalPresenter {
    private FuelingTotalView mFuelingTotalView;
    private final FuelingTotalModel mFuelingTotalModel;

    public FuelingTotalPresenter(@NonNull FuelingTotalView fuelingTotalView) {
        mFuelingTotalView = fuelingTotalView;
        mFuelingTotalModel = new FuelingTotalModel();
        mFuelingTotalModel.setOnChangeListener(new FuelingTotalModel.OnChangeListener() {
            @Override
            public void onChange() {
                mFuelingTotalView.setAverage(mFuelingTotalModel.getAverage());
                mFuelingTotalView.setCostSum(mFuelingTotalModel.getCostSum());
            }
        });
    }

    public void onFuelingRecordsChanged(@Nullable List<FuelingRecord> fuelingRecords) {
        mFuelingTotalModel.setFuelingRecords(fuelingRecords);
    }

    public void onDestroy() {
        mFuelingTotalModel.destroy();
    }
}