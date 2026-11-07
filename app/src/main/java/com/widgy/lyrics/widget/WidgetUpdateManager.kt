package com.widgy.lyrics.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object WidgetUpdateManager {
    const val WORK_NAME = "widgy_daily_rotation_work"

    fun scheduleDailyRotation(context: Context) {
        val prefs = com.widgy.lyrics.data.preferences.WidgetPreferences(context)
        val intervalHours = prefs.getRotationIntervalHours()
        scheduleRotation(context, intervalHours)
    }

    fun scheduleRotation(context: Context, intervalHours: Int) {
        val workRequest = if (intervalHours >= 24) {
            val now = LocalDateTime.now()
            val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay()
            val initialDelayMinutes = Duration.between(now, nextMidnight).toMinutes().coerceAtLeast(1)
            PeriodicWorkRequestBuilder<DailyWidgetWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
                .build()
        } else {
            val validHours = intervalHours.coerceIn(1, 12).toLong()
            PeriodicWorkRequestBuilder<DailyWidgetWorker>(validHours, TimeUnit.HOURS)
                .build()
        }

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    suspend fun updateAllWidgets(context: Context) {
        try {
            DailyLyricWidget().updateAll(context)
        } catch (_: Exception) {
            try {
                val glanceManager = GlanceAppWidgetManager(context)
                val glanceIds = glanceManager.getGlanceIds(DailyLyricWidget::class.java)
                val widget = DailyLyricWidget()
                for (id in glanceIds) {
                    widget.update(context, id)
                }
            } catch (_: Exception) {}
        }
    }
}
