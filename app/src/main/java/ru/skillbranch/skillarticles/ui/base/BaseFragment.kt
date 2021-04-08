package ru.skillbranch.skillarticles.ui.base

import android.os.Bundle
import android.view.*
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.android.synthetic.main.activity_root.*
import ru.skillbranch.skillarticles.ui.RootActivity
import ru.skillbranch.skillarticles.viewmodels.base.BaseViewModel
import ru.skillbranch.skillarticles.viewmodels.base.IViewModelState
import ru.skillbranch.skillarticles.viewmodels.base.Loading
import javax.inject.Inject

abstract class BaseFragment<T : BaseViewModel<out IViewModelState>> : Fragment() {

    @Inject
    lateinit var root: RootActivity

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    abstract val viewModel: T
    protected abstract val layout: Int

    open val binding: Binding? = null

    open val prepareToolbar: (ToolbarBuilder.() -> Unit)? = null
    open val prepareBottombar: (BottombarBuilder.() -> Unit)? = null

    val toolbar: MaterialToolbar
        get() = root.toolbar

    // set listeners, tuning views
    abstract fun setupViews()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(layout, container, false)


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // restore state
        viewModel.restoreState()
        binding?.restoreUi(savedInstanceState)

        // owner it is view
        viewModel.observeState(viewLifecycleOwner) { binding?.bind(it) }
        // bind default values if viewmodel not loaded data
        if (binding?.isInflated == false) binding?.onFinishInflate()

        viewModel.observeNotifications(viewLifecycleOwner) { root.renderNotification(it) }
        viewModel.observeNavigation(viewLifecycleOwner) { root.viewModel.navigate(it) }
        viewModel.observeLoading(viewLifecycleOwner) { renderLoading(it) }

    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        // 12: 53:40
        // prepare toolbar & bottombar, setupViews() перенесли сюда из onViewCreated
        // потому что с новой версией activity и fragment result немного изменились вызовы callback'ов:
        // метод onViewCreated теперь вызывается раньше, чем onCreate активити
        // и получится обращение к ещё не существующим вью
        // onViewStateRestored будет вызван точно тогда, когда lifecycle находится в onResumed стэйте

        // prepare toolbar
        root.toolbarBuilder
            .invalidate()
            .prepare(prepareToolbar)
            .build(root)

        // prepare bottombar
        root.bottombarBuilder
            .invalidate()
            .prepare(prepareBottombar)
            .build(root)

        setupViews()

        binding?.rebind()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        viewModel.saveState()
        binding?.saveUi(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        if (root.toolbarBuilder.items.isNotEmpty()) {
            for ((index, menuHolder) in root.toolbarBuilder.items.withIndex()) {
                val item = menu.add(0, menuHolder.menuId, index, menuHolder.title)
                item.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS or MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW)
                    .setIcon(menuHolder.icon)
                    .setOnMenuItemClickListener {
                        menuHolder.clickListener?.invoke(it)?.let { true } ?: false
                    }

                if (menuHolder.actionViewLayout != null) item.setActionView(menuHolder.actionViewLayout)
            }
        } else menu.clear()
        super.onPrepareOptionsMenu(menu)
    }

    // open, потому что в конкретном фрагменте поведение загрузчика может отличаться
    open fun renderLoading(loadingState: Loading) {
        root.renderLoading(loadingState)
    }

}