package com.widgy.lyrics.data.preferences

import android.content.Context
import android.content.SharedPreferences

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
    REGULAR("Medium", 20, 27),
    LARGE("Large", 24, 32),
    EXTRA_LARGE("Extra Large", 28, 36),
    JUMBO("Jumbo", 32, 40)
}

data class WidgetConfig(
    val template: WidgetTemplate = WidgetTemplate.GENIUS,
    val fontFamily: WidgetFontFamily = WidgetFontFamily.SYSTEM,
    val attribution: WidgetAttribution = WidgetAttribution.ARTIST_ONLY,
    val fontSize: WidgetFontSize = WidgetFontSize.LARGE
)

class WidgetPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)

    fun getConfig(): WidgetConfig {
        val templateName = prefs.getString("template", WidgetTemplate.GENIUS.name) ?: WidgetTemplate.GENIUS.name
        val fontName = prefs.getString("font_family", WidgetFontFamily.SYSTEM.name) ?: WidgetFontFamily.SYSTEM.name
        val attrName = prefs.getString("attribution", WidgetAttribution.ARTIST_ONLY.name) ?: WidgetAttribution.ARTIST_ONLY.name
        val sizeName = prefs.getString("font_size", WidgetFontSize.LARGE.name) ?: WidgetFontSize.LARGE.name

        return WidgetConfig(
            template = runCatching { WidgetTemplate.valueOf(templateName) }.getOrDefault(WidgetTemplate.GENIUS),
            fontFamily = runCatching { WidgetFontFamily.valueOf(fontName) }.getOrDefault(WidgetFontFamily.SYSTEM),
            attribution = runCatching { WidgetAttribution.valueOf(attrName) }.getOrDefault(WidgetAttribution.ARTIST_ONLY),
            fontSize = runCatching { WidgetFontSize.valueOf(sizeName) }.getOrDefault(WidgetFontSize.LARGE)
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
