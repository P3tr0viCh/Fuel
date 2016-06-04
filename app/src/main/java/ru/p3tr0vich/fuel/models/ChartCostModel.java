package ru.p3tr0vich.fuel.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Size;

import java.util.Arrays;

import ru.p3tr0vich.fuel.utils.UtilsDate;

public class ChartCostModel implements Parcelable {

    public static final String NAME = "CHART_COST_MODEL";

    private int mYear;
    private int[] mYears;
    private float[] mSums = new float[12];
    private boolean mHasData;
    private float mMedian;
    private float mSum;
    boolean mSumsNotEquals;

    public ChartCostModel() {
        setYears(null);
        setSums(null);

        mYear = UtilsDate.getCurrentYear();
    }

    public int getYear() {
        return mYear;
    }

    public void setYear(int year) {
        mYear = year;
    }

    @Nullable
    public int[] getYears() {
        return mYears;
    }

    public void setYears(@Nullable int[] years) {
        mYears = years;
        if (years == null) mHasData = false;
    }

    @NonNull
    @Size(12)
    public float[] getSums() {
        return mSums;
    }

    public void setSums(@Nullable @Size(12) float[] sums) {
        if (sums == null) {
            for (int i = 0; i < 12; i++) mSums[i] = 0;
            mMedian = 0;
            mSum = 0;
            mHasData = false;
            mSumsNotEquals = false;
        } else {
            mSums = sums;
            mMedian = calcMedian(mSums);
            mSum = calcSum(mSums);
            mHasData = true;
        }
    }

    private float median(@NonNull float[] values) {
        int middle = values.length / 2;
        return values.length % 2 == 1 ? values[middle] : (values[middle - 1] + values[middle]) / 2f;
    }

    private float calcMedian(float[] sums) {
        int aboveZeroCount = 0;
        for (float value : sums) if (value > 0) aboveZeroCount++;

        if (aboveZeroCount > 1) {
            float[] sortedSums = new float[aboveZeroCount];

            int i = 0;
            for (float value : sums)
                if (value > 0) {
                    sortedSums[i] = value;
                    i++;
                }

            mSumsNotEquals = false;
            float value = sortedSums[0];
            for (int j = 1; j < sortedSums.length; j++)
                if (sortedSums[j] != value) {
                    mSumsNotEquals = true;
                    break;
                }

            Arrays.sort(sortedSums);

            return median(sortedSums);
        } else
            return 0;
    }

    private float calcSum(float[] sums) {
        float sum = 0;
        for (float value : sums) sum += value;
        return sum;
    }

    public boolean hasData() {
        return mHasData;
    }

    public float getMedian() {
        return mMedian;
    }

    public float getSum() {
        return mSum;
    }

    public boolean isSumsNotEquals() {
        return mSumsNotEquals;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mYear);
        dest.writeIntArray(mYears);
        dest.writeFloatArray(mSums);
        dest.writeInt(mHasData ? 1 : 0);
        dest.writeFloat(mMedian);
        dest.writeFloat(mSum);
        dest.writeInt(mSumsNotEquals ? 1 : 0);
    }

    public static final Parcelable.Creator<ChartCostModel> CREATOR = new Parcelable.Creator<ChartCostModel>() {

        @Override
        public ChartCostModel createFromParcel(Parcel in) {
            ChartCostModel chartCostModel = new ChartCostModel();

            chartCostModel.mYear = in.readInt();
            in.readIntArray(chartCostModel.mYears);
            in.readFloatArray(chartCostModel.mSums);
            chartCostModel.mHasData = in.readInt() != 0;
            chartCostModel.mMedian = in.readFloat();
            chartCostModel.mSum = in.readFloat();
            chartCostModel.mSumsNotEquals = in.readInt() != 0;

            return chartCostModel;
        }

        @Override
        public ChartCostModel[] newArray(int size) {
            return new ChartCostModel[size];
        }
    };

    @Override
    public String toString() {
        return "hasData: " + mHasData + ", years: " + Arrays.toString(mYears) +
                ", year: " + mYear + ", sums: " + Arrays.toString(mSums) +
                ", median: " + mMedian + ", sum: " + mSum + ", sumsNotEquals: " + mSumsNotEquals;
    }
}
