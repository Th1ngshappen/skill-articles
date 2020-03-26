package ru.skillbranch.skillarticles.ui.custom.spans

import android.graphics.*
import android.text.style.ReplacementSpan
import androidx.annotation.ColorInt
import androidx.annotation.Px
import androidx.annotation.VisibleForTesting
import ru.skillbranch.skillarticles.data.repositories.Element


class BlockCodeSpan(
    @ColorInt
    private val textColor: Int,
    @ColorInt
    private val bgColor: Int,
    @Px
    private val cornerRadius: Float,
    @Px
    private val padding: Float,
    private val type: Element.BlockCode.Type
) : ReplacementSpan() {
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    var rect = RectF()
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    var path = Path()

    override fun draw(
        canvas: Canvas,
        text: CharSequence,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint
    ) {
        paint.forBackground {
            when (type) {
                Element.BlockCode.Type.SINGLE -> {
                    rect.set(0f, top + padding, canvas.width.toFloat(), bottom - padding)
                    canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
                }
                Element.BlockCode.Type.START -> {
                    rect.set(0f, top + padding, canvas.width.toFloat(), bottom.toFloat())
                    path.reset()
                    path.addRoundRect(
                        rect,
                        floatArrayOf(
                            cornerRadius, cornerRadius, // Top left radius in px
                            cornerRadius, cornerRadius, // Top right radius in px
                            0f, 0f, // Bottom right radius in px
                            0f, 0f // Bottom left radius in px
                        ),
                        Path.Direction.CW
                    )
                    canvas.drawPath(path, paint)
                }
                Element.BlockCode.Type.MIDDLE -> {
                    rect.set(0f, top.toFloat(), canvas.width.toFloat(), bottom.toFloat())
                    canvas.drawRect(rect, paint)
                }
                Element.BlockCode.Type.END -> {
                    rect.set(0f, top.toFloat(), canvas.width.toFloat(), bottom - padding)
                    path.reset()
                    path.addRoundRect(
                        rect,
                        floatArrayOf(
                            0f, 0f, // Top left radius in px
                            0f, 0f, // Top right radius in px
                            cornerRadius, cornerRadius, // Bottom right radius in px
                            cornerRadius, cornerRadius // Bottom left radius in px
                        ),
                        Path.Direction.CW
                    )
                    canvas.drawPath(path, paint)
                }
            }
        }

        paint.forText {
            canvas.drawText(text, start, end, x + padding, y.toFloat(), paint)
        }

    }

    override fun getSize(
        paint: Paint,
        text: CharSequence,
        start: Int,
        end: Int,
        fm: Paint.FontMetricsInt?
    ): Int {

        fm ?: return 0

        val ascent = paint.ascent()
        val descent = paint.descent()

        when (type) {
            Element.BlockCode.Type.SINGLE -> {
                // вычитаем 2 отступа
                // первый - от верхней линии спана до бэкграунда
                // второй - от верхнего края бэкграунда до текста
                fm.ascent = (ascent - 2 * padding).toInt()
                fm.descent = (descent + 2 * padding).toInt()
            }
            Element.BlockCode.Type.START -> {
                fm.ascent = (ascent - 2 * padding).toInt()
                fm.descent = (descent).toInt()
            }
            Element.BlockCode.Type.MIDDLE -> {
                fm.ascent = (ascent).toInt()
                fm.descent = (descent).toInt()
            }
            Element.BlockCode.Type.END -> {
                fm.ascent = (ascent).toInt()
                fm.descent = (descent + 2 * padding).toInt()
            }
        }

        fm.top = fm.ascent
        fm.bottom = fm.descent

        return 0
    }

    private inline fun Paint.forText(block: () -> Unit) {

        val oldSize = textSize
        val oldStyle = typeface?.style ?: 0
        val oldFont = typeface
        val oldColor = color

        color = textColor
        typeface = Typeface.create(Typeface.MONOSPACE, oldStyle)
        textSize *= 0.85f

        block()

        color = oldColor
        typeface = oldFont
        textSize = oldSize

    }

    private inline fun Paint.forBackground(block: () -> Unit) {
        val oldColor = color
        val oldStyle = style

        color = bgColor
        style = Paint.Style.FILL

        block()

        color = oldColor
        style = oldStyle
    }

}
