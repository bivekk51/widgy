package com.widgy.lyrics.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.widgy.lyrics.data.db.AppDatabase
import com.widgy.lyrics.data.repository.LyricsRepository

class DailyWidgetWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val db = AppDatabase.getDatabase(appContext)
            val repository = LyricsRepository(db)
            repository.getTodayDailyLyric()

            val glanceManager = GlanceAppWidgetManager(appContext)
            val glanceIds = glanceManager.getGlanceIds(DailyLyricWidget::class.java)
            val widget = DailyLyricWidget()
            for (id in glanceIds) {
                widget.update(appContext, id)
            }
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
