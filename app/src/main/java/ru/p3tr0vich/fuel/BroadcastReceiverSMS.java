package ru.p3tr0vich.fuel;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class BroadcastReceiverSMS extends BroadcastReceiver {

    private class Message {
        int id;
        String message;

        Message(int id, String message) {
            this.id = id;
            this.message = message;
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) {

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

            if (smsMessages != null) {
                Map<String, Message> messages = new HashMap<>(smsMessages.length);

                for (SmsMessage smsMessage : smsMessages) {
                    String originatingAddress = smsMessage.getOriginatingAddress();
                    int id = smsMessage.hashCode();
                    String messageBody = smsMessage.getMessageBody();

                    if (messages.containsKey(originatingAddress)) {
                        Message message = messages.get(originatingAddress);
                        message.message += messageBody;

                        messages.put(originatingAddress, message);
                    } else
                        messages.put(originatingAddress, new Message(id, messageBody));
                }

                for (String address : messages.keySet()) {
                    Message message = messages.get(address);

                    showNotification(context, message.id, address, message.message);
                }
            }
        }
    }

    private void showNotification(Context context, int id, String address, String message) {
        FuelingRecord fuelingRecord = new FuelingRecord(
                new Random().nextInt(1000),
                new Random().nextInt(100),
                PreferenceManagerFuel.getLastTotal());

        PendingIntent contentIntent = PendingIntent.getActivity(context, id,
                ActivityFuelingRecordChange.getIntent(context, fuelingRecord)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                                Intent.FLAG_ACTIVITY_SINGLE_TOP |
                                Intent.FLAG_ACTIVITY_NO_HISTORY),
                PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder = new Notification.Builder(context)
                .setContentTitle(address)
                .setContentText(message)
                .setContentIntent(contentIntent)
                .setSmallIcon(R.drawable.ic_stat_maps_local_gas_station)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher))
                .setAutoCancel(true);

        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE))
                .notify(id, builder.build());
    }
}