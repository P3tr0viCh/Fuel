package ru.p3tr0vich.fuel.models;

import android.os.AsyncTask;
import android.support.annotation.Nullable;

import java.util.List;

public class FuelingTotalModel {
    private float mAverage;
    private float mCostSum;

    private CalcTotalTask mCalcTotalTask;

    private OnChangeListener mOnChangeListener;

    public interface OnChangeListener {
        void onChange();
    }

    public FuelingTotalModel() {
    }

    public void destroy() {
        cancel();
    }

    public void setOnChangeListener(@Nullable OnChangeListener onChangeListener) {
        mOnChangeListener = onChangeListener;
    }

    public float getAverage() {
        return mAverage;
    }

    public float getCostSum() {
        return mCostSum;
    }

    private void cancel() {
        if (mCalcTotalTask != null) mCalcTotalTask.cancel(false);
    }

    public void setFuelingRecords(@Nullable List<FuelingRecord> fuelingRecords) {
        cancel();

        mCalcTotalTask = new CalcTotalTask(fuelingRecords);
        mCalcTotalTask.execute();
    }

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
                mAverage = result[0];
                mCostSum = result[1];

                if (mOnChangeListener != null) mOnChangeListener.onChange();
            }
        }
    }
}