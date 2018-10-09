package ru.p3tr0vich.fuel.listeners

import android.content.Context
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener

internal abstract class OnSwipeTouchListener(context: Context?) : OnTouchListener {

    private val gestureDetector: GestureDetector = GestureDetector(context, GestureListener())

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event)
    }

    private inner class GestureListener : SimpleOnGestureListener() {

        override fun onDown(e: MotionEvent): Boolean {
            return true
        }

        override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            try {
                val diffX = e2.x - e1.x
                val diffY = e2.y - e1.y

                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0)
                            onSwipeRight()
                        else
                            onSwipeLeft()

                        return true
                    }
                } else if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffY > 0)
                        onSwipeBottom()
                    else
                        onSwipeTop()

                    return true
                }
            } catch (exception: Exception) {
                exception.printStackTrace()
            }

            return false
        }
    }

    companion object {
        private const val SWIPE_THRESHOLD = 100
        private const val SWIPE_VELOCITY_THRESHOLD = 100
    }

    abstract fun onSwipeRight()

    abstract fun onSwipeLeft()

    abstract fun onSwipeTop()

    abstract fun onSwipeBottom()
}