package ru.skillbranch.skillarticles

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import dagger.hilt.android.HiltAndroidApp
import ru.skillbranch.skillarticles.data.local.PrefManager
import ru.skillbranch.skillarticles.data.remote.NetworkMonitor
import javax.inject.Inject

@HiltAndroidApp
class App : Application() {

    companion object {
        private var instance: App? = null

        // App хранит в себе applicationContext, чтобы его можно было получить из любых других объектов
        fun applicationContext(): Context {
            return instance!!.applicationContext
        }
    }

    @Inject
    lateinit var monitor: NetworkMonitor

    @Inject
    lateinit var preferences: PrefManager

    init {
        instance = this
        Log.e("App", "variant: ${BuildConfig.BUILD_TYPE}")
    }

    override fun onCreate() {
        super.onCreate()

        // start network monitoring
        monitor.registerNetworkMonitor()

        // set saved night/day mode
        val mode = if (preferences.isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
        else AppCompatDelegate.MODE_NIGHT_NO
        AppCompatDelegate.setDefaultNightMode(mode)
    }

}