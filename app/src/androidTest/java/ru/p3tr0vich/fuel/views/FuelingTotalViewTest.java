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

    private FuelingTotalPresenter mFuelingTotalPresenter;

    private float mActualAverage;
    private float mActualCostSum;
    private float mActualLastConsumption;
    private float mActualEstimatedMileage;
    private float mActualEstimatedTotal;

    public FuelingTotalViewTest() {
        super();
        mFuelingTotalPresenter = new FuelingTotalPresenter(this);
    }

    public void testOnFuelingRecordsChanged() {
        onFuelingRecordsChanged(null);
        assertEquals(0.0f, mActualAverage, 0.01f);
        assertEquals(0.0f, mActualCostSum, 0.01f);

        List<FuelingRecord> records = new ArrayList<>();

        onFuelingRecordsChanged(records);
        assertEquals(0.0f, mActualAverage, 0.01f);
        assertEquals(0.0f, mActualCostSum, 0.01f);

        // FuelingRecord(сумма, объём, пробег).
        // Сортировка по дате заправки в убывающем порядке.
        // Первая запись -- последняя заправка.

        records.add(new FuelingRecord(100f, 0f, 200f));
        records.add(new FuelingRecord(150f, 0f, 100f));
        records.add(new FuelingRecord(250f, 0f, 000f));
        onFuelingRecordsChanged(records);
        // Объёмы не указаны -- средний расход не считается.
        assertEquals(0.0f, mActualAverage, 0.01f);
        // Общая сумма указаных сумм.
        assertEquals(500.0f, mActualCostSum, 0.01f);

        records.clear();
        records.add(new FuelingRecord(0f, 10f, 200f));
        records.add(new FuelingRecord(0f, 10f, 100f));
        records.add(new FuelingRecord(0f, 10f, 000f));
        onFuelingRecordsChanged(records);
        // Для всех записей указаны объём и пробег.
        // Средний расход ==
        // (сумма объёмов без учёта последнего объёма / (пробег в последней записи - пробег в первой записи)) * 100.
        assertEquals(10.0f, mActualAverage, 0.01f);
        // Суммы не указаны -- общая равна нулю.
        assertEquals(0.0f, mActualCostSum, 0.01f);

        records.clear();
        records.add(new FuelingRecord(333.3f, 33f, 333f));
        records.add(new FuelingRecord(222.2f, 22f, 222f));
        records.add(new FuelingRecord(111.1f, 11f, 111f));
        onFuelingRecordsChanged(records);
        // ((22 + 11) / (333 - 111)) * 100
        assertEquals(14.86f, mActualAverage, 0.01f);
        // 333.3 + 222.2 + 111.1
        assertEquals(666.60f, mActualCostSum, 0.01f);
    }

    public void testOnLastRecordsChanged() {
        onLastFuelingRecordsChanged(null);
        assertEquals(0.0f, mActualLastConsumption, 0.01f);
        assertEquals(0.0f, mActualEstimatedMileage, 0.01f);
        assertEquals(0.0f, mActualEstimatedTotal, 0.01f);

        List<FuelingRecord> records = new ArrayList<>();

        onLastFuelingRecordsChanged(records);
        assertEquals(0.0f, mActualLastConsumption, 0.01f);
        assertEquals(0.0f, mActualEstimatedMileage, 0.01f);
        assertEquals(0.0f, mActualEstimatedTotal, 0.01f);

        // FuelingRecord(сумма, объём, пробег).
        // Сортировка по дате заправки в убывающем порядке.
        // Первая запись -- последняя заправка.

        records.add(new FuelingRecord(0, 0f, 0f));
        records.add(new FuelingRecord(0, 0f, 0f));
        onLastFuelingRecordsChanged(records);
        // Указаны не все данные.
        assertEquals(0.0f, mActualLastConsumption, 0.01f);
        assertEquals(0.0f, mActualEstimatedMileage, 0.01f);
        assertEquals(0.0f, mActualEstimatedTotal, 0.01f);

        records.clear();
        records.add(new FuelingRecord(0, 20f, 300f));
        records.add(new FuelingRecord(0, 10f, 200f));
        onLastFuelingRecordsChanged(records);
        // Пройдено 100 у.е. расстояния (300 - 200) на 10 у.е. топлива.
        // Расход 10 у.е. топлива на 100 у.е. расстояния ((10 / (300 - 200)) * 100)
        assertEquals(10.0f, mActualLastConsumption, 0.01f);
        // Залито 20 у.е. Предполагаемый пробег 200.
        assertEquals(200.0f, mActualEstimatedMileage, 0.01f);
        // Общий пробег 500.
        assertEquals(500.0f, mActualEstimatedTotal, 0.01f);

        records.clear();
        records.add(new FuelingRecord(0, 33.3f, 234.5f));
        records.add(new FuelingRecord(0, 22.2f, 123.4f));
        onLastFuelingRecordsChanged(records);
        // Пройдено 111.1 у.е. расстояния (234.5 - 123.4) на 22.2 у.е. топлива.
        // Расход 19.982 у.е. топлива на 100 у.е. расстояния ((22.2 / 111.1) * 100)
        assertEquals(19.98f, mActualLastConsumption, 0.01f);
        // Предполагаемый пробег на 33.3 у.е. -- 166.65 у.е. ((100 / 19.982) * 33.3)
        assertEquals(166.65f, mActualEstimatedMileage, 0.01f);
        // Общий пробег 401.15 (234.5 + 166.65).
        assertEquals(401.15f, mActualEstimatedTotal, 0.01f);
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
    public void setLastConsumption(float lastConsumption) {
        mActualLastConsumption = lastConsumption;
        mSignal.countDown();
    }

    @Override
    public void setEstimatedMileage(float estimatedMileage) {
        mActualEstimatedMileage = estimatedMileage;
        mSignal.countDown();
    }

    @Override
    public void setEstimatedTotal(float estimatedTotal) {
        mActualEstimatedTotal = estimatedTotal;
        mSignal.countDown();
    }

    @Override
    public void onLastFuelingRecordsChanged(@Nullable List<FuelingRecord> fuelingRecords) {
        mSignal = new CountDownLatch(3);
        mFuelingTotalPresenter.onLastFuelingRecordsChanged(fuelingRecords);
        try {
            mSignal.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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