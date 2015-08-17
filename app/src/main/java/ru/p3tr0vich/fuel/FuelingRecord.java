package ru.p3tr0vich.fuel;

import android.os.Parcel;
import android.os.Parcelable;

public class FuelingRecord implements Parcelable {

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
        setId(in.readLong());
        setSQLiteDate(in.readString());
        setCost(in.readFloat());
        setVolume(in.readFloat());
        setTotal(in.readFloat());
    }

    FuelingRecord() {
        setId(0);
        setSQLiteDate("");
        setCost(0);
        setVolume(0);
        setTotal(0);
    }

/*     FuelingRecord(Intent intent) {
        setId(intent.getLongExtra(FuelingDBHelper._ID, 0));
        setSQLiteDate(intent.getStringExtra(FuelingDBHelper.COLUMN_DATETIME));
        setCost(intent.getFloatExtra(FuelingDBHelper.COLUMN_COST, 0));
        setVolume(intent.getFloatExtra(FuelingDBHelper.COLUMN_VOLUME, 0));
        setTotal(intent.getFloatExtra(FuelingDBHelper.COLUMN_TOTAL, 0));
    }*/

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
