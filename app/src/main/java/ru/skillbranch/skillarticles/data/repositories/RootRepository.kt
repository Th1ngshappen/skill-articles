package ru.skillbranch.skillarticles.data.repositories

import androidx.lifecycle.LiveData
import ru.skillbranch.skillarticles.data.local.PrefManager
import ru.skillbranch.skillarticles.data.remote.NetworkManager
import ru.skillbranch.skillarticles.data.remote.req.LoginReq
import ru.skillbranch.skillarticles.data.remote.req.RegisterReq
import ru.skillbranch.skillarticles.data.remote.res.AuthRes

// 11: 01:48:27
object RootRepository {

    private val preferences = PrefManager
    private val network = NetworkManager.api

    fun isAuth(): LiveData<Boolean> = preferences.isAuthLive

    suspend fun login(login: String, pass: String) {
        val auth = network.login(LoginReq(login, pass))
        updatePreferences(auth)
    }

    suspend fun register(name: String, login: String, password: String) {
        val auth = network.register(RegisterReq(name, login, password))
        updatePreferences(auth)
    }

    private fun updatePreferences(auth: AuthRes) {
        preferences.profile = auth.user
        preferences.accessToken = "Bearer ${auth.accessToken}"
        preferences.refreshToken = auth.refreshToken
    }
}