package com.widgy.lyrics.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.widgy.lyrics.domain.model.DailyLyric

enum class WidgetTemplate(
    val displayName: String,
    val subtitle: String
) {
    GENIUS("Genius Style", "Pitch-black, bold all-caps quotes"),
    SPOTIFY("Spotify Glow", "Dark mode with signature green accent"),
    CLASSIC("Classic Serif", "Warm charcoal with elegant italics"),
    TERMINAL("Retro Terminal", "Hacker amber monospace aesthetic"),
    MINIMAL("Clean OLED", "Ultra-minimal pure monochrome")
}

enum class WidgetFontFamily(val displayName: String) {
    SYSTEM("Modern Sans"),
    SERIF("Classic Serif"),
    MONOSPACE("Retro Mono")
}

enum class WidgetAttribution(val displayName: String) {
    NONE("No Attribution"),           // Pure lyric only
    ARTIST_ONLY("Artist Only"),       // "— Artist"
    SONG_AND_ARTIST("Song & Artist")  // "— Song • Artist"
}

enum class WidgetFontSize(
    val displayName: String,
    val sizeSp: Int,
    val lineHeightSp: Int
) {
    SMALL("Small", 16, 22),
    MEDIUM("Medium", 22, 28),
    LARGE("Large", 30, 38),
    EXTRA_LARGE("Extra Large", 40, 48)
}

data class WidgetConfig(
    val template: WidgetTemplate = WidgetTemplate.GENIUS,
    val fontFamily: WidgetFontFamily = WidgetFontFamily.SYSTEM,
    val attribution: WidgetAttribution = WidgetAttribution.ARTIST_ONLY,
    val fontSize: WidgetFontSize = WidgetFontSize.LARGE
)

data class CachedLyric(
    val text: String,
    val songTitle: String,
    val artistName: String,
    val songId: Long = 0L
)

class WidgetPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)

    fun getCachedLyric(): CachedLyric? {
        val text = prefs.getString("cached_lyric_text", null) ?: return null
        val title = prefs.getString("cached_lyric_title", "") ?: ""
        val artist = prefs.getString("cached_lyric_artist", "") ?: ""
        val songId = prefs.getLong("cached_lyric_song_id", 0L)
        return CachedLyric(text, title, artist, songId)
    }

    fun saveCachedLyric(lyric: DailyLyric) {
        prefs.edit()
            .putString("cached_lyric_text", lyric.text)
            .putString("cached_lyric_title", lyric.songTitle)
            .putString("cached_lyric_artist", lyric.artistName)
            .putLong("cached_lyric_song_id", lyric.songId)
            .apply()
    }

    fun getConfig(): WidgetConfig {
        val templateName = prefs.getString("template", WidgetTemplate.GENIUS.name) ?: WidgetTemplate.GENIUS.name
        val fontName = prefs.getString("font_family", WidgetFontFamily.SYSTEM.name) ?: WidgetFontFamily.SYSTEM.name
        val attrName = prefs.getString("attribution", WidgetAttribution.ARTIST_ONLY.name) ?: WidgetAttribution.ARTIST_ONLY.name
        val sizeName = prefs.getString("font_size", WidgetFontSize.LARGE.name) ?: WidgetFontSize.LARGE.name

        val parsedSize = when (sizeName) {
            "SMALL" -> WidgetFontSize.SMALL
            "REGULAR", "MEDIUM" -> WidgetFontSize.MEDIUM
            "LARGE" -> WidgetFontSize.LARGE
            "EXTRA_LARGE", "JUMBO" -> WidgetFontSize.EXTRA_LARGE
            else -> runCatching { WidgetFontSize.valueOf(sizeName) }.getOrDefault(WidgetFontSize.LARGE)
        }

        return WidgetConfig(
            template = runCatching { WidgetTemplate.valueOf(templateName) }.getOrDefault(WidgetTemplate.GENIUS),
            fontFamily = runCatching { WidgetFontFamily.valueOf(fontName) }.getOrDefault(WidgetFontFamily.SYSTEM),
            attribution = runCatching { WidgetAttribution.valueOf(attrName) }.getOrDefault(WidgetAttribution.ARTIST_ONLY),
            fontSize = parsedSize
        )
    }

    fun saveConfig(config: WidgetConfig) {
        prefs.edit()
            .putString("template", config.template.name)
            .putString("font_family", config.fontFamily.name)
            .putString("attribution", config.attribution.name)
            .putString("font_size", config.fontSize.name)
            .apply()
    }
}
