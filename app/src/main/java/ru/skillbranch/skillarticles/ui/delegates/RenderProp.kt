package ru.skillbranch.skillarticles.ui.delegates

import ru.skillbranch.skillarticles.ui.base.Binding
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class RenderProp<T>(
    var value: T,
    needInit: Boolean = true,
    private val onChange: ((T) -> Unit)? = null
) : ReadWriteProperty<Binding, T> {

    private val listeners: MutableList<() -> Unit> = mutableListOf()

    init {
        if (needInit) onChange?.invoke(value)
    }

    override fun getValue(thisRef: Binding, property: KProperty<*>): T = value

    override fun setValue(thisRef: Binding, property: KProperty<*>, value: T) {
        if (value == this.value) return
        this.value = value
        onChange?.invoke(this.value)
        if (listeners.isNotEmpty()) listeners.forEach { it.invoke() }
    }

    // register additional listener
    fun addListener(listener: () -> Unit) {
        listeners.add(listener)
    }
}

// provide delegate (when by call)
// можно было бы обращаться к каждому свойству при помощи рефлексии и получить его делегат,
// но она небыстрая и kotlin.reflection - тяжёлая бибилиотека
class ObserveProp<T : Any>(private var value: T, private val onChange: ((T) -> Unit)? = null) {
    operator fun provideDelegate(
        thisRef: Binding,
        prop: KProperty<*>
    ): ReadWriteProperty<Binding, T> {
        val delegate = RenderProp(value, true, onChange)
        registerDelegate(thisRef, prop.name, delegate)
        return delegate
    }

    // register new delegate for property in Binding
    private fun registerDelegate(thisRef: Binding, name: String, delegate: RenderProp<T>) {
        thisRef.delegates[name] = delegate
    }
}