package ru.p3tr0vich.fuel;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;

import java.util.Map;

public class BroadcastReceiverSMS extends BroadcastReceiverSMSBase {

    private final boolean mEnabled;

    private final String mAddress;

    private String mPattern;

    @Override
    public boolean isEnabled() {
        return mEnabled;
    }

    @Override
    public boolean isCheckAddress(String originatingAddress) {
        return PhoneNumberUtils.compare(originatingAddress, mAddress);
    }

    public BroadcastReceiverSMS() {
        mAddress = PreferencesHelper.getSMSAddress();
        mPattern = PreferencesHelper.getSMSTextPattern();

        UtilsLog.d(this, "BroadcastReceiverSMS", "mPattern == " + mPattern);

        mEnabled = !(TextUtils.isEmpty(mAddress) || TextUtils.isEmpty(mPattern)) &&
                PreferencesHelper.isSMSEnabled();

        if (mEnabled) {
            mPattern = mPattern.replaceAll("[\\s]+", "-");

            mPattern = mPattern.replaceAll("[/][@]", "\056");
//            mPattern = mPattern.replaceAll("[@]", "([-]?\\d*[.,]?\\d+)");
//            mPattern = mPattern.replaceAll("[\0]", "@");
        }
    }

    @Nullable
    private Float getCostFromMessage(String message) {
        UtilsLog.d(this, "getCostFromMessage", "pattern == " + mPattern);
        UtilsLog.d(this, "getCostFromMessage", "message == " + message);

        try {
            return Float.valueOf(message);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public void onReceive(Context context, @NonNull Map<String, Message> messages) {
        Message message;
        Float cost;

        for (Map.Entry<String, Message> entry : messages.entrySet()) {
            message = entry.getValue();

            cost = getCostFromMessage(message.message);

            if (cost != null)
                showNotification(context, message.id, cost);
        }
    }

    private void showNotification(Context context, int id, float cost) {
        final float volume;

        final float defaultCost = PreferencesHelper.getDefaultCost();
        final float defaultVolume = PreferencesHelper.getDefaultVolume();

        if (cost != 0 && defaultCost != 0 && defaultVolume != 0) {
            if (cost == defaultCost)
                volume = defaultVolume;
            else
                volume = cost / (defaultCost / defaultVolume);
        } else
            volume = 0;

        FuelingRecord fuelingRecord = new FuelingRecord(cost, volume,
                PreferencesHelper.getLastTotal());

        PendingIntent contentIntent = PendingIntent.getActivity(context, id,
                ActivityFuelingRecordChange.getIntentForStart(context, fuelingRecord),
                PendingIntent.FLAG_UPDATE_CURRENT);

        String title = context.getString(R.string.text_sms_title);
        String text = context.getString(R.string.text_sms_text,
                UtilsFormat.floatToString(cost),
                UtilsFormat.floatToString(volume));

        Notification.Builder builder = new Notification.Builder(context)
                .setTicker(title)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(contentIntent)
                .setSmallIcon(R.drawable.ic_stat_maps_local_gas_station)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher))
                .setAutoCancel(true);

        SystemServicesHelper.getNotificationManager(context).notify(id, builder.build());
    }
}