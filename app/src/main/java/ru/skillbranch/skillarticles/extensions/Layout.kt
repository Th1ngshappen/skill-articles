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

// TODO
// 6: 02:15:50 line spacing добавляет отступ для новой строки каждый раз, кроме последней (см презентацию)
// есть недокументированное условие: если последняя строка является whitespace символом, отступ также не добавляется
// с этим связан этот баг - в экст. функции мы отнимаем спэйсинг только для последней стрки -
// для последних строчек будет некорректно рассчитываться отступ
fun Layout.getLineBottomWithoutPadding(line: Int): Int {
    return getLineBottom(line) - if (line == lineCount - 1) bottomPadding else 0
}

/**
 * Get the line bottom discarding the line spacing added.
 */
// TODO найти и исправить какой-то визуальный баг (около 01:00:00)
fun Layout.getLineBottomWithoutSpacing(line: Int): Int {
    val lineBottom = getLineBaseline(line)
    val isLastLine = line == lineCount.dec()
    val hasLineSpacing = spacingAdd != 0f

    return if (!hasLineSpacing || isLastLine) {
        lineBottom + spacingAdd.toInt()
    } else {
        lineBottom - spacingAdd.toInt()
    }
}