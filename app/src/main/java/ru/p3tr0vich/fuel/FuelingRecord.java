package ru.p3tr0vich.fuel;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

public class FuelingRecord implements Parcelable {

    private static final String NAME = "FUELING_RECORD_NAME";

    private long mId;
    private String mSqlLiteDate;    // Дата заправки
    private float mCost;            // Стоимость
    private float mVolume;          // Объём заправки
    private float mTotal;           // Общий пробег

    FuelingRecord(long id, String sqlLiteDate, float cost, float volume, float total) {
        setId(id);
        setSQLiteDate(sqlLiteDate);
        setCost(cost);
        setVolume(volume);
        setTotal(total);
    }

    private FuelingRecord(Parcel in) {
        this(in.readLong(), in.readString(), in.readFloat(), in.readFloat(), in.readFloat());
    }

    FuelingRecord() {
        this(0, "", 0, 0, 0);
    }

    FuelingRecord(FuelingRecord fuelingRecord) {
        this(fuelingRecord.mId, fuelingRecord.mSqlLiteDate, fuelingRecord.mCost,
                fuelingRecord.mVolume, fuelingRecord.mTotal);
    }

    FuelingRecord(Intent intent) {
        this((FuelingRecord) intent.getParcelableExtra(NAME));
    }

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
        this.mId = id;
    }

    public String getSQLiteDate() {
        return mSqlLiteDate;
    }

    public void setSQLiteDate(String date) {
        this.mSqlLiteDate = date;
    }

    public float getCost() {
        return mCost;
    }

    public void setCost(float cost) {
        this.mCost = cost;
    }

    public float getVolume() {
        return mVolume;
    }

    public void setVolume(float volume) {
        this.mVolume = volume;
    }

    public float getTotal() {
        return mTotal;
    }

    public void setTotal(float total) {
        this.mTotal = total;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mId);
        dest.writeString(mSqlLiteDate);
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
        return mId + ", " + mSqlLiteDate + ", " + mCost + ", " + mVolume + ", " + mTotal;
    }
}
