package ru.p3tr0vich.fuel.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsMessage
import java.util.*

abstract class BroadcastReceiverSMSBase : BroadcastReceiver() {

    protected abstract val isEnabled: Boolean

    inner class Message(val id: Int, var message: String)

    protected abstract fun onCreate(context: Context)

    protected abstract fun isCheckAddress(originatingAddress: String?): Boolean

    override fun onReceive(context: Context, intent: Intent?) {

        onCreate(context)

        if (!isEnabled) {
            return
        }

        if (intent == null) {
            return
        }

        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION != intent.action) {
            return
        }

        var smsMessages: Array<SmsMessage?>? = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            smsMessages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        } else {
            val pduArray = intent.extras?.get("pdus") as Array<*>?

            if (pduArray != null) {
                smsMessages = arrayOfNulls(pduArray.size)

                for (i in pduArray.indices) {
                    @Suppress("DEPRECATION")
                    smsMessages[i] = SmsMessage.createFromPdu(pduArray[i] as ByteArray)
                }
            }
        }

        if (smsMessages == null || smsMessages.isEmpty()) return

        val messages = HashMap<String, Message>()

        var originatingAddress: String?
        var id: Int
        var messageBody: String
        var message: Message?

        for (smsMessage in smsMessages) {
            if (smsMessage == null || smsMessage.isEmail) continue

            originatingAddress = smsMessage.originatingAddress

            if (!isCheckAddress(originatingAddress)) {
                continue
            }

            id = smsMessage.hashCode()
            messageBody = smsMessage.messageBody

            message = messages[originatingAddress]

            if (message != null) {
                message.message += messageBody
            } else {
                message = Message(id, messageBody)
            }

            messages[originatingAddress] = message
        }

        if (!messages.isEmpty()) {
            onReceive(context, messages)
        }
    }

    protected abstract fun onReceive(context: Context, messages: Map<String, Message>)
}