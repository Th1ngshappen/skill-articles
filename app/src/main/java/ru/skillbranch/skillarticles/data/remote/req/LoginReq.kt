package ru.skillbranch.skillarticles.data.remote.req

data class LoginReq(val login: String, val password: String)

data class RegisterReq(
    val name: String,
    val email: String,
    val password: String
)
