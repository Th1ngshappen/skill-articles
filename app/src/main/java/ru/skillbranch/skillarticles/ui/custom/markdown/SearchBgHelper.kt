package ru.skillbranch.skillarticles.ui.custom.markdown

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.text.Layout
import android.text.Spanned
import androidx.core.graphics.ColorUtils
import androidx.core.text.getSpans
import ru.skillbranch.skillarticles.R
import ru.skillbranch.skillarticles.extensions.*
import ru.skillbranch.skillarticles.ui.custom.spans.HeaderSpan
import ru.skillbranch.skillarticles.ui.custom.spans.InlineCodeSpan
import ru.skillbranch.skillarticles.ui.custom.spans.SearchFocusSpan
import ru.skillbranch.skillarticles.ui.custom.spans.SearchSpan

// класс для отрисовки фона под текст вью
class SearchBgHelper(
    context: Context,
    private val focusListener: ((Int, Int) -> Unit)? = null, // (top, bottom)
    mockDrawable: Drawable? = null // for mock drawable
) {
    constructor(context: Context, focusListener: ((Int, Int) -> Unit)) : this(
        context,
        focusListener,
        null
    )

    private val padding: Int = context.dpToIntPx(4)
    private val radius: Float = context.dpToPx(8)
    private val borderWidth: Int = context.dpToIntPx(1)

    private val secondaryColor: Int = context.attrValue(R.attr.colorSecondary)
    private val alphaColor: Int = ColorUtils.setAlphaComponent(secondaryColor, 160)

    // 00:47:30 процедурное создание drawable немного быстрее, чем из xml (ненамного, но быстрее)
    // поэтому мы создаём их программно через lazy

    val drawable: Drawable by lazy {
        mockDrawable ?: GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadii = FloatArray(8).apply { fill(radius, 0, size) }
            color = ColorStateList.valueOf(alphaColor)
            setStroke(borderWidth, secondaryColor)
        }
    }

    val drawableLeft: Drawable by lazy {
        mockDrawable ?: GradientDrawable().apply {
            GradientDrawable.RECTANGLE
            cornerRadii = floatArrayOf(
                radius, radius, // Top left radius in px
                0f, 0f, // Top right
                0f, 0f, // Bottom right
                radius, radius // Bottom left
            )
            color = ColorStateList.valueOf(alphaColor)
            setStroke(borderWidth, secondaryColor)
        }
    }

    val drawableMiddle: Drawable by lazy {
        mockDrawable ?: GradientDrawable().apply {
            GradientDrawable.RECTANGLE
            color = ColorStateList.valueOf(alphaColor)
            setStroke(borderWidth, secondaryColor)
        }
    }

    val drawableRight: Drawable by lazy {
        mockDrawable ?: GradientDrawable().apply {
            GradientDrawable.RECTANGLE
            cornerRadii = floatArrayOf(
                0f, 0f, // Top left radius in px
                radius, radius, // Top right
                radius, radius, // Bottom right
                0f, 0f // Bottom left
            )
            color = ColorStateList.valueOf(alphaColor)
            setStroke(borderWidth, secondaryColor)
        }
    }

    private lateinit var render: SearchBgRender
    // рендеры будут созданы единожды и точно не будут изменяться
    private val singleLineRender: SearchBgRender by lazy {
        SingleLineRender(
            padding, drawable
        )
    }
    private val multiLineRender: SearchBgRender by lazy {
        MultiLineRender(
            padding, drawableLeft, drawableMiddle, drawableRight
        )
    }

    // так как код идёт в продакшн, мы не делаем аллокацию в методе draw(),
    // а заранее создаём приватное свойство с поздней инициализацией -
    // массив, где каждый потомок будет являться invarianted? SearchSpan
    private lateinit var spans: Array<out SearchSpan>
    private lateinit var headerSpans: Array<out HeaderSpan>
    private lateinit var inlineCodeSpans: Array<out InlineCodeSpan>

    private var spanStart = 0
    private var spanEnd = 0
    private var codeSpanStart = 0
    private var codeSpanEnd = 0
    private var startLine = 0
    private var endLine = 0

    // позиция по оси x по отношению к началу и к концу строки для найденного поискового вхождения
    private var startOffset = 0
    private var endOffset = 0
    private var topExtraPadding = 0
    private var bottomExtraPadding = 0
    private var startExtraPadding = 0
    private var endExtraPadding = 0

    fun draw(canvas: Canvas, text: Spanned, layout: Layout) {
        spans = text.getSpans()
        spans.forEach {
            spanStart = text.getSpanStart(it)
            spanEnd = text.getSpanEnd(it)
            startLine = layout.getLineForOffset(spanStart)
            endLine = layout.getLineForOffset(spanEnd)

            if (it is SearchFocusSpan) {
                // if search focus invoke listener for focus
                focusListener?.invoke(layout.getLineTop(startLine), layout.getLineBottom(startLine))
            }

            // т.к. Layout ничего не знает о fontSize и lineHeight используемых для отрисовки шрифта,
            // нам нужно обращаться непосредствено к спанам, которые мы отрисовываем
            headerSpans = text.getSpans(spanStart, spanEnd, HeaderSpan::class.java)

            topExtraPadding = 0
            bottomExtraPadding = 0

            if (headerSpans.isNotEmpty()) {
                topExtraPadding =
                    if (spanStart in headerSpans[0].firstLineBounds
                        || spanEnd in headerSpans[0].firstLineBounds
                    ) headerSpans[0].topExtraPadding else 0

                // если это последняя строка, будет возвращён bottomExtraPadding,
                // в случае, если начало или конец входят в границы последней строки хэдера
                bottomExtraPadding =
                    if (spanStart in headerSpans[0].lastLineBounds
                        || spanEnd in headerSpans[0].lastLineBounds
                    ) headerSpans[0].bottomExtraPadding else 0
            }

            inlineCodeSpans = text.getSpans(spanStart, spanEnd, InlineCodeSpan::class.java)

            startExtraPadding = 0
            endExtraPadding = 0

            if (inlineCodeSpans.isNotEmpty()) {
                with (inlineCodeSpans[0]) {
                    codeSpanStart = text.getSpanStart(this)
                    codeSpanEnd = text.getSpanEnd(this)

                    startExtraPadding = when {
                        spanStart < codeSpanStart -> 0
                        spanStart > codeSpanStart -> -this.padding.toInt()
                        else -> this.padding.toInt()
                    }

                    endExtraPadding = when {
                        spanEnd < codeSpanEnd -> this.padding.toInt()
                        spanEnd > codeSpanEnd -> 0
                        else -> this.padding.toInt()
                    }
                }
            }

            startOffset = layout.getPrimaryHorizontal(spanStart).toInt()
            endOffset = layout.getPrimaryHorizontal(spanEnd).toInt()

            render = if (startLine == endLine) singleLineRender else multiLineRender
            render.draw(
                canvas,
                layout,
                startLine,
                endLine,
                startOffset,
                endOffset,
                topExtraPadding,
                bottomExtraPadding,
                startExtraPadding,
                endExtraPadding
            )

        }
    }
}

