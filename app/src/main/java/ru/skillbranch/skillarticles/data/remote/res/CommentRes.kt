package ru.skillbranch.skillarticles.data.remote.res

import com.squareup.moshi.Json
import ru.skillbranch.skillarticles.data.models.User
import java.util.*

// 8: 01:22:10 slug, как правило - текстовой id, часто выполняет роль первичного ключа
// в отличие от id обычно является строкой и указывает на определённую иерархию связей
// (можно определить, кто его родитель)
data class CommentRes(
    val id: String,
    @Json(name = "author")
    val user: User,
    @Json(name = "message")
    val body: String,
    val date: Date,
    val slug: String,
    val answerTo: String? = null
)