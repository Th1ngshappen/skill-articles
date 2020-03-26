package ru.skillbranch.skillarticles.ui.custom.spans

import android.graphics.Canvas
import android.graphics.Paint
import android.text.style.ReplacementSpan
import androidx.annotation.ColorInt
import androidx.annotation.Px

// ReplacementSpan - абстрактный класс, который используется для замещения какой-то части текста
// и там можно отрисовать что угодно, в т.ч. изображение
class HorizontalRuleSpan(
    @Px
    val ruleWidth: Float,
    @ColorInt
    val ruleColor: Int
) : ReplacementSpan() {

    override fun getSize(
        paint: Paint,
        text: CharSequence?,
        start: Int,
        end: Int,
        fm: Paint.FontMetricsInt?
    ): Int {
        return 0
    }

    override fun draw(
        canvas: Canvas,
        text: CharSequence?,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint
    ) {
        paint.forLine {
            canvas.drawLine(
                0f,
                (top + bottom) / 2f,
                canvas.width.toFloat(),
                (top + bottom) / 2f,
                paint
            )
        }
    }

    private inline fun Paint.forLine(block: () -> Unit) {
        val oldColor = color
        val oldStyle = style
        val oldWidth = strokeWidth

        strokeWidth = ruleWidth
        color = ruleColor
        style = Paint.Style.STROKE

        block()

        color = oldColor
        style = oldStyle
        strokeWidth = oldWidth
    }

}