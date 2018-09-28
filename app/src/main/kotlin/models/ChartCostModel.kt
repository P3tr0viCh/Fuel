package ru.p3tr0vich.fuel.models

import android.os.Parcel
import android.os.Parcelable
import android.support.annotation.Size
import kotlinx.android.parcel.Parcelize
import ru.p3tr0vich.fuel.utils.UtilsDate
import java.util.*

@Parcelize
class ChartCostModel() : Parcelable {

    companion object {
        const val NAME = "CHART_COST_MODEL"
    }

    var year: Int = 0

    var years: IntArray? = null
        set(years) {
            field = years
            if (years == null) {
                mHasData = false
            }
        }

    @get:Size(12)
    var sums: FloatArray? = FloatArray(12)
        set(@Size(12) sums) = if (sums == null) {
            for (i in 0..11) {
                this.sums?.set(i, 0f)
            }

            median = 0f
            sum = 0f
            mHasData = false
            isSumsNotEquals = false
        } else {
            field = sums
            median = calcMedian(this.sums!!)
            sum = calcSum(this.sums!!)
            mHasData = true
        }

    private var mHasData: Boolean = false

    var median: Float = 0.toFloat()
        private set

    var sum: Float = 0.toFloat()
        private set

    var isSumsNotEquals: Boolean = false
        private set

    constructor(parcel: Parcel) : this() {
        year = parcel.readInt()
        mHasData = parcel.readByte() != 0.toByte()
    }

    init {
        years = null
        sums = null

        year = UtilsDate.currentYear
    }

    private fun median(values: FloatArray): Float {
        val middle = values.size / 2
        return if (values.size % 2 == 1) values[middle] else (values[middle - 1] + values[middle]) / 2f
    }

    private fun calcMedian(sums: FloatArray): Float {
        var aboveZeroCount = 0
        for (value in sums) {
            if (value > 0) {
                aboveZeroCount++
            }
        }

        if (aboveZeroCount > 1) {
            val sortedSums = FloatArray(aboveZeroCount)

            var i = 0
            for (value in sums)
                if (value > 0) {
                    sortedSums[i] = value
                    i++
                }

            isSumsNotEquals = false
            val value = sortedSums[0]
            for (j in 1 until sortedSums.size)
                if (sortedSums[j] != value) {
                    isSumsNotEquals = true
                    break
                }

            Arrays.sort(sortedSums)

            return median(sortedSums)
        } else
            return 0f
    }

    private fun calcSum(sums: FloatArray): Float {
        var sum = 0f
        for (value in sums) sum += value
        return sum
    }

    fun hasData(): Boolean {
        return mHasData
    }

    override fun toString(): String {
        return "hasData: " + mHasData + ", years: " + Arrays.toString(years) +
                ", year: " + year + ", sums: " + Arrays.toString(sums) +
                ", median: " + median + ", sum: " + sum + ", sumsNotEquals: " + isSumsNotEquals
    }
}