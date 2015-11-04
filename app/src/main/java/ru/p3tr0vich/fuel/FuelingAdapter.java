package ru.p3tr0vich.fuel;

import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import ru.p3tr0vich.fuel.databinding.FuelingListitemBinding;

public class FuelingAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;
    private static final int TYPE_FOOTER = 2;

    private static final int HEADER_ID = -2;
    private static final int FOOTER_ID = -3;

    public static final int HEADER_POSITION = 0;

    private List<FuelingRecord> mFuelingRecords;

    private boolean mShowYear;

    private int mFirstRecordPosition;

    private final View.OnClickListener mOnClickListener;

    FuelingAdapter(View.OnClickListener onClickListener) {
        super();
        setHasStableIds(true);

        mFirstRecordPosition = 1;

        mFuelingRecords = new ArrayList<>();

        mOnClickListener = onClickListener;
    }

    public void swapCursor(Cursor data) {
        mFuelingRecords.clear();

        notifyDataSetChanged();

        if (data == null) return;

        if (data.moveToFirst()) do
            mFuelingRecords.add(new FuelingRecord(data, mShowYear));
        while (data.moveToNext());

        notifyDataSetChanged();
    }

    public int addRecord(FuelingRecord fuelingRecord) {
        fuelingRecord.showYear = mShowYear;

        int position = findPositionForDate(fuelingRecord.getTimeStamp());

        mFuelingRecords.add(position, fuelingRecord);

        position += mFirstRecordPosition;

        notifyItemInserted(position);

        return position;
    }

    public int updateRecord(FuelingRecord fuelingRecord) {
        // TODO: sort if date changed

        fuelingRecord.showYear = mShowYear;

        int position = getPositionById(fuelingRecord.getId());

        if (position > -1) {
            long oldTimeStamp = mFuelingRecords.get(position).getTimeStamp();

            long newTimeStamp = fuelingRecord.getTimeStamp();

            mFuelingRecords.set(position, fuelingRecord);

            notifyItemChanged(position + mFirstRecordPosition);

            if (oldTimeStamp != newTimeStamp) {
                FuelingRecord temp = mFuelingRecords.remove(position);

                int newPosition = findPositionForDate(newTimeStamp);

                mFuelingRecords.add(newPosition, temp);

                notifyItemMoved(position + mFirstRecordPosition, newPosition + mFirstRecordPosition);

                position = newPosition;
            }
        }

        return position + mFirstRecordPosition;
    }

    public void deleteRecord(FuelingRecord fuelingRecord) {
        int position = getPositionById(fuelingRecord.getId());
        if (position > -1) {
            mFuelingRecords.remove(position);

            notifyItemRemoved(position + mFirstRecordPosition);
        }
    }

    private int getPositionById(long id) {
        for (int i = 0; i < mFuelingRecords.size(); i++)
            if (mFuelingRecords.get(i).getId() == id) return i;

        return -1;
    }

    public int findPositionById(long id) {
        int position = getPositionById(id);
        return position != -1 ? position + mFirstRecordPosition : -1;
    }

    private int findPositionForDate(long date) {
        if (mFuelingRecords.isEmpty() || date >= mFuelingRecords.get(0).getTimeStamp()) return 0;

        int hi = 0;
        int lo = mFuelingRecords.size() - 1;

        while (hi <= lo) {
            int mid = (lo + hi) >>> 1;
            long midVal = mFuelingRecords.get(mid).getTimeStamp();

            if (midVal < date) {
                lo = mid - 1;
            } else if (midVal > date) {
                hi = mid + 1;
            } else {
                return mid;
            }
        }
        return hi;
    }

    public void setShowYear(boolean showYear) {
        mShowYear = showYear;
    }

    @Override
    public int getItemCount() {
        int size = mFuelingRecords.size();
        return size > 0 ? size + 2 : 0;
    }

    public int getFirstRecordPosition() {
        return mFirstRecordPosition;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_ITEM) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            FuelingListitemBinding binding = FuelingListitemBinding.inflate(inflater, parent, false);

            return new FuelingItemViewHolder(binding.getRoot());
        } else if (viewType == TYPE_HEADER)
            return new HeaderViewHolder(
                    LayoutInflater.from(parent.getContext()).
                            inflate(R.layout.fueling_listview_header, parent, false));
        else if (viewType == TYPE_FOOTER)
            return new HeaderViewHolder(
                    LayoutInflater.from(parent.getContext()).
                            inflate(R.layout.fueling_listview_footer, parent, false));

        throw new RuntimeException("Wrong type: " + viewType);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof FuelingItemViewHolder) {
            FuelingRecord fuelingRecord = mFuelingRecords.get(position - mFirstRecordPosition);
            ((FuelingItemViewHolder) holder).binding.setFuelingRecord(fuelingRecord);

            // TODO: don't use Tag

            ((FuelingItemViewHolder) holder).binding.ibMenu.setTag(fuelingRecord.getId());
            ((FuelingItemViewHolder) holder).binding.ibMenu.setOnClickListener(mOnClickListener);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) return TYPE_HEADER;
        if (position == mFuelingRecords.size() + 1) return TYPE_FOOTER;
        return TYPE_ITEM;
    }

    @Override
    public long getItemId(int position) {
        if (position == HEADER_POSITION) return HEADER_ID;
        if (position == mFuelingRecords.size() + 1) return FOOTER_ID;

        return mFuelingRecords.get(position - mFirstRecordPosition).getId();
    }

    public class FuelingItemViewHolder extends RecyclerView.ViewHolder {

        FuelingListitemBinding binding;

        public FuelingItemViewHolder(View itemView) {
            super(itemView);
            binding = DataBindingUtil.bind(itemView);
        }
    }

    public class HeaderViewHolder extends RecyclerView.ViewHolder {

        public HeaderViewHolder(View itemView) {
            super(itemView);
        }
    }
}
