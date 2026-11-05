package com.widgy.lyrics.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object WidgetUpdateManager {
    private const val WORK_NAME = "widgy_daily_rotation_work"

    fun scheduleDailyRotation(context: Context) {
        val now = LocalDateTime.now()
        val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay()
        val initialDelayMinutes = Duration.between(now, nextMidnight).toMinutes().coerceAtLeast(1)

        val workRequest = PeriodicWorkRequestBuilder<DailyWidgetWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    suspend fun updateAllWidgets(context: Context) {
        try {
            val glanceManager = GlanceAppWidgetManager(context)
            val glanceIds = glanceManager.getGlanceIds(DailyLyricWidget::class.java)
            val widget = DailyLyricWidget()
            for (id in glanceIds) {
                widget.update(context, id)
            }
        } catch (_: Exception) {
            // Safely ignored if widget is not active
        }
    }
}
