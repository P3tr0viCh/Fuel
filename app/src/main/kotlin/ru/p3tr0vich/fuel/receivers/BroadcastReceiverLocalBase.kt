package ru.p3tr0vich.fuel.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.support.v4.content.LocalBroadcastManager

abstract class BroadcastReceiverLocalBase : BroadcastReceiver() {

    protected abstract val action: String

    fun register(context: Context) {
        LocalBroadcastManager.getInstance(context).registerReceiver(this, IntentFilter(action))
    }

    fun unregister(context: Context) {
        LocalBroadcastManager.getInstance(context).unregisterReceiver(this)
    }
}