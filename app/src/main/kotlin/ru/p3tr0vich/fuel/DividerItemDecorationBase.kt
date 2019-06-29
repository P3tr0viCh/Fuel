package ru.p3tr0vich.fuel

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

// TODO: не тестировалось с orientation != LinearLayoutManager.VERTICAL

abstract class DividerItemDecorationBase(context: Context?) : RecyclerView.ItemDecoration() {

    private val divider: Drawable?

    init {
        val a = context?.obtainStyledAttributes(intArrayOf(android.R.attr.listDivider))

        divider = a?.getDrawable(0)

        a?.recycle()
    }

    protected abstract fun shouldDrawDivider(parent: RecyclerView, childViewIndex: Int): Boolean

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        if (divider == null) {
            super.onDrawOver(c, parent, state)
            return
        }

        var left = 0
        var right = 0
        var top = 0
        var bottom = 0
        val size: Int
        val orientation = getOrientation(parent)
        val childCount = parent.childCount

        if (orientation == LinearLayoutManager.VERTICAL) {
            size = divider.intrinsicHeight
            left = parent.paddingLeft
            right = parent.width - parent.paddingRight
        } else {
            size = divider.intrinsicWidth
            top = parent.paddingTop
            bottom = parent.height - parent.paddingBottom
        }

        var i = 0
        val count = childCount - 1
        while (i < count) {
            if (!shouldDrawDivider(parent, i)) {
                i++
                continue
            }

            val child = parent.getChildAt(i)

            val params = child.layoutParams as RecyclerView.LayoutParams

            if (orientation == LinearLayoutManager.VERTICAL) {
                bottom = child.bottom - params.bottomMargin
                top = bottom - size
            } else {
                left = child.left - params.leftMargin
                right = left + size
            }

            divider.setBounds(left, top, right, bottom)
            divider.draw(c)

            i++
        }
    }

    private fun getOrientation(parent: RecyclerView): Int {
        return (parent.layoutManager as LinearLayoutManager?)?.orientation
                ?: throw IllegalStateException("DividerItemDecorationBase can only be used with a LinearLayoutManager.")
    }
}