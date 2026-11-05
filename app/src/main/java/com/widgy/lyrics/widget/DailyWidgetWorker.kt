package com.widgy.lyrics.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.widgy.lyrics.data.db.AppDatabase
import com.widgy.lyrics.data.preferences.WidgetPreferences
import com.widgy.lyrics.data.repository.LyricsRepository

class DailyWidgetWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val db = AppDatabase.getDatabase(appContext)
            val repository = LyricsRepository(db)
            val dailyLyric = repository.getTodayDailyLyric()
            if (dailyLyric != null) {
                WidgetPreferences(appContext).saveCachedLyric(dailyLyric)
            }
            WidgetUpdateManager.updateAllWidgets(appContext)
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
