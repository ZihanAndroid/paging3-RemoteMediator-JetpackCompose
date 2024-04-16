package com.example.paging.advanced

import android.content.Context
import androidx.room.Room
import com.example.paging.advanced.db.RemoteKeyDao
import com.example.paging.advanced.db.RepoDAO
import com.example.paging.advanced.db.RepoDatabase
import com.example.paging.advanced.networkClient.GithubService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

object DefaultConfiguration {
    const val BASE_URL = "https://api.github.com/"
    const val NETWORK_PAGE_SIZE = 30
    const val GITHUB_STARTING_PAGE_INDEX = 1
    const val DEFAULT_QUERY = "Android"
    const val IN_QUALIFIER = " in:name,description"
    const val USE_ROOM_FOR_CACHE = true
}

@Module
@InstallIn(SingletonComponent::class)
object APIModule {
    // Retrofit
    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor =
        //HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }

    @Provides
    @Singleton
    fun provideOKHttpClient(
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient
    ): Retrofit = Retrofit.Builder()
        .baseUrl(DefaultConfiguration.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()

    @Provides
    @Singleton
    fun provideGithubService(
        retrofit: Retrofit
    ): GithubService = retrofit.create(GithubService::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
object DataBaseModule{
    @Provides
    @Singleton
    fun provideRepoDatabase(
        @ApplicationContext context: Context
    ): RepoDatabase = Room.databaseBuilder(
        context = context,
        klass = RepoDatabase::class.java,
        name = "repos-db"
    ).fallbackToDestructiveMigration()
        .build()

    @Provides
    @Singleton
    fun provideRepoDao(
        repoDatabase: RepoDatabase
    ): RepoDAO = repoDatabase.getRepoDao()

    @Provides
    @Singleton
    fun provideRemoteKeyDao(
        repoDatabase: RepoDatabase
    ): RemoteKeyDao = repoDatabase.getRemoteKeyDao()
}
