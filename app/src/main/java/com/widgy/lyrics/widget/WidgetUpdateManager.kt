package com.widgy.lyrics.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object WidgetUpdateManager {
    const val WORK_NAME = "widgy_daily_rotation_work"
    private const val TAG = "WidgetUpdateManager"

    val UPDATE_TIMESTAMP_KEY = longPreferencesKey("widget_update_timestamp")

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Fire-and-forget widget refresh on application scope.
     * Safe to call from UI/ViewModels when navigation immediately disposes the caller's scope.
     */
    fun notifyWidgetUpdate(context: Context) {
        val appContext = context.applicationContext
        applicationScope.launch {
            updateAllWidgets(appContext)
        }
    }

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

    /**
     * Comprehensive multi-phase widget update:
     * 1. Mutates Glance DataStore state timestamp so Glance recomposition is guaranteed.
     * 2. Calls targeted Glance update on each placed glanceId.
     * 3. Falls back to global updateAll.
     * 4. Broadcasts standard ACTION_APPWIDGET_UPDATE with explicit appWidgetIds to notify Android OS launcher host.
     */
    suspend fun updateAllWidgets(context: Context) {
        val appContext = context.applicationContext
        withContext(Dispatchers.IO + NonCancellable) {
            val timestamp = System.currentTimeMillis()

            // 1 & 2: Update Glance state per instance and request Glance redraw
            try {
                val glanceManager = GlanceAppWidgetManager(appContext)
                val glanceIds = glanceManager.getGlanceIds(DailyLyricWidget::class.java)

                for (glanceId in glanceIds) {
                    try {
                        updateAppWidgetState(appContext, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                            prefs.toMutablePreferences().apply {
                                this[UPDATE_TIMESTAMP_KEY] = timestamp
                            }
                        }
                        DailyLyricWidget().update(appContext, glanceId)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed targeted update on glanceId $glanceId", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed resolving Glance IDs", e)
            }

            // 3: Fallback / safety global updateAll
            try {
                DailyLyricWidget().updateAll(appContext)
            } catch (e: Exception) {
                Log.e(TAG, "DailyLyricWidget().updateAll fallback failed", e)
            }

            // 4: Explicit system broadcast to ensure Android launcher AppWidgetHost refreshes
            try {
                val appWidgetManager = AppWidgetManager.getInstance(appContext)
                val componentName = ComponentName(appContext, DailyLyricWidgetReceiver::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                    val updateIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).apply {
                        component = componentName
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                    }
                    appContext.sendBroadcast(updateIntent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed broadcasting ACTION_APPWIDGET_UPDATE", e)
            }
        }
    }
}
