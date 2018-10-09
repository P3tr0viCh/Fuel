package ru.p3tr0vich.fuel.receivers

import android.content.Context
import android.content.Intent
import android.support.v4.content.LocalBroadcastManager
import ru.p3tr0vich.fuel.BuildConfig

abstract class BroadcastReceiverLoading : BroadcastReceiverLocalBase() {

    override val action: String
        get() = ACTION

    override fun onReceive(context: Context, intent: Intent) {
        onReceive(intent.getBooleanExtra(EXTRA_LOADING, false))
    }

    abstract fun onReceive(loading: Boolean)

    companion object {
        private const val ACTION = "${BuildConfig.APPLICATION_ID}.ACTION_LOADING"
        private const val EXTRA_LOADING = "${BuildConfig.APPLICATION_ID}.EXTRA_LOADING"

        @JvmStatic
        fun send(context: Context, loading: Boolean) {
            LocalBroadcastManager.getInstance(context)
                    .sendBroadcast(Intent(ACTION).putExtra(EXTRA_LOADING, loading))
        }
    }
}