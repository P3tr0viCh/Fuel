package ru.p3tr0vich.fuel.models

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class FuelingRecord @JvmOverloads constructor(val _id: Long = 0, private val _dateTime: Long = 0,
                                              private val _cost: Float = 0f,
                                              private val _volume: Float = 0f,
                                              private val _total: Float = 0f) : Parcelable {

    companion object {
        const val NAME = "FUELING_RECORD"
    }

    // Устанавливается во время добавления записи в БД равной mDateTime,
    // при изменении даты не меняется.
    // По умолчанию равна 0. Возможны проблемы, связанные с датой.
    var id: Long = 0
        private set
    // Дата заправки в формате Unix epoch, временная зона UTC.
    // В базе хранится со сдвигом локальной зоны.
    var dateTime: Long = 0
    // Стоимость.
    var cost: Float = 0f
    // Объём заправки.
    var volume: Float = 0f
    // Общий пробег.
    var total: Float = 0f

    init {
        id = _id
        dateTime = _dateTime
        cost = _cost
        volume = _volume
        total = _total
    }

    constructor(cost: Float, volume: Float, total: Float) : this(0, System.currentTimeMillis(), cost, volume, total) {}

    internal constructor(fuelingRecord: FuelingRecord?) : this() {

        if (fuelingRecord != null) {
            id = fuelingRecord.id
            dateTime = fuelingRecord.dateTime
            cost = fuelingRecord.cost
            volume = fuelingRecord.volume
            total = fuelingRecord.total
        }
    }

    constructor(intent: Intent) : this(intent.getParcelableExtra<Parcelable>(NAME) as FuelingRecord) {}

    constructor(bundle: Bundle) : this(bundle.getParcelable<Parcelable>(NAME) as FuelingRecord) {}

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

    override fun toString(): String {
        return id.toString() + ", " + dateTime + ", " + cost + ", " + volume + ", " + total
    }
}