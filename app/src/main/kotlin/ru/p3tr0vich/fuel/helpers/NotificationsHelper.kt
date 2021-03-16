package ru.p3tr0vich.fuel.helpers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.graphics.BitmapFactory
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

    fun showNotification(
        channelId: String, channelName: String,
        id: Int, title: String, text: String, contentIntent: PendingIntent
    ) {
        notificationManager?.createNotificationChannel(
            NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
        
        notificationManager?.notify(
            id,
            Notification.Builder(context, channelId)
                .setTicker(title)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(contentIntent)
                .setSmallIcon(R.drawable.ic_stat_maps_local_gas_station)
                .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
                .setAutoCancel(true)
                .build()
        )
    }
}