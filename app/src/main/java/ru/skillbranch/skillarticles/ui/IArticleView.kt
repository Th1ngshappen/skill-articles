package ru.skillbranch.skillarticles.ui

interface IArticleView {

    // отрисовать все вхождения поискового запроса в контент (spannable)
    fun renderSearchResult(searchResult: List<Pair<Int, Int>>)

    // отрисовать текущее положение поиска и перевести фокус на него (spannable)
    fun renderSearchPosition(searchPosition: Int)

    // очистить результаты поиска (удалить все spannable)
    fun clearSearchResult()

    fun showSearchBar()

    fun hideSearchBar()
}