package com.example.paging.basic.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.paging.basic.data.Article
import com.example.paging.basic.data.ArticleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

private const val ITEMS_PER_PAGE = 50

@HiltViewModel
class ArticleViewModel @Inject constructor(
    private val repository: ArticleRepository
) : ViewModel() {
    // expose a Flow<PagingData<T>> by Pager in ViewModel

    val items: Flow<PagingData<Article>> = Pager(
        config = PagingConfig(
            pageSize = ITEMS_PER_PAGE,
            initialLoadSize = 2* ITEMS_PER_PAGE
        ),
        // The pagingSourceFactory lambda should always return a brand new PagingSource when invoked as PagingSource instances are not reusable.
        pagingSourceFactory = {
            repository.articlePagingSource()
        }
    ).flow
        // Call cachedIn so that any downstream collection from this flow will share the same PagingData.
        // Do not use the stateIn() or sharedIn() operators with PagingData Flows as PagingData Flows are not cold after calling cachedIn.
        // cachedIn() calls shareIn() and make the cold flow turn to hot flow
        .cachedIn(viewModelScope)

    fun invalidatePageSource(){
        repository.invalidateArticlePagingSource()
    }
}