package ru.skillbranch.skillarticles.di.modules

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import ru.skillbranch.skillarticles.App
import ru.skillbranch.skillarticles.data.local.AppDb
import ru.skillbranch.skillarticles.data.local.dao.*
import javax.inject.Singleton

// модуль провайдит базу данных и необходимые дао
// 14: 02:57:10 для каждого provide метода поставить аннотацию @Singleton

@InstallIn(ApplicationComponent::class)
@Module
class DbModule {
    @Provides
    @Singleton
    fun provideAppDb(@ApplicationContext context: Context): AppDb = Room.databaseBuilder(
        context,
        AppDb::class.java,
        AppDb.DATABASE_NAME
    ).build()
    /*init {
        // use instead of changing database version when no need to save data
        App.applicationContext().deleteDatabase(AppDb.DATABASE_NAME)
    }*/

    @Provides
    @Singleton
    fun provideArticlesDao(db: AppDb): ArticlesDao = db.articlesDao()

    @Provides
    @Singleton
    fun provideArticleCountsDao(db: AppDb): ArticleCountsDao = db.articleCountsDao()

    @Provides
    @Singleton
    fun provideCategoriesDao(db: AppDb): CategoriesDao = db.categoriesDao()

    @Provides
    @Singleton
    fun provideArticlePersonalInfosDao(db: AppDb): ArticlePersonalInfosDao =
        db.articlePersonalInfosDao()

    @Provides
    @Singleton
    fun provideTagsDao(db: AppDb): TagsDao = db.tagsDao()

    @Provides
    @Singleton
    fun provideArticleContentsDao(db: AppDb): ArticleContentsDao = db.articleContentsDao()
}