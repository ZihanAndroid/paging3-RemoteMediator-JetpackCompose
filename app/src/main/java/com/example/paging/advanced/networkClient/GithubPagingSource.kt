package com.example.paging.advanced.networkClient

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.paging.advanced.DefaultConfiguration
import com.example.paging.advanced.data.Repo

class GithubPagingSource(
    private val service: GithubService,
    private val query: String
) : PagingSource<Int, Repo>() {
    // From https://blog.ah.technology/paging-3s-placeholders-and-jumping-features-805344191df
    // support jumping of paging
    override val jumpingSupported: Boolean
        get() = true

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Repo> {
        // each loaded page is cached by Pager, and identified by a key, like params.key
        val position = params.key ?: DefaultConfiguration.GITHUB_STARTING_PAGE_INDEX
        val apiQuery = query + DefaultConfiguration.IN_QUALIFIER

        return try {
            // By default, the initial "params.loadSize" is 3 * pageSize defined in PagingConfig of Pager
            val response = service.searchRepos(apiQuery, position, params.loadSize)
            val repos = response.items
            val nextKey = if (repos.isEmpty()) {
                null
            } else {
                // initial load size = 3 * NETWORK_PAGE_SIZE
                // ensure we're not requesting duplicating items, at the 2nd request
                position + (params.loadSize / DefaultConfiguration.NETWORK_PAGE_SIZE)
            }
            LoadResult.Page(
                // put a List<Repo> as the data of a page, the PagingData is just a wrapper to the LoadResult.Page
                data = repos,
                prevKey = if (position == DefaultConfiguration.GITHUB_STARTING_PAGE_INDEX) null else position - 1,
                nextKey = nextKey
            )
        } catch (e: Throwable) {
            Log.e("GithubRemoteMediator", e.stackTraceToString())
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Repo>): Int? {
        // Note that for jumping, if anchorPosition > maximum cached item position,
        // state.closestPageToPosition() only returns the last page, not desired for jumping
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
//        val anchorPosition = state.anchorPosition ?: return null
//        return state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
//                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
//                    ?.let {computedPagePosition ->
//                        if(computedPagePosition * DefaultConfiguration.NETWORK_PAGE_SIZE <= anchorPosition) anchorPosition / DefaultConfiguration.NETWORK_PAGE_SIZE
//                        else computedPagePosition
//                    }
    }
}