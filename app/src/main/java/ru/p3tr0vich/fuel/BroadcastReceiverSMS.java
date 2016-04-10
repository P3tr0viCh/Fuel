package ru.p3tr0vich.fuel;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.telephony.PhoneNumberUtils;

import java.util.Map;
import java.util.Random;

public class BroadcastReceiverSMS extends BroadcastReceiverSMSBase {

    private boolean mEnabled;

    private String mOriginatingAddress;

    @Override
    public boolean isEnabled() {
        return mEnabled;
    }

    @Override
    public boolean isCheckAddress(String originatingAddress) {
        return PhoneNumberUtils.compare(originatingAddress, mOriginatingAddress);
    }

    public BroadcastReceiverSMS() {
        mEnabled = PreferencesHelper.isSMSEnabled();
        mOriginatingAddress = PreferencesHelper.getSMSAddress();
    }

    @Override
    public void onReceive(Context context, @NonNull Map<String, Message> messages) {
        for (String address : messages.keySet()) {
            Message message = messages.get(address);

            showNotification(context, message.id, address, message.message);
        }
    }

    private void showNotification(Context context, int id, String address, String message) {
        // TODO:
        FuelingRecord fuelingRecord = new FuelingRecord(
                new Random().nextInt(1000),
                new Random().nextInt(100),
                PreferencesHelper.getLastTotal());

        PendingIntent contentIntent = PendingIntent.getActivity(context, id,
                ActivityFuelingRecordChange.getIntentForStart(context, fuelingRecord),
                PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder = new Notification.Builder(context)
                .setContentTitle(address)
                .setContentText(message)
                .setContentIntent(contentIntent)
                .setSmallIcon(R.drawable.ic_stat_maps_local_gas_station)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher))
                .setAutoCancel(true);

        SystemServicesHelper.getNotificationManager(context).notify(id, builder.build());
    }
}