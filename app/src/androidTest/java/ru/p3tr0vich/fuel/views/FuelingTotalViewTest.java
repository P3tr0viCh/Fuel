package ru.p3tr0vich.fuel.views;

import android.support.annotation.Nullable;
import android.test.InstrumentationTestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import ru.p3tr0vich.fuel.models.FuelingRecord;
import ru.p3tr0vich.fuel.presenters.FuelingTotalPresenter;

public class FuelingTotalViewTest extends InstrumentationTestCase implements FuelingTotalView {

    private CountDownLatch mSignal;

    private List<FuelingRecord> mFuelingRecords;

    private FuelingTotalPresenter mFuelingTotalPresenter;

    private float mActualAverage;
    private float mActualCostSum;

    public FuelingTotalViewTest() {
        super();
        mFuelingRecords = null;
        mFuelingTotalPresenter = new FuelingTotalPresenter(this);
    }

    public void testOnFuelingRecordsChanged() throws InterruptedException {
        onFuelingRecordsChanged(null);
        assertEquals(0.0f, mActualAverage, 0.01f);
        assertEquals(0.0f, mActualCostSum, 0.01f);

        mFuelingRecords = new ArrayList<>();
        onFuelingRecordsChanged(mFuelingRecords);
        assertEquals(0.0f, mActualAverage, 0.01f);
        assertEquals(0.0f, mActualCostSum, 0.01f);

        // FuelingRecord(сумма, объём, пробег).
        // Сортировка по дате заправки в убывающем порядке.
        // Первая запись -- последняя заправка.

        mFuelingRecords.add(new FuelingRecord(100f, 0f, 200f));
        mFuelingRecords.add(new FuelingRecord(150f, 0f, 100f));
        mFuelingRecords.add(new FuelingRecord(250f, 0f, 000f));
        onFuelingRecordsChanged(mFuelingRecords);
        // Объёмы не указаны -- средний расход не считается.
        assertEquals(0.0f, mActualAverage, 0.01f);
        // Общая сумма указаных сумм.
        assertEquals(500.0f, mActualCostSum, 0.01f);

        mFuelingRecords.clear();
        mFuelingRecords.add(new FuelingRecord(0f, 10f, 200f));
        mFuelingRecords.add(new FuelingRecord(0f, 10f, 100f));
        mFuelingRecords.add(new FuelingRecord(0f, 10f, 000f));
        onFuelingRecordsChanged(mFuelingRecords);
        // Для всех записей указаны объём и пробег.
        // Средний расход ==
        // (сумма объёмов без учёта последнего объёма / (пробег в последней записи - пробег в первой записи)) * 100.
        assertEquals(10.0f, mActualAverage, 0.01f);
        // Суммы не указаны -- общая равна нулю.
        assertEquals(0.0f, mActualCostSum, 0.01f);

        mFuelingRecords.clear();
        mFuelingRecords.add(new FuelingRecord(333.3f, 33f, 333f));
        mFuelingRecords.add(new FuelingRecord(222.2f, 22f, 222f));
        mFuelingRecords.add(new FuelingRecord(111.1f, 11f, 111f));
        onFuelingRecordsChanged(mFuelingRecords);
        // ((22 + 11) / (333 - 111)) * 100
        assertEquals(14.86f, mActualAverage, 0.01f);
        // 333.3 + 222.2 + 111.1
        assertEquals(666.60f, mActualCostSum, 0.01f);
    }

    @Override
    public void setAverage(float average) {
        mActualAverage = average;
        mSignal.countDown();
    }

    @Override
    public void setCostSum(float costSum) {
        mActualCostSum = costSum;
        mSignal.countDown();
    }

    @Override
    public void onFuelingRecordsChanged(@Nullable List<FuelingRecord> fuelingRecords) {
        mSignal = new CountDownLatch(2);
        mFuelingTotalPresenter.onFuelingRecordsChanged(fuelingRecords);
        try {
            mSignal.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void destroy() {
        mFuelingTotalPresenter.onDestroy();
    }
}