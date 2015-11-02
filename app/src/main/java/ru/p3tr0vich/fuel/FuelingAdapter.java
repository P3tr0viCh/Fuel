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

        if (data != null && data.moveToFirst()) do
            mFuelingRecords.add(new FuelingRecord(
                    data.getInt(0),
                    data.getString(1),
                    data.getFloat(2),
                    data.getFloat(3),
                    data.getFloat(4),
                    mShowYear));
        while (data.moveToNext());

        notifyDataSetChanged();
    }

    public int addRecord(FuelingRecord fuelingRecord) {
        fuelingRecord.showYear = mShowYear;

        int position = findPositionForDate(fuelingRecord.getTimeStamp());

        mFuelingRecords.add(position, fuelingRecord); // TODO: need insert in right position

        position += mFirstRecordPosition;

        notifyItemInserted(position);

        return position;
    }

    public int updateRecord(FuelingRecord fuelingRecord) {
        // TODO: sort if date changed

        fuelingRecord.showYear = mShowYear;

        int position = positionOfRecordById(fuelingRecord.getId());
        if (position > -1) {

            mFuelingRecords.set(position - mFirstRecordPosition, fuelingRecord);

            notifyItemChanged(position);
        }

        return position;
    }

    public void deleteRecord(FuelingRecord fuelingRecord) {
        int position = positionOfRecordById(fuelingRecord.getId());
        if (position > -1) {
            mFuelingRecords.remove(position - mFirstRecordPosition);

            notifyItemRemoved(position);
        }
    }

    public int positionOfRecordById(long id) {
        // TODO: need use fast search?

        for (int i = 0; i < mFuelingRecords.size(); i++)
            if (mFuelingRecords.get(i).getId() == id) return i + mFirstRecordPosition;

        return -1;
    }

    private int findPositionForDate(long date) {
        if (mFuelingRecords.isEmpty()) return 0;

        // Взято из Arrays.binarySearch() с учётом того,
        // что массив отсортирован в обратном порядке

        // TODO

        int hi = 0;
        int lo = mFuelingRecords.size() - 1;

        if (date >= mFuelingRecords.get(hi).getTimeStamp()) return hi;

        if (date <= mFuelingRecords.get(lo).getTimeStamp()) return lo + 1;

        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            long midVal = mFuelingRecords.get(mid).getTimeStamp();

            if (midVal < date) {
                lo = mid - 1;
            } else if (midVal > date) {
                hi = mid + 1;
            } else {
                return mid;  // value found
            }
        }
        return ~lo;  // value not present
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
