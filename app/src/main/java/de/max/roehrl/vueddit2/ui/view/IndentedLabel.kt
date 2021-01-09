package de.max.roehrl.vueddit2.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

class IndentedLabel(context: Context, attrs: AttributeSet) : AppCompatTextView(context, attrs) {
    private var depth = 0
    private var paddingTop2 = 10
    private val factor = 40f
    private val paint: Paint = Paint()

    init {
        paint.style = Paint.Style.STROKE
        paint.color = Color.parseColor("#282828")
        paint.strokeWidth = 5f
    }

    override fun onDraw(canvas: Canvas?) {
        super.setPadding((factor * this.depth + 20).toInt(), this.paddingTop2, 10, 10)
        super.onDraw(canvas)
        for (i in 0..depth) {
            val indent = i * factor
            canvas?.drawLine(indent, 0f, indent, super.getHeight().toFloat(), paint)
        }
    }

    fun setDepth(depth: Int, paddingTop: Int) {
        this.depth = depth
        this.paddingTop2 = paddingTop
    }
}