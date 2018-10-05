package ru.p3tr0vich.fuel.observers

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler

/**
 * Базовый наблюдатель.
 *
 * @see DatabaseObserver
 *
 * @see PreferencesObserver
 */
open class ContentObserverBase private constructor(handler: Handler) : ContentObserver(handler) {

    internal constructor() : this(Handler()) {}

    /**
     * Регистрирует наблюдатель.
     *
     * @see android.content.ContentResolver.registerContentObserver
     */
    protected fun register(context: Context, uri: Uri, notifyForDescendents: Boolean) {
        context.contentResolver.registerContentObserver(uri, notifyForDescendents, this)
    }

    /**
     * Удаляет наблюдатель.
     *
     * @see android.content.ContentResolver.unregisterContentObserver
     */
    fun unregister(context: Context) {
        context.contentResolver.unregisterContentObserver(this)
    }

    override fun onChange(selfChange: Boolean) {
        onChange(selfChange, null)
    }
}