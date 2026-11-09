package com.widgy.lyrics.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.currentState
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.widgy.lyrics.MainActivity
import com.widgy.lyrics.data.db.AppDatabase
import com.widgy.lyrics.data.preferences.WidgetPreferences
import com.widgy.lyrics.data.repository.LyricsRepository
import com.widgy.lyrics.domain.model.DailyLyric
import com.widgy.lyrics.domain.theme.ThemeConfig
import com.widgy.lyrics.ui.theme.LyricRenderer
import kotlin.math.max

class DailyLyricWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = AppDatabase.getDatabase(context)
        val prefs = WidgetPreferences(context)
        val repository = LyricsRepository(db, pinnedLyricProvider = { prefs.getPinnedLyric() })
        val pinned = prefs.getPinnedLyric()
        val cached = prefs.getCachedLyric()
        val intervalHours = prefs.getRotationIntervalHours()
        val initialLyric = pinned ?: repository.getTodayDailyLyric(intervalHours = intervalHours) ?: cached?.let {
            DailyLyric(
                date = "",
                songId = it.songId,
                snippetId = null,
                text = it.text,
                songTitle = it.songTitle,
                artistName = it.artistName
            )
        }
        if (initialLyric != null) {
            prefs.saveCachedLyric(initialLyric)
        }

        provideContent {
            val glancePrefs = currentState<androidx.datastore.preferences.core.Preferences>()
            val updateTimestamp = glancePrefs[WidgetUpdateManager.UPDATE_TIMESTAMP_KEY] ?: 0L

            // Reactively re-read latest user configurations and cached lyrics whenever state updates
            val currentContext = androidx.glance.LocalContext.current
            val currentPrefs = androidx.compose.runtime.remember(updateTimestamp) {
                WidgetPreferences(currentContext)
            }
            val themeConfig = androidx.compose.runtime.remember(updateTimestamp) {
                currentPrefs.getThemeConfig()
            }
            val pinnedLyric = androidx.compose.runtime.remember(updateTimestamp) {
                currentPrefs.getPinnedLyric()
            }
            val cachedLyric = androidx.compose.runtime.remember(updateTimestamp) {
                currentPrefs.getCachedLyric()
            }
            val dailyLyric = pinnedLyric ?: cachedLyric?.let {
                DailyLyric(
                    date = "",
                    songId = it.songId,
                    snippetId = null,
                    text = it.text,
                    songTitle = it.songTitle,
                    artistName = it.artistName
                )
            } ?: initialLyric

            WidgetContent(
                context = currentContext,
                dailyLyric = dailyLyric,
                themeConfig = themeConfig
            )
        }
    }
}

@androidx.compose.runtime.Composable
private fun WidgetContent(
    context: Context,
    dailyLyric: DailyLyric?,
    themeConfig: ThemeConfig
) {
    val clickIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        if (dailyLyric != null && dailyLyric.songId > 0) {
            putExtra(MainActivity.EXTRA_SONG_ID, dailyLyric.songId)
            putExtra(MainActivity.EXTRA_SNIPPET_TEXT, dailyLyric.text)
        }
    }

    val size = LocalSize.current
    val density = context.resources.displayMetrics.density
    val widthPx = max((size.width.value * density).toInt(), 120)
    val heightPx = max((size.height.value * density).toInt(), 100)

    val lines = dailyLyric?.text?.lines()?.map { it.trim() }?.filter { it.isNotEmpty() }
        ?: listOf("Add lyrics to see your widget")

    val songTitle = dailyLyric?.songTitle ?: ""
    val artistName = dailyLyric?.artistName ?: ""
    val songId = dailyLyric?.songId ?: 0L

    val albumArtBitmap = androidx.compose.runtime.remember(songId, themeConfig.useAlbumArtBackdrop, themeConfig.blurRadius) {
        if (themeConfig.useAlbumArtBackdrop && songId > 0) {
            com.widgy.lyrics.data.image.ArtworkManager(context)
                .loadCachedArtworkBitmap(songId, maxDimension = 180)?.let {
                    com.widgy.lyrics.data.image.StackBlur.blur(it, themeConfig.blurRadius)
                }
        } else null
    }

    val isWide = size.width.value >= 260f
    val isCompactHeight = size.height.value <= 170f
    val resolvedPadHorizontal = if (isWide) 26f else 18f
    val resolvedPadTop = if (themeConfig.showRefreshButton && isCompactHeight) 32f else 20f
    val resolvedPadBottom = if (themeConfig.creditMode != com.widgy.lyrics.domain.theme.ThemeCreditMode.NONE) 36f else 20f

    val bitmap = androidx.compose.runtime.remember(themeConfig, lines, songTitle, artistName, widthPx, heightPx, albumArtBitmap) {
        LyricRenderer.render(
            context = context,
            config = themeConfig,
            lines = lines,
            songTitle = songTitle,
            artistName = artistName,
            widthPx = widthPx,
            heightPx = heightPx,
            padTopDp = resolvedPadTop,
            padBottomDp = resolvedPadBottom,
            padHorizontalDp = resolvedPadHorizontal,
            albumArtBitmap = albumArtBitmap
        )
    }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(28.dp)
            .clickable(actionStartActivity(clickIntent)),
        contentAlignment = Alignment.Center
    ) {
        // Pixel-identical rendered bitmap
        Image(
            provider = ImageProvider(bitmap),
            contentDescription = "Daily Lyric",
            modifier = GlanceModifier.fillMaxSize()
        )

        // Top-Right Refresh Button (only if showRefreshButton is enabled)
        if (themeConfig.showRefreshButton) {
            Box(
                modifier = GlanceModifier.fillMaxSize(),
                contentAlignment = Alignment.TopEnd
            ) {
                Box(
                    modifier = GlanceModifier
                        .padding(8.dp)
                        .size(44.dp)
                        .cornerRadius(22.dp)
                        .background(ColorProvider(Color(0x44000000)))
                        .clickable(actionRunCallback<RefreshLyricActionCallback>()),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(com.widgy.lyrics.R.drawable.ic_widget_refresh),
                        contentDescription = "Shuffle Lyric",
                        modifier = GlanceModifier.size(18.dp)
                    )
                }
            }
        }
    }
}

class RefreshLyricActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val db = AppDatabase.getDatabase(context)
        val prefs = WidgetPreferences(context)
        val repository = LyricsRepository(db, pinnedLyricProvider = { prefs.getPinnedLyric() })
        val intervalHours = prefs.getRotationIntervalHours()
        val newLyric = repository.refreshTodayLyric(intervalHours = intervalHours)
        if (newLyric != null) {
            prefs.saveCachedLyric(newLyric)
        }
        WidgetUpdateManager.updateAllWidgets(context)
    }
}
