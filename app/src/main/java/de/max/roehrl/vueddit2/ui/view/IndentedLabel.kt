package de.max.roehrl.vueddit2.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import de.max.roehrl.vueddit2.R

class IndentedLabel(context: Context, attrs: AttributeSet) : AppCompatTextView(context, attrs) {
    private var depth = 0
    private var paddingTop2 = 10
    private val factor = 40f
    private val paint: Paint = Paint()

    init {
        paint.style = Paint.Style.STROKE
        paint.color = ContextCompat.getColor(context, R.color.comment_indent_lines)
        paint.strokeWidth = 5f
    }

    override fun onDraw(canvas: Canvas?) {
        setPadding((factor * depth + 20).toInt(), paddingTop2, 10, 10)
        super.onDraw(canvas)
        for (i in 0..depth) {
            val indent = i * factor
            canvas?.drawLine(indent, 0f, indent, height.toFloat(), paint)
        }
    }

    fun setDepth(depth: Int, paddingTop: Int) {
        this.depth = depth
        paddingTop2 = paddingTop
    }
}