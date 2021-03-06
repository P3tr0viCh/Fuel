package ru.p3tr0vich.fuel.listeners

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs

internal abstract class OnRecyclerViewScrollListener(private val scrollThreshold: Int) : RecyclerView.OnScrollListener() {

    private var offset = 0

    abstract fun onScrollUp()

    abstract fun onScrollDown()

    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
        if (newState == RecyclerView.SCROLL_STATE_IDLE) {
            val layoutManager = (recyclerView.layoutManager as LinearLayoutManager?)!!

            if (layoutManager.findFirstCompletelyVisibleItemPosition() == 0) {
                onScrollDown()
            } else {
                val adapter = recyclerView.adapter!!

                if (layoutManager.findLastCompletelyVisibleItemPosition() == adapter.itemCount - 1) {
                    onScrollUp()
                } else {
                    if (abs(offset) > scrollThreshold) {
                        if (offset > 0) {
                            onScrollUp()
                        } else {
                            onScrollDown()
                        }
                    }
                }
            }

            offset = 0
        }
    }

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        offset += dy
    }
}