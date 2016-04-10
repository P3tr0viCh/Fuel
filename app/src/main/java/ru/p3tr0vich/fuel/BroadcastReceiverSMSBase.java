package ru.p3tr0vich.fuel;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.support.annotation.NonNull;
import android.telephony.SmsMessage;

import java.util.HashMap;
import java.util.Map;

abstract class BroadcastReceiverSMSBase extends BroadcastReceiver {

    public class Message {
        final int id;
        String message;

        Message(int id, String message) {
            this.id = id;
            this.message = message;
        }
    }

    protected abstract boolean isEnabled();

    protected abstract boolean isCheckAddress(String originatingAddress);

    @Override
    public final void onReceive(Context context, Intent intent) {

        if (!isEnabled()) return;

        if (intent == null) return;

        if (!Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) return;

        SmsMessage[] smsMessages = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            smsMessages = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        else {
            Bundle extras = intent.getExtras();

            if (extras != null) {
                Object[] pduArray = (Object[]) extras.get("pdus");

                if (pduArray != null) {
                    smsMessages = new SmsMessage[pduArray.length];

                    for (int i = 0; i < pduArray.length; i++) {
                        //noinspection deprecation
                        smsMessages[i] = SmsMessage.createFromPdu((byte[]) pduArray[i]);
                    }
                }
            }
        }

        if (smsMessages == null || smsMessages.length == 0) return;

        Map<String, Message> messages = new HashMap<>(smsMessages.length);

        for (SmsMessage smsMessage : smsMessages) {
            String originatingAddress = smsMessage.getOriginatingAddress();

            if (!isCheckAddress(originatingAddress)) continue;

            int id = smsMessage.hashCode();
            String messageBody = smsMessage.getMessageBody();

            if (messages.containsKey(originatingAddress)) {
                Message message = messages.get(originatingAddress);
                message.message += messageBody;

                messages.put(originatingAddress, message);
            } else
                messages.put(originatingAddress, new Message(id, messageBody));
        }

        if (!messages.isEmpty()) onReceive(context, messages);
    }

    protected abstract void onReceive(Context context, @NonNull Map<String, Message> messages);
}