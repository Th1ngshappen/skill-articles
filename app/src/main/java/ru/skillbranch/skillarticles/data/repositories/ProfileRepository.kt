package ru.skillbranch.skillarticles.data.repositories

import androidx.lifecycle.LiveData
import okhttp3.MultipartBody
import ru.skillbranch.skillarticles.data.local.PrefManager
import ru.skillbranch.skillarticles.data.models.User
import ru.skillbranch.skillarticles.data.remote.NetworkManager
import ru.skillbranch.skillarticles.data.remote.req.EditProfileReq

interface IProfileRepository {
    fun getProfile(): LiveData<User?>
    fun logout()
    suspend fun uploadAvatar(body: MultipartBody.Part)
    suspend fun removeAvatar()
    suspend fun editProfile(name: String, about: String)
}

// 11: репозиторий-object по сути - ленивый singleton, в DI будет по-другому
object ProfileRepository: IProfileRepository {

    private val prefs = PrefManager
    private val network = NetworkManager.api

    override fun getProfile(): LiveData<User?> = prefs.profileLive

    override fun logout() {
        prefs.profile = null
        prefs.accessToken = ""
        prefs.refreshToken = ""
    }

    override suspend fun uploadAvatar(body: MultipartBody.Part) {
        val (url) = network.upload(body, prefs.accessToken)
        prefs.profile = prefs.profile!!.copy(avatar = url)
    }

    override suspend fun removeAvatar() {
        network.remove(prefs.accessToken)
        prefs.profile = prefs.profile!!.copy(avatar = "")
    }

    override suspend fun editProfile(name: String, about: String) {
        network.editProfile(EditProfileReq(name, about), prefs.accessToken)
        prefs.profile = prefs.profile!!.copy(name = name, about = about)
    }
}