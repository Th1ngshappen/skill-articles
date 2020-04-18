package ru.skillbranch.skillarticles.extensions

import android.text.Layout

/**
 * Get the line height of a line.
 */
fun Layout.getLineHeight(line: Int): Int {
    return getLineTop(line.inc()) - getLineTop(line)
}

/**
 * Returns the top of the Layout after removing the extra padding applied by the Layout.
 */
// отнимает топ паддинг от текущей высоты строки, если это самая верхняя строка
fun Layout.getLineTopWithoutPadding(line: Int): Int {
    return getLineTop(line) - if (line == 0) topPadding else 0
}

/**
 * Returns the bottom of the Layout after removing the extra padding applied by the Layout.
 */

fun Layout.getLineBottomWithoutPadding(line: Int): Int {
    return getLineBottomWithoutSpacing(line) - if (line == lineCount - 1) bottomPadding else 0
}

/**
 * Get the line bottom discarding the line spacing added.
 */
fun Layout.getLineBottomWithoutSpacing(line: Int): Int {
    val lineBottom = getLineBottom(line)
    val isLastLine = line == lineCount.dec()
    val hasLineSpacing = spacingAdd != 0f

    val nextLineIsLast = line == lineCount - 2

    // 6: 00:58:10, 02:15:50 line spacing добавляет отступ для новой строки каждый раз, кроме последней (см презентацию)
    // есть недокументированное условие: если последняя строка является whitespace символом, отступ также не добавляется
    val onlyWhitespaceIsAfter = if (nextLineIsLast) {
        val start = getLineStart(line + 1)
        val lastVisible = getLineVisibleEnd(line + 1)
        start == lastVisible
    } else false

    return if (!hasLineSpacing || isLastLine || onlyWhitespaceIsAfter) {
        lineBottom
    } else {
        lineBottom - spacingAdd.toInt()
    }
}