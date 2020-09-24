package ru.skillbranch.skillarticles.data.local

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import ru.skillbranch.skillarticles.App
import ru.skillbranch.skillarticles.data.delegates.PrefDelegate
import ru.skillbranch.skillarticles.data.models.AppSettings

object PrefManager {

    internal val preferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(App.applicationContext())
    }

    private var isDarkMode by PrefDelegate(false)
    private var isBigText by PrefDelegate(false)
    private var isAuth by PrefDelegate(false)

    private val settings = MutableLiveData(
        AppSettings(
            isDarkMode ?: false,
            isBigText ?: false
        )
    )
    private val isAuthStatus = MutableLiveData(isAuth ?: false)

    fun clearAll() {
        preferences.edit().clear().apply()
    }

    fun getAppSettings(): LiveData<AppSettings> = settings

    fun updateAppSettings(appSettings: AppSettings) {
        isDarkMode = appSettings.isDarkMode
        isBigText = appSettings.isBigText
        settings.value = appSettings
    }

    fun isAuth(): MutableLiveData<Boolean> = isAuthStatus

    fun setAuth(auth: Boolean) {
        isAuth = auth
        isAuthStatus.value = auth
    }
}