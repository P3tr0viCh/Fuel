package ru.p3tr0vich.fuel.models;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.support.annotation.Nullable;

import java.util.List;

public class FuelingTotalModel {
    // Средний расход в заданном периоде.
    private float mAverageConsumption;
    // Сумма стоимости для заданного периода.
    private float mCostSum;
    // Расход перед последней записью.
    private float mLastConsumption;
    // Предполагаемый пробег после последней заправки.
    private float mEstimatedMileage;
    // Предполагаемый общий пробег после последней заправки.
    private float mEstimatedTotal;

    private CalcTotalTask mCalcTotalTask;

    private OnChangeListener mOnChangeListener;

    public interface OnChangeListener {
        void onChange();
    }

    public FuelingTotalModel() {
        mAverageConsumption = 0;
        mCostSum = 0;
        mLastConsumption = 0;
        mEstimatedMileage = 0;
        mEstimatedTotal = 0;
    }

    public void destroy() {
        cancel();
    }

    public void setOnChangeListener(@Nullable OnChangeListener onChangeListener) {
        mOnChangeListener = onChangeListener;
    }

    public float getAverage() {
        return mAverageConsumption;
    }

    public float getCostSum() {
        return mCostSum;
    }

    public float getLastConsumption() {
        return mLastConsumption;
    }

    public float getEstimatedMileage() {
        return mEstimatedMileage;
    }

    public float getEstimatedTotal() {
        return mEstimatedTotal;
    }

    /**
     * Вычисляет по содержимому двух последних записей в БД
     * последний расход и берёт из последней записи объём залитого топлива и общий пробег.
     * На основании этих данных вычисляет предполагаемый пробег
     * на объёме последней заправки и предполагемый общий пробег.
     *
     * @param fuelingRecords Последняя и предпоследняя записи в БД.
     */
    public void setLastRecords(@Nullable List<FuelingRecord> fuelingRecords) {
        mLastConsumption = 0;
        mEstimatedMileage = 0;
        mEstimatedTotal = 0;

        if (fuelingRecords == null || fuelingRecords.size() < 2) return;

        FuelingRecord lastRecord = fuelingRecords.get(0);
        // Объём последней заправки.
        float lastVolume = lastRecord.getVolume();
        // Общий пробег в последней записи.
        float lastTotal = lastRecord.getTotal();

        if (lastVolume <= 0 || lastTotal <= 0) return;

        FuelingRecord penultimateRecord = fuelingRecords.get(1);

        float penultimateVolume = penultimateRecord.getVolume();

        if (penultimateVolume <= 0) return;

        float penultimateTotal = penultimateRecord.getTotal();

        // Единственное, что может равняться нулю.
        if (penultimateTotal < 0 || penultimateTotal >= lastTotal) return;

        // Все необходимые значения в записях указаны корректно.

        mLastConsumption = (penultimateVolume / (lastTotal - penultimateTotal)) * 100;

        // Пробег на одной единице объёма (литр, галлон).
        float distancePerOneFuel = 100.0f / mLastConsumption;

        mEstimatedMileage = distancePerOneFuel * lastVolume;

        mEstimatedTotal = lastTotal + mEstimatedMileage;
    }

    private void cancel() {
        if (mCalcTotalTask != null) mCalcTotalTask.cancel(false);
    }

    public void setFuelingRecords(@Nullable List<FuelingRecord> fuelingRecords) {
        cancel();

        mCalcTotalTask = new CalcTotalTask(fuelingRecords);
        mCalcTotalTask.execute();
    }

    //// TODO: 21.09.2018 make static
    @SuppressLint("StaticFieldLeak")
    private class CalcTotalTask extends AsyncTask<Void, Void, Float[]> {

        private final List<FuelingRecord> mFuelingRecords;

        CalcTotalTask(@Nullable List<FuelingRecord> fuelingRecords) {
            mFuelingRecords = fuelingRecords;
        }

        @Override
        protected Float[] doInBackground(Void... params) {
            if (mFuelingRecords == null) return new Float[]{0f, 0f};

            float costSum = 0, volumeSum = 0;
            float volume, total, firstTotal = 0, lastTotal = 0;

            FuelingRecord fuelingRecord;

            boolean completeData = true; // Во всех записях указаны объём заправки и текущий пробег

            final int size = mFuelingRecords.size();

            for (int i = 0; i < size; i++) {

                if (isCancelled()) return null;

                fuelingRecord = mFuelingRecords.get(i);

                costSum += fuelingRecord.getCost();

                if (completeData) {
                    volume = fuelingRecord.getVolume();
                    total = fuelingRecord.getTotal();

                    if (volume == 0 || total == 0) completeData = false;
                    else {
                        // Сортировка записей по дате в обратном порядке
                        // 0 -- последняя заправка
                        // Последний (i == 0) объём заправки не нужен -- неизвестно, сколько на ней будет пробег,
                        // в volumeSum не включается
                        if (i == 0) lastTotal = total;
                        else {
                            volumeSum += volume;
                            if (i == size - 1) firstTotal = total;
                        }
                    }
                }
            }

            float average;

            if (completeData)
                average = volumeSum != 0 ? (volumeSum / (lastTotal - firstTotal)) * 100 : 0;
            else {
                average = 0;
                int averageCount = 0;

                for (int i = 0, count = size - 1; i < count; i++) {

                    if (isCancelled()) return null;

                    lastTotal = mFuelingRecords.get(i).getTotal();
                    if (lastTotal != 0) {
                        fuelingRecord = mFuelingRecords.get(i + 1);

                        volume = fuelingRecord.getVolume();
                        total = fuelingRecord.getTotal();

                        if (volume != 0 && total != 0) {
                            average += (volume / (lastTotal - total)) * 100;
                            averageCount++;
                        }
                    }
                }

                average = averageCount != 0 ? average / averageCount : 0;
            }

            return new Float[]{average, costSum};
        }

        @Override
        protected void onPostExecute(Float[] result) {
            if (result != null) {
                mAverageConsumption = result[0];
                mCostSum = result[1];

                if (mOnChangeListener != null) mOnChangeListener.onChange();
            }
        }
    }
}