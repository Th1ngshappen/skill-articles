package ru.skillbranch.skillarticles.viewmodels.base

import android.os.Bundle
import android.util.Log
import androidx.annotation.UiThread
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.*
import androidx.navigation.NavDirections
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import ru.skillbranch.skillarticles.data.remote.err.ApiError
import ru.skillbranch.skillarticles.data.remote.err.NoNetworkError
import java.net.SocketTimeoutException

abstract class BaseViewModel<T : IViewModelState>(
    private val handleState: SavedStateHandle,
    initState: T
) : ViewModel() {

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    val notifications = MutableLiveData<Event<Notify>>()

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    val navigation = MutableLiveData<Event<NavigationCommand>>()

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    val permissions = MutableLiveData<Event<List<String>>>()

    private val loading = MutableLiveData<Loading>(Loading.HIDE_LOADING)

    /***
     * Инициализация начального состояния аргументом конструктоа, и объявления состояния как
     * MediatorLiveData - медиатор используется для того чтобы учитывать изменяемые данные модели
     * и обновлять состояние ViewModel исходя из полученных данных
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    val state: MediatorLiveData<T> = MediatorLiveData<T>().apply {
        value = initState
    }

    /***
     * getter для получения not null значения текущего состояния ViewModel
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    val currentState
        get() = state.value!!

    // лямбда-выражение принимает в качестве аргумента лямбду, в которую передаётся текущее состояние
    // и она возвращает модифицированное состояние, которое присваивается текущему состоянию
    @UiThread
    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    inline fun updateState(update: (currentState: T) -> T) {
        val updatedState: T = update(currentState)
        state.value = updatedState
    }

    /***
     * функция для создания уведомления пользователя о событии (событие обрабатывается только один раз)
     * соответственно при изменении конфигурации и пересоздании Activity уведомление не будет вызвано повторно
     */
    @UiThread
    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    fun notify(content: Notify) {
        notifications.value = Event(content)
    }

    /***
     * отображение индикатора загрузки (по умолчанию не блокирующий ui поток loading)
     */
    protected fun showLoading(loadingType: Loading = Loading.SHOW_LOADING) {
        loading.value = loadingType
    }

    /***
     * скрытие индикатора загрузки
     */
    protected fun hideLoading() {
        loading.value = Loading.HIDE_LOADING
    }

    open fun navigate(command: NavigationCommand) {
        navigation.value = Event(command)
    }

    fun navigateWithAction(navDirections: NavDirections) {
        navigate(NavigationCommand.To(navDirections.actionId, navDirections.arguments))
    }

    // более компактная форма записи observe() метода LiveData; принимает последним аргументом
    // лямбда-выражение, обрабатывающее изменение текущего состояния
    fun observeState(owner: LifecycleOwner, onChanged: (newState: T) -> Unit) {
        state.observe(owner, Observer { onChanged(it!!) })
    }

    // более компактная форма записи observe() метода LiveData; принимает последним аргументом
    // лямбда-выражение, обрабатывающее изменение текущего индикатора загрузки
    fun observeLoading(owner: LifecycleOwner, onChanged: (newState: Loading) -> Unit) {
        loading.observe(owner, Observer { onChanged(it!!) })
    }

    // более компактная форма записи observe() метода LiveData; вызывает лямбда-выражение обработчик
    // только в том случае, если сообщение не было уже обработано ранее,
    // реализует данное поведение благодаря EventObserver
    fun observeNotifications(owner: LifecycleOwner, onNotify: (notification: Notify) -> Unit) {
        notifications.observe(owner,
            EventObserver { onNotify(it) })
    }

    fun observeNavigation(owner: LifecycleOwner, onNavigate: (command: NavigationCommand) -> Unit) {
        navigation.observe(owner, EventObserver { onNavigate(it) })
    }

    // функция принимает источник данных и лямбда-выражение, обрабатывающее поступающие данные источника
    // лямбда принимает новые данные и текущее состояние ViewModel в качестве аргументов,
    // изменяет его и возвращает модифицированное состояние, которое устанавливается как текущее
    protected fun <S> subscribeOnDataSource(
        source: LiveData<S>,
        onChanged: (newValue: S, currentState: T) -> T?
    ) {
        state.addSource(source) {
            // В лямбде обычный return не работает, он заставит выйти из функции, в которой лямбда вызвана.
            // Чтобы выйти из лямбды, после return ставят метку - @lambda, указывающую на нужную лямбду
            state.value = onChanged(it, currentState) ?: return@addSource
        }
    }

    fun saveState() {
        currentState.save(handleState)
    }

    @Suppress("UNCHECKED_CAST")
    fun restoreState() {
        // чтобы не было избыточных срабатываний стэйта при ресторе - не обновляем лайвдату лишний раз
        val restoreState = currentState.restore(handleState) as T
        if (currentState == restoreState) return
        state.value = currentState.restore(handleState) as T
    }

    protected fun launchSafety(
        errHandler: ((Throwable) -> Unit)? = null,
        compHandler: ((Throwable?) -> Unit)? = null,
        block: suspend CoroutineScope.() -> Unit
    ) {
        // используется обработчик ошибок, переданный в качестве аргумента или обработчик ошибок по умолчанию
        val errHand = CoroutineExceptionHandler { _, err ->
            errHandler?.invoke(err) ?: when (err) {
                is NoNetworkError -> notify(Notify.TextMessage("Network not available, check internet connection"))

                is SocketTimeoutException -> notify(
                    Notify.ActionMessage(
                        "Network timeout exception - please try again",
                        "Retry"
                    ) { launchSafety(errHandler, compHandler, block) }
                )

                is ApiError.InternalServerError -> notify(
                    Notify.ErrorMessage(
                        err.message,
                        "Retry"
                    ) { launchSafety(errHandler, compHandler, block) }
                )

                is ApiError -> notify(Notify.ErrorMessage(err.message))
                else -> notify(Notify.ErrorMessage(err.message ?: "Something wrong"))
            }
            Log.d("M_BaseViewModel", "${err.message}")
        }

        (viewModelScope + errHand).launch {
            // отобразить индикатор загрузки
            showLoading()
            // если внутри блока лямбды обратиться к this, то это будет обращение к CoroutineScope вью модели
            block()
        }.invokeOnCompletion {
            // скрыть индикатор загрузки по окончанию выполнения suspend функции
            hideLoading()
            // вызвать обработчик окончания выполнения suspend функции если имеется
            compHandler?.invoke(it)
        }
    }

    fun requestPermissions(requestedPermissions: List<String>) {
        permissions.value = Event(requestedPermissions)
    }

    fun observePermissions(owner: LifecycleOwner, handle: (permissions: List<String>) -> Unit) {
        permissions.observe(owner, EventObserver { handle(it) })
    }

}

