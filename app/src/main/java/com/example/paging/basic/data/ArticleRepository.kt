package com.example.paging.basic.data

import java.time.LocalDateTime
import javax.inject.Inject

private val firstArticleCreatedTime = LocalDateTime.now()

class ArticleRepository @Inject constructor() {
    private lateinit var currentPagingSource: ArticlePagingSource
//    val articleStream: Flow<List<Article>> = flowOf(
//        (1..500).map { number ->
//            Article(
//                id = number,
//                title = "Article $number",
//                description = "This describes article $number",
//                created = firstArticleCreatedTime.minusDays(number.toLong())
//            )
//        }
//    )
    fun articlePagingSource(): ArticlePagingSource {
        currentPagingSource = ArticlePagingSource()
        return currentPagingSource
    }
    fun invalidateArticlePagingSource(){
        currentPagingSource.invalidate()
    }
}