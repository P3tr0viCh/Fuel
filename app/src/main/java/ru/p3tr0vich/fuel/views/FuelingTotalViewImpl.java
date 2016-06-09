package ru.p3tr0vich.fuel.views;

import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Size;
import android.view.View;
import android.widget.TextView;

import java.util.List;

import ru.p3tr0vich.fuel.models.FuelingRecord;
import ru.p3tr0vich.fuel.presenters.FuelingTotalPresenter;
import ru.p3tr0vich.fuel.utils.UtilsFormat;

public class FuelingTotalViewImpl implements FuelingTotalView {
    private final TextView[] mAverageTextViews;
    private final TextView[] mCostSumTextViews;

    private final FuelingTotalPresenter mFuelingTotalPresenter;

    public FuelingTotalViewImpl(@NonNull View view,
                                @NonNull @IdRes @Size(min = 1) int[] averageIds,
                                @NonNull @IdRes @Size(min = 1) int[] costSumIds) {
        mAverageTextViews = new TextView[averageIds.length];
        mCostSumTextViews = new TextView[costSumIds.length];

        for (int i = 0; i < averageIds.length; i++)
            mAverageTextViews[i] = (TextView) view.findViewById(averageIds[i]);
        for (int i = 0; i < costSumIds.length; i++)
            mCostSumTextViews[i] = (TextView) view.findViewById(costSumIds[i]);

        mFuelingTotalPresenter = new FuelingTotalPresenter(this);
    }

    @Override
    public void setAverage(float average) {
        String text = UtilsFormat.floatToString(average);
        for (TextView textView : mAverageTextViews) textView.setText(text);
    }

    @Override
    public void setCostSum(float costSum) {
        String text = UtilsFormat.floatToString(costSum);
        for (TextView textView : mCostSumTextViews) textView.setText(text);
    }

    public void onFuelingRecordsChanged(@Nullable List<FuelingRecord> fuelingRecords) {
        mFuelingTotalPresenter.onFuelingRecordsChanged(fuelingRecords);
    }

    public void destroy() {
        mFuelingTotalPresenter.onDestroy();
    }
}