package com.example.paging.advanced.data

import com.squareup.moshi.Json

data class RepoSearchResponse(
    @field:Json(name = "total_count") val total: Int = 0,
    @field:Json(name = "items") val items: List<Repo> = emptyList(),
    val nextPage: Int? = null
)