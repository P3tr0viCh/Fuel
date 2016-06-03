package ru.p3tr0vich.fuel;

import android.databinding.DataBindingUtil;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import ru.p3tr0vich.fuel.databinding.FuelingListItemBinding;
import ru.p3tr0vich.fuel.models.FuelingRecord;

class FuelingAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;
    public static final int TYPE_FOOTER = 2;

    private static final long HEADER_ID = Long.MAX_VALUE;
    private static final long FOOTER_ID = Long.MIN_VALUE;

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

    FuelingAdapter(View.OnClickListener onClickListener,
                   boolean showHeader, boolean showFooter) {
        super();
        setHasStableIds(true);

        setShowHeader(showHeader);
        setShowFooter(showFooter);

        mFuelingRecords = new ArrayList<>();
        if (isShowHeader()) mFuelingRecords.add(null);
        if (isShowFooter()) mFuelingRecords.add(null);

        mOnClickListener = onClickListener;
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

    public void swapRecords(@Nullable List<FuelingRecord> records) {
//        UtilsLog.d("FuelingAdapter", "swapCursor",
//                data == null ? "data == null" : "data count == " + data.getCount());

        mFuelingRecords.clear();

        if (isShowHeader()) mFuelingRecords.add(null);

        if (records != null)
            mFuelingRecords.addAll(records);

        if (isShowFooter()) mFuelingRecords.add(null);

        notifyDataSetChanged();
    }

    public int findPositionById(long id) {
        for (int i = mShowHeader, count = mFuelingRecords.size() - mShowFooter; i < count; i++)
            if (mFuelingRecords.get(i).getId() == id) return i;

        return -1;
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
        switch (viewType) {
            case TYPE_ITEM:
                return new FuelingItemViewHolder(
                        FuelingListItemBinding.inflate(
                                LayoutInflater.from(parent.getContext()), parent, false).getRoot());
            case TYPE_HEADER:
                return new HeaderViewHolder(
                        LayoutInflater.from(parent.getContext()).
                                inflate(R.layout.fueling_recycler_view_header, parent, false));
            case TYPE_FOOTER:
                return new HeaderViewHolder(
                        LayoutInflater.from(parent.getContext()).
                                inflate(R.layout.fueling_recycler_view_footer, parent, false));
            default:
                throw new RuntimeException("onCreateViewHolder: wrong viewType == " + viewType);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof FuelingItemViewHolder) {
            final FuelingRecord fuelingRecord = mFuelingRecords.get(position);

            final FuelingListItemBinding binding = ((FuelingItemViewHolder) holder).getBinding();

            binding.setFuelingRecord(fuelingRecord);

            binding.btnMenu.setTag(fuelingRecord.getId());
            binding.btnMenu.setOnClickListener(mOnClickListener);

            binding.setShowYear(mShowYear);
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

        private final FuelingListItemBinding mBinding;

        public FuelingItemViewHolder(View itemView) {
            super(itemView);
            mBinding = DataBindingUtil.bind(itemView);
        }

        public FuelingListItemBinding getBinding() {
            return mBinding;
        }
    }

    public class HeaderViewHolder extends RecyclerView.ViewHolder {

        public HeaderViewHolder(View itemView) {
            super(itemView);
        }
    }
}