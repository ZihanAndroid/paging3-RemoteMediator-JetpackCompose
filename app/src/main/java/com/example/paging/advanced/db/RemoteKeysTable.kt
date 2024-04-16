package com.example.paging.advanced.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "remote_keys")
data class RemoteKey(
    @PrimaryKey @ColumnInfo(name = "repoId") val repoId: Long,
    @ColumnInfo(name = "prevKey") val prevKey: Int?,
    @ColumnInfo(name = "nextKey") val nextKey: Int?
)