package ru.skillbranch.skillarticles.data.delegates

import com.squareup.moshi.JsonAdapter
import ru.skillbranch.skillarticles.data.local.PrefManager
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class PrefDelegate<T>(private val defaultValue: T) {

    private var storedValue: T? = null

    operator fun provideDelegate(
        thisRef: PrefManager,
        property: KProperty<*>
    ): ReadWriteProperty<PrefManager, T> {

        val key = property.name
        return object : ReadWriteProperty<PrefManager, T> {

            override fun getValue(thisRef: PrefManager, property: KProperty<*>): T {
                if (storedValue == null) {
                    @Suppress("UNCHECKED_CAST")
                    storedValue = when (defaultValue) {
                        is Int -> thisRef.preferences.getInt(key, defaultValue) as T
                        is Long -> thisRef.preferences.getLong(key, defaultValue) as T
                        is Float -> thisRef.preferences.getFloat(key, defaultValue) as T
                        is String -> thisRef.preferences.getString(key, defaultValue) as T
                        is Boolean -> thisRef.preferences.getBoolean(key, defaultValue) as T
                        else -> throw IllegalArgumentException("Only primitives types can be stored in Shared Preferences")
                    }
                }
                return storedValue!!
            }

            override fun setValue(thisRef: PrefManager, property: KProperty<*>, value: T) {

                with(thisRef.preferences.edit()) {
                    when (value) {
                        is Int -> putInt(key, value)
                        is Long -> putLong(key, value)
                        is Float -> putFloat(key, value)
                        is String -> putString(key, value)
                        is Boolean -> putBoolean(key, value)
                        else -> throw IllegalArgumentException("Only primitives types can be stored in Shared Preferences")
                    }
                    apply()
                }
                storedValue = value
            }

        }
    }
}

class PrefObjDelegate<T>(
    private val adapter: JsonAdapter<T>
) {
    private var storedValue: T? = null

    operator fun provideDelegate(
        thisRef: PrefManager,
        property: KProperty<*>
    ): ReadWriteProperty<PrefManager, T?> {
        val key = property.name
        return object : ReadWriteProperty<PrefManager, T?> {

            override fun getValue(thisRef: PrefManager, property: KProperty<*>): T? {
                if (storedValue == null) {
                    storedValue = thisRef.preferences.getString(key, null)
                        ?.let { adapter.fromJson(it) }
                }
                return storedValue
            }

            override fun setValue(thisRef: PrefManager, property: KProperty<*>, value: T?) {
                storedValue = value
                with(thisRef.preferences.edit()) {
                    putString(key, value?.let { adapter.toJson(it) })
                    apply()
                }
            }
        }
    }

}