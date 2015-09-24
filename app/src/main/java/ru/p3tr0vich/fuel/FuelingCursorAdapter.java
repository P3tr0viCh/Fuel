package ru.p3tr0vich.fuel;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

class FuelingCursorAdapter extends SimpleCursorAdapter {

    private final View.OnClickListener mOnClickListener;

    public boolean showYear;

    public FuelingCursorAdapter(Context context, String[] from, int[] to, View.OnClickListener onClickListener) {
        super(context, R.layout.fueling_listitem, null, from, to, 1);
        this.mOnClickListener = onClickListener;
    }

    static class ViewHolder {
        public TextView tvDate;
        public TextView tvCost;
        public TextView tvVolume;
        public TextView tvTotal;
        public ImageButton ibMenu;
    }

    @Override
    public void bindView(@NonNull View view, Context context, @NonNull Cursor cursor) {
        long id = cursor.getLong(cursor.getColumnIndex(FuelingDBHelper._ID));
        String date = cursor.getString(cursor.getColumnIndex(FuelingDBHelper.COLUMN_DATETIME));
        float cost = cursor.getFloat(cursor.getColumnIndex(FuelingDBHelper.COLUMN_COST));
        float volume = cursor.getFloat(cursor.getColumnIndex(FuelingDBHelper.COLUMN_VOLUME));
        float total = cursor.getFloat(cursor.getColumnIndex(FuelingDBHelper.COLUMN_TOTAL));

        ViewHolder holder = (ViewHolder) view.getTag();

        holder.tvDate.setText(Functions.sqlDateToString(date, showYear));
        holder.tvCost.setText(Functions.floatToString(cost));
        holder.tvVolume.setText(Functions.floatToString(volume));
        holder.tvTotal.setText(Functions.floatToString(total));

        holder.ibMenu.setTag(id);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.fueling_listitem, parent, false);

        ViewHolder holder = new ViewHolder();

        holder.tvDate = (TextView) view.findViewById(R.id.tvDate);
        holder.tvCost = (TextView) view.findViewById(R.id.tvCost);
        holder.tvVolume = (TextView) view.findViewById(R.id.tvVolume);
        holder.tvTotal = (TextView) view.findViewById(R.id.tvTotal);

        holder.ibMenu = (ImageButton) view.findViewById(R.id.ibMenu);
        holder.ibMenu.setOnClickListener(mOnClickListener);

        view.setTag(holder);

        return view;
    }
}