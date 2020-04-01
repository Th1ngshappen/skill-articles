package ru.skillbranch.skillarticles.ui.custom.markdown

import android.text.Spannable
import android.text.SpannableString
import android.util.SizeF
import androidx.core.text.getSpans
import ru.skillbranch.skillarticles.ui.custom.spans.SearchFocusSpan
import ru.skillbranch.skillarticles.ui.custom.spans.SearchSpan

// здесь будет дефолтная реализация для поиска 01:09:15

interface IMarkdownView {
    var fontSize: Float
    val spannableContent: Spannable

    fun renderSearchResult(
        results: List<Pair<Int, Int>>,
        offset: Int
    ) {
        clearSearchResult()
        val offsetResult = results.map { (start, end) ->
            start.minus(offset) to end.minus(offset)
        }

        // ctrl + alt + t - shortcut for surround with
        try {
            offsetResult.forEach { (start, end) ->
                spannableContent.setSpan(
                    SearchSpan(),
                    start,
                    end,
                    SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        } catch (e: Exception) {
        }
    }

    fun renderSearchPosition(
        searchPosition: Pair<Int, Int>,
        offset: Int
    ) {
        spannableContent.getSpans<SearchFocusSpan>().forEach {
            spannableContent.removeSpan(it)
        }

        spannableContent.setSpan(
            SearchFocusSpan(),
            searchPosition.first.minus(offset),
            searchPosition.second.minus(offset),
            SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    fun clearSearchResult() {
        spannableContent.getSpans<SearchSpan>().forEach {
            spannableContent.removeSpan(it)
        }
    }
}