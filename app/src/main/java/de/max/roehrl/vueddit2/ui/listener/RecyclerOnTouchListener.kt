package de.max.roehrl.vueddit2.ui.listener

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class RecyclerOnTouchListener(
        context: Context,
        recyclerView: RecyclerView,
        onLongPress: ((view: View, position: Int) -> Unit)?,
        onClick: ((view: View, position: Int, e: MotionEvent) -> Unit)?,
) : RecyclerView.SimpleOnItemTouchListener() {
    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            val child = recyclerView.findChildViewUnder(e.x, e.y)
            if (child != null) {
                onClick?.invoke(child, recyclerView.getChildAdapterPosition(child), e)
            }
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            val child = recyclerView.findChildViewUnder(e.x, e.y)
            if (child != null) {
                onLongPress?.invoke(child, recyclerView.getChildAdapterPosition(child))
            }
        }
    })

    override fun onInterceptTouchEvent(recyclerView: RecyclerView, e: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(e)
        return false
    }
}