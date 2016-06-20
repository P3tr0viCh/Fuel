package ru.p3tr0vich.fuel.factories;

import android.support.annotation.NonNull;
import android.view.View;

import ru.p3tr0vich.fuel.R;
import ru.p3tr0vich.fuel.views.FuelingTotalView;
import ru.p3tr0vich.fuel.views.FuelingTotalViewOnePanel;
import ru.p3tr0vich.fuel.views.FuelingTotalViewTwoPanels;

public class FuelingTotalViewFactory {

    private FuelingTotalViewFactory() {
    }

    @NonNull
    public static FuelingTotalView getFuelingTotalView(@NonNull View view) {
        return view.findViewById(R.id.expansion_panel) != null ?
                new FuelingTotalViewTwoPanels(view) :
                new FuelingTotalViewOnePanel(view);
    }
}