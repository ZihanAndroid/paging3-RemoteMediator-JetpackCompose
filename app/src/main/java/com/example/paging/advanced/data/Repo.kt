package com.example.paging.advanced.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json

@Entity(tableName = "repos")
data class Repo(
    @field:Json(name = "id") @PrimaryKey @ColumnInfo(name = "id") val id: Long,
    @field:Json(name = "name") @ColumnInfo(name = "name") val name: String,    // query string like "Android"
    @field:Json(name = "full_name") @ColumnInfo(name = "fullName") val fullName: String,
    @field:Json(name = "description") @ColumnInfo(name = "description") val description: String?,
    @field:Json(name = "html_url") @ColumnInfo(name = "url") val url: String,
    @field:Json(name = "stargazers_count") @ColumnInfo(name = "stars") val stars: Int,
    @field:Json(name = "forks_count") @ColumnInfo(name = "forks") val forks: Int,
    @field:Json(name = "language") @ColumnInfo(name = "language") val language: String?
)