package ru.p3tr0vich.fuel;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class FuelingRecord implements Parcelable {

    public static final String NAME = "FUELING_RECORD";

    // Устанавливается во время добавления записи в БД равной mDateTime,
    // при изменении даты не меняется.
    // По умолчанию равна 0. Возможны проблемы, связанные с датой.
    private long mId;
    // Дата заправки в формате Unix epoch, временная зона UTC.
    // В базе хранится со сдвигом локальной зоны.
    private long mDateTime;
    // Стоимость.
    private float mCost;
    // Объём заправки.
    private float mVolume;
    // Общий пробег.
    private float mTotal;

    FuelingRecord(long id, long dateTime, float cost, float volume, float total) {
        setId(id);
        setDateTime(dateTime);
        setCost(cost);
        setVolume(volume);
        setTotal(total);
    }

    private FuelingRecord(Parcel in) {
        this(in.readLong(), in.readLong(), in.readFloat(), in.readFloat(), in.readFloat());
    }

    FuelingRecord() {
        this(0, 0, 0, 0, 0);
    }

    FuelingRecord(float cost, float volume, float total) {
        this(0, System.currentTimeMillis(), cost, volume, total);
    }

    FuelingRecord(@Nullable FuelingRecord fuelingRecord) {
        this();

        if (fuelingRecord != null) {
            mId = fuelingRecord.mId;
            mDateTime = fuelingRecord.mDateTime;
            mCost = fuelingRecord.mCost;
            mVolume = fuelingRecord.mVolume;
            mTotal = fuelingRecord.mTotal;
        }
    }

    FuelingRecord(Intent intent) {
        this((FuelingRecord) intent.getParcelableExtra(NAME));
    }

    FuelingRecord(@NonNull Bundle bundle) {
        this((FuelingRecord) bundle.getParcelable(NAME));
    }

    public Intent toIntent(@NonNull Intent intent) {
        intent.putExtra(NAME, this);
        return intent;
    }

    public Intent toIntent() {
        return this.toIntent(new Intent());
    }

    public Bundle toBundle(@NonNull Bundle bundle) {
        bundle.putParcelable(NAME, this);
        return bundle;
    }

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