package ru.p3tr0vich.fuel.models

import android.os.Parcelable
import androidx.annotation.Size
import kotlinx.android.parcel.Parcelize
import ru.p3tr0vich.fuel.utils.UtilsDate
import java.util.*

@Parcelize
class ChartCostModel : Parcelable {

    companion object {
        const val NAME = "CHART_COST_MODEL"
    }

    var year: Int = 0

    var years: IntArray? = null
        set(years) {
            field = years
            if (years == null) {
                hasData = false
            }
        }

    @get:Size(12)
    var sums: FloatArray = FloatArray(12)
        set(@Size(12) value) {
            field = value

            median = calcMedian(sums)
            sum = calcSum(sums)

            hasData = true
        }

    fun clear() {
        for (i in 0..11) {
            this.sums[i] = 0f
        }

        median = 0f
        sum = 0f

        hasData = false
        isSumsNotEquals = false
    }

    var median: Float = 0f
        private set

    var sum: Float = 0f
        private set

    var hasData: Boolean = false
        private set

    var isSumsNotEquals: Boolean = false
        private set

    init {
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

    override fun toString(): String {
        return "hasData: " + hasData + ", years: " + years?.contentToString() +
                ", year: " + year + ", sums: " + sums.contentToString() +
                ", median: " + median + ", sum: " + sum + ", sumsNotEquals: " + isSumsNotEquals
    }
}
