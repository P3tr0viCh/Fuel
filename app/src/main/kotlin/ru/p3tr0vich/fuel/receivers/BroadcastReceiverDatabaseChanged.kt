package ru.p3tr0vich.fuel.receivers

import android.content.Context
import android.content.Intent
import android.support.v4.content.LocalBroadcastManager
import ru.p3tr0vich.fuel.BuildConfig

abstract class BroadcastReceiverDatabaseChanged : BroadcastReceiverLocalBase() {

    override val action: String
        get() = ACTION

    override fun onReceive(context: Context, intent: Intent) {
        onReceive(intent.getLongExtra(EXTRA_ID, -1))
    }

    abstract fun onReceive(id: Long)

    companion object {
        private const val ACTION = "${BuildConfig.APPLICATION_ID}.ACTION_DATABASE_CHANGED"
        private const val EXTRA_ID = "${BuildConfig.APPLICATION_ID}.EXTRA_ID"

        @JvmStatic
        fun send(context: Context, id: Long) {
            LocalBroadcastManager.getInstance(context)
                    .sendBroadcast(Intent(ACTION).putExtra(EXTRA_ID, id))
        }
    }
}