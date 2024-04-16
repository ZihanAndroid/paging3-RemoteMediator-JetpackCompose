package com.example.paging.basic.ui

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.example.paging.basic.data.Article
import com.example.paging.basic.data.createdText

@Composable
fun ArticleComponent(
    article: Article,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        Text(article.title)
        Text(article.description)
        Text(article.createdText)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BasicPaging(
    viewModel: ArticleViewModel,
    modifier: Modifier = Modifier
) {
    // collect values from a flow, works like a listener to the flow change
    // when the flow emits new values, lazyPagingItems is changed
    val lazyPagingItems = viewModel.items.collectAsLazyPagingItems()
    // you can monitor the pagerState in other composable to provide visual effect when pagerState changes (causes recomposition in the composable reading the pagerState)
    val pagerState = rememberPagerState { lazyPagingItems.itemCount }
    lazyPagingItems.loadState

    Column(
        modifier = modifier
    ) {
        Button(
            onClick = { viewModel.invalidatePageSource() }
        ) {
            Text("Invalidate Data")
        }
        Divider()
        // we use the items() of LazyColumn to add paging data when user scrolls the content
//        LazyColumn(
//            modifier = modifier
//        ) {
//
//            // refreshing, caused by paging data invalidation
//            if (lazyPagingItems.loadState.refresh == LoadState.Loading) {
//                item {
//                    Text(
//                        text = "Waiting for items to load from the backend",
//                        modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally)
//                    )
//                }
//            }
//            items(count = lazyPagingItems.itemCount) { index: Int ->
//                lazyPagingItems[index]?.let {
//                    ArticleComponent(it)
//                }
//
//            }
//            if (lazyPagingItems.loadState.append == LoadState.Loading) {
//                item {
//                    CircularProgressIndicator(
//                        modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally)
//                    )
//                }
//            }
//        }
        //https://medium.com/@domen.lanisnik/exploring-the-official-pager-in-compose-8c2698c49a98
        VerticalPager(
            state = pagerState,
            pageSize = PageSize.Fixed(100.dp),
            key = lazyPagingItems.itemKey { it.id }
        ) { index ->
            lazyPagingItems[index]?.let { ArticleComponent(it) }
        }

        LaunchedEffect(pagerState.currentPage){
            Log.i("LaunchedEffect", "current page: ${pagerState.currentPage}")
        }
    }
}