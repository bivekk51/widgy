package com.widgy.lyrics.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.widgy.lyrics.data.db.AppDatabase
import com.widgy.lyrics.data.image.ArtworkManager
import com.widgy.lyrics.data.preferences.WidgetPreferences
import com.widgy.lyrics.data.repository.LyricsRepository

class DailyWidgetWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val prefs = WidgetPreferences(appContext)

            // If a lyric is pinned/locked by the user, preserve it and skip rotation
            if (prefs.isLyricPinned()) {
                WidgetUpdateManager.updateAllWidgets(appContext)
                return Result.success()
            }

            val db = AppDatabase.getDatabase(appContext)
            val repository = LyricsRepository(db, pinnedLyricProvider = { prefs.getPinnedLyric() })
            val intervalHours = prefs.getRotationIntervalHours()
            val dailyLyric = repository.getTodayDailyLyric(intervalHours = intervalHours)
            if (dailyLyric != null) {
                prefs.saveCachedLyric(dailyLyric)

                // Prefetch album art in the background so the widget renders immediately with backdrop
                if (prefs.getThemeConfig().useAlbumArtBackdrop && dailyLyric.songId > 0) {
                    runCatching {
                        ArtworkManager(appContext).getOrFetchArtworkBitmap(
                            songId = dailyLyric.songId,
                            title = dailyLyric.songTitle,
                            artist = dailyLyric.artistName,
                            maxDimension = 200
                        )
                    }
                }
            }
            WidgetUpdateManager.updateAllWidgets(appContext)
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
