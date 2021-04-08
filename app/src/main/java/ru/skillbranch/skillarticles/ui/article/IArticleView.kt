package ru.skillbranch.skillarticles.ui.article

import ru.skillbranch.skillarticles.data.remote.res.CommentRes

interface IArticleView {

    fun showSearchBar()

    fun hideSearchBar()

    fun clickOnComment(comment: CommentRes)
}