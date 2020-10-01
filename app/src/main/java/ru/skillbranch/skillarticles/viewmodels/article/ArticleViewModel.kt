package ru.skillbranch.skillarticles.viewmodels.article

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.*
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import kotlinx.coroutines.launch
import ru.skillbranch.skillarticles.data.remote.res.CommentRes
import ru.skillbranch.skillarticles.data.repositories.ArticleRepository
import ru.skillbranch.skillarticles.data.repositories.CommentsDataFactory
import ru.skillbranch.skillarticles.data.repositories.MarkdownElement
import ru.skillbranch.skillarticles.data.repositories.clearContent
import ru.skillbranch.skillarticles.extensions.data.toAppSettings
import ru.skillbranch.skillarticles.extensions.indexesOf
import ru.skillbranch.skillarticles.extensions.shortFormat
import ru.skillbranch.skillarticles.viewmodels.base.BaseViewModel
import ru.skillbranch.skillarticles.viewmodels.base.IViewModelState
import ru.skillbranch.skillarticles.viewmodels.base.NavigationCommand
import ru.skillbranch.skillarticles.viewmodels.base.Notify
import java.util.concurrent.Executors

class ArticleViewModel(
    handle: SavedStateHandle,
    private val articleId: String
) : BaseViewModel<ArticleState>(handle, ArticleState()), IArticleViewModel {

    private val repository = ArticleRepository
    private var clearContent: String? = null
    private val listConfig by lazy {
        PagedList.Config.Builder()
            .setEnablePlaceholders(true)
            .setPageSize(5)
            .build()
    }

    // 8: 01:05:35 подписываемся не на стэйт, а на отдельный метод репозитория - getArticleData(),
    // потому что комментарии не зависят от стэйта
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    val listData: LiveData<PagedList<CommentRes>> =
        Transformations.switchMap(repository.findArticleCommentCount(articleId)) {
            buildPagedList(repository.loadAllComments(articleId, it, ::commentLoadErrorHandler))
        }

    init {
        // subscribe on mutable data
        subscribeOnDataSource(repository.findArticle(articleId)) { article, state ->
            if (article.content == null) fetchContent()
            state.copy(
                shareLink = article.shareLink,
                title = article.title,
                category = article.category.title,
                categoryIcon = article.category.icon,
                date = article.date.shortFormat(),
                author = article.author,
                isBookmark = article.isBookmark,
                isLike = article.isLike,
                content = article.content ?: emptyList(),
                source = article.source,
                hashtags = article.tags,
                isLoadingContent = article.content == null
            )
        }

        // настройки будут тянуться из shared preferences все данные,
        // которые завязаны на android зависимые источники, возвращающие live data,
        // модифицировать не придётся - модификация будет происходить на уровне репозитория
        // ~01:13:20 лекции Архитектура приложения. Coordinator layout
        subscribeOnDataSource(repository.getAppSettings()) { settings, state ->
            state.copy(
                isDarkMode = settings.isDarkMode,
                isBigText = settings.isBigText
            )
        }

        subscribeOnDataSource(repository.isAuth()) { auth, state ->
            state.copy(isAuth = auth)
        }
    }

    fun refresh() {
        launchSafety {
            // этот CoroutineScope принадлежит вью модели
            // 11: 1:43:25 благодаря тому, что используется контекст корутины,
            // мы можем вот так параллельно вызвать несколько запросов
            launch { repository.fetchArticleContent(articleId) }
            launch { repository.refreshCommentsCount(articleId) }
        }
    }

    private fun commentLoadErrorHandler(throwable: Throwable) {
        // handle network errors
    }

    private fun fetchContent() {
        launchSafety {
            repository.fetchArticleContent(articleId)
        }
    }

    // app settings
    override fun handleNightMode() {
        val settings = currentState.toAppSettings()
        repository.updateSettings(settings.copy(isDarkMode = settings.isDarkMode.not()))
    }

    override fun handleUpText() {
        val settings = currentState.toAppSettings()
        repository.updateSettings(settings.copy(isBigText = true))
    }

    override fun handleDownText() {
        val settings = currentState.toAppSettings()
        repository.updateSettings(settings.copy(isBigText = false))
    }

    // personal article info
    override fun handleBookmark() {
        launchSafety {
            val isBookmarked = repository.toggleBookmark(articleId)
            if (isBookmarked) repository.addBookmark(articleId)
            else repository.removeBookmark(articleId)

            val msg = if (isBookmarked) "Add to bookmarks" else "Remove from bookmarks"
            notify(Notify.TextMessage(msg))
        }
    }

    override fun handleLike() {
        launchSafety {
            val isLiked = repository.toggleLike(articleId)
            if (isLiked) repository.incrementLike(articleId)
            else repository.decrementLike(articleId)

            val msg = if (isLiked) Notify.TextMessage("Article marked as liked")
            else Notify.ActionMessage(
                "Don`t like it anymore",
                "No, still like it"
                // handler function, if press "No, still like it" on snackbar, then toggle again
            ) { handleLike() }

            notify(msg)
        }

    }

    override fun handleShare() {
        val msg = "Share is not implemented"
        notify(Notify.ErrorMessage(msg, "OK", null))
    }

    // session state
    override fun handleToggleMenu() {
        updateState { it.copy(isShowMenu = it.isShowMenu.not()) }
    }

    override fun handleSearchMode(isSearch: Boolean) {
        if (isSearch == currentState.isSearch) return
        updateState { it.copy(isSearch = isSearch, isShowMenu = false, searchPosition = 0) }
    }

    // 50 минута мастер-класса
    override fun handleSearch(query: String?) {
        query ?: return

        if (clearContent == null && currentState.content.isNotEmpty())
            clearContent = currentState.content.clearContent()

        val result = clearContent
            .indexesOf(query)
            .map { it to it + query.length }

        val position = with(currentState) {

            if (result.isEmpty()) 0
            else {
                // save the same visual position if the result overlaps with the previous query
                val shifted =
                    if ((searchResults.isEmpty() || searchQuery.isNullOrEmpty() || query.isEmpty())) -1 else {
                        // was 'uer' now 'query', shift = -1
                        val newContainsOld = query.indexOf(searchQuery)
                        // was 'query' now 'uer', shift = 1
                        val oldContainsNew = searchQuery.indexOf(query)

                        when {
                            newContainsOld >= 0 -> {
                                val index = searchResults[searchPosition].first + newContainsOld
                                result.indexOfFirst { it.first == index }
                            }
                            oldContainsNew >= 0 -> {
                                val index = searchResults[searchPosition].first - oldContainsNew
                                result.indexOfFirst { it.first == index }
                            }
                            else -> -1
                        }
                    }

                when {
                    shifted >= 0 -> shifted
                    searchPosition > result.lastIndex -> result.lastIndex
                    else -> searchPosition
                }

            }

        }

        updateState {
            it.copy(
                searchQuery = query,
                searchResults = result,
                searchPosition = position
            )
        }
    }

    override fun handleUpResult() {
        updateState { it.copy(searchPosition = it.searchPosition.dec()) }
    }

    override fun handleDownResult() {
        updateState { it.copy(searchPosition = it.searchPosition.inc()) }
    }

    override fun handleCopyCode() {
        notify(Notify.TextMessage("Code copy to clipboard"))
    }

    override fun handleCommentInput(comment: String) {
        updateState { it.copy(commentText = comment) }
    }

    override fun handleSendComment(comment: String) {
        if (comment.isEmpty()) {
            notify(Notify.TextMessage("Comment must be not empty"))
            return
        }
        updateState { it.copy(commentText = comment) }
        if (!currentState.isAuth) {
            navigate(NavigationCommand.StartLogin())
        } else {
            launchSafety(null, {
                updateState {
                    it.copy(
                        answerTo = null,
                        answerToMessageId = null,
                        commentText = null
                    )
                }
            }) {
                repository.sendMessage(
                    articleId,
                    currentState.commentText!!,
                    currentState.answerToMessageId
                )
            }
        }
    }

    fun observeList(
        owner: LifecycleOwner,
        onChange: (list: PagedList<CommentRes>) -> Unit
    ) {
        listData.observe(owner, Observer { onChange(it) })
    }

    private fun buildPagedList(
        dataFactory: CommentsDataFactory
    ): LiveData<PagedList<CommentRes>> {
        return LivePagedListBuilder<String, CommentRes>(
            dataFactory,
            listConfig
        )
            .setFetchExecutor(Executors.newSingleThreadExecutor())
            .build()
    }

    fun handleCommentFocus(hasFocus: Boolean) {
        updateState { it.copy(showBottomBar = !hasFocus) }
    }

    fun handleClearComment() {
        updateState { it.copy(answerTo = null, answerToMessageId = null, commentText = null) }
    }

    fun handleReplyTo(messageId: String, name: String) {
        updateState { it.copy(answerToMessageId = messageId, answerTo = "Reply to $name") }
    }

}

