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

class FuelingAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;
    private static final int TYPE_FOOTER = 2;

    private static final int HEADER_ID = -2;
    private static final int FOOTER_ID = -3;

    public static final int HEADER_POSITION = 0;

    private final List<FuelingRecord> mFuelingRecords;

    private boolean mShowYear;

    private int mShowHeader;
    private int mShowFooter;

    private final View.OnClickListener mOnClickListener;

    FuelingAdapter(View.OnClickListener onClickListener) {
        super();
        setHasStableIds(true);

        setShowHeader(true);
        setShowFooter(true);

        mFuelingRecords = new ArrayList<>();

        mOnClickListener = onClickListener;
    }

    public boolean isShowHeader() {
        return mShowHeader == 1;
    }

    private void setShowHeader(boolean showHeader) {
        mShowHeader = showHeader ? 1 : 0;
    }

    public boolean isShowFooter() {
        return mShowFooter == 1;
    }

    private void setShowFooter(boolean showFooter) {
        mShowFooter = showFooter ? 1 : 0;
    }

    public void swapCursor(Cursor data) {
        mFuelingRecords.clear();

        notifyDataSetChanged();

        if (data == null || data.getCount() == 0) return;

        if (isShowHeader()) mFuelingRecords.add(null);

        if (data.moveToFirst()) do
            mFuelingRecords.add(new FuelingRecord(data, mShowYear));
        while (data.moveToNext());

        if (isShowFooter()) mFuelingRecords.add(null);

        notifyDataSetChanged();
    }

    public int addRecord(FuelingRecord fuelingRecord) {
        fuelingRecord.showYear = mShowYear;

        int position = findPositionForDate(fuelingRecord.getTimeStamp());

        mFuelingRecords.add(position, fuelingRecord);

        notifyItemInserted(position);

        return position;
    }

    public int updateRecord(FuelingRecord fuelingRecord) {
        fuelingRecord.showYear = mShowYear;

        int position = findPositionById(fuelingRecord.getId());

        if (position > -1) {
            long oldTimeStamp = mFuelingRecords.get(position).getTimeStamp();

            long newTimeStamp = fuelingRecord.getTimeStamp();

            mFuelingRecords.set(position, fuelingRecord);

            notifyItemChanged(position);

            if (oldTimeStamp != newTimeStamp) {
                FuelingRecord temp = mFuelingRecords.remove(position);

                int newPosition = findPositionForDate(newTimeStamp);

                mFuelingRecords.add(newPosition, temp);

                notifyItemMoved(position, newPosition);

                position = newPosition;
            }
        }

        return position;
    }

    public void deleteRecord(FuelingRecord fuelingRecord) {
        int position = findPositionById(fuelingRecord.getId());
        if (position > -1) {
            mFuelingRecords.remove(position);
            notifyItemRemoved(position);
        }
    }

    public int findPositionById(long id) {
        for (int i = mShowHeader; i < mFuelingRecords.size() - mShowFooter; i++)
            if (mFuelingRecords.get(i).getId() == id) return i;

        return -1;
    }

    private int findPositionForDate(long date) {
        if (mFuelingRecords.isEmpty() || date >= mFuelingRecords.get(mShowHeader).getTimeStamp())
            return mShowHeader;

        int hi = mShowHeader;
        int lo = mFuelingRecords.size() - 1 - mShowFooter;

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
        return mFuelingRecords.size();
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
            FuelingRecord fuelingRecord = mFuelingRecords.get(position);
            ((FuelingItemViewHolder) holder).binding.setFuelingRecord(fuelingRecord);

            ((FuelingItemViewHolder) holder).binding.ibMenu.setTag(fuelingRecord.getId());
            ((FuelingItemViewHolder) holder).binding.ibMenu.setOnClickListener(mOnClickListener);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (isShowHeader() && position == HEADER_POSITION) return TYPE_HEADER;
        if (isShowFooter() && position == mFuelingRecords.size() - 1) return TYPE_FOOTER;
        return TYPE_ITEM;
    }

    @Override
    public long getItemId(int position) {
        if (isShowHeader() && position == HEADER_POSITION) return HEADER_ID;
        if (isShowFooter() && position == mFuelingRecords.size() - 1) return FOOTER_ID;

        return mFuelingRecords.get(position).getId();
    }

    public class FuelingItemViewHolder extends RecyclerView.ViewHolder {

        final FuelingListitemBinding binding;

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
