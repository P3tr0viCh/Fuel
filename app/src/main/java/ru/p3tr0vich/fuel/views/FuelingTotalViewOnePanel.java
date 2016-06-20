package ru.p3tr0vich.fuel.views;

import android.support.annotation.NonNull;
import android.view.View;
import android.widget.TextView;

import ru.p3tr0vich.fuel.R;

public class FuelingTotalViewOnePanel extends FuelingTotalViewBase {

    private final TextView mAverage;
    private final TextView mCostSum;
    private final TextView mLastConsumption;
    private final TextView mEstimatedMileage;
    private final TextView mEstimatedTotal;

    public FuelingTotalViewOnePanel(@NonNull View view) {
        mAverage = (TextView) view.findViewById(R.id.text_average);
        mCostSum = (TextView) view.findViewById(R.id.text_cost_sum);
        mLastConsumption = (TextView) view.findViewById(R.id.text_last_cons);
        mEstimatedMileage = (TextView) view.findViewById(R.id.text_estimated_mileage);
        mEstimatedTotal = (TextView) view.findViewById(R.id.text_estimated_total);
    }

    @Override
    public void setAverage(float average) {
        mAverage.setText(floatToString(average, false));
    }

    @Override
    public void setCostSum(float costSum) {
        mCostSum.setText(floatToString(costSum, false));
    }

    @Override
    public void setLastConsumption(float lastConsumption) {
        mLastConsumption.setText(floatToString(lastConsumption, false));
    }

    @Override
    public void setEstimatedMileage(float estimatedMileage) {
        mEstimatedMileage.setText(floatToString(estimatedMileage, true));
    }

    @Override
    public void setEstimatedTotal(float estimatedTotal) {
        mEstimatedTotal.setText(floatToString(estimatedTotal, true));
    }
}