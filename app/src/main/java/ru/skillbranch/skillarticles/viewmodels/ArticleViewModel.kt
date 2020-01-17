package ru.skillbranch.skillarticles.viewmodels

import androidx.lifecycle.LiveData
import ru.skillbranch.skillarticles.data.ArticleData
import ru.skillbranch.skillarticles.data.ArticlePersonalInfo
import ru.skillbranch.skillarticles.data.repositories.ArticleRepository
import ru.skillbranch.skillarticles.extensions.data.toAppSettings
import ru.skillbranch.skillarticles.extensions.data.toArticlePersonalInfo
import ru.skillbranch.skillarticles.extensions.format

class ArticleViewModel(private val articleId: String) :
    BaseViewModel<ArticleState>(ArticleState()), IArticleViewModel {

    private val repository = ArticleRepository

    init {
        // subscribe to mutable data

        subscribeOnDataSource(getArticleData()) { article, state ->
            article ?: return@subscribeOnDataSource null
            state.copy(
                shareLink = article.shareLink,
                title = article.title,
                category = article.category,
                categoryIcon = article.categoryIcon,
                date = article.date.format(),
                author = article.author
            )
        }

        subscribeOnDataSource(getArticleContent()) { content, state ->
            // В лямбде обычный return не работает, он заставит выйти из функции, в которой лямбда вызвана.
            // Чтобы выйти из лямбды, после return ставят метку - @lambda, указывающую на нужную лямбду

            content ?: return@subscribeOnDataSource null
            state.copy(
                isLoadingContent = false,
                content = content
            )
        }

        subscribeOnDataSource(getArticlePersonalInfo()) { info, state ->
            info ?: return@subscribeOnDataSource null
            state.copy(
                isBookmark = info.isBookmark,
                isLike = info.isLike
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
    }

    // load text from network
    override fun getArticleContent(): LiveData<List<Any>?> {
        return repository.loadArticleContent(articleId)
    }

    // load data from db
    override fun getArticleData(): LiveData<ArticleData?> {
        return repository.getArticle(articleId)
    }

    // load data from db
    override fun getArticlePersonalInfo(): LiveData<ArticlePersonalInfo?> {
        return repository.loadArticlePersonalInfo(articleId)
    }

    override fun handleUpText() {
        val settings = currentState.toAppSettings()
        repository.updateSettings(settings.copy(isBigText = true))
    }

    override fun handleDownText() {
        val settings = currentState.toAppSettings()
        repository.updateSettings(settings.copy(isBigText = false))
    }

    override fun handleNightMode() {
        val settings = currentState.toAppSettings()
        repository.updateSettings(settings.copy(isDarkMode = settings.isDarkMode.not()))
    }

    override fun handleLike() {
        val toggleLike = {
            val info = currentState.toArticlePersonalInfo()
            repository.updateArticlePersonalInfo(info.copy(isLike = info.isLike.not()))
        }

        toggleLike()

        val msg = if (currentState.isLike) Notify.TextMessage("Mark is liked")
        else {
            Notify.ActionMessage(
                "Don`t like it anymore",
                "No, still like it",
                toggleLike
            )
        }

        notify(msg)
    }

    override fun handleBookmark() {
        val toggleBookmark = {
            val info = currentState.toArticlePersonalInfo()
            repository.updateArticlePersonalInfo(info.copy(isBookmark = info.isBookmark.not()))
        }

        toggleBookmark()

        val msg = if (currentState.isBookmark) Notify.TextMessage("Add to bookmarks")
        else Notify.TextMessage("Remove from bookmarks")

        notify(msg)
    }

    override fun handleShare() {
        val msg = "Share is not implemented"
        notify(Notify.ErrorMessage(msg, "OK", null))
    }

    override fun handleToggleMenu() {
        updateState { it.copy(isShowMenu = it.isShowMenu.not()) }
    }

    override fun handleSearchMode(isSearch: Boolean) {
        if (isSearch == currentState.isSearch) return
        updateState { it.copy(isSearch = isSearch) }
    }

    override fun handleSearch(query: String?) {
        updateState { it.copy(searchQuery = query) }
    }

}

data class ArticleState(
    val isAuth: Boolean = false, // пользователь авторизован
    val isLoadingContent: Boolean = true,
    val isLoadingReviews: Boolean = true,
    val isLike: Boolean = false,
    val isBookmark: Boolean = false,
    val isShowMenu: Boolean = false,
    val isBigText: Boolean = false,
    val isDarkMode: Boolean = false,
    val isSearch: Boolean = false,
    val searchQuery: String? = null,
    val searchResults: List<Pair<Int, Int>> = emptyList(),
    val searchPosition: Int = 0,
    val shareLink: String? = null,
    val title: String? = null,
    val category: String? = null,
    val categoryIcon: Any? = null,
    val date: String? = null,
    val author: Any? = null,
    val poster: String? = null,
    val content: List<Any> = emptyList(), // контент
    val reviews: List<Any> = emptyList() // комментарии
)