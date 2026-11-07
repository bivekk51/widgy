package com.widgy.lyrics.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface SnippetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnippet(snippet: SnippetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnippets(snippets: List<SnippetEntity>): List<Long>

    @Query("SELECT * FROM snippets WHERE songId = :songId")
    suspend fun getSnippetsForSong(songId: Long): List<SnippetEntity>

    @Query("SELECT * FROM snippets WHERE isActive = 1")
    suspend fun getActiveSnippets(): List<SnippetEntity>

    @Query("UPDATE snippets SET isActive = :isActive WHERE id = :id")
    suspend fun updateSnippetActive(id: Long, isActive: Boolean)

    @Query("DELETE FROM snippets WHERE id = :id")
    suspend fun deleteSnippetById(id: Long)

    @Query("DELETE FROM snippets WHERE songId = :songId")
    suspend fun deleteAllSnippetsForSong(songId: Long)
}
