package ru.p3tr0vich.fuel.models

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * @property id
 * Устанавливается во время добавления записи в БД равной mDateTime,
 * при изменении даты не меняется.
 * По умолчанию равна 0. Возможны проблемы, связанные с датой.
 * @property dateTime
 * Дата заправки в формате Unix epoch, временная зона UTC.
 * В базе хранится со сдвигом локальной зоны.
 * @property cost Стоимость.
 * @property volume Объём заправки.
 * @property total Общий пробег.
 */
@Parcelize
data class FuelingRecord constructor(var id: Long = 0,
                                     var dateTime: Long = 0,
                                     var cost: Float = 0f,
                                     var volume: Float = 0f,
                                     var total: Float = 0f) : Parcelable {

    companion object {
        const val NAME = "FUELING_RECORD"
    }

    constructor(cost: Float, volume: Float, total: Float) : this(0, System.currentTimeMillis(), cost, volume, total)

    internal constructor(fuelingRecord: FuelingRecord?) : this() {
        if (fuelingRecord != null) {
            id = fuelingRecord.id
            dateTime = fuelingRecord.dateTime
            cost = fuelingRecord.cost
            volume = fuelingRecord.volume
            total = fuelingRecord.total
        }
    }

    constructor(intent: Intent) : this(intent.getParcelableExtra<Parcelable>(NAME) as FuelingRecord)

    constructor(bundle: Bundle) : this(bundle.getParcelable<Parcelable>(NAME) as FuelingRecord)

    fun toIntent(intent: Intent): Intent {
        intent.putExtra(NAME, this)
        return intent
    }

    private fun toBundle(bundle: Bundle): Bundle {
        bundle.putParcelable(NAME, this)
        return bundle
    }

    fun toBundle(): Bundle {
        return toBundle(Bundle())
    }
}