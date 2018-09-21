package ru.p3tr0vich.fuel.views;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.TextView;

import ru.p3tr0vich.fuel.R;

public class FuelingTotalViewTwoPanels extends FuelingTotalViewBase {

    private final TextView mAverageAndLastConsumptionCollapsed;
    private final TextView mEstimatedMileageAndTotalCollapsed;
    @Nullable
    private final TextView mCostSumCollapsed;

    private final TextView mCostSumExpanded;
    private final TextView mAverageExpanded;
    private final TextView mLastConsumptionExpanded;
    private final TextView mEstimatedMileageExpanded;
    private final TextView mEstimatedTotalExpanded;

    private String mAverage;
    private String mLastConsumption;
    private String mEstimatedMileage;
    private String mEstimatedTotal;

    public FuelingTotalViewTwoPanels(@NonNull View view) {
        mAverageAndLastConsumptionCollapsed = view.findViewById(R.id.text_average_and_last_cons_collapsed);
        mEstimatedMileageAndTotalCollapsed = view.findViewById(R.id.text_estimated_mileage_and_total_collapsed);
        mCostSumCollapsed = view.findViewById(R.id.text_cost_sum_collapsed);

        mAverageExpanded = view.findViewById(R.id.text_average_expanded);
        mCostSumExpanded = view.findViewById(R.id.text_cost_sum_expanded);
        mLastConsumptionExpanded = view.findViewById(R.id.text_last_cons_expanded);
        mEstimatedMileageExpanded = view.findViewById(R.id.text_estimated_mileage_expanded);
        mEstimatedTotalExpanded = view.findViewById(R.id.text_estimated_total_expanded);
    }

    private void updateAverageAndLastConsumptionCollapsed() {
        mAverageAndLastConsumptionCollapsed.setText(String.format("%s/%s", mLastConsumption, mAverage));
    }

    private void updateEstimatedMileageAndTotalCollapsed() {
        mEstimatedMileageAndTotalCollapsed.setText(String.format("%s/%s", mEstimatedMileage, mEstimatedTotal));
    }

    @Override
    public void setAverage(float average) {
        mAverage = floatToString(average, false);
        updateAverageAndLastConsumptionCollapsed();
        mAverageExpanded.setText(mAverage);
    }

    @Override
    public void setCostSum(float costSum) {
        String text = floatToString(costSum, false);
        if (mCostSumCollapsed != null) mCostSumCollapsed.setText(text);
        mCostSumExpanded.setText(text);
    }

    @Override
    public void setLastConsumption(float lastConsumption) {
        mLastConsumption = floatToString(lastConsumption, false);
        updateAverageAndLastConsumptionCollapsed();
        mLastConsumptionExpanded.setText(mLastConsumption);
    }

    @Override
    public void setEstimatedMileage(float estimatedMileage) {
        mEstimatedMileage = floatToString(estimatedMileage, true);
        updateEstimatedMileageAndTotalCollapsed();
        mEstimatedMileageExpanded.setText(mEstimatedMileage);
    }

    @Override
    public void setEstimatedTotal(float estimatedTotal) {
        mEstimatedTotal = floatToString(estimatedTotal, true);
        updateEstimatedMileageAndTotalCollapsed();
        mEstimatedTotalExpanded.setText(mEstimatedTotal);
    }
}