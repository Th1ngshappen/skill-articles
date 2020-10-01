package ru.skillbranch.skillarticles.data.repositories

import androidx.lifecycle.LiveData
import ru.skillbranch.skillarticles.data.local.PrefManager
import ru.skillbranch.skillarticles.data.models.User

// 11: репозиторий-object по сути - ленивый singleton, в DI будет по-другому
object ProfileRepository {

    private val prefs = PrefManager

    fun getProfile(): LiveData<User?> = prefs.profileLive
}