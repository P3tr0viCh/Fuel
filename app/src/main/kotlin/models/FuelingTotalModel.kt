package ru.p3tr0vich.fuel.models

import android.annotation.SuppressLint
import android.os.AsyncTask

/**
 * @property average Средний расход в заданном периоде.
 * @property costSum Сумма стоимости для заданного периода.
 * @property lastConsumption Расход перед последней записью.
 * @property estimatedMileage Предполагаемый пробег после последней заправки.
 * @property estimatedTotal Предполагаемый общий пробег после последней заправки.
 */
class FuelingTotalModel {
    var average: Float = 0f
        private set
    var costSum: Float = 0f
        private set
    var lastConsumption: Float = 0f
        private set
    var estimatedMileage: Float = 0f
        private set
    var estimatedTotal: Float = 0f
        private set

    var onChangeListener: OnChangeListener? = null

    interface OnChangeListener {
        fun onChange()
    }

    private var mCalcTotalTask: CalcTotalTask? = null

    fun destroy() {
        cancel()
    }

    /**
     * Вычисляет по содержимому двух последних записей в БД
     * последний расход и берёт из последней записи объём залитого топлива и общий пробег.
     * На основании этих данных вычисляет предполагаемый пробег
     * на объёме последней заправки и предполагемый общий пробег.
     *
     * @param fuelingRecords Последняя и предпоследняя записи в БД.
     */
    fun setLastRecords(fuelingRecords: List<FuelingRecord>?) {
        lastConsumption = 0f
        estimatedMileage = 0f
        estimatedTotal = 0f

        if (fuelingRecords == null || fuelingRecords.size < 2) return

        // Последняя запись
        val lastRecord = fuelingRecords[0]
        // Объём последней заправки.
        val lastVolume = lastRecord.volume
        // Общий пробег в последней записи.
        val lastTotal = lastRecord.total

        if (lastVolume <= 0 || lastTotal <= 0) return

        // Предпоследняя запись
        val penultimateRecord = fuelingRecords[1]

        val penultimateVolume = penultimateRecord.volume

        if (penultimateVolume <= 0) return

        val penultimateTotal = penultimateRecord.total

        // Единственное, что может равняться нулю.
        if (penultimateTotal < 0 || penultimateTotal >= lastTotal) return

        // ----
        // Все необходимые значения в записях указаны корректно.

        lastConsumption = penultimateVolume / (lastTotal - penultimateTotal) * 100

        // Пробег на одной единице объёма (литр, галлон).
        val distancePerOneFuel = 100.0f / lastConsumption

        estimatedMileage = distancePerOneFuel * lastVolume

        estimatedTotal = lastTotal + estimatedMileage
    }

    private fun cancel() {
        mCalcTotalTask?.cancel(false)
    }

    fun setFuelingRecords(fuelingRecords: List<FuelingRecord>?) {
        cancel()

        mCalcTotalTask = CalcTotalTask(fuelingRecords)
        mCalcTotalTask?.execute()
    }

    @SuppressLint("StaticFieldLeak")
    private inner class CalcTotalTask(private val mFuelingRecords: List<FuelingRecord>?) : AsyncTask<Void, Void, Array<Float>>() {

        override fun doInBackground(vararg params: Void): Array<Float>? {
            if (mFuelingRecords == null) return arrayOf(0f, 0f)

            var costSum = 0f
            var volumeSum = 0f
            var volume: Float
            var total: Float
            var firstTotal = 0f
            var lastTotal = 0f

            var fuelingRecord: FuelingRecord

            var completeData = true // Во всех записях указаны объём заправки и текущий пробег

            val size = mFuelingRecords.size

            for (i in 0 until size) {

                if (isCancelled) return null

                fuelingRecord = mFuelingRecords[i]

                costSum += fuelingRecord.cost

                if (completeData) {
                    volume = fuelingRecord.volume
                    total = fuelingRecord.total

                    if (volume == 0f || total == 0f) {
                        completeData = false
                    } else {
                        // Сортировка записей по дате в обратном порядке
                        // 0 -- последняя заправка
                        // Последний (i == 0) объём заправки не нужен -- неизвестно, сколько на ней будет пробег,
                        // в volumeSum не включается
                        if (i == 0) {
                            lastTotal = total
                        } else {
                            volumeSum += volume
                            if (i == size - 1) firstTotal = total
                        }
                    }
                }
            }

            var average: Float

            if (completeData) {
                average = if (volumeSum != 0f) volumeSum / (lastTotal - firstTotal) * 100 else 0f
            } else {
                average = 0f
                var averageCount = 0

                var i = 0
                val count = size - 1
                while (i < count) {

                    if (isCancelled) return null

                    lastTotal = mFuelingRecords[i].total

                    if (lastTotal != 0f) {
                        fuelingRecord = mFuelingRecords[i + 1]

                        volume = fuelingRecord.volume
                        total = fuelingRecord.total

                        if (volume != 0f && total != 0f) {
                            average += volume / (lastTotal - total) * 100
                            averageCount++
                        }
                    }

                    i++
                }

                average = if (averageCount != 0) average / averageCount else 0f
            }

            return arrayOf(average, costSum)
        }

        override fun onPostExecute(result: Array<Float>?) {
            if (result != null) {
                average = result[0]
                costSum = result[1]

                onChangeListener?.onChange()
            }
        }
    }
}