class Event<out E>(private val content: E) {
    var hasBeenHandled = false

    // возвращает контент, который ещё не был обработан, иначе null
    fun getContentIfNotHandled(): E? {
        return if (hasBeenHandled) null
        else {
            hasBeenHandled = true
            content
        }
    }

    fun peekContent(): E = content
}

class EventObserver<E>(private val onEventUnhandledContent: (E) -> Unit) : Observer<Event<E>> {
    // в качестве аргумента принимает лямбда-выражение обработчик в которую передаётся необработанное
    // ранее событие получаемое в реализации метода Observer'а onChanged
    override fun onChanged(event: Event<E>?) {
        // если есть необработанное событие (контент),
        // передай в качестве аргумента в лямбду onEventUnhandledContent
        event?.getContentIfNotHandled()?.let {
            onEventUnhandledContent(it)
        }
    }
}

// концепция sealed классов в kotlin очень похожа на enum, только они могут сохранять внутри себя какое-то состояине,
// то есть хранить внутри экземпляры, поэтому их удобно применять в when конструкции
sealed class Notify() {
    // data class'ы не оддерживают наследование, могут только реализовывать интерфейсы,
    // исключение - если они являются подклассами sealed класса

    abstract val message: String

    data class TextMessage(override val message: String) : Notify()

    data class ActionMessage(
        override val message: String,
        val actionLabel: String,
        val actionHandler: (() -> Unit)
    ) : Notify()

    data class ActionMessageWithFlag(
        override val message: String,
        val actionLabel: String,
        val actionHandler: ((Boolean) -> Unit),
        val flag: Boolean
    ) : Notify()

    data class ErrorMessage(
        override val message: String,
        val errLabel: String? = null,
        val errHandler: (() -> Unit)? = null
    ) : Notify()
}

sealed class NavigationCommand() {
    data class To(
        val destination: Int,
        val args: Bundle? = null,
        val options: NavOptions? = null,
        val extras: Navigator.Extras? = null
    ) : NavigationCommand()

    data class StartLogin(
        val privateDestination: Int? = null
    ) : NavigationCommand()

    data class FinishLogin(
        val privateDestination: Int? = null
    ) : NavigationCommand()

    object Logout: NavigationCommand()

}

enum class Loading {
    SHOW_LOADING, SHOW_BLOCKING_LOADING, HIDE_LOADING
}