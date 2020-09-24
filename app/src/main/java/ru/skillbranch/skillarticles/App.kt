package ru.skillbranch.skillarticles

import android.app.Application
import android.content.Context
import com.facebook.stetho.Stetho

class App : Application() {

    companion object {
        private var instance: App? = null

        // App хранит в себе applicationContext, чтобы его можно было получить из любых других объектов
        fun applicationContext(): Context {
            return instance!!.applicationContext
        }
    }

    init {
        instance = this
    }

    override fun onCreate() {
        super.onCreate()

        // TODO set default Night Mode

        Stetho.initializeWithDefaults(this)
    }

}