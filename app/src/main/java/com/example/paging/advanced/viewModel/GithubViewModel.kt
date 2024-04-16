package com.example.paging.advanced.viewModel

import androidx.lifecycle.ViewModel
import androidx.paging.*
import com.example.paging.advanced.DefaultConfiguration
import com.example.paging.advanced.data.Repo
import com.example.paging.advanced.repository.GithubRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class GithubViewModel @Inject constructor(
    private val repository: GithubRepository
) : ViewModel() {
    // convert a Flow<PagingData<Repo>> to Flow<PagingData<UiModel>> by mapping pagingData with insertSeparators()
    var _pagingDataFlowStateFlow: MutableStateFlow<Flow<PagingData<UiModel>>> =
        MutableStateFlow(emptyFlow())
    val pagingDataFlowStateFlow: StateFlow<Flow<PagingData<UiModel>>>
        get() = _pagingDataFlowStateFlow

    private var currentQuery = DefaultConfiguration.DEFAULT_QUERY

    private fun getNewPagingDataFlowForQuery(
        query: String,
        coroutineScope: CoroutineScope,
        initialKey: Int? = null
    ): Flow<PagingData<UiModel>> =
        repository.getGithubPagerFlow(query, initialKey)
            // transform PagingData (defined in Repository) in ViewModel
            .map {
                // (1) you can filter the content of PagingData by "pagingData.filter()"
                it.filter { true }
            }
            .map { pagingData ->
                pagingData.map {
                    UiModel.RepoItem(it)
                }
            }.map { pagingData ->
                // (2) you can add separators by "pagingData.insertSeparators()"
                // When using separators, you will need to implement your own type (UiModel) that models the new separator items and existing items
                pagingData.insertSeparators { before, after ->
                    // for every pair of existing items, decide whether to insert a separator item or not (returning null)
                    if (after == null) null // no separator
                    else if (before == null || before.roundedStarCount > after.roundedStarCount) {
                        if (after.roundedStarCount >= 1) {
                            UiModel.SeparatorItem("${after.roundedStarCount}0,000+ stars")
                        } else {
                            UiModel.SeparatorItem("< 10,000 stars")
                        }
                    } else null
                }
            }//.map { it.insertHeaderItem(item = UiModel.RepoHeader("Search Results for [$query]")) }
            // Note you should NOT write the following code to cachedIn "viewModelScope",
            // Instead you should cachedIn the CoroutineScope of a composable(returned by rememberCoroutineScope())
            // .cachedIn(viewModelScope)
            .cachedIn(coroutineScope)

    // Bad way:
    fun triggerNewQuery(query: String, coroutineScope: CoroutineScope) {
        currentQuery = query
        _pagingDataFlowStateFlow.value = getNewPagingDataFlowForQuery(query, coroutineScope)
    }

    fun triggerPageJump(newPage: Int, coroutineScope: CoroutineScope) {
        _pagingDataFlowStateFlow.value = getNewPagingDataFlowForQuery(currentQuery, coroutineScope, newPage)
    }
}

fun CombinedLoadStates.isLoading(): Boolean =
    append is LoadState.Loading || prepend is LoadState.Loading || refresh is LoadState.Loading

fun CombinedLoadStates.isError(): Boolean =
    append is LoadState.Error || prepend is LoadState.Error || refresh is LoadState.Error

sealed class UiModel {
    data class RepoItem(val repo: Repo) : UiModel()
    data class SeparatorItem(val description: String) : UiModel()
}

private val UiModel.RepoItem.roundedStarCount: Int
    get() = repo.stars / 10_000


