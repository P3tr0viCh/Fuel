package ru.p3tr0vich.fuel.receivers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import android.telephony.PhoneNumberUtils
import android.text.TextUtils
import ru.p3tr0vich.fuel.BuildConfig
import ru.p3tr0vich.fuel.R
import ru.p3tr0vich.fuel.activities.ActivityFuelingRecordChange
import ru.p3tr0vich.fuel.helpers.PreferencesHelper
import ru.p3tr0vich.fuel.helpers.SMSTextPatternHelper
import ru.p3tr0vich.fuel.helpers.SystemServicesHelper
import ru.p3tr0vich.fuel.models.FuelingRecord
import ru.p3tr0vich.fuel.utils.UtilsFormat


class BroadcastReceiverSMS : BroadcastReceiverSMSBase() {

    override var isEnabled: Boolean = false
        private set

    private var address: String? = null

    private var pattern: String? = null

    private var preferencesHelper: PreferencesHelper? = null

    override fun isCheckAddress(originatingAddress: String?): Boolean {
        return PhoneNumberUtils.compare(originatingAddress, address)
    }

    public override fun onCreate(context: Context) {
        preferencesHelper = PreferencesHelper.getInstance(context)

        address = preferencesHelper!!.smsAddress
        pattern = preferencesHelper!!.smsTextPattern

        isEnabled = !(TextUtils.isEmpty(address) || TextUtils.isEmpty(pattern)) && preferencesHelper!!.isSMSEnabled
    }

    public override fun onReceive(context: Context, messages: Map<String, BroadcastReceiverSMSBase.Message>) {
        var message: BroadcastReceiverSMSBase.Message
        var cost: Float?

        for (entry in messages) {
            message = entry.value

            cost = try {
                SMSTextPatternHelper.getValue(pattern, message.message)
            } catch (e: Exception) {
                null
            }

            if (cost != null) {
                showNotification(context, message.id, cost)
            }
        }
    }

    private fun showNotification(context: Context, id: Int, cost: Float) {
        var volume = 0f

        val price = preferencesHelper!!.price

        if (price != 0f) {
            volume = cost / price
        }

        val fuelingRecord = FuelingRecord(cost, volume, preferencesHelper!!.lastTotal)

        val contentIntent = PendingIntent.getActivity(context, id,
                ActivityFuelingRecordChange.getIntentForStart(context, fuelingRecord),
                PendingIntent.FLAG_UPDATE_CURRENT)

        val title = context.getString(R.string.text_notification_sms_title)
        val text = context.getString(R.string.text_notification_sms_text,
                UtilsFormat.floatToString(cost),
                UtilsFormat.floatToString(volume))

        @Suppress("DEPRECATION")
        val builder = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
            -> {
                SystemServicesHelper.getNotificationManager(context)?.createNotificationChannel(
                        NotificationChannel(CHANNEL_ID, CHANNEL_NAME,
                                NotificationManager.IMPORTANCE_DEFAULT))

                Notification.Builder(context, CHANNEL_ID)
            }
            else -> {
                Notification.Builder(context)
            }
        }

        builder.apply {
            setTicker(title)
            setContentTitle(title)
            setContentText(text)
            setContentIntent(contentIntent)
            setSmallIcon(R.drawable.ic_stat_maps_local_gas_station)
            setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
            setAutoCancel(true)
        }

        SystemServicesHelper.getNotificationManager(context)?.notify(id, builder.build())
    }

    companion object {
        private const val CHANNEL_ID = "${BuildConfig.APPLICATION_ID}.CHANNEL_ID"
        private const val CHANNEL_NAME = "${BuildConfig.APPLICATION_ID}.CHANNEL_NAME"
    }
}