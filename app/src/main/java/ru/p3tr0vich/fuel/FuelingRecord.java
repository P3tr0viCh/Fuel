package ru.p3tr0vich.fuel;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

public class FuelingRecord implements Parcelable {

    private static final String NAME = "FUELING_RECORD";

    private long mId;
    private long mDateTime;     // Дата заправки в миллисекундах
    private float mCost;        // Стоимость
    private float mVolume;      // Объём заправки
    private float mTotal;       // Общий пробег

    public final boolean showYear;

    FuelingRecord(long id, long dateTime, float cost, float volume, float total, boolean showYear) {
        setId(id);
        setDateTime(dateTime);
        setCost(cost);
        setVolume(volume);
        setTotal(total);
        this.showYear = showYear;
    }

    FuelingRecord(Cursor cursor) {
        this(cursor, true);
    }

    FuelingRecord(Cursor cursor, boolean showYear) {
        this(cursor.getLong(DatabaseHelper.Fueling._ID_INDEX),
                cursor.getLong(DatabaseHelper.Fueling.DATETIME_INDEX),
                cursor.getFloat(DatabaseHelper.Fueling.COST_INDEX),
                cursor.getFloat(DatabaseHelper.Fueling.VOLUME_INDEX),
                cursor.getFloat(DatabaseHelper.Fueling.TOTAL_INDEX),
                showYear);
    }

    private FuelingRecord(Parcel in) {
        this(in.readLong(), in.readLong(), in.readFloat(), in.readFloat(), in.readFloat(), in.readInt() == 1);
    }

    FuelingRecord() {
        this(0, Long.MIN_VALUE, 0, 0, 0, true);
    }

    FuelingRecord(FuelingRecord fuelingRecord) {
        this(fuelingRecord.mId, fuelingRecord.mDateTime, fuelingRecord.mCost,
                fuelingRecord.mVolume, fuelingRecord.mTotal, fuelingRecord.showYear);
    }

    FuelingRecord(Intent intent) {
        this((FuelingRecord) intent.getParcelableExtra(NAME));
    }

    @SuppressWarnings("unused")
    FuelingRecord(Bundle bundle) {
        this((FuelingRecord) bundle.getParcelable(NAME));
    }

    public Intent toIntent(@NonNull Intent intent) {
        intent.putExtra(NAME, this);
        return intent;
    }

    public Intent toIntent() {
        return this.toIntent(new Intent());
    }

    @SuppressWarnings("WeakerAccess")
    public Bundle toBundle(@NonNull Bundle bundle) {
        bundle.putParcelable(NAME, this);
        return bundle;
    }

    @SuppressWarnings("unused")
    public Bundle toBundle() {
        return this.toBundle(new Bundle());
    }

    public long getId() {
        return mId;
    }

    private void setId(long id) {
        mId = id;
    }

    public long getDateTime() {
        return mDateTime;
    }

    public void setDateTime(long dateTime) {
        mDateTime = dateTime;
    }

    public float getCost() {
        return mCost;
    }

    public void setCost(float cost) {
        mCost = cost;
    }

    public float getVolume() {
        return mVolume;
    }

    public void setVolume(float volume) {
        mVolume = volume;
    }

    public float getTotal() {
        return mTotal;
    }

    public void setTotal(float total) {
        mTotal = total;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mId);
        dest.writeLong(mDateTime);
        dest.writeFloat(mCost);
        dest.writeFloat(mVolume);
        dest.writeFloat(mTotal);
        dest.writeInt(showYear ? 1 : 0);
    }

    public static final Parcelable.Creator<FuelingRecord> CREATOR = new Parcelable.Creator<FuelingRecord>() {

        @Override
        public FuelingRecord createFromParcel(Parcel in) {
            return new FuelingRecord(in);
        }

        @Override
        public FuelingRecord[] newArray(int size) {
            return new FuelingRecord[size];
        }
    };

    @Override
    public String toString() {
        return mId + ", " + mDateTime + ", " + mCost + ", " + mVolume + ", " + mTotal;
    }
}
