package ru.p3tr0vich.fuel.helpers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import ru.p3tr0vich.fuel.R

class NotificationsHelper(private val context: Context) {

    companion object {
        const val TAG = "NotificationsHelper"

        private var notificationManager: NotificationManager? = null
    }

    init {
        if (notificationManager == null) {
            notificationManager = SystemServicesHelper.getNotificationManager(context)
        }
    }

    private fun createChannel(id: String, name: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        notificationManager?.createNotificationChannel(NotificationChannel(id, name, NotificationManager.IMPORTANCE_DEFAULT))
    }


    fun showNotification(channelId: String, channelName: String,
                         id: Int, title: String, text: String, contentIntent: PendingIntent) {
        createChannel(channelId, channelName)

        val builder = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                Notification.Builder(context, channelId)
            }
            else -> {
                @Suppress("DEPRECATION")
                Notification.Builder(context)
            }
        }

        builder
                .setTicker(title)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(contentIntent)
                .setSmallIcon(R.drawable.ic_stat_maps_local_gas_station)
                .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
                .setAutoCancel(true)

        notificationManager?.notify(id, builder.build())
    }
}