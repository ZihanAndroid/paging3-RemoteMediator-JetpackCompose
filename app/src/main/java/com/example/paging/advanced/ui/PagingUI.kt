package com.example.paging.advanced.ui

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.paging.R
import com.example.paging.advanced.DefaultConfiguration
import com.example.paging.advanced.viewModel.GithubViewModel
import com.example.paging.advanced.viewModel.UiModel
import com.example.paging.advanced.viewModel.isError
import com.example.paging.advanced.viewModel.isLoading
import kotlinx.coroutines.*

data class PagingRequest(
    val searchOrJump: Boolean,
    val pageNum: Int,
    var coroutine: CoroutineScope?
) {
    // compare references instead of content for triggering recomposition conveniently by pagingRequest.copy()
    override fun equals(other: Any?): Boolean {
        return this === other
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PagingUI(
    modifier: Modifier = Modifier
) {
    val githubViewModel: GithubViewModel = viewModel()
    var searchText by rememberSaveable { mutableStateOf(DefaultConfiguration.DEFAULT_QUERY) }

    val coroutineScope = rememberCoroutineScope()
    // Note that not all MutableStates are used to trigger recomposition.
    // Uou can also use it to store some local variables that are consistent with each composable call PagingUI()
    // As long as you do not read the state inside a Composable, no concern for recomposition
    var pagingRequest by remember { mutableStateOf(PagingRequest(true, 0, null)) }

    // pager
    // Note in LazyPagingItems, there is a itemSnapshotList that is MutableState and stores the cached pages
    // the recomposition is triggered when the page data changes
    //    var itemSnapshotList by mutableStateOf(
    //        pagingDataDiffer.snapshot()
    //    )
    // val pagingItems = githubViewModel.pagingDataFlow.collectAsLazyPagingItems()
    val pagingDataFlowState by githubViewModel.pagingDataFlowStateFlow.collectAsStateWithLifecycle()
    val pagingItems = pagingDataFlowState.collectAsLazyPagingItems()

    // Note the pageState.scrollToPage() scrolls to an existing item (not page) in the cache, do not support scrolling to an unloaded page
    //      see "page.coerceInPageRange()" in scrollToPage()
    // "pagingItems.itemCount" returns "itemSnapshotList.size", not "pages.size", itemSnapshotList is created by pages.flatMap(...)
    // And "pageState" is not valid for LazyColumn (it does not accept a PageState), only valid for HorizontalPager and VerticalPager
    // val pageState = rememberPagerState { pagingItems.itemCount }
    // For LazyColumn, use lazyListState
    val lazyListState = rememberLazyListState()

    // The equality of pagingRequest is based on pagingRequest.equals() method
    // So to trigger a recomposition by changing pagingRequest, you must
    // change a field in PagingRequest or override the "equals" method of PagingRequest to compare references like (this === other) instead of contents
    LaunchedEffect(pagingRequest) {
        pagingRequest.coroutine?.cancel()
        // From: https://slack-chats.kotlinlang.org/t/9574814/if-i-want-to-make-a-child-coroutine-scope-for-my-repository-
        pagingRequest.coroutine = this + SupervisorJob(parent = this.coroutineContext.job) + Dispatchers.IO
        if (pagingRequest.searchOrJump) {
            githubViewModel.triggerNewQuery(searchText, pagingRequest.coroutine!!)
        } else {
            // for simplicity, jumping
            githubViewModel.triggerPageJump(pagingRequest.pageNum, pagingRequest.coroutine!!)
        }
    }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val currentPage by remember {
        derivedStateOf { lazyListState.firstVisibleItemIndex / DefaultConfiguration.NETWORK_PAGE_SIZE }
    }
    // Note pivotPage is not a State, change it does not cause recomposition
    var pivotPage = rememberSaveable(currentPage) {
        if(currentPage <= PAGE_STEP) PAGE_STEP
        else currentPage
    }


    ConstraintLayout(
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures(onTap = {
                keyboardController?.hide()
                focusManager.clearFocus()
            })
        }
    ) {
        val (searchBar, bottomBar, lazyColumn) = createRefs()
        SearchBarComposable(
            modifier = Modifier.constrainAs(searchBar) {
                top.linkTo(parent.top)
            },
            value = searchText,
            onValueChange = { searchText = it },
            onSearch = {
                Log.i("SearchBarComposable", "onSearch is called!")
                // To start a new query, change the StateFlow of pagingDataFlow by viewModel
                // then pagingDataFlowState changes, which triggers a recomposition.
                // Check the source code of collectAsLazyPagingItems(), when pagingDataFlow changes,
                // a new lazyPagingItems is created and two LaunchedEffect collecting page data from the new source of the query
                // then the UI shows new content for the new lazyPagingItems
                // githubViewModel.triggerNewQuery(searchText)
                pagingRequest =
                    pagingRequest.copy(searchOrJump = true) // trigger the recomposition of LaunchedEffect pagingRequest
                keyboardController?.hide()
                focusManager.clearFocus()
            }
        )
        // JumpBarComposable(
        //     modifier = Modifier.constrainAs(jumpBar) {
        //         top.linkTo(searchBar.bottom)
        //     }.padding(8.dp),
        //     currentPage = currentPage,
        //     pivotPage = pivotPage,
        //     step = PAGE_STEP,
        //     onCurrentPageChanged = { selectedPage ->
        //         if (selectedPage < pivotPage - PAGE_STEP || selectedPage > pivotPage) {
        //             pivotPage = max(selectedPage, PAGE_STEP)
        //         }
        //         coroutineScope.launch {
        //             lazyListState.scrollToItem(selectedPage * DefaultConfiguration.NETWORK_PAGE_SIZE)
        //         }
        //         // we change the currentPage by calling a viewModel method
        //         // githubViewModel.triggerPageJump(selectedPage)
        //         pagingRequest = pagingRequest.copy(
        //             searchOrJump = false,
        //             pageNum = selectedPage
        //         )
        //     }
        // )
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.constrainAs(lazyColumn) {
                top.linkTo(searchBar.bottom)
                // top.linkTo(bottomBar.top) do not rely on the top of bottomBar because it can be changed when LoadState changes, not a reliable source
            }
        ) {
            items(count = pagingItems.itemCount) { index ->
                Log.d("LazyColumn", "itemCount: ${pagingItems.itemCount}, index: $index")
                pagingItems[index]?.let {
                    when (it) {
                        is UiModel.SeparatorItem -> PageTitle(it.description)
                        is UiModel.RepoItem -> PageItem(it.repo)
                    }
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.constrainAs(bottomBar) {
                bottom.linkTo(parent.bottom)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            }.padding(if (pagingItems.loadState.isLoading() || pagingItems.loadState.isError()) 16.dp else 0.dp)
                .fillMaxWidth().wrapContentHeight()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // check pagingItems.loadState
            if (pagingItems.loadState.isLoading()) {
                Text(
                    text = stringResource(R.string.page_loading),
                    style = MaterialTheme.typography.titleMedium
                )
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else if (pagingItems.loadState.isError()) {
                Button(
                    // retry the previous query by "pagingItems.retry()"
                    onClick = { pagingItems.retry() }
                ) {
                    Text(text = stringResource(R.string.retry))
                }
            } // else, show nothing
        }
    }
}

const val PAGE_STEP = 2