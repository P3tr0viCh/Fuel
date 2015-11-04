package ru.p3tr0vich.fuel;

import android.content.Context;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.util.AttributeSet;
import android.view.View;

import com.melnykov.fab.FloatingActionButton;

public class FloatingActionButtonBehavior extends CoordinatorLayout.Behavior<FloatingActionButton> {

    public FloatingActionButtonBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, FloatingActionButton child, View dependency) {
        return dependency instanceof Snackbar.SnackbarLayout;
    }

    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent, FloatingActionButton child, View dependency) {
        // FAB начинает сдвигаться вверх, только когда Snackbar приближается на
        // величину fab_margin_bottom_snackbar

        float translationY = Math.min(0, dependency.getTranslationY() - dependency.getHeight() +
                Functions.getDimensionPixelSize(R.dimen.fab_margin_bottom) -
                Functions.getDimensionPixelSize(R.dimen.fab_margin_bottom_snackbar));
        child.setTranslationY(translationY);
        return true;
    }
}
