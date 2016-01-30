package ru.p3tr0vich.fuel;

import android.support.v7.widget.RecyclerView;

abstract class OnRecyclerViewScrollListener extends RecyclerView.OnScrollListener {
    private int mScrollThreshold;

    abstract void onScrollUp();

    abstract void onScrollDown();

    OnRecyclerViewScrollListener(int scrollThreshold) {
        setScrollThreshold(scrollThreshold);
    }

    @Override
    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        if (Math.abs(dy) > mScrollThreshold) {
            if (dy > 0)
                onScrollUp();
            else
                onScrollDown();
        }
    }

    private void setScrollThreshold(int scrollThreshold) {
        mScrollThreshold = scrollThreshold;
    }
}