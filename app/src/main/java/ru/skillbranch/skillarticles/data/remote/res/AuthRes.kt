package ru.skillbranch.skillarticles.data.remote.res

import ru.skillbranch.skillarticles.data.models.User

data class AuthRes(
    val user: User,
    // 11: 01:50:00 refresh token нужен, чтобы лишний раз не передавать на сервер
    // логин и пароль (что небезопасно), а с его помощью получить новый access token, если он истёк
    // надо будет реализовать это в домашнем задании с помощью интерсепторов
    // access token может быть вечный, как и access token, но его время жизни всегода больше
    val refreshToken: String,
    val accessToken: String
)