abstract class SearchBgRender(
    val padding: Int // отступ слева и справа
) {
    abstract fun draw(
        canvas: Canvas,
        layout: Layout,
        startLine: Int,
        endLine: Int,
        startOffset: Int,
        endOffset: Int,
        topExtraPadding: Int = 0,
        bottomExtraPadding: Int = 0,
        startExtraPadding: Int = 0,
        endExtraPadding: Int = 0
    )

    fun getLineTop(layout: Layout, line: Int): Int {
        return layout.getLineTopWithoutPadding(line)
    }

    fun getLineBottom(layout: Layout, line: Int): Int {
        return layout.getLineBottomWithoutPadding(line)
    }
}

class SingleLineRender(
    padding: Int,
    val drawable: Drawable
) : SearchBgRender(padding) {

    // создаём свойства, чтобы не делать аллокацию в draw
    private var lineTop: Int = 0
    private var lineBottom: Int = 0

    override fun draw(
        canvas: Canvas,
        layout: Layout,
        startLine: Int,
        endLine: Int,
        startOffset: Int,
        endOffset: Int,
        topExtraPadding: Int,
        bottomExtraPadding: Int,
        startExtraPadding: Int,
        endExtraPadding: Int
    ) {
        lineTop = getLineTop(layout, startLine) + topExtraPadding
        lineBottom = getLineBottom(layout, startLine) - bottomExtraPadding
        drawable.setBounds(
            startOffset + startExtraPadding - padding,
            lineTop,
            endOffset - endExtraPadding + padding,
            lineBottom)
        drawable.draw(canvas)
    }

}

class MultiLineRender(
    padding: Int,
    private val drawableLeft: Drawable,
    private val drawableMiddle: Drawable,
    private val drawableRight: Drawable
) : SearchBgRender(padding) {

    private var lineTop: Int = 0
    private var lineBottom: Int = 0
    private var lineEndOffset: Int = 0
    private var lineStartOffset: Int = 0

    override fun draw(
        canvas: Canvas,
        layout: Layout,
        startLine: Int,
        endLine: Int,
        startOffset: Int,
        endOffset: Int,
        topExtraPadding: Int,
        bottomExtraPadding: Int,
        startExtraPadding: Int,
        endExtraPadding: Int
    ) {
        // draw first line
        // вычисляем, где будет заканчиваться строка (где последняя позиция по оси x в конце строки)
        lineEndOffset = (layout.getLineRight(startLine) + padding).toInt()
        lineTop = getLineTop(layout, startLine) + topExtraPadding
        lineBottom = getLineBottom(layout, startLine)
        drawStart(canvas, startOffset - padding, lineTop, lineEndOffset, lineBottom)

        // draw middle lines
        for (line in startLine.inc() until endLine) {
            lineTop = getLineTop(layout, line)
            lineBottom = getLineBottom(layout, line)
            drawableMiddle.setBounds(
                layout.getLineLeft(line).toInt() - padding,
                lineTop,
                layout.getLineRight(line).toInt() + padding,
                lineBottom
            )
            drawableMiddle.draw(canvas)
        }

        // draw lastLine
        lineStartOffset = (layout.getLineLeft(endLine) - padding).toInt()
        lineTop = getLineTop(layout, endLine)
        lineBottom = getLineBottom(layout, endLine) - bottomExtraPadding
        drawEnd(canvas, lineStartOffset, lineTop, endOffset + padding, lineBottom)
    }

    // drawable, скруглённая слева (для первой строки)
    private fun drawStart(
        canvas: Canvas,
        start: Int,
        top: Int,
        end: Int,
        bottom: Int
    ) {
        drawableLeft.setBounds(start, top, end, bottom)
        drawableLeft.draw(canvas)
    }

    // drawable, скруглённая справа (для последней строки)
    private fun drawEnd(
        canvas: Canvas,
        start: Int,
        top: Int,
        end: Int,
        bottom: Int
    ) {
        drawableRight.setBounds(start, top, end, bottom)
        drawableRight.draw(canvas)
    }
}