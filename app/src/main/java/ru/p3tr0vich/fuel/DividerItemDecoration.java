package ru.p3tr0vich.fuel;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;

@SuppressWarnings("SameParameterValue")
class DividerItemDecoration extends RecyclerView.ItemDecoration {

    private final Drawable mDivider;

    private final boolean mShowFirstDivider;
    private final boolean mShowLastDivider;
    private final int mFooterType;

    public DividerItemDecoration(Context context, AttributeSet attrs, boolean showFirstDivider,
                                 boolean showLastDivider, int footerType) {
        final TypedArray a = context
                .obtainStyledAttributes(attrs, new int[]{android.R.attr.listDivider});
        mDivider = a.getDrawable(0);
        a.recycle();

        mShowFirstDivider = showFirstDivider;
        mShowLastDivider = showLastDivider;
        mFooterType = footerType;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                               RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);
        if (mDivider == null) return;

        if (parent.getChildAdapterPosition(view) < 1) return;

        if (getOrientation(parent) == LinearLayoutManager.VERTICAL)
            outRect.top = mDivider.getIntrinsicHeight();
        else
            outRect.left = mDivider.getIntrinsicWidth();
    }

    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
        if (mDivider == null) {
            super.onDrawOver(c, parent, state);
            return;
        }

        int left = 0, right = 0, top = 0, bottom = 0, size;
        int orientation = getOrientation(parent);
        int childCount = parent.getChildCount();

        if (orientation == LinearLayoutManager.VERTICAL) {
            size = mDivider.getIntrinsicHeight();
            left = parent.getPaddingLeft();
            right = parent.getWidth() - parent.getPaddingRight();
        } else {
            size = mDivider.getIntrinsicWidth();
            top = parent.getPaddingTop();
            bottom = parent.getHeight() - parent.getPaddingBottom();
        }

        for (int i = mShowFirstDivider ? 0 : 1; i < childCount; i++) {
            View child = parent.getChildAt(i);

            if (mFooterType > -1 && i == childCount - 1 &&
                    parent.getAdapter().getItemViewType(parent.getChildAdapterPosition(child)) ==
                            mFooterType)
                return;

            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();

            if (orientation == LinearLayoutManager.VERTICAL) {
                top = child.getTop() - params.topMargin;
                bottom = top + size;
            } else {
                left = child.getLeft() - params.leftMargin;
                right = left + size;
            }
            mDivider.setBounds(left, top, right, bottom);
            mDivider.draw(c);
        }

        if (mShowLastDivider && childCount > 0) {
            View child = parent.getChildAt(childCount - 1);

            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();
            if (orientation == LinearLayoutManager.VERTICAL) {
                top = child.getBottom() + params.bottomMargin;
                bottom = top + size;
            } else {
                left = child.getRight() + params.rightMargin;
                right = left + size;
            }
            mDivider.setBounds(left, top, right, bottom);
            mDivider.draw(c);
        }
    }

    private int getOrientation(RecyclerView parent) {
        RecyclerView.LayoutManager layoutManager = parent.getLayoutManager();
        if (layoutManager instanceof LinearLayoutManager)
            return ((LinearLayoutManager) layoutManager).getOrientation();
        else
            throw new IllegalStateException(
                    "DividerItemDecoration can only be used with a LinearLayoutManager.");
    }
}