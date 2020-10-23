package ru.skillbranch.skillarticles.data.remote.req

data class RegisterReq(
    val name: String,
    val email: String,
    val password: String
)