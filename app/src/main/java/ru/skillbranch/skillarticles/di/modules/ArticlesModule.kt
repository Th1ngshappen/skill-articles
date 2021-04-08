package ru.skillbranch.skillarticles.di.modules

import androidx.cursoradapter.widget.CursorAdapter
import androidx.cursoradapter.widget.SimpleCursorAdapter
import androidx.fragment.app.Fragment
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.FragmentComponent
import ru.skillbranch.skillarticles.data.repositories.ArticlesRepository
import ru.skillbranch.skillarticles.data.repositories.IRepository
import ru.skillbranch.skillarticles.ui.articles.ArticlesFragment
import ru.skillbranch.skillarticles.ui.articles.IArticlesView

@InstallIn(FragmentComponent::class)
@Module
abstract class ArticlesModule {
    @Binds
    // 14: 02:58:20 что-то не так с этим методом)
    abstract fun bindArticleRepository(repo: ArticlesRepository): IRepository

    @Binds
    abstract fun bindClickListener(fragment: ArticlesFragment): IArticlesView

    companion object {
        @Provides
        fun provideArticlesFragment(fragment: Fragment): ArticlesFragment =
            fragment as ArticlesFragment // 14: 03:09:00 хилт знает, что фрагмент привязан к fragment компоненту

        @Provides
        fun provideSimpleCursorAdapter(fragment: Fragment): SimpleCursorAdapter =
            SimpleCursorAdapter(
                fragment.context,
                android.R.layout.simple_list_item_1,
                null, // cursor
                arrayOf("tag"), // cursor column for bind on view
                intArrayOf(android.R.id.text1), // text view id for bind data from cursor columns
                CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER
            )
    }
}