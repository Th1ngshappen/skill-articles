package ru.skillbranch.skillarticles.extensions.data

import ru.skillbranch.skillarticles.data.AppSettings
import ru.skillbranch.skillarticles.data.ArticlePersonalInfo
import ru.skillbranch.skillarticles.data.SearchInfo
import ru.skillbranch.skillarticles.viewmodels.ArticleState

fun ArticleState.toAppSettings(): AppSettings {
    return AppSettings(isDarkMode, isBigText)
}

fun ArticleState.toArticlePersonalInfo(): ArticlePersonalInfo {
    return ArticlePersonalInfo(isLike, isBookmark)
}

fun ArticleState.toSearchInfo(): SearchInfo {
    return SearchInfo(isSearch, searchQuery, searchResults, searchPosition)
}