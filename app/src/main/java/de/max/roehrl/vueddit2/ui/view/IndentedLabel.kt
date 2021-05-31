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
    private val factor = 40
    private var paddingTop2 = 10
    private var paddingBottom2 = 10
    private val paint = Paint().apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.comment_indent_lines)
        strokeWidth = 0f
    }

    override fun onDraw(canvas: Canvas?) {
        setPadding(factor * depth + 20, paddingTop2, 10, paddingBottom2)
        for (i in 0..depth) {
            val indent = i * factor
            canvas?.drawRect(indent + 0f, 0f, indent + 5f, height.toFloat(), paint)
        }
        super.onDraw(canvas)
    }

    fun setDepth(depth: Int, paddingTop: Int = 10, paddingBottom: Int = 10) {
        this.depth = depth
        paddingTop2 = paddingTop
        paddingBottom2 = paddingBottom
    }
}