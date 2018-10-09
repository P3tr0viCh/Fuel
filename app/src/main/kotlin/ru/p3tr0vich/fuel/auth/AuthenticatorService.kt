package ru.p3tr0vich.fuel.auth

import android.app.Service
import android.content.Intent
import android.os.IBinder

class AuthenticatorService : Service() {

    private var authenticator: Authenticator? = null

    override fun onCreate() {
        super.onCreate()
        authenticator = Authenticator(applicationContext)
    }

    override fun onBind(intent: Intent): IBinder? {
        return authenticator!!.iBinder
    }
}