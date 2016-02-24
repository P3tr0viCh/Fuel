package ru.p3tr0vich.fuel;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.util.AttributeSet;
import android.view.View;

import com.melnykov.fab.FloatingActionButton;

public class FloatingActionButtonBehavior extends CoordinatorLayout.Behavior<FloatingActionButton> {

    private int mMarginBottom;
    private int mMinDistance;

    public FloatingActionButtonBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.getTheme().obtainStyledAttributes(attrs,
                R.styleable.FloatingActionButtonBehavior, 0, 0);

        try {
            mMarginBottom = a.getDimensionPixelSize(R.styleable.FloatingActionButtonBehavior_behavior_marginBottom, 0);
            mMinDistance = a.getDimensionPixelSize(R.styleable.FloatingActionButtonBehavior_behavior_minDistance, 0);
        } finally {
            a.recycle();
        }
    }

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, FloatingActionButton child, View dependency) {
        return dependency instanceof Snackbar.SnackbarLayout;
    }

    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent, FloatingActionButton child, View dependency) {
        // FAB начинает сдвигаться вверх, только когда Snackbar приближается на величину mMinDistance

        if (!child.isVisible()) return false;

        float translationY = Math.min(0, dependency.getTranslationY() - dependency.getHeight() +
                mMarginBottom - mMinDistance);

        child.setTranslationY(translationY);

        return true;
    }
}