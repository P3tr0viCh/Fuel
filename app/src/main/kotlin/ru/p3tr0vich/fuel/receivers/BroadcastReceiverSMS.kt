package ru.p3tr0vich.fuel.receivers

import android.app.PendingIntent
import android.content.Context
import android.telephony.PhoneNumberUtils
import android.text.TextUtils
import ru.p3tr0vich.fuel.ApplicationFuel
import ru.p3tr0vich.fuel.BuildConfig
import ru.p3tr0vich.fuel.R
import ru.p3tr0vich.fuel.activities.ActivityFuelingRecordChange
import ru.p3tr0vich.fuel.helpers.NotificationsHelper
import ru.p3tr0vich.fuel.helpers.PreferencesHelper
import ru.p3tr0vich.fuel.helpers.SMSTextPatternHelper
import ru.p3tr0vich.fuel.models.FuelingRecord
import ru.p3tr0vich.fuel.utils.UtilsFormat


class BroadcastReceiverSMS : BroadcastReceiverSMSBase() {

    override var isEnabled: Boolean = false
        private set

    private var address: String = ""
    private var pattern: String = ""

    private var price: Float = 0f
    private var total: Float = 0f

    override fun isCheckAddress(originatingAddress: String?): Boolean {
        return PhoneNumberUtils.compare(originatingAddress, address)
    }

    public override fun onCreate(context: Context) {
        PreferencesHelper.getInstance(context).let {
            address = it.smsAddress
            pattern = it.smsTextPattern

            price = it.price
            total = it.lastTotal

            isEnabled =
                !(TextUtils.isEmpty(address) || TextUtils.isEmpty(pattern)) && it.isSMSEnabled
        }
    }

    public override fun onReceive(context: Context, messages: Map<String, Message>) {
        var message: Message
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
        val volume = if (price != 0f) cost / price else 0f

        val fuelingRecord = FuelingRecord(cost, volume, total)

        val contentIntent = PendingIntent.getActivity(
            context, id,
            ActivityFuelingRecordChange.getIntentForStart(context, fuelingRecord),
            PendingIntent.FLAG_UPDATE_CURRENT + PendingIntent.FLAG_IMMUTABLE
        )

        NotificationsHelper(context)
            .showNotification(
                CHANNEL_ID, context.getString(R.string.text_notification_sms_channel_name), id,
                context.getString(R.string.text_notification_sms_title),
                context.getString(
                    R.string.text_notification_sms_text,
                    UtilsFormat.floatToString(cost),
                    UtilsFormat.floatToString(volume)
                ),
                contentIntent
            )
    }

    companion object {
        private const val CHANNEL_ID = "${BuildConfig.APPLICATION_ID}.CHANNEL_ID"
    }
}