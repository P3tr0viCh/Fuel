package ru.p3tr0vich.fuel

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context

import ru.p3tr0vich.fuel.utils.UtilsLog

class ApplicationFuel : Application() {

    override fun onCreate() {
        super.onCreate()

        sContext = applicationContext

        UtilsLog.d("ApplicationFuel", ">>> onCreate *************************************")

        ContentObserverService.start(this)
    }

    override fun onTerminate() {
        UtilsLog.d("ApplicationFuel", ">>> onTerminate **********************************")
        super.onTerminate()
    }

    companion object {

        @SuppressLint("StaticFieldLeak")
        private var sContext: Context? = null

        val context: Context
            get() {
                if (sContext == null) {
                    throw AssertionError("Application context is null")
                }

                return sContext!!
            }
    }
}