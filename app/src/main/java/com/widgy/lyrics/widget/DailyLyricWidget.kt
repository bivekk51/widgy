package com.widgy.lyrics.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
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
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontFamily
import androidx.glance.text.FontStyle
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.widgy.lyrics.MainActivity
import com.widgy.lyrics.data.db.AppDatabase
import com.widgy.lyrics.data.preferences.WidgetAttribution
import com.widgy.lyrics.data.preferences.WidgetConfig
import com.widgy.lyrics.data.preferences.WidgetFontFamily
import com.widgy.lyrics.data.preferences.WidgetPreferences
import com.widgy.lyrics.data.preferences.WidgetTemplate
import com.widgy.lyrics.data.repository.LyricsRepository
import com.widgy.lyrics.domain.model.DailyLyric
import com.widgy.lyrics.widget.WidgetUpdateManager

class DailyLyricWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*>? = null
    override val sizeMode: SizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = AppDatabase.getDatabase(context)
        val repository = LyricsRepository(db)
        val dailyLyric = repository.getTodayDailyLyric()
        if (dailyLyric != null) {
            WidgetPreferences(context).saveCachedLyric(dailyLyric)
        }

        provideContent {
            val config = WidgetPreferences(context).getConfig()
            val cached = WidgetPreferences(context).getCachedLyric()
            val lyricToDisplay = dailyLyric ?: cached?.let {
                DailyLyric(
                    date = "",
                    songId = it.songId,
                    snippetId = null,
                    text = it.text,
                    songTitle = it.songTitle,
                    artistName = it.artistName
                )
            }
            WidgetContent(context = context, dailyLyric = lyricToDisplay, config = config)
        }
    }
}

@androidx.compose.runtime.Composable
private fun WidgetContent(context: Context, dailyLyric: DailyLyric?, config: WidgetConfig) {
    val clickIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        if (dailyLyric != null) {
            putExtra(MainActivity.EXTRA_SONG_ID, dailyLyric.songId)
            putExtra(MainActivity.EXTRA_SNIPPET_TEXT, dailyLyric.text)
        }
    }

    // Card styling based on chosen template
    val bgColor = when (config.template) {
        WidgetTemplate.GENIUS -> Color(0xFF0A0A0A)
        WidgetTemplate.SPOTIFY -> Color(0xFF121212)
        WidgetTemplate.CLASSIC -> Color(0xFF18181A)
        WidgetTemplate.TERMINAL -> Color(0xFF0D1117)
        WidgetTemplate.MINIMAL -> Color(0xFF000000)
    }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(22.dp)
            .background(ColorProvider(bgColor))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        // 1. Center Clickable Area (Opens App)
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 2.dp)
                .clickable(actionStartActivity(clickIntent)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (dailyLyric == null) {
                Text(
                    text = "No lyrics curated yet",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFFE0E0E0)),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                )
                Spacer(modifier = GlanceModifier.height(4.dp))
                Text(
                    text = "Tap to search and pick lines",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF8E8E93)),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                )
            } else {
                val cleanText = dailyLyric.text.trim().trim('"', '“', '”', '‘', '’')
                val formattedText = when (config.template) {
                    WidgetTemplate.GENIUS -> "“${cleanText.uppercase()}”"
                    WidgetTemplate.SPOTIFY -> "“$cleanText”"
                    WidgetTemplate.CLASSIC -> "“$cleanText”"
                    WidgetTemplate.TERMINAL -> "> $cleanText"
                    WidgetTemplate.MINIMAL -> cleanText
                }

                val glanceFontFamily = when (config.fontFamily) {
                    WidgetFontFamily.SERIF -> FontFamily.Serif
                    WidgetFontFamily.MONOSPACE -> FontFamily.Monospace
                    WidgetFontFamily.SYSTEM -> when (config.template) {
                        WidgetTemplate.CLASSIC -> FontFamily.Serif
                        WidgetTemplate.TERMINAL -> FontFamily.Monospace
                        else -> FontFamily.SansSerif
                    }
                }

                val finalFontFamily = if (config.template == WidgetTemplate.TERMINAL) {
                    FontFamily.Monospace
                } else glanceFontFamily

                val textStyle = when (config.template) {
                    WidgetTemplate.GENIUS -> TextStyle(
                        color = ColorProvider(Color(0xFFFFFFFF)),
                        fontSize = config.fontSize.sizeSp.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = finalFontFamily,
                        textAlign = TextAlign.Center
                    )
                    WidgetTemplate.SPOTIFY -> TextStyle(
                        color = ColorProvider(Color(0xFFFFFFFF)),
                        fontSize = config.fontSize.sizeSp.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = finalFontFamily,
                        textAlign = TextAlign.Center
                    )
                    WidgetTemplate.CLASSIC -> TextStyle(
                        color = ColorProvider(Color(0xFFF2F2F7)),
                        fontSize = config.fontSize.sizeSp.sp,
                        fontStyle = FontStyle.Italic,
                        fontFamily = finalFontFamily,
                        textAlign = TextAlign.Center
                    )
                    WidgetTemplate.TERMINAL -> TextStyle(
                        color = ColorProvider(Color(0xFFFFB454)),
                        fontSize = config.fontSize.sizeSp.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )
                    WidgetTemplate.MINIMAL -> TextStyle(
                        color = ColorProvider(Color(0xFFEBEBF0)),
                        fontSize = config.fontSize.sizeSp.sp,
                        fontWeight = FontWeight.Normal,
                        fontFamily = finalFontFamily,
                        textAlign = TextAlign.Center
                    )
                }

                Text(
                    text = formattedText,
                    style = textStyle,
                    maxLines = 4
                )

                // Optional Attribution Footer
                val attributionText = when (config.attribution) {
                    WidgetAttribution.NONE -> null
                    WidgetAttribution.ARTIST_ONLY -> when (config.template) {
                        WidgetTemplate.SPOTIFY -> "♫ ${dailyLyric.artistName}"
                        WidgetTemplate.TERMINAL -> "$ ${dailyLyric.artistName}"
                        else -> "— ${dailyLyric.artistName}"
                    }
                    WidgetAttribution.SONG_AND_ARTIST -> when (config.template) {
                        WidgetTemplate.SPOTIFY -> "♫ ${dailyLyric.songTitle} • ${dailyLyric.artistName}"
                        WidgetTemplate.TERMINAL -> "$ ${dailyLyric.songTitle} :: ${dailyLyric.artistName}"
                        else -> "— ${dailyLyric.songTitle} • ${dailyLyric.artistName}"
                    }
                }

                val attributionColor = when (config.template) {
                    WidgetTemplate.SPOTIFY -> Color(0xFF1ED760)
                    WidgetTemplate.TERMINAL -> Color(0xFFE5A93C)
                    WidgetTemplate.MINIMAL -> Color(0xFF636366)
                    else -> Color(0xFF8E8E93)
                }

                if (attributionText != null) {
                    Spacer(modifier = GlanceModifier.height(8.dp))
                    Text(
                        text = attributionText,
                        style = TextStyle(
                            color = ColorProvider(attributionColor),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = if (config.template == WidgetTemplate.TERMINAL) FontFamily.Monospace else FontFamily.SansSerif,
                            textAlign = TextAlign.Center
                        ),
                        maxLines = 1
                    )
                }
            }
        }

        // 2. Top-Right Refresh Button (rendered ON TOP of Column to capture clicks cleanly)
        Box(
            modifier = GlanceModifier
                .fillMaxSize(),
            contentAlignment = Alignment.TopEnd
        ) {
            Box(
                modifier = GlanceModifier
                    .width(48.dp)
                    .height(48.dp)
                    .clickable(actionRunCallback<RefreshLyricActionCallback>()),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "↻",
                    style = TextStyle(
                        color = ColorProvider(
                            when (config.template) {
                                WidgetTemplate.SPOTIFY -> Color(0xAA1ED760)
                                WidgetTemplate.TERMINAL -> Color(0xAAFFB454)
                                else -> Color(0x88FFFFFF)
                            }
                        ),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
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
        val newLyric = repository.refreshTodayLyric()
        if (newLyric != null) {
            WidgetPreferences(context).saveCachedLyric(newLyric)
        }
        WidgetUpdateManager.updateAllWidgets(context)
    }
}
