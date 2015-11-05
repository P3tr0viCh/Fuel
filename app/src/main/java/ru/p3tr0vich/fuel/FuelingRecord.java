package ru.p3tr0vich.fuel;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.text.TextUtils;

public class FuelingRecord implements Parcelable {

    private static final String NAME = "FUELING_RECORD_NAME";

    private long mId;
    private String mSQLiteDate;    // Дата заправки
    private long mTimeStamp;        // Дата заправки в миллисекундах
    private float mCost;            // Стоимость
    private float mVolume;          // Объём заправки
    private float mTotal;           // Общий пробег

    public boolean showYear;

    FuelingRecord(long id, String sqlLiteDate, float cost, float volume, float total, boolean showYear) {
        setId(id);
        setSQLiteDate(sqlLiteDate);
        setCost(cost);
        setVolume(volume);
        setTotal(total);
        this.showYear = showYear;
    }

    FuelingRecord(Cursor cursor) {
        this(cursor, true);
    }

    FuelingRecord(Cursor cursor, boolean showYear) {
        this(cursor.getInt(FuelingDBHelper.COLUMN_ID_INDEX),
                cursor.getString(FuelingDBHelper.COLUMN_DATETIME_INDEX),
                cursor.getFloat(FuelingDBHelper.COLUMN_COST_INDEX),
                cursor.getFloat(FuelingDBHelper.COLUMN_VOLUME_INDEX),
                cursor.getFloat(FuelingDBHelper.COLUMN_TOTAL_INDEX),
                showYear);
    }

    FuelingRecord(long id, String sqlLiteDate, float cost, float volume, float total) {
        this(id, sqlLiteDate, cost, volume, total, true);
    }

    private FuelingRecord(Parcel in) {
        this(in.readLong(), in.readString(), in.readFloat(), in.readFloat(), in.readFloat(), in.readInt() == 1);
    }

    FuelingRecord() {
        this(0, "", 0, 0, 0, true);
    }

    FuelingRecord(FuelingRecord fuelingRecord) {
        this(fuelingRecord.mId, fuelingRecord.mSQLiteDate, fuelingRecord.mCost,
                fuelingRecord.mVolume, fuelingRecord.mTotal, fuelingRecord.showYear);
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

    public void setId(long id) {
        mId = id;
    }

    public String getSQLiteDate() {
        return mSQLiteDate;
    }

    public long getTimeStamp() {
        return mTimeStamp;
    }

    public String getDateText() {
        return Functions.sqlDateToString(mSQLiteDate, showYear)
//                + " (" + String.valueOf(mId) + ")"
//                + " (" + String.valueOf(mTimeStamp) + ")"
                ;
    }

    public void setSQLiteDate(String date) {
        mSQLiteDate = date;
        if (TextUtils.isEmpty(date))
            mTimeStamp = Long.MIN_VALUE;
        else
            mTimeStamp = Functions.sqlDateToDate(date).getTime();
    }

    public float getCost() {
        return mCost;
    }

    public String getCostText() {
        return Functions.floatToString(mCost);
    }

    public void setCost(float cost) {
        mCost = cost;
    }

    public float getVolume() {
        return mVolume;
    }

    public String getVolumeText() {
        return Functions.floatToString(mVolume);
    }

    public void setVolume(float volume) {
        mVolume = volume;
    }

    public float getTotal() {
        return mTotal;
    }

    public String getTotalText() {
        return Functions.floatToString(mTotal);
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
        dest.writeString(mSQLiteDate);
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
        return mId + ", " + mSQLiteDate + ", " + mCost + ", " + mVolume + ", " + mTotal;
    }
}
