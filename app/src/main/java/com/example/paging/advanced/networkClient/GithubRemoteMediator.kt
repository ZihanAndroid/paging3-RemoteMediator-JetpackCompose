package com.example.paging.advanced.networkClient

import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.example.paging.advanced.DefaultConfiguration
import com.example.paging.advanced.data.Repo
import com.example.paging.advanced.db.RemoteKey
import com.example.paging.advanced.db.RemoteKeyDao
import com.example.paging.advanced.db.RepoDAO
import com.example.paging.advanced.db.RepoDatabase

// The Paging library uses the database as a source of truth for the data that needs to be displayed in the UI.
// Whenever we don't have any more data in the database, we need to request more from the network.
// This class (GithubRemoteMediator) will be recreated for every new query
@OptIn(ExperimentalPagingApi::class)

class GithubRemoteMediator(
    private val query: String,  // query is unknown, not the target of dependency injection
    // Note you cannot use dependency injection in GithubRemoteMediator unless you annotate GithubRemoteMediator with @EntryPoint and @ @InstallIn(SingletonComponent::class)
    // From https://developer.android.com/training/dependency-injection/hilt-android#not-supported
    // But it requires extra boilerplate code which is annoying, so pass parameters here
    private val service: GithubService,
    private val repoDatabase: RepoDatabase
) : RemoteMediator<Int, Repo>() {
    //    @Inject
//    lateinit var service: GithubService
//    @Inject
//    lateinit var repoDatabase: RepoDatabase
//    @Inject
//    lateinit var remoteKeyDao: RemoteKeyDao
//    @Inject
//    lateinit var repoDAO: RepoDAO
    private val repoDAO: RepoDAO = repoDatabase.getRepoDao()
    private val remoteKeyDao: RemoteKeyDao = repoDatabase.getRemoteKeyDao()

    /* override suspend fun initialize(): InitializeAction {
        val cacheTimeout = TimeUnit.HOURS.convert(1, TimeUnit.MILLISECONDS)
        return if (System.currentTimeMillis() - userDao.lastUpdated() >= cacheTimeout) {
            // Cached data is up-to-date, so there is no need to re-fetch from network.
            InitializeAction.SKIP_INITIAL_REFRESH
        } else {
            // Need to refresh cached data from network; returning LAUNCH_INITIAL_REFRESH here
            // will also block RemoteMediator's APPEND and PREPEND from running until REFRESH succeeds.
            InitializeAction.LAUNCH_INITIAL_REFRESH
        }
    } */

    private suspend fun getRemoteKeyForFirstItem(state: PagingState<Int, Repo>): RemoteKey? {
        return state.firstItemOrNull()?.let {
            remoteKeyDao.selectKeyByRepoId(it.id)
        }
    }

    private suspend fun getRemoteKeyForLastItem(state: PagingState<Int, Repo>): RemoteKey? {
        return state.lastItemOrNull()?.let {
            remoteKeyDao.selectKeyByRepoId(it.id)
        }
    }

    private suspend fun getRemoteKeyToCurrentItem(state: PagingState<Int, Repo>): RemoteKey? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestItemToPosition(anchorPosition)?.let {
                remoteKeyDao.selectKeyByRepoId(it.id)
            }
        }
    }

    // https://developer.android.com/topic/libraries/architecture/paging/v3-network-db
    // The load() method is responsible for updating the backing dataset and invalidating the PagingSource.
    override suspend fun load(loadType: LoadType, state: PagingState<Int, Repo>): MediatorResult {
        Log.i("GithubRemoteMediator.load()", "PagingState: ${state.anchorPosition}")
        // From https://developer.android.com/topic/libraries/architecture/paging/v3-network-db#add-rk-table
        // When remote keys are not directly associated with list items, it is best to store them in a separate table in the local database.
        val pageKey = when (loadType) {
            LoadType.REFRESH -> {
                // if you do not support page jumping, just return null to request pages from the beginning
                val remoteKey = getRemoteKeyToCurrentItem(state)
                remoteKey?.nextKey?.minus(1) ?: (remoteKey?.prevKey?.plus(1)
                    ?: DefaultConfiguration.GITHUB_STARTING_PAGE_INDEX)
            }

            LoadType.PREPEND -> {
                val remoteKey = getRemoteKeyForFirstItem(state)
                remoteKey?.prevKey ?: return MediatorResult.Success(endOfPaginationReached = remoteKey != null)
            }

            LoadType.APPEND -> {
                val remoteKey = getRemoteKeyForLastItem(state)
                // If remoteKeys is null, that means the refresh result is not in the database yet.
                // We can return Success with endOfPaginationReached = false
                // because Paging will call this method again if RemoteKeys becomes non-null.
                remoteKey?.nextKey ?: return MediatorResult.Success(endOfPaginationReached = remoteKey != null)
            }
        }
        val apiQuery = query + DefaultConfiguration.IN_QUALIFIER

        return try {
            val apiResponse = service.searchRepos(
                apiQuery,
                pageKey,
                state.config.pageSize
            )
            val repos = apiResponse.items
            // cache pages to database
            repoDatabase.withTransaction {
                if (loadType == LoadType.REFRESH) {
                    // every time you start a new query, delete all the record in Room DB
                    repoDAO.deleteAllRepos()
                    remoteKeyDao.deleteAllKeys()
                }
                val prevKey = if (pageKey == DefaultConfiguration.GITHUB_STARTING_PAGE_INDEX) null else pageKey
                val nextKey = if (repos.isEmpty()) null else pageKey + 1
                // In fact, here when the end of pagination is reached, no RemoteKey is inserted,
                // so in remote_keys table, the "nextKey" of the last RemoteKey record are always not null
                // But it does not matter, because we will always get empty list for a "nextKey" that bigger than the actual last key.
                // And Finally we return "MediatorResult.Success(endOfPaginationReached = repos.isEmpty())"
                // in which the "repos.isEmpty()" is checked for endOfPaginationReached
                // => We do not need to update the database and set the "nextKey" to null manually,
                //    Here, we just send one more request to confirm the repos.isEmpty()
                val keys = repos.map { repo ->
                    // for each Repo(NOT page), storing a RemoteKey
                    RemoteKey(
                        repoId = repo.id,
                        prevKey = prevKey,
                        nextKey = nextKey,
                    )
                }
                // Modifying the Room database invalidates the current PagingData, allowing Paging to present the updates in the DB.
                repoDAO.insertRepos(*repos.toTypedArray())
                remoteKeyDao.insertKeys(keys)
            }
            Log.i("GithubRemoteMediator", "repos.isEmpty(): ${repos.isEmpty()}")
            MediatorResult.Success(endOfPaginationReached = repos.isEmpty())
        } catch (e: Throwable) {
            // Whenever you come across an exception, log it
            Log.e("GithubRemoteMediator", e.stackTraceToString())
            MediatorResult.Error(e)
        }
    }
}