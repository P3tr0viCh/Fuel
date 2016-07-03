package ru.p3tr0vich.fuel;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;

import java.util.Map;

import ru.p3tr0vich.fuel.helpers.PreferencesHelper;
import ru.p3tr0vich.fuel.helpers.SMSTextPatternHelper;
import ru.p3tr0vich.fuel.helpers.SystemServicesHelper;
import ru.p3tr0vich.fuel.models.FuelingRecord;
import ru.p3tr0vich.fuel.utils.UtilsFormat;

public class BroadcastReceiverSMS extends BroadcastReceiverSMSBase {

    private boolean mEnabled;

    private String mAddress;

    private String mPattern;

    private PreferencesHelper mPreferencesHelper;

    @Override
    public boolean isEnabled() {
        return mEnabled;
    }

    @Override
    public boolean isCheckAddress(String originatingAddress) {
        return PhoneNumberUtils.compare(originatingAddress, mAddress);
    }

    @Override
    public void onCreate(Context context) {
        mPreferencesHelper = PreferencesHelper.getInstance(context);

        mAddress = mPreferencesHelper.getSMSAddress();
        mPattern = mPreferencesHelper.getSMSTextPattern();

        mEnabled = !(TextUtils.isEmpty(mAddress) || TextUtils.isEmpty(mPattern)) &&
                mPreferencesHelper.isSMSEnabled();
    }

    @Override
    public void onReceive(Context context, @NonNull Map<String, Message> messages) {
        Message message;
        Float cost;

        for (Map.Entry<String, Message> entry : messages.entrySet()) {
            message = entry.getValue();

            try {
                cost = SMSTextPatternHelper.getValue(mPattern, message.message);
            } catch (Exception e) {
                cost = null;
            }

            if (cost != null)
                showNotification(context, message.id, cost);
        }
    }

    private void showNotification(Context context, int id, float cost) {
        float volume = 0;

        float price = mPreferencesHelper.getPrice();

        if (price != 0) volume = cost / price;

        FuelingRecord fuelingRecord = new FuelingRecord(cost, volume, mPreferencesHelper.getLastTotal());

        PendingIntent contentIntent = PendingIntent.getActivity(context, id,
                ActivityFuelingRecordChange.getIntentForStart(context, fuelingRecord),
                PendingIntent.FLAG_UPDATE_CURRENT);

        String title = context.getString(R.string.text_notification_sms_title);
        String text = context.getString(R.string.text_notification_sms_text,
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