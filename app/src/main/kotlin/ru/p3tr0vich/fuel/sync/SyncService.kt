package ru.p3tr0vich.fuel.sync

import android.app.Service
import android.content.Intent
import android.os.IBinder

class SyncService : Service() {

    override fun onCreate() {
        synchronized(syncAdapterLock) {
            if (syncAdapter == null) {
                syncAdapter = SyncAdapter(applicationContext)
            }
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return syncAdapter?.syncAdapterBinder
    }

    companion object {
        private var syncAdapter: SyncAdapter? = null

        private val syncAdapterLock = Any()
    }
}