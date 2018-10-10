package ru.p3tr0vich.fuel

import android.annotation.SuppressLint
import android.content.Context
import android.support.v7.preference.PreferenceCategory
import android.support.v7.preference.PreferenceGroupAdapter
import android.support.v7.widget.RecyclerView

class DividerItemDecorationPreferences(context: Context?) : DividerItemDecorationBase(context) {

    @SuppressLint("RestrictedApi")
    public override fun shouldDrawDivider(parent: RecyclerView, childViewIndex: Int): Boolean {
        return (parent.adapter as PreferenceGroupAdapter?)?.getItem(childViewIndex) !is PreferenceCategory
    }
}