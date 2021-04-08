package ru.skillbranch.skillarticles.viewmodels.auth

import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.SavedStateHandle
import ru.skillbranch.skillarticles.data.repositories.RootRepository
import ru.skillbranch.skillarticles.viewmodels.base.BaseViewModel
import ru.skillbranch.skillarticles.viewmodels.base.IViewModelState
import ru.skillbranch.skillarticles.viewmodels.base.NavigationCommand
import ru.skillbranch.skillarticles.viewmodels.base.Notify

class AuthViewModel @ViewModelInject constructor(
    @Assisted handle: SavedStateHandle,
    private val repository: RootRepository
) : BaseViewModel<AuthState>(handle, AuthState()) {

    // "The name must be at least 3 characters long and contain
    // only letters and numbers and can also contain the characters "-" and "_"
    private val validNameRegex = Regex("^[\\w\\d-_]{3,}$")

    // Password must be at least 8 characters long and contain only letters and numbers
    private val validPasswordRegex = Regex("^[\\w\\d]{8,}$")

    init {
        subscribeOnDataSource(repository.isAuth()) { isAuth, state ->
            state.copy(isAuth = isAuth)
        }
    }

    fun handleLogin(login: String, pass: String, dest: Int?) {
        launchSafety {
            repository.login(login, pass)
            navigate(NavigationCommand.FinishLogin(dest))
        }
    }

    fun handleRegister(
        name: String,
        login: String,
        password: String,
        dest: Int?
        // context: Context
    ) {
        if (name.isEmpty() || login.isEmpty() || password.isEmpty()) {
            // showErrorMessage(context.getString(R.string.reg_error_empty_field))
            showErrorMessage("Name, login, password it is required fields and not must be empty")
            return
        }

        if (!isNameValid((name))) {
            // showErrorMessage(context.getString(R.string.reg_error_invalid_name))
            showErrorMessage("The name must be at least 3 characters long and contain only letters, digits, and the \" - \" and \"_ \" symbols")
            return
        }

        if (!isEmailValid((login))) {
            // showErrorMessage(context.getString(R.string.reg_error_invalid_email))
            showErrorMessage("Incorrect email entered")
            return
        }

        if (!isPasswordValid(password)) {
            // showErrorMessage(context.getString(R.string.reg_error_invalid_password))
            showErrorMessage("The password must be at least 8 characters long and contain only letters and digits")
            return
        }

        launchSafety {
            repository.register(name, login, password)
            navigate(NavigationCommand.FinishLogin(dest))
        }
    }

    fun isNameValid(name: String): Boolean =
        name.isNotEmpty() && name.matches(validNameRegex)

    fun isEmailValid(email: String): Boolean =
        email.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()

    fun isPasswordValid(password: String): Boolean =
        password.isNotEmpty() && password.matches(validPasswordRegex)

    private fun showErrorMessage(msg: String) = notify(Notify.ErrorMessage(msg))

}

data class AuthState(val isAuth: Boolean = false) : IViewModelState