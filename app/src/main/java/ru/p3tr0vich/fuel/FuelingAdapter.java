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

    private List<FuelingRecord> mFuelingRecords;

    private boolean mShowYear;

    private final View.OnClickListener mOnClickListener;

    FuelingAdapter(View.OnClickListener onClickListener) {
        super();
        setHasStableIds(true);

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

    public void addRecord(FuelingRecord fuelingRecord) {
        fuelingRecord.showYear = mShowYear;

        mFuelingRecords.add(0, fuelingRecord); // TODO: need insert in right position

        notifyItemInserted(1);
        // TODO: scroll to
    }

    public void updateRecord(FuelingRecord fuelingRecord) {
        fuelingRecord.showYear = mShowYear;

        int position = getRecordPosition(fuelingRecord);
        if (position >= 0) {
            mFuelingRecords.set(position, fuelingRecord);
            notifyItemChanged(position + 1);
        }
        // TODO: sort if date changed
    }

    public void deleteRecord(FuelingRecord fuelingRecord) {
        int position = getRecordPosition(fuelingRecord);
        if (position >= 0) {
            mFuelingRecords.remove(position);
            notifyItemRemoved(position + 1);
        }
    }

    private int getRecordPosition(FuelingRecord fuelingRecord) {
        // TODO: need use fast search?

        for (int i = 0; i < mFuelingRecords.size(); i++)
            if (mFuelingRecords.get(i).getId() == fuelingRecord.getId()) return i;

        return -1;
    }

    public void setShowYear(boolean showYear) {
        mShowYear = showYear;
    }

    @Override
    public int getItemCount() {
        int size = mFuelingRecords.size();
        return size > 0 ? size + 2 : 0;
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
            FuelingRecord fuelingRecord = mFuelingRecords.get(position - 1);
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
        if (position == 0) return HEADER_ID;
        if (position == mFuelingRecords.size() + 1) return FOOTER_ID;

        return mFuelingRecords.get(position - 1).getId();
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
