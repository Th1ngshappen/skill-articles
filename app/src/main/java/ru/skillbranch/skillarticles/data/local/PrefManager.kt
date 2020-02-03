package ru.skillbranch.skillarticles.data.local

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import ru.skillbranch.skillarticles.data.delegates.PrefDelegate


class PrefManager(context: Context) {

    var storedBoolean by PrefDelegate(false)
    var storedString by PrefDelegate("")
    var storedFloat by PrefDelegate(0f)
    var storedInt by PrefDelegate(0)
    var storedLong by PrefDelegate(0)

    val preferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(context)
    }

    fun clearAll(): Unit = with(preferences.edit()) {
        clear()
        apply()
    }
}