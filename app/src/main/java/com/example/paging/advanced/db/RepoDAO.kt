package com.example.paging.advanced.db

import androidx.paging.PagingSource
import androidx.room.*
import com.example.paging.advanced.data.Repo

@Dao
interface RepoDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRepos(vararg repos: Repo)

    @Query("DELETE FROM repos")
    suspend fun deleteAllRepos()

    // Room already implements PagingSource to retrieve the search results List<Repo> lazily
    // Like its support for returning Flow<List<Repo>> for an observable query
    @Query("""
        SELECT * FROM repos
        WHERE name LIKE :query OR description LIKE :query
        ORDER BY stars DESC, name ASC""")
    fun reposByName(query: String): PagingSource<Int, Repo>
    // Note the function "reposByName()" is NOT a suspend function
    // The DAO functions returning PagingSource are not suspend.
    // Otherwise, you get "error: Not sure how to convert a Cursor to this method's return type (androidx.paging.PagingSource<..>)"
}

@Dao
interface RemoteKeyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKeys(keys: List<RemoteKey>)

    @Query("DELETE FROM remote_keys")
    suspend fun deleteAllKeys()

    @Query("""
        SELECT * FROM remote_keys 
        WHERE repoId = :repoId
    """)
    suspend fun selectKeyByRepoId(repoId: Long): RemoteKey
}