package ru.p3tr0vich.fuel;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

// TODO: не тестировалось с orientation != LinearLayoutManager.VERTICAL

public abstract class DividerItemDecorationBase extends RecyclerView.ItemDecoration {

    private final Drawable mDivider;

    DividerItemDecorationBase(Context context) {
        final TypedArray a = context.obtainStyledAttributes(new int[]{android.R.attr.listDivider});

        mDivider = a.getDrawable(0);

        a.recycle();
    }

    protected abstract boolean shouldDrawDivider(RecyclerView parent, int childViewIndex);

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

        for (int i = 0, count = childCount - 1; i < count; i++) {
            if (!shouldDrawDivider(parent, i)) continue;

            View child = parent.getChildAt(i);

            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();

            if (orientation == LinearLayoutManager.VERTICAL) {
                bottom = child.getBottom() - params.bottomMargin;
                top = bottom - size;
            } else {
                left = child.getLeft() - params.leftMargin;
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
                    "DividerItemDecorationBase can only be used with a LinearLayoutManager.");
    }
}