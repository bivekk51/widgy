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

    override val stateDefinition: GlanceStateDefinition<*>? = null
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = AppDatabase.getDatabase(context)
        val repository = LyricsRepository(db)
        val prefs = WidgetPreferences(context)
        val cached = prefs.getCachedLyric()
        val dailyLyric = repository.getTodayDailyLyric() ?: cached?.let {
            DailyLyric(
                date = "",
                songId = it.songId,
                snippetId = null,
                text = it.text,
                songTitle = it.songTitle,
                artistName = it.artistName
            )
        }
        if (dailyLyric != null) {
            prefs.saveCachedLyric(dailyLyric)
        }
        val themeConfig = prefs.getThemeConfig()

        provideContent {
            WidgetContent(
                context = context,
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

    val bitmap = rememberBitmap(
        context = context,
        themeConfig = themeConfig,
        lines = lines,
        songTitle = songTitle,
        artistName = artistName,
        widthPx = widthPx,
        heightPx = heightPx
    )

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(22.dp)
            .clickable(actionStartActivity(clickIntent)),
        contentAlignment = Alignment.Center
    ) {
        // Pixel-identical rendered bitmap
        Image(
            provider = ImageProvider(bitmap),
            contentDescription = "Daily Lyric",
            modifier = GlanceModifier.fillMaxSize()
        )

        // Top-Right Refresh Button (44dp circle, 8dp padding from edge, never overlaps lyrics text)
        Box(
            modifier = GlanceModifier.fillMaxSize(),
            contentAlignment = Alignment.TopEnd
        ) {
            Box(
                modifier = GlanceModifier
                    .padding(8.dp)
                    .size(44.dp)
                    .cornerRadius(22.dp)
                    .background(ColorProvider(Color(0x33000000)))
                    .clickable(actionRunCallback<RefreshLyricActionCallback>()),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "↻",
                    style = TextStyle(
                        color = ColorProvider(Color(0xE6FFFFFF)),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

private fun rememberBitmap(
    context: Context,
    themeConfig: ThemeConfig,
    lines: List<String>,
    songTitle: String,
    artistName: String,
    widthPx: Int,
    heightPx: Int
): android.graphics.Bitmap {
    return LyricRenderer.render(
        context = context,
        config = themeConfig,
        lines = lines,
        songTitle = songTitle,
        artistName = artistName,
        widthPx = widthPx,
        heightPx = heightPx
    )
}

class RefreshLyricActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val db = AppDatabase.getDatabase(context)
        val repository = LyricsRepository(db)
        val newLyric = repository.refreshTodayLyric()
        if (newLyric != null) {
            WidgetPreferences(context).saveCachedLyric(newLyric)
        }
        WidgetUpdateManager.updateAllWidgets(context)
    }
}
