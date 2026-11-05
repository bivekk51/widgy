package com.widgy.lyrics.data.preferences

import android.content.Context
import android.content.SharedPreferences

enum class WidgetTemplate(val displayName: String) {
    GENIUS("Genius Style"),     // Bold all-caps, pure black card (matches reference image)
    CLASSIC("Classic Serif"),   // Elegant serif italics
    MINIMAL("Clean Sans")       // Minimal modern sans-serif
}

enum class WidgetAttribution(val displayName: String) {
    NONE("No Attribution"),           // Pure lyric only
    ARTIST_ONLY("Artist Only"),       // "— Artist"
    SONG_AND_ARTIST("Song & Artist")  // "— Song • Artist"
}

enum class WidgetFontSize(val displayName: String, val sizeSp: Int) {
    REGULAR("Medium", 20),
    LARGE("Large", 23),
    EXTRA_LARGE("Extra Large", 26)
}

data class WidgetConfig(
    val template: WidgetTemplate = WidgetTemplate.GENIUS,
    val attribution: WidgetAttribution = WidgetAttribution.ARTIST_ONLY,
    val fontSize: WidgetFontSize = WidgetFontSize.LARGE
)

class WidgetPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)

    fun getConfig(): WidgetConfig {
        val templateName = prefs.getString("template", WidgetTemplate.GENIUS.name) ?: WidgetTemplate.GENIUS.name
        val attrName = prefs.getString("attribution", WidgetAttribution.ARTIST_ONLY.name) ?: WidgetAttribution.ARTIST_ONLY.name
        val sizeName = prefs.getString("font_size", WidgetFontSize.LARGE.name) ?: WidgetFontSize.LARGE.name

        return WidgetConfig(
            template = runCatching { WidgetTemplate.valueOf(templateName) }.getOrDefault(WidgetTemplate.GENIUS),
            attribution = runCatching { WidgetAttribution.valueOf(attrName) }.getOrDefault(WidgetAttribution.ARTIST_ONLY),
            fontSize = runCatching { WidgetFontSize.valueOf(sizeName) }.getOrDefault(WidgetFontSize.LARGE)
        )
    }

    fun saveConfig(config: WidgetConfig) {
        prefs.edit()
            .putString("template", config.template.name)
            .putString("attribution", config.attribution.name)
            .putString("font_size", config.fontSize.name)
            .apply()
    }
}
