package ru.skillbranch.skillarticles.viewmodels.articles

import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import androidx.paging.DataSource
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import kotlinx.coroutines.launch
import ru.skillbranch.skillarticles.data.local.entities.ArticleItem
import ru.skillbranch.skillarticles.data.local.entities.CategoryData
import ru.skillbranch.skillarticles.data.remote.err.NoNetworkError
import ru.skillbranch.skillarticles.data.repositories.ArticleFilter
import ru.skillbranch.skillarticles.data.repositories.ArticlesRepository
import ru.skillbranch.skillarticles.viewmodels.base.BaseViewModel
import ru.skillbranch.skillarticles.viewmodels.base.IViewModelState
import ru.skillbranch.skillarticles.viewmodels.base.Notify
import java.util.concurrent.Executors

class ArticlesViewModel @ViewModelInject constructor(
    @Assisted handle: SavedStateHandle,
    private val repository: ArticlesRepository
) :
    BaseViewModel<ArticlesState>(handle, ArticlesState()) {

    private var isLoadingInitial = false
    private var isLoadingAfter = false
    private val listConfig by lazy {
        PagedList.Config.Builder()
            .setEnablePlaceholders(false)
            .setPageSize(10)
            .setPrefetchDistance(30)
            .setInitialLoadSizeHint(50)
            .build()
    }

    // 8: switchMap - подписываемся на текущий стэйт и в зависимости от его состояния
    // будем переключаться между источниками данных, которые возвращают данные в page list
    private val listData = Transformations.switchMap(state) {
        val filter = it.toArticleFilter()
        return@switchMap buildPagedList(repository.rawQueryArticles(filter))
    }

    fun observeList(
        owner: LifecycleOwner,
        isBookmark: Boolean = false,
        onChange: (list: PagedList<ArticleItem>) -> Unit
    ) {
        updateState { it.copy(isBookmark = isBookmark) }
        listData.observe(owner, Observer { onChange(it) })
    }

    fun observeTags(owner: LifecycleOwner, onChange: (list: List<String>) -> Unit) {
        repository.findTags().observe(owner, Observer(onChange))
    }

    fun observeCategories(owner: LifecycleOwner, onChange: (list: List<CategoryData>) -> Unit) {
        repository.findCategoriesData().observe(owner, Observer(onChange))
    }

    private fun buildPagedList(
        dataFactory: DataSource.Factory<Int, ArticleItem>
    ): LiveData<PagedList<ArticleItem>> {

        val builder = LivePagedListBuilder<Int, ArticleItem>(
            dataFactory,
            listConfig
        )

        if (isEmptyFilter()) {
            builder.setBoundaryCallback(
                ArticlesBoundaryCallback(
                    ::zeroLoadingHandle,
                    ::itemAtEndHandle
                )
            )
        }

        return builder
            .setFetchExecutor(Executors.newSingleThreadExecutor())
            .build()
    }

    private fun isEmptyFilter(): Boolean = currentState.searchQuery.isNullOrEmpty()
            && !currentState.isBookmark
            && currentState.selectedCategories.isEmpty()
            && !currentState.isHashtagSearch

    private fun itemAtEndHandle(lastLoadArticle: ArticleItem) {
        // 11: 01:07:45 если запрос уже отправлен, не дублировать
        if (isLoadingAfter) return
        else isLoadingAfter = true

        launchSafety(null, { isLoadingAfter = false }) {
            // преимущество suspend функций в том, что они не блокируют тот поток, на котором вызываются
            repository.loadArticlesFromNetwork(
                start = lastLoadArticle.id,
                size = listConfig.pageSize
            )
        }
    }

    private fun zeroLoadingHandle() {
        if (isLoadingInitial) return
        else isLoadingInitial = true

        launchSafety(null, { isLoadingInitial = false }) {
            repository.loadArticlesFromNetwork(
                start = null,
                size = listConfig.initialLoadSizeHint
            )
        }
    }

    fun handleSearch(query: String?) {
        query ?: return
        updateState { it.copy(searchQuery = query, isHashtagSearch = query.startsWith('#', true)) }
    }

    fun handleSearchMode(isSearch: Boolean) {
        updateState { it.copy(isSearch = isSearch) }
    }

    fun handleToggleBookmark(articleId: String) {
        launchSafety({
            when (it) {
                is NoNetworkError -> notify(Notify.TextMessage("Network Not Available, failed to fetch article"))
                else -> notify(Notify.ErrorMessage(it.message ?: "Something wrong"))
            }
        }) {
            val isBookmarked = repository.toggleBookmark(articleId)
            // if bookmarked need to fetch content and handle network error
            // когда статья добавляется в закладки, мы её кэшируем и она доступна без подключения к сети
            if (isBookmarked) repository.fetchArticleContent(articleId)
            // else remove article content from db
        }
    }

    fun handleSuggestion(tag: String) {
        viewModelScope.launch { repository.incrementTagUseCount(tag) }
    }

    fun applyCategories(selectedCategories: List<String>) {
        updateState { it.copy(selectedCategories = selectedCategories) }
    }

    fun refresh() {
        launchSafety {
            val lastArticleId: String? = repository.findLastArticleId()
            val count = repository.loadArticlesFromNetwork(
                start = lastArticleId,
                size = if (lastArticleId == null) listConfig.initialLoadSizeHint else -listConfig.pageSize
                // минус, чтобы найти статьи, которые были добавлены *после* lastArticleId
                // т.е. отправляем отрицательный либо положительный LIMIT в запросе
            )
            notify(Notify.TextMessage("Load $count new articles"))
        }
    }

}

private fun ArticlesState.toArticleFilter(): ArticleFilter = ArticleFilter(
    search = searchQuery,
    isBookmark = isBookmark,
    categories = selectedCategories,
    isHashtag = isHashtagSearch
)

data class ArticlesState(
    val isSearch: Boolean = false,
    val searchQuery: String? = null,
    val isLoading: Boolean = true,
    val isBookmark: Boolean = false,
    val selectedCategories: List<String> = emptyList(),
    val isHashtagSearch: Boolean = false
) : IViewModelState

// будет уведомлять о том, что в источнике данных закончились данные или что мы доскроллили до самого низа
class ArticlesBoundaryCallback(
    private val zeroLoadingHandle: () -> Unit,
    private val itemAtEndHandle: (ArticleItem) -> Unit
) : PagedList.BoundaryCallback<ArticleItem>() {

    override fun onZeroItemsLoaded() {
        // storage is empty (вообще нет данных)
        zeroLoadingHandle()
    }

    override fun onItemAtEndLoaded(itemAtEnd: ArticleItem) {
        // user scrolls down -> need to load more items
        itemAtEndHandle(itemAtEnd)
    }
}