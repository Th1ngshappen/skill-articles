package ru.skillbranch.skillarticles.data.delegates

import ru.skillbranch.skillarticles.data.local.PrefManager
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class PrefDelegate<T>(private val defaultValue: T) : ReadWriteProperty<PrefManager, T?> {

    override fun getValue(thisRef: PrefManager, property: KProperty<*>): T? {

        return when (defaultValue) {
            is Boolean -> thisRef.preferences.getBoolean(property.name, defaultValue) as T
            is String -> thisRef.preferences.getString(property.name, defaultValue) as T
            is Float -> thisRef.preferences.getFloat(property.name, defaultValue) as T
            is Int -> thisRef.preferences.getInt(property.name, defaultValue) as T
            is Long -> thisRef.preferences.getLong(property.name, defaultValue) as T
            else -> throw IllegalArgumentException("Only primitives types can be stored in Shared Preferences")
        }
    }

    override fun setValue(thisRef: PrefManager, property: KProperty<*>, value: T?) {

        with(thisRef.preferences.edit()) {

            val key = property.name
            when (value) {
                is Boolean -> putBoolean(key, value)
                is String -> putString(key, value)
                is Float -> putFloat(key, value)
                is Int -> putInt(key, value)
                is Long -> putLong(key, value)
                else -> throw IllegalArgumentException("Only primitives types can be stored in Shared Preferences")
            }
            apply()
        }
    }
}