package ru.p3tr0vich.fuel

import android.content.Context
import android.support.v7.widget.RecyclerView

class DividerItemDecorationFueling(context: Context?) : DividerItemDecorationBase(context) {

    var footerType: Int = -1

    public override fun shouldDrawDivider(parent: RecyclerView, childViewIndex: Int): Boolean {
        return footerType <= -1 || childViewIndex != parent.childCount - 2 ||
                parent.adapter?.getItemViewType(
                        parent.getChildAdapterPosition(
                                parent.getChildAt(childViewIndex + 1))) != footerType
    }
}