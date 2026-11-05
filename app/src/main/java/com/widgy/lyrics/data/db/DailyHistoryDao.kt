package com.widgy.lyrics.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyHistoryDao {
    @Query("SELECT * FROM daily_history WHERE date = :date LIMIT 1")
    suspend fun getDailyLyric(date: String): DailyHistoryEntity?

    @Query("SELECT * FROM daily_history WHERE date = :date LIMIT 1")
    fun observeDailyLyric(date: String): Flow<DailyHistoryEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: DailyHistoryEntity)

    @Query("DELETE FROM daily_history WHERE date = :date")
    suspend fun deleteForDate(date: String)
}