data class ArticleState(
    val isAuth: Boolean = false, // пользователь авторизован
    val isLoadingContent: Boolean = true, // контент загружается
    val isLoadingReviews: Boolean = true, // отзывы загружаются
    val isLike: Boolean = false,
    val isBookmark: Boolean = false,
    val isShowMenu: Boolean = false,
    val isBigText: Boolean = false, // шрифт увеличен
    val isDarkMode: Boolean = false,
    val isSearch: Boolean = false,
    val searchQuery: String? = null,
    val searchResults: List<Pair<Int, Int>> = emptyList(), // результаты поика (стартовая и конечная позиции)
    val searchPosition: Int = 0, // текущая позиция найденного результата
    val shareLink: String? = null,
    val title: String? = null,
    val category: String? = null,
    val categoryIcon: Any? = null,
    val date: String? = null, // дата публикации
    val author: Any? = null, // автор статьи
    val poster: String? = null, // обложка статьи
    val content: List<MarkdownElement> = emptyList(), // контент
    val commentsCount: Int = 0, // комментарии
    val answerTo: String? = null,
    val answerToMessageId: String? = null,
    val showBottomBar: Boolean = true, // чтобы не показывать боттомбар при написании комментария
    val commentText: String? = null,
    val source: String? = null, // источник контента
    val hashtags: List<String> = emptyList() // хэтеги контента
) : IViewModelState {

    override fun save(outState: SavedStateHandle) {
        // 1:04:30 save state
        // только эти, остальные можно подтянуть из персистентного хранилища
        outState.set("isSearch", isSearch)
        outState.set("searchQuery", searchQuery)
        outState.set("searchResults", searchResults)
        outState.set("searchPosition", searchPosition)
        outState.set("commentsCount", commentsCount)
        outState.set("answerTo", answerTo)
        outState.set("answerToMessageId", answerToMessageId)
    }

    override fun restore(savedState: SavedStateHandle): ArticleState {
        // 8: 1:04:30 restore state
        return copy(
            isSearch = savedState["isSearch"] ?: false,
            searchQuery = savedState["searchQuery"],
            searchResults = savedState["searchResults"] ?: emptyList(),
            searchPosition = savedState["searchPosition"] ?: 0,
            commentsCount = savedState["commentsCount"] ?: 0,
            answerTo = savedState["answerTo"],
            answerToMessageId = savedState["answerToMessageId"]
        )
    }
}