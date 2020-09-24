package ru.skillbranch.skillarticles.viewmodels.auth

import androidx.lifecycle.SavedStateHandle
import ru.skillbranch.skillarticles.data.local.PrefManager
import ru.skillbranch.skillarticles.viewmodels.base.BaseViewModel
import ru.skillbranch.skillarticles.viewmodels.base.IViewModelState
import ru.skillbranch.skillarticles.viewmodels.base.NavigationCommand

class AuthViewModel(handle: SavedStateHandle) : BaseViewModel<AuthState>(handle, AuthState()) {
    private val preferences = PrefManager
    init {
        subscribeOnDataSource(preferences.isAuth()) { isAuth, state ->
            state.copy(isAuth = isAuth)
        }
    }

    fun handleLogin(login: String, pass: String, dest: Int?) {
        preferences.setAuth(true)
        navigate(NavigationCommand.FinishLogin(dest))
    }
}

data class AuthState(val isAuth: Boolean = false): IViewModelState