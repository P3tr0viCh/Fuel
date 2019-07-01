package ru.p3tr0vich.fuel

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.snackbar.Snackbar
import com.melnykov.fab.FloatingActionButton
import kotlin.math.min

class FloatingActionButtonBehavior(context: Context?, attrs: AttributeSet) :
        CoordinatorLayout.Behavior<FloatingActionButton>(context, attrs) {

    private var marginBottom: Int = 0
    private var minDistance: Int = 0

    init {
        val a = context?.theme?.obtainStyledAttributes(attrs,
                R.styleable.FloatingActionButtonBehavior, 0, 0)

        try {
            marginBottom = a?.getDimensionPixelSize(R.styleable.FloatingActionButtonBehavior_behavior_marginBottom, 0) ?: 0
            minDistance = a?.getDimensionPixelSize(R.styleable.FloatingActionButtonBehavior_behavior_minDistance, 0) ?: 0
        } finally {
            a?.recycle()
        }
    }

    override fun layoutDependsOn(parent: CoordinatorLayout, child: FloatingActionButton, dependency: View): Boolean {
        return dependency is Snackbar.SnackbarLayout
    }

    override fun onDependentViewChanged(parent: CoordinatorLayout, child: FloatingActionButton, dependency: View): Boolean {
        // FAB начинает сдвигаться вверх, только когда Snackbar приближается на величину minDistance

        if (!child.isVisible) return false

        val translationY = min(0f, dependency.translationY - dependency.height + marginBottom - minDistance)

        child.translationY = translationY

        return true
    }
}