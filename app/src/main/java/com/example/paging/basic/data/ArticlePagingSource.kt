package com.example.paging.basic.data

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import kotlin.math.max

// From https://developer.android.com/codelabs/android-paging-basics#4

private val firstArticleCreatedTime = LocalDateTime.now()
private const val STARTING_KEY = 0
private var refreshTimes = 0

class ArticlePagingSource : PagingSource<Int, Article>(){
    init {
        refreshTimes++;
    }
    private fun ensureMinValidKey(key: Int) = max(STARTING_KEY, key)

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Article> {
        // at first, the params.key is null
        val start = params.key ?: STARTING_KEY
        // loadSize is determined by PagingConfig.pageSize
        // val range = start until start+params.loadSize
        // change the data after each invalidation
        val range = start* refreshTimes until (start+params.loadSize)* refreshTimes step refreshTimes
        Log.i("ArticlePagingSource", "loading...")
        delay(3000)
        return LoadResult.Page(
            data = range.map { number ->
                Article(
                    id = number,
                    title = "Article $number",
                    description = "This describes article $number",
                    created = firstArticleCreatedTime.minusDays(number.toLong())
                )
            },
            prevKey = when(start){
                STARTING_KEY -> null
                else -> ensureMinValidKey(start - params.loadSize)
            },
            nextKey = range.last + 1
        )

    }

    // This method is called when the Paging library needs to reload items for the UI because the data
    // in its backing PagingSource has changed.

    // This situation where the underlying data for a PagingSource has changed
    // and needs to be updated in the UI is called invalidation.
    // When invalidated, the Paging Library creates a new PagingSource to reload the data,
    // and informs the UI by emitting new PagingData

    // When loading from a new PagingSource, getRefreshKey() is called
    // to provide the key the new PagingSource should start loading with
    // to make sure the user does not lose their current place in the list after the refresh.
    override fun getRefreshKey(state: PagingState<Int, Article>): Int? {
        // The key returned (in our case, an Int) will be passed to the next call of the load() method
        // in the new PagingSource via the LoadParams argument.
        val anchorPosition = state.anchorPosition ?: return null
        val article = state.closestItemToPosition(anchorPosition) ?: return null
        // In our case we grab the item closest to the anchor position
        // then return its id - (state.config.pageSize / 2) as a buffer
        return ensureMinValidKey(article.id - (state.config.pageSize / 2))
    }

    // invalidataion in the paging library occurs for one of two reasons:
    // 1. You called refresh() on the PagingAdapter. (eg: the user presses refresh button )
    // 2. You called invalidate() on the PagingSource. (eg: the server notify the client about data change)

}