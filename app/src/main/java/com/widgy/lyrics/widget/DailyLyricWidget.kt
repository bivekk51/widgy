package com.widgy.lyrics.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontFamily
import androidx.glance.text.FontStyle
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.widgy.lyrics.MainActivity
import com.widgy.lyrics.data.db.AppDatabase
import com.widgy.lyrics.data.repository.LyricsRepository
import com.widgy.lyrics.domain.model.DailyLyric

class DailyLyricWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = AppDatabase.getDatabase(context)
        val repository = LyricsRepository(db)
        val dailyLyric = repository.getTodayDailyLyric()

        provideContent {
            GlanceTheme {
                WidgetContent(context = context, dailyLyric = dailyLyric)
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun WidgetContent(context: Context, dailyLyric: DailyLyric?) {
    val size = LocalSize.current
    val isCompact = size.height < 130.dp
    val containerPadding = if (isCompact) 10.dp else 14.dp
    val quoteMaxLines = if (isCompact) 2 else 4
    val quoteFontSize = if (isCompact) 13.sp else 15.sp

    val clickIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        if (dailyLyric != null) {
            putExtra(MainActivity.EXTRA_SONG_ID, dailyLyric.songId)
            putExtra(MainActivity.EXTRA_SNIPPET_TEXT, dailyLyric.text)
        }
    }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .padding(containerPadding)
            .clickable(actionStartActivity(clickIntent)),
        contentAlignment = Alignment.Center
    ) {
        if (dailyLyric == null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "No lyrics curated yet",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                Spacer(modifier = GlanceModifier.height(4.dp))
                Text(
                    text = "Tap to search and add lyrics",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                )
            }
        } else {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DAILY LYRIC",
                        style = TextStyle(
                            color = GlanceTheme.colors.primary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = GlanceModifier.defaultWeight()
                    )
                    Text(
                        text = "↻",
                        style = TextStyle(
                            color = GlanceTheme.colors.primary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = GlanceModifier.padding(2.dp).clickable(actionRunCallback<RefreshLyricActionCallback>())
                    )
                }

                Spacer(modifier = GlanceModifier.height(4.dp))

                Text(
                    text = "“${dailyLyric.text}”",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = quoteFontSize,
                        fontStyle = FontStyle.Italic,
                        fontFamily = FontFamily.Serif
                    ),
                    maxLines = quoteMaxLines
                )

                Spacer(modifier = GlanceModifier.height(4.dp))

                Text(
                    text = "— ${dailyLyric.songTitle} • ${dailyLyric.artistName}",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1
                )
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
        val repository = LyricsRepository(db)
        repository.refreshTodayLyric()
        DailyLyricWidget().update(context, glanceId)
    }
}
