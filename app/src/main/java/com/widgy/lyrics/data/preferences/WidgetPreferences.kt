package com.widgy.lyrics.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.widgy.lyrics.domain.model.DailyLyric
import com.widgy.lyrics.domain.theme.ThemeConfig
import com.widgy.lyrics.domain.theme.ThemeCreditMode
import com.widgy.lyrics.domain.theme.ThemeSizeMode
import com.widgy.lyrics.domain.theme.ThemeSpecs
import com.widgy.lyrics.domain.theme.ThemeTypeface

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
            .commit()
    }

    fun isLyricPinned(): Boolean = prefs.getBoolean("is_lyric_pinned", false)

    fun getPinnedLyric(): DailyLyric? {
        if (!isLyricPinned()) return null
        val text = prefs.getString("pinned_lyric_text", null) ?: return null
        val songTitle = prefs.getString("pinned_lyric_title", "") ?: ""
        val artistName = prefs.getString("pinned_lyric_artist", "") ?: ""
        val songId = prefs.getLong("pinned_lyric_song_id", 0L)
        val snippetId = prefs.getLong("pinned_lyric_snippet_id", -1L).takeIf { it != -1L }
        val date = prefs.getString("pinned_lyric_date", "") ?: ""
        return DailyLyric(
            date = date,
            songId = songId,
            snippetId = snippetId,
            text = text,
            songTitle = songTitle,
            artistName = artistName
        )
    }

    fun setLyricPinned(pinned: Boolean, lyric: DailyLyric? = null) {
        val editor = prefs.edit().putBoolean("is_lyric_pinned", pinned)
        if (pinned && lyric != null) {
            editor.putString("pinned_lyric_text", lyric.text)
                .putString("pinned_lyric_title", lyric.songTitle)
                .putString("pinned_lyric_artist", lyric.artistName)
                .putLong("pinned_lyric_song_id", lyric.songId)
                .putLong("pinned_lyric_snippet_id", lyric.snippetId ?: -1L)
                .putString("pinned_lyric_date", lyric.date)
        } else if (!pinned) {
            editor.remove("pinned_lyric_text")
                .remove("pinned_lyric_title")
                .remove("pinned_lyric_artist")
                .remove("pinned_lyric_song_id")
                .remove("pinned_lyric_snippet_id")
                .remove("pinned_lyric_date")
        }
        editor.apply()
    }

    fun getThemeConfig(): ThemeConfig {
        val themeId = prefs.getString("theme_id", null)
        val useAlbumArt = prefs.getBoolean("theme_use_album_art", false)
        val showRefresh = prefs.getBoolean("theme_show_refresh_btn", true)

        if (themeId != null) {
            val typefaceStr = prefs.getString("theme_typeface", ThemeTypeface.SANS.name) ?: ThemeTypeface.SANS.name
            val sizeModeStr = prefs.getString("theme_size_mode", ThemeSizeMode.AUTO.name) ?: ThemeSizeMode.AUTO.name
            val creditModeStr = prefs.getString("theme_credit_mode", ThemeCreditMode.NONE.name) ?: ThemeCreditMode.NONE.name
            return ThemeConfig(
                themeId = themeId,
                typeface = runCatching { ThemeTypeface.valueOf(typefaceStr) }.getOrDefault(ThemeTypeface.SANS),
                sizeMode = runCatching { ThemeSizeMode.valueOf(sizeModeStr) }.getOrDefault(ThemeSizeMode.AUTO),
                creditMode = runCatching { ThemeCreditMode.valueOf(creditModeStr) }.getOrDefault(ThemeCreditMode.NONE),
                useAlbumArtBackdrop = useAlbumArt,
                showRefreshButton = showRefresh
            )
        }

        // Migrate from legacy WidgetConfig if present
        val legacyTemplate = prefs.getString("template", null)
        if (legacyTemplate != null) {
            val mappedThemeId = when (legacyTemplate.uppercase()) {
                "GENIUS" -> "genius"
                "SPOTIFY" -> "spotify"
                "CLASSIC" -> "classic"
                "TERMINAL" -> "terminal"
                "MINIMAL" -> "genius"
                else -> "amber"
            }
            val legacyFont = prefs.getString("font_family", "SYSTEM") ?: "SYSTEM"
            val mappedTypeface = when (legacyFont.uppercase()) {
                "SERIF" -> ThemeTypeface.SERIF
                "MONOSPACE" -> ThemeTypeface.MONO
                else -> ThemeTypeface.SANS
            }
            val legacySize = prefs.getString("font_size", "LARGE") ?: "LARGE"
            val mappedSize = when (legacySize.uppercase()) {
                "SMALL" -> ThemeSizeMode.SP_16
                "MEDIUM", "REGULAR" -> ThemeSizeMode.SP_22
                "LARGE" -> ThemeSizeMode.SP_30
                "EXTRA_LARGE", "JUMBO" -> ThemeSizeMode.SP_40
                else -> ThemeSizeMode.AUTO
            }
            val legacyAttr = prefs.getString("attribution", "NONE") ?: "NONE"
            val mappedCredit = when (legacyAttr.uppercase()) {
                "ARTIST_ONLY" -> ThemeCreditMode.ARTIST
                "SONG_AND_ARTIST" -> ThemeCreditMode.SONG_AND_ARTIST
                else -> ThemeCreditMode.NONE
            }
            return ThemeConfig(
                themeId = mappedThemeId,
                typeface = mappedTypeface,
                sizeMode = mappedSize,
                creditMode = mappedCredit,
                useAlbumArtBackdrop = useAlbumArt,
                showRefreshButton = showRefresh
            )
        }

        // Default as specified in prompt: Amber, Sans, Auto, None
        return ThemeConfig(
            themeId = ThemeSpecs.AMBER.id,
            typeface = ThemeTypeface.SANS,
            sizeMode = ThemeSizeMode.AUTO,
            creditMode = ThemeCreditMode.NONE,
            useAlbumArtBackdrop = useAlbumArt,
            showRefreshButton = showRefresh
        )
    }

    fun saveThemeConfig(config: ThemeConfig) {
        prefs.edit()
            .putString("theme_id", config.themeId)
            .putString("theme_typeface", config.typeface.name)
            .putString("theme_size_mode", config.sizeMode.name)
            .putString("theme_credit_mode", config.creditMode.name)
            .putBoolean("theme_use_album_art", config.useAlbumArtBackdrop)
            .putBoolean("theme_show_refresh_btn", config.showRefreshButton)
            // Also keep legacy keys in sync for backward compatibility
            .putString("template", config.themeId.uppercase())
            .putString("font_family", when (config.typeface) {
                ThemeTypeface.SANS -> WidgetFontFamily.SYSTEM.name
                ThemeTypeface.SERIF -> WidgetFontFamily.SERIF.name
                ThemeTypeface.MONO -> WidgetFontFamily.MONOSPACE.name
            })
            .putString("font_size", when (config.sizeMode) {
                ThemeSizeMode.SP_16 -> WidgetFontSize.SMALL.name
                ThemeSizeMode.SP_22 -> WidgetFontSize.MEDIUM.name
                ThemeSizeMode.SP_30 -> WidgetFontSize.LARGE.name
                ThemeSizeMode.SP_40 -> WidgetFontSize.EXTRA_LARGE.name
                ThemeSizeMode.AUTO -> WidgetFontSize.LARGE.name
            })
            .putString("attribution", when (config.creditMode) {
                ThemeCreditMode.NONE -> WidgetAttribution.NONE.name
                ThemeCreditMode.ARTIST -> WidgetAttribution.ARTIST_ONLY.name
                ThemeCreditMode.SONG_AND_ARTIST -> WidgetAttribution.SONG_AND_ARTIST.name
            })
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
            .commit()
    }
}
