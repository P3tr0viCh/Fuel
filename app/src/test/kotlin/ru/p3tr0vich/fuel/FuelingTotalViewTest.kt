package ru.p3tr0vich.fuel

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.p3tr0vich.fuel.models.FuelingRecord
import ru.p3tr0vich.fuel.presenters.FuelingTotalPresenter
import ru.p3tr0vich.fuel.views.FuelingTotalView
import java.util.*
import java.util.concurrent.CountDownLatch

class FuelingTotalViewTest : FuelingTotalView {

    private var signal: CountDownLatch? = null

    private val fuelingTotalPresenter = FuelingTotalPresenter(this)

    private var testId = 0

    private var expectedAverage = 0f
    private var expectedCostSum = 0f
    private var expectedLastConsumption = 0f
    private var expectedEstimatedMileage = 0f
    private var expectedEstimatedTotal = 0f

    @Test
    fun testOnFuelingRecordsChanged() {
        testId = 100
        expectedAverage = 0f
        expectedCostSum = 0f
        onFuelingRecordsChanged(null)
//        assertEquals(0.0f, expectedAverage, 0.01f)
//        assertEquals(0.0f, expectedCostSum, 0.01f)

        val records = ArrayList<FuelingRecord>()

        testId = 101
        expectedAverage = 0f
        expectedCostSum = 10f
        onFuelingRecordsChanged(records)
//        assertEquals(0.0f, expectedAverage, 0.01f)
//        assertEquals(0.0f, expectedCostSum, 0.01f)

        // FuelingRecord(сумма, объём, пробег).
        // Сортировка по дате заправки в убывающем порядке.
        // Первая запись -- последняя заправка.

        records.add(FuelingRecord(100f, 0f, 200f))
        records.add(FuelingRecord(150f, 0f, 100f))
        records.add(FuelingRecord(250f, 0f, 000f))
        testId = 102
        // Объёмы не указаны -- средний расход не считается.
        expectedAverage = 0f
        expectedCostSum = 0f
        onFuelingRecordsChanged(records)
//        assertEquals(0.0f, expectedAverage, 0.01f)
        // Общая сумма указаных сумм.
//        assertEquals(500.0f, expectedCostSum, 0.01f)

        records.clear()
        records.add(FuelingRecord(0f, 10f, 200f))
        records.add(FuelingRecord(0f, 10f, 100f))
        records.add(FuelingRecord(0f, 10f, 000f))
        testId = 103
        // Для всех записей указаны объём и пробег.
        expectedAverage = 0f
        expectedCostSum = 0f
        onFuelingRecordsChanged(records)
        // Средний расход ==
        // (сумма объёмов без учёта последнего объёма / (пробег в последней записи - пробег в первой записи)) * 100.
//        assertEquals(10.0f, expectedAverage, 0.01f)
        // Суммы не указаны -- общая равна нулю.
//        assertEquals(0.0f, expectedCostSum, 0.01f)

        records.clear()
        records.add(FuelingRecord(333.3f, 33f, 333f))
        records.add(FuelingRecord(222.2f, 22f, 222f))
        records.add(FuelingRecord(111.1f, 11f, 111f))
        testId = 104
        expectedAverage = 0f
        expectedCostSum = 0f
        onFuelingRecordsChanged(records)
        // ((22 + 11) / (333 - 111)) * 100
//        assertEquals(14.86f, expectedAverage, 0.01f)
        // 333.3 + 222.2 + 111.1
//        assertEquals(666.60f, expectedCostSum, 0.01f)
    }

    @Test
    fun testOnLastRecordsChanged() {
        testId = 200
        expectedLastConsumption = 0f
        expectedEstimatedMileage = 0f
        expectedEstimatedTotal = 0f
        onLastFuelingRecordsChanged(null)

        val records = ArrayList<FuelingRecord>()

        testId = 201
        expectedLastConsumption = 0f
        expectedEstimatedMileage = 0f
        expectedEstimatedTotal = 0f
        onLastFuelingRecordsChanged(records)

        // FuelingRecord(сумма, объём, пробег).
        // Сортировка по дате заправки в убывающем порядке.
        // Первая запись -- последняя заправка.

        records.add(FuelingRecord(0f, 0f, 0f))
        records.add(FuelingRecord(0f, 0f, 0f))
        // Указаны не все данные.
        testId = 202
        expectedLastConsumption = 0f
        expectedEstimatedMileage = 0f
        expectedEstimatedTotal = 0f
        onLastFuelingRecordsChanged(records)

        records.clear()
        records.add(FuelingRecord(0f, 20f, 300f))
        records.add(FuelingRecord(0f, 10f, 200f))
        testId = 203
        // Пройдено 100 у.е. расстояния (300 - 200) на 10 у.е. топлива.
        // Расход 10 у.е. топлива на 100 у.е. расстояния ((10 / (300 - 200)) * 100)
        expectedLastConsumption = 10.0f
        // Залито 20 у.е. Предполагаемый пробег 200.
        expectedEstimatedMileage = 200.0f
        // Общий пробег 500.
        expectedEstimatedTotal = 500.0f
        onLastFuelingRecordsChanged(records)

        records.clear()
        records.add(FuelingRecord(0f, 33.3f, 234.5f))
        records.add(FuelingRecord(0f, 22.2f, 123.4f))
        testId = 204
        // Пройдено 111.1 у.е. расстояния (234.5 - 123.4) на 22.2 у.е. топлива.
        // Расход 19.982 у.е. топлива на 100 у.е. расстояния ((22.2 / 111.1) * 100)
        expectedLastConsumption = 19.98f
        // Предполагаемый пробег на 33.3 у.е. -- 166.65 у.е. ((100 / 19.982) * 33.3)
        expectedEstimatedMileage = 166.65f
        // Общий пробег 401.15 (234.5 + 166.65).
        expectedEstimatedTotal = 401.15f
        onLastFuelingRecordsChanged(records)
    }

    override fun setAverage(value: Float) {
        assertEquals("$testId: expectedAverage", expectedAverage, value, 0.01f)
        signal?.countDown()
    }

    override fun setCostSum(value: Float) {
        assertEquals("$testId: expectedCostSum", expectedCostSum, value, 0.01f)
        signal?.countDown()
    }

    override fun setLastConsumption(value: Float) {
        assertEquals("$testId: expectedLastConsumption", expectedLastConsumption, value, 0.01f)
    }

    override fun setEstimatedMileage(value: Float) {
        assertEquals("$testId: expectedEstimatedMileage", expectedEstimatedMileage, value, 0.01f)
    }

    override fun setEstimatedTotal(value: Float) {
        assertEquals("$testId: expectedEstimatedTotal", expectedEstimatedTotal, value, 0.01f)
    }

    override fun onLastFuelingRecordsChanged(fuelingRecords: List<FuelingRecord>?) {
        fuelingTotalPresenter.onLastFuelingRecordsChanged(fuelingRecords)
    }

    override fun onFuelingRecordsChanged(fuelingRecords: List<FuelingRecord>?) {
        signal = CountDownLatch(2)
        fuelingTotalPresenter.onFuelingRecordsChanged(fuelingRecords)
        try {
            signal?.await()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    override fun destroy() {
        fuelingTotalPresenter.onDestroy()
    }
}