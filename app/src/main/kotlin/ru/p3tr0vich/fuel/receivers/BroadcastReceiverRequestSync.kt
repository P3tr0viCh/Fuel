package ru.p3tr0vich.fuel.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import ru.p3tr0vich.fuel.BuildConfig
import ru.p3tr0vich.fuel.utils.UtilsLog

abstract class BroadcastReceiverRequestSync : BroadcastReceiverLocalBase() {
    override val action: String
        get() = ACTION

    override fun onReceive(context: Context, intent: Intent) {
        onReceive(intent.getIntExtra(EXTRA_RESULT, -1))
    }

    abstract fun onReceive(resultCode: Int)

    companion object {
        private const val ACTION = "${BuildConfig.APPLICATION_ID}.ACTION_REQUEST_SYNC"
        private const val EXTRA_RESULT = "${BuildConfig.APPLICATION_ID}.EXTRA_RESULT"

        fun send(context: Context, resultCode: Int) {
            send(context, Intent(ACTION).putExtra(EXTRA_RESULT, resultCode))
        }
    }
}