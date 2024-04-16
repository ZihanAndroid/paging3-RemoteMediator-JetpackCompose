package com.example.paging.advanced.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.paging.advanced.data.Repo

@Database(
    entities = [Repo::class, RemoteKey::class],
    version = 1,
    exportSchema = false
)
abstract class RepoDatabase : RoomDatabase() {
    abstract fun getRepoDao(): RepoDAO
    abstract fun getRemoteKeyDao(): RemoteKeyDao
}