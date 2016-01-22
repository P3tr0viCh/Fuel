package ru.p3tr0vich.fuel;

import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
    public static final int TYPE_FOOTER = 2;

    private static final int HEADER_ID = -2;
    private static final int FOOTER_ID = -3;

    public static final int HEADER_POSITION = 0;

    private final List<FuelingRecord> mFuelingRecords;

    private boolean mShowYear;

    @HeaderFooter
    private int mShowHeader;
    @HeaderFooter
    private int mShowFooter;

    @IntDef({HF_HIDE, HF_SHOW})
    public @interface HeaderFooter {
    }

    private static final int HF_HIDE = 0;
    private static final int HF_SHOW = 1;

    private final View.OnClickListener mOnClickListener;

    private final OnFuelingRecordsChangeListener mOnFuelingRecordsChangeListener;

    FuelingAdapter(View.OnClickListener onClickListener,
                   OnFuelingRecordsChangeListener onFuelingRecordsChangeListener,
                   boolean showHeader, boolean showFooter) {
        super();
        setHasStableIds(true);

        setShowHeader(showHeader);
        setShowFooter(showFooter);

        mFuelingRecords = new ArrayList<>();
        if (isShowHeader()) mFuelingRecords.add(null);
        if (isShowFooter()) mFuelingRecords.add(null);

        mOnClickListener = onClickListener;

        mOnFuelingRecordsChangeListener = onFuelingRecordsChangeListener;
    }

    public interface OnFuelingRecordsChangeListener {
        void OnFuelingRecordsChange(@NonNull List<FuelingRecord> fuelingRecords);
    }

    public boolean isShowHeader() {
        return mShowHeader == HF_SHOW;
    }

    private void setShowHeader(boolean showHeader) {
        mShowHeader = showHeader ? HF_SHOW : HF_HIDE;
    }

    private boolean isShowFooter() {
        return mShowFooter == HF_SHOW;
    }

    private void setShowFooter(boolean showFooter) {
        mShowFooter = showFooter ? HF_SHOW : HF_HIDE;
    }

    public void swapCursor(@Nullable Cursor data) {
        UtilsLog.d("FuelingAdapter", "swapCursor",
                data == null ? "data == null" : "data count == " + data.getCount());

        mFuelingRecords.clear();

        notifyDataSetChanged();

        if (isShowHeader()) mFuelingRecords.add(null);

        if (data != null && data.getCount() != 0 && data.moveToFirst())
            do
                mFuelingRecords.add(new FuelingRecord(data, mShowYear));
            while (data.moveToNext());

        if (isShowFooter()) mFuelingRecords.add(null);

        notifyDataSetChanged();

        notifyFuelingRecordsChanged();
    }

    private void notifyFuelingRecordsChanged() {
        List<FuelingRecord> tempFuelingRecords = new ArrayList<>(mFuelingRecords);
        if (tempFuelingRecords.size() != 0) {
            if (isShowHeader()) tempFuelingRecords.remove(0);
            if (isShowFooter()) tempFuelingRecords.remove(tempFuelingRecords.size() - 1);
        }

        mOnFuelingRecordsChangeListener.OnFuelingRecordsChange(tempFuelingRecords);
    }

    public int insertRecord(FuelingRecord fuelingRecord) {
        fuelingRecord.showYear = mShowYear;

        int position = findPositionForDate(fuelingRecord.getDateTime());

        mFuelingRecords.add(position, fuelingRecord);

        notifyItemInserted(position);

        notifyFuelingRecordsChanged();

        return position;
    }

    public int updateRecord(FuelingRecord fuelingRecord) {
        fuelingRecord.showYear = mShowYear;

        int position = findPositionById(fuelingRecord.getId());

        if (position > -1) {
            long oldDateTime = mFuelingRecords.get(position).getDateTime();

            long newDateTime = fuelingRecord.getDateTime();

            mFuelingRecords.set(position, fuelingRecord);

            notifyItemChanged(position);

            if (oldDateTime != newDateTime) {
                FuelingRecord temp = mFuelingRecords.remove(position);

                int newPosition = findPositionForDate(newDateTime);

                mFuelingRecords.add(newPosition, temp);

                notifyItemMoved(position, newPosition);

                position = newPosition;
            }
        }

        notifyFuelingRecordsChanged();

        return position;
    }

    public int findPositionById(long id) {
        for (int i = mShowHeader; i < mFuelingRecords.size() - mShowFooter; i++)
            if (mFuelingRecords.get(i).getId() == id) return i;

        return -1;
    }

    public void deleteRecord(long id) {
        int position = findPositionById(id);
        if (position > -1) {
            mFuelingRecords.remove(position);
            notifyItemRemoved(position);

            notifyFuelingRecordsChanged();
        }
    }

    private boolean isEmpty() {
        return mFuelingRecords.size() - mShowHeader - mShowFooter == 0;
    }

    private int findPositionForDate(long date) {
        if (isEmpty() || date >= mFuelingRecords.get(mShowHeader).getDateTime())
            return mShowHeader;

        int hi = mShowHeader;
        int lo = mFuelingRecords.size() - 1 - mShowFooter;

        while (hi <= lo) {
            int mid = (lo + hi) >>> 1;
            long midVal = mFuelingRecords.get(mid).getDateTime();

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
