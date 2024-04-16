package com.example.paging.advanced.repository

import androidx.paging.*
import com.example.paging.advanced.DefaultConfiguration
import com.example.paging.advanced.data.Repo
import com.example.paging.advanced.db.RepoDAO
import com.example.paging.advanced.db.RepoDatabase
import com.example.paging.advanced.networkClient.GithubPagingSource
import com.example.paging.advanced.networkClient.GithubRemoteMediator
import com.example.paging.advanced.networkClient.GithubService
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GithubRepository @Inject constructor(
    private val githubService: GithubService,
    private val repoDatabase: RepoDatabase,
    private val repoDAO: RepoDAO,
) {
    private lateinit var currentPagingSource: PagingSource<Int, Repo>
    //private var currentQuery: String = DefaultConfiguration.DEFAULT_QUERY   // default value should be read from DataStore

    private fun pagingSourceFactoryForQuery(query: String): PagingSource<Int, Repo> {
        currentPagingSource = if(DefaultConfiguration.USE_ROOM_FOR_CACHE){
            // appending '%' so we can allow other characters to be before and after the query string
            val dbQuery = "%${query.replace(' ', '%')}%"
            repoDAO.reposByName(dbQuery)
        }else GithubPagingSource(githubService, query)
        return currentPagingSource
    }
    // for refresh
    fun invalidatePagingSource() {
        currentPagingSource.invalidate()
    }
//    fun triggerNewQuery(query: String){
//        if(query.isNotEmpty() && query != currentQuery){
//            currentQuery = query
//            // invalidate PagingSource to make the new query is read by pagingSourceFactoryForQuery() to create a new query
//            // currentQuery is used by pagingSourceFactoryForQuery() to control the content of loaded DataSource
//            invalidatePagingSource()
//        }
//    }

    // The pager flow is for certain query and the results of that query
    // the Pager.flow returns a cold flow, check the source code,
    // and we can see that the returned flow is created by "simpleChannelFlow()" which calls "flow()" to create a cold flow
    @OptIn(ExperimentalPagingApi::class)
    fun getGithubPagerFlow(query: String, initialKey: Int? = null): Flow<PagingData<Repo>> = Pager(
        initialKey = initialKey,
        config = PagingConfig(
            pageSize = DefaultConfiguration.NETWORK_PAGE_SIZE,
            // From https://blog.ah.technology/paging-3s-placeholders-and-jumping-features-805344191df
            // when you access lazyPagingItems[index] in composable UI, (val lazyPagingItems = pagingDataFlow.collectAsLazyPagingItems())
            // if enablePlaceholders = true, you will get a null and you can use another composable to show the null item like:
            // inside a LazyColumn, items{...}
            //      val item = lazyPagingItems[index]
            //      if (item != null) {
            //          ActualItem(item)
            //      } else {
            //          PlaceholderItem()
            //      }
            enablePlaceholders = true,
            maxSize = DefaultConfiguration.NETWORK_PAGE_SIZE * 8,
            // Whenever a jump happens, Paging will invalidate itself and start loading from scratch based on the key we return from getRefreshKey()
            // ??? to support jumping, you may add some buttons showing pages, and when one of them is click, do the same thing as scrollTo callback does to trigger jumping of paging
            jumpThreshold = DefaultConfiguration.NETWORK_PAGE_SIZE * 5
        ),
        pagingSourceFactory = { pagingSourceFactoryForQuery(query) },
        // Do NOT forget to add RemoteMediator to Pager
        remoteMediator = if(DefaultConfiguration.USE_ROOM_FOR_CACHE) GithubRemoteMediator(query, githubService, repoDatabase) else null
    ).flow
}