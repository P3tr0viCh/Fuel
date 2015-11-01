package ru.p3tr0vich.fuel;

import android.databinding.DataBindingUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import ru.p3tr0vich.fuel.databinding.FuelingListitemBinding;

public class FuelingAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;
    private static final int TYPE_FOOTER = 2;

    private List<FuelingRecord> mFuelingRecords;

    private final View.OnClickListener mOnClickListener;

    FuelingAdapter(View.OnClickListener onClickListener) {
        super();
        mOnClickListener = onClickListener;
    }

    public void setRecords(List<FuelingRecord> fuelingRecords) {
        mFuelingRecords = fuelingRecords;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        if (mFuelingRecords == null) return 0;
        int size = mFuelingRecords.size();
        if (size > 0) return size + 2;
        return 0;
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

        throw new RuntimeException("there is no type that matches the type " + viewType);
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
        if (mFuelingRecords == null) {
            if (position == 1) return TYPE_FOOTER;
            else return TYPE_ITEM;
        }
        if (position == mFuelingRecords.size() + 1)
            return TYPE_FOOTER;
        return TYPE_ITEM;
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
