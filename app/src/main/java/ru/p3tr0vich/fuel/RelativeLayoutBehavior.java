package ru.p3tr0vich.fuel;

import android.content.Context;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;

public class RelativeLayoutBehavior extends CoordinatorLayout.Behavior<RelativeLayout> {

    public RelativeLayoutBehavior(Context context, AttributeSet attrs) {
    }

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, RelativeLayout child, View dependency) {
        return dependency instanceof Snackbar.SnackbarLayout;
    }

    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent, RelativeLayout child, View dependency) {
        int padding = (int) Math.min(0, dependency.getTranslationY() - dependency.getHeight());
        child.setPadding(0, 0, 0, -padding);
        return true;
    }
}