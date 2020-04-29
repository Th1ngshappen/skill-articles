package ru.skillbranch.skillarticles.ui.base

import android.os.Bundle
import ru.skillbranch.skillarticles.ui.delegates.RenderProp
import ru.skillbranch.skillarticles.viewmodels.base.IViewModelState
import kotlin.reflect.KProperty

abstract class Binding {
    // mutableMapOf<поле делегата, сам делегат>
    val delegates = mutableMapOf<String, RenderProp<out Any>>()
    var isInflated = false

    open val afterInflated: (() -> Unit)? = null
    fun onFinishInflate() {
        if (!isInflated) {
            afterInflated?.invoke()
            isInflated = true
        }
    }

    // Binding будет создан единожды, но метод rebind будет
    // у него вызываться каждый раз, когда будет создаваться новая вью
    fun rebind() {
        delegates.forEach { it.value.bind() }
    }

    abstract fun bind(data: IViewModelState)
    /**
     * override this if need save binding in bundle
     */
    open fun saveUi(outState: Bundle) {
        //empty default implementation
    }
    /**
     * override this if need restore binding from bundle
     */
    open fun restoreUi(savedState: Bundle?) {
        //empty default implementation
    }


    // onChange - обработчик, который будет вызван в том случае, если одно из значений, переданных первым
    // аргументом полей было изменено
    // все key property преобразуются в список из имён,
    // в мапе делегатов по имени property ищется делегат,
    // если находится - вызывается у этого делегата метод addListener
    // таким образом, когда свойство этого делегата будет изменено,
    // будет вызван обработчик onChange,
    // в который будет переданы все текущие значения наблюдаемых полей из мапы delegates
    fun <A, B, C> dependsOn(
        vararg fields: KProperty<*>,
        onChange: (A, B, C) -> Unit
    ) {
        check(fields.size == 3) { "Names size must be 3, current ${fields.size}" }
        val names = fields.map { it.name }

        names.forEach {
            delegates[it]?.addListener {
                onChange(
                    delegates[names[0]]?.value as A,
                    delegates[names[1]]?.value as B,
                    delegates[names[2]]?.value as C
                )
            }
        }

    }
}