package ru.skillbranch.skillarticles.ui.articles

import android.database.Cursor
import android.database.MatrixCursor
import android.os.Bundle
import android.provider.BaseColumns
import android.view.Menu
import android.view.MenuItem
import android.widget.AutoCompleteTextView
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.cursoradapter.widget.SimpleCursorAdapter
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_root.*
import kotlinx.android.synthetic.main.fragment_articles.*
import kotlinx.android.synthetic.main.search_view_layout.view.*
import ru.skillbranch.skillarticles.R
import ru.skillbranch.skillarticles.data.local.entities.ArticleItem
import ru.skillbranch.skillarticles.data.local.entities.CategoryData
import ru.skillbranch.skillarticles.ui.base.BaseFragment
import ru.skillbranch.skillarticles.ui.base.Binding
import ru.skillbranch.skillarticles.ui.base.MenuItemHolder
import ru.skillbranch.skillarticles.ui.base.ToolbarBuilder
import ru.skillbranch.skillarticles.ui.delegates.RenderProp
import ru.skillbranch.skillarticles.ui.dialogs.ChoseCategoryDialog
import ru.skillbranch.skillarticles.viewmodels.articles.ArticlesState
import ru.skillbranch.skillarticles.viewmodels.articles.ArticlesViewModel
import ru.skillbranch.skillarticles.viewmodels.base.IViewModelState
import ru.skillbranch.skillarticles.viewmodels.base.Loading
import javax.inject.Inject

@AndroidEntryPoint
class ArticlesFragment : BaseFragment<ArticlesViewModel>(), IArticlesView {

    override val viewModel: ArticlesViewModel by activityViewModels()
    // 14: привязываемся к activityViewModels, потому что articles - фрагменты верхнего уровня,
    // которые живут всегда в активити, и, когда мы переключаемся через navigation меню,
    // у нас всегда должен быть один и тот же инстанс вью модели

    override val layout: Int = R.layout.fragment_articles
    override val binding: ArticlesBinding by lazy { ArticlesBinding() }
    private val args: ArticlesFragmentArgs by navArgs()

    @Inject
    lateinit var suggestionAdapter: SimpleCursorAdapter

    @Inject
    lateinit var articlesAdapter: ArticlesAdapter

    override val prepareToolbar: (ToolbarBuilder.() -> Unit) = {
        addMenuItem(
            MenuItemHolder(
                "Search",
                R.id.action_search,
                R.drawable.ic_search_black_24dp,
                R.layout.search_view_layout
            )
        )

        addMenuItem(
            MenuItemHolder(
                "Filter",
                R.id.action_filter,
                R.drawable.ic_filter_list_black_24dp
            ) {
                val action = ArticlesFragmentDirections.chooseCategory(
                    binding.selectedCategories.toTypedArray(),
                    binding.categories.toTypedArray()
                )
                viewModel.navigateWithAction(action)
            }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setFragmentResultListener(ChoseCategoryDialog.CHOOSE_CATEGORY_KEY) { _, bundle ->
            @Suppress("UNCHECKED_CAST")
            viewModel.applyCategories(bundle[ChoseCategoryDialog.SELECTED_CATEGORIES] as List<String>)
        }

        suggestionAdapter.setFilterQueryProvider { constraint -> populateAdapter(constraint) }

        setHasOptionsMenu(true)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)

        val menuItem = menu.findItem(R.id.action_search)
        val searchView = menuItem.actionView as SearchView
        if (binding.isSearch) {
            menuItem.expandActionView()
            searchView.setQuery(binding.searchQuery, false)
        }

        val filterMenuItem = menu.findItem(R.id.action_filter)
        val colorRes =
            if (binding.selectedCategories.isEmpty()) R.color.color_on_article_bar else R.color.color_accent
        filterMenuItem.icon.setTint(requireContext().getColor(colorRes))

        val autoTv = searchView.findViewById<AutoCompleteTextView>(R.id.search_src_text)
        autoTv.threshold = 1

        searchView.suggestionsAdapter = suggestionAdapter
        searchView.setOnSuggestionListener(object : SearchView.OnSuggestionListener {
            override fun onSuggestionSelect(position: Int): Boolean = false

            override fun onSuggestionClick(position: Int): Boolean {
                suggestionAdapter.cursor.moveToPosition(position)
                val tag = suggestionAdapter.cursor.getString(1)
                searchView.setQuery(tag, true)
                viewModel.handleSuggestion(tag)
                return false
            }
        })

        menuItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                viewModel.handleSearchMode(true)
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                viewModel.handleSearchMode(false)
                return true
            }
        })

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                viewModel.handleSearch(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.handleSearch(newText)
                return true
            }
        })

        // 8: 52:30 нужно отслеживать закрытие меню,
        // чтобы при возврате со страницы статьи тулбар не считал, что мы вставили пустой запрос
        searchView.setOnCloseListener {
            viewModel.handleSearchMode(false)
            true
        }
    }

    override fun onDestroyView() {
        toolbar.search_view?.setOnQueryTextListener(null)
        super.onDestroyView()
    }

    override fun renderLoading(loadingState: Loading) {
        when (loadingState) {
            Loading.SHOW_LOADING -> if (!refresh.isRefreshing) root.progress.isVisible = true
            Loading.SHOW_BLOCKING_LOADING -> root.progress.isVisible = false
            Loading.HIDE_LOADING -> {
                root.progress.isVisible = false
                if (refresh.isRefreshing) refresh.isRefreshing = false
            }
        }
    }

    override fun setupViews() {
        with(rv_articles) {
            layoutManager = LinearLayoutManager(context)
            adapter = articlesAdapter
            addItemDecoration(DividerItemDecoration(context, LinearLayoutManager.VERTICAL))
        }

        viewModel.observeList(viewLifecycleOwner, args.isBookmarks) {
            articlesAdapter.submitList(it)
        }

        viewModel.observeTags(viewLifecycleOwner) {
            binding.tags = it
        }

        viewModel.observeCategories(viewLifecycleOwner) {
            binding.categories = it
        }

        refresh.setOnRefreshListener {
            viewModel.refresh()
        }
    }

    private fun populateAdapter(constraint: CharSequence?): Cursor {
        val cursor = MatrixCursor(
            arrayOf(
                BaseColumns._ID,
                "tag"
            )
        ) // create cursor for table with 2 colum _id, tag
        constraint ?: return cursor

        val currentCursor = suggestionAdapter.cursor
        currentCursor.moveToFirst()

        for (i in 0 until currentCursor.count) {
            val tagValue = currentCursor.getString(1) // 2 column with name tag
            if (tagValue.contains(constraint, true)) cursor.addRow(arrayOf<Any>(i, tagValue))
            currentCursor.moveToNext()
        }
        return cursor
    }

    inner class ArticlesBinding : Binding() {
        var categories: List<CategoryData> = emptyList()
        var selectedCategories: List<String> by RenderProp(emptyList()) {
            val filterMenuItem = toolbar.menu.findItem(R.id.action_filter) ?: return@RenderProp
            val colorRes = if (it.isEmpty()) R.color.color_on_article_bar else R.color.color_accent
            filterMenuItem.icon.setTint(requireContext().getColor(colorRes))
        }
        var searchQuery: String? = null
        var isSearch: Boolean = false
        var isLoading: Boolean by RenderProp(true) {
            // TODO show shimmer on rv_list
        }

        var isHashtagSearch: Boolean by RenderProp(false)
        var tags: List<String> by RenderProp(emptyList())

        override fun bind(data: IViewModelState) {
            data as ArticlesState
            isSearch = data.isSearch
            searchQuery = data.searchQuery
            isLoading = data.isLoading
            isHashtagSearch = data.isHashtagSearch
            selectedCategories = data.selectedCategories
        }

        override val afterInflated: (() -> Unit)? = {
            dependsOn<Boolean, List<String>>(::isHashtagSearch, ::tags) { ihs, tags ->
                val cursor = MatrixCursor(
                    arrayOf(
                        BaseColumns._ID,
                        "tag"
                    )
                )

                if (ihs && tags.isNotEmpty()) {
                    for ((counter, tag) in tags.withIndex()) {
                        cursor.addRow(arrayOf<Any>(counter, tag))
                    }
                }

                suggestionAdapter.changeCursor(cursor)
            }
        }

        // 8: 47:20 TODO сохранение UI элементов нужно реализовать самостоятельно
        // через bundle (methods saveUI & restoreUi) и то же с вью моделью
    }

    override fun clickArticle(item: ArticleItem, isToggleBookmark: Boolean) {
        if (isToggleBookmark) {
            viewModel.handleToggleBookmark(item.id)
        } else {
            val action = ArticlesFragmentDirections.actionToPageArticle(
                item.id,
                item.author,
                item.authorAvatar!!,
                item.category,
                item.categoryIcon,
                item.date,
                item.poster,
                item.title
            )

            viewModel.navigateWithAction(action)
        }
    }

}

interface IArticlesView {
    fun clickArticle(item: ArticleItem, isBookmarked: Boolean)
}