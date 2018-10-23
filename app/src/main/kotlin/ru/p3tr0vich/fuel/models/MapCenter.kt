package ru.p3tr0vich.fuel.models

import android.content.Intent

/**
 * Центр карты.
 * @property text Полное наименование географической точки.
 * @property latitude Широта.
 * @property longitude Долгота.
 */
class MapCenter() {
    var text = DEFAULT_MAP_CENTER_TEXT
    var latitude = DEFAULT_MAP_CENTER_LATITUDE
    var longitude = DEFAULT_MAP_CENTER_LONGITUDE

    var title = ""
    var subtitle = ""

    constructor(text: String, latitude: Double, longitude: Double) : this() {
        this.text = text
        this.latitude = latitude
        this.longitude = longitude
    }

    constructor(data: Intent?) : this() {
        if (data == null) return

        text = data.getStringExtra(EXTRA_MAP_CENTER_TEXT)
        latitude = data.getDoubleExtra(EXTRA_MAP_CENTER_LATITUDE, latitude)
        longitude = data.getDoubleExtra(EXTRA_MAP_CENTER_LONGITUDE, longitude)
    }

    companion object {
        /**
         * Центр карты по умолчанию.
         */
        const val DEFAULT_MAP_CENTER_TEXT = "Москва, Кремль"
        /**
         * Широта центра карты по умолчанию.
         */
        const val DEFAULT_MAP_CENTER_LATITUDE = 55.752023
        /**
         * Долгота центра карты по умолчанию.
         */
        const val DEFAULT_MAP_CENTER_LONGITUDE = 37.617499

        const val EXTRA_MAP_CENTER_TEXT = "EXTRA_MAP_CENTER_TEXT"
        const val EXTRA_MAP_CENTER_LATITUDE = "EXTRA_MAP_CENTER_LATITUDE"
        const val EXTRA_MAP_CENTER_LONGITUDE = "EXTRA_MAP_CENTER_LONGITUDE"
    }
}