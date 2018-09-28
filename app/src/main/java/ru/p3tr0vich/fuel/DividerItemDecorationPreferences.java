package ru.p3tr0vich.fuel;

import android.content.Context;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceGroupAdapter;
import android.support.v7.widget.RecyclerView;

public class DividerItemDecorationPreferences extends DividerItemDecorationBase {

    DividerItemDecorationPreferences(Context context) {
        super(context);
    }

    @Override
    public boolean shouldDrawDivider(RecyclerView parent, int childViewIndex) {
        RecyclerView.Adapter adapter = parent.getAdapter();

        assert adapter != null;

        return !(((PreferenceGroupAdapter) adapter).getItem(childViewIndex)
                instanceof PreferenceCategory);
    }
}