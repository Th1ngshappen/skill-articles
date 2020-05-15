package ru.skillbranch.skillarticles.data.models

import java.util.*

// 8: 01:22:10 slug, как правило - текстовой id, часто выполняет роль первичного ключа
// в отличие от id обычно является строкой и указывает на определённую иерархию связей
// (можно определить, кто его родитель)
data class CommentItemData(
    val id: String,
    val articleId: String,
    val user: User,
    val body: String,
    val date: Date,
    val slug: String,
    val answerTo: String? = null
) {

}