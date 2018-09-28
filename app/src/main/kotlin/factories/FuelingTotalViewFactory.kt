package ru.p3tr0vich.fuel.factories

import android.view.View

import ru.p3tr0vich.fuel.R
import ru.p3tr0vich.fuel.views.FuelingTotalView
import ru.p3tr0vich.fuel.views.FuelingTotalViewOnePanel
import ru.p3tr0vich.fuel.views.FuelingTotalViewTwoPanels

object FuelingTotalViewFactory {

    @JvmStatic
    fun getFuelingTotalView(view: View): FuelingTotalView {
        return if (view.findViewById<View>(R.id.expansion_panel) == null)
            FuelingTotalViewOnePanel(view)
        else
            FuelingTotalViewTwoPanels(view)
    }
}