package com.example.paging.basic.data

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class Article(
    val id: Int,
    val title: String,
    val description: String,
    val created: LocalDateTime
)

private val articleDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

val Article.createdText: String
    get() = articleDateFormatter.format(created)