package ru.skillbranch.skillarticles.ui.auth

import androidx.annotation.VisibleForTesting
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import androidx.savedstate.SavedStateRegistryOwner
import kotlinx.android.synthetic.main.fragment_registration.*
import ru.skillbranch.skillarticles.R
import ru.skillbranch.skillarticles.extensions.getTrimmedString
import ru.skillbranch.skillarticles.ui.RootActivity
import ru.skillbranch.skillarticles.ui.base.BaseFragment
import ru.skillbranch.skillarticles.viewmodels.auth.AuthViewModel

class RegistrationFragment() : BaseFragment<AuthViewModel>() {

    // for testing
    var _mockFactory: ((SavedStateRegistryOwner) -> ViewModelProvider.Factory)? = null

    override val viewModel: AuthViewModel by viewModels {
        _mockFactory?.invoke(this) ?: defaultViewModelProviderFactory
    }
    override val layout: Int = R.layout.fragment_registration

    // testing constructors
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    constructor(
        mockRoot: RootActivity,
        mockFactory: ((SavedStateRegistryOwner) -> ViewModelProvider.Factory)? = null
    ) : this() {
        _mockRoot = mockRoot
        _mockFactory = mockFactory
    }

    private val args: RegistrationFragmentArgs by navArgs()

    override fun setupViews() {

        btn_register.setOnClickListener {
            viewModel.handleRegister(
                et_name.getTrimmedString(),
                et_login.getTrimmedString(),
                et_password.getTrimmedString(),
                if (args.privateDestination == -1) null else args.privateDestination
                // requireContext()
            )
        }

        et_name.doAfterTextChanged {
            if (!viewModel.isNameValid(it.toString())) {
                wrap_name.error = getString(R.string.reg_error_invalid_name)
            } else wrap_name.error = null
        }

        et_login.doAfterTextChanged {
            if (!viewModel.isEmailValid(it.toString())) {
                wrap_login.error = getString(R.string.reg_error_invalid_email)
            } else wrap_login.error = null
        }

        et_password.doAfterTextChanged {
            if (!viewModel.isPasswordValid(it.toString())) {
                wrap_password.error = getString(R.string.reg_error_invalid_password)
            } else wrap_password.error = null
        }

        et_confirm.doAfterTextChanged {
            if (et_confirm.text.toString() != et_password.text.toString()) {
                wrap_confirm.error = getString(R.string.reg_error_mismatching_passwords)
            } else wrap_confirm.error = null
        }

    }
}