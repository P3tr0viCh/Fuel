package ru.p3tr0vich.fuel;

import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

abstract class OnRecyclerViewScrollListener extends RecyclerView.OnScrollListener {

    private final int mScrollThreshold;

    private int mOffset = 0;

    public abstract void onScrollUp();

    public abstract void onScrollDown();

    OnRecyclerViewScrollListener(int scrollThreshold) {
        mScrollThreshold = scrollThreshold;
    }

    @Override
    public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
        if (newState == RecyclerView.SCROLL_STATE_IDLE) {
            LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();

            assert layoutManager != null;

            if (layoutManager.findFirstCompletelyVisibleItemPosition() == 0) {
                onScrollDown();
            } else {
                RecyclerView.Adapter adapter = recyclerView.getAdapter();

                assert adapter != null;

                if (layoutManager.findLastCompletelyVisibleItemPosition() ==
                        (adapter.getItemCount() - 1)) {
                    onScrollUp();
                } else {
                    if (Math.abs(mOffset) > mScrollThreshold) {
                        if (mOffset > 0)
                            onScrollUp();
                        else
                            onScrollDown();
                    }
                }
            }
            mOffset = 0;
        }
    }

    @Override
    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
        mOffset += dy;
    }
}