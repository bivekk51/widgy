package com.widgy.lyrics.domain.theme

enum class ThemeTypeface(val displayName: String) {
    SANS("Sans"),
    SERIF("Serif"),
    MONO("Mono")
}

enum class ThemeSizeMode(val displayName: String, val fixedSp: Float? = null) {
    AUTO("Auto", null),
    SP_16("16", 16f),
    SP_22("22", 22f),
    SP_30("30", 30f),
    SP_40("40", 40f)
}

enum class ThemeCreditMode(val displayName: String) {
    NONE("None"),
    ARTIST("Artist"),
    SONG_AND_ARTIST("Song and artist")
}

data class ThemeSpec(
    val id: String,
    val name: String,
    val description: String,
    val backgroundColor: Int,
    val foregroundColor: Int,
    val defaultTypeface: ThemeTypeface,
    val isLeftAligned: Boolean = false,
    val fontWeight: Int = 500, // 500 medium, 700 bold
    val uppercase: Boolean = false,
    val italic: Boolean = false,
    val promptMarker: Boolean = false,
    val promptOnEveryLine: Boolean = false,
    val accentBar: Boolean = false,
    val swatchGlyph: String = "अ"
)

object ThemeSpecs {
    val AMBER = ThemeSpec(
        id = "amber",
        name = "Amber",
        description = "Warm amber on deep navy",
        backgroundColor = 0xFF0D1117.toInt(),
        foregroundColor = 0xFFFFB454.toInt(),
        defaultTypeface = ThemeTypeface.SANS,
        isLeftAligned = false,
        fontWeight = 500,
        swatchGlyph = "अ"
    )

    val TERMINAL = ThemeSpec(
        id = "terminal",
        name = "Terminal",
        description = "Prompt marker, monospace, left aligned",
        backgroundColor = 0xFF0A0F0A.toInt(),
        foregroundColor = 0xFF4ADE80.toInt(),
        defaultTypeface = ThemeTypeface.MONO,
        isLeftAligned = true,
        fontWeight = 500,
        promptMarker = true,
        promptOnEveryLine = false,
        swatchGlyph = ">"
    )

    val GENIUS = ThemeSpec(
        id = "genius",
        name = "Genius style",
        description = "Pitch-black, bold all-caps",
        backgroundColor = 0xFF000000.toInt(),
        foregroundColor = 0xFFFFFFFF.toInt(),
        defaultTypeface = ThemeTypeface.SANS,
        isLeftAligned = false,
        fontWeight = 700,
        uppercase = true,
        swatchGlyph = "अ"
    )

    val SPOTIFY = ThemeSpec(
        id = "spotify",
        name = "Spotify glow",
        description = "Dark with a green accent",
        backgroundColor = 0xFF121212.toInt(),
        foregroundColor = 0xFFFFFFFF.toInt(),
        defaultTypeface = ThemeTypeface.SANS,
        isLeftAligned = false,
        fontWeight = 500,
        accentBar = true,
        swatchGlyph = "अ"
    )

    val CLASSIC = ThemeSpec(
        id = "classic",
        name = "Classic",
        description = "Warm paper, italic serif",
        backgroundColor = 0xFFF1E6D0.toInt(),
        foregroundColor = 0xFF3A2A14.toInt(),
        defaultTypeface = ThemeTypeface.SERIF,
        isLeftAligned = false,
        fontWeight = 500,
        italic = true,
        swatchGlyph = "अ"
    )

    // --- Curated Expansion (Council Approved) ---

    val KYOTO = ThemeSpec(
        id = "kyoto",
        name = "Kyoto",
        description = "Deep indigo, Japanese washi silk",
        backgroundColor = 0xFF0B132B.toInt(),
        foregroundColor = 0xFFEAE5D9.toInt(),
        defaultTypeface = ThemeTypeface.SERIF,
        isLeftAligned = false,
        fontWeight = 500,
        swatchGlyph = "※"
    )

    val OLIVETTI = ThemeSpec(
        id = "olivetti",
        name = "Olivetti",
        description = "Industrial putty, typewriter carbon",
        backgroundColor = 0xFFEBE7DF.toInt(),
        foregroundColor = 0xFF1C1F22.toInt(),
        defaultTypeface = ThemeTypeface.MONO,
        isLeftAligned = true,
        fontWeight = 500,
        promptMarker = false,
        swatchGlyph = "¶"
    )

    val BORDEAUX = ThemeSpec(
        id = "bordeaux",
        name = "Bordeaux",
        description = "Velvet wine, champagne gold serif",
        backgroundColor = 0xFF18090E.toInt(),
        foregroundColor = 0xFFF4E6CC.toInt(),
        defaultTypeface = ThemeTypeface.SERIF,
        isLeftAligned = false,
        fontWeight = 500,
        italic = true,
        swatchGlyph = "§"
    )

    val EDITORIAL = ThemeSpec(
        id = "editorial",
        name = "Editorial",
        description = "Titanium ivory, bold monochrome",
        backgroundColor = 0xFFF8F7F4.toInt(),
        foregroundColor = 0xFF121316.toInt(),
        defaultTypeface = ThemeTypeface.SANS,
        isLeftAligned = false,
        fontWeight = 700,
        uppercase = true,
        swatchGlyph = "A"
    )

    val NORDIC = ThemeSpec(
        id = "nordic",
        name = "Nordic",
        description = "Fjord slate, arctic mist sans",
        backgroundColor = 0xFF0F171E.toInt(),
        foregroundColor = 0xFFDCEBF5.toInt(),
        defaultTypeface = ThemeTypeface.SANS,
        isLeftAligned = false,
        fontWeight = 500,
        swatchGlyph = "Ø"
    )

    val BOTANICAL = ThemeSpec(
        id = "botanical",
        name = "Botanical",
        description = "Deep pine moss, sage porcelain",
        backgroundColor = 0xFF0B160E.toInt(),
        foregroundColor = 0xFFE1EEE5.toInt(),
        defaultTypeface = ThemeTypeface.SERIF,
        isLeftAligned = false,
        fontWeight = 500,
        swatchGlyph = "अ"
    )

    val ALL_THEMES = listOf(
        AMBER,
        TERMINAL,
        GENIUS,
        SPOTIFY,
        CLASSIC,
        KYOTO,
        OLIVETTI,
        BORDEAUX,
        EDITORIAL,
        NORDIC,
        BOTANICAL
    )

    fun findById(id: String): ThemeSpec {
        return ALL_THEMES.find { it.id.equals(id, ignoreCase = true) }
            ?: when (id.uppercase()) {
                "MINIMAL" -> GENIUS
                else -> AMBER
            }
    }
}

data class ThemeConfig(
    val themeId: String = ThemeSpecs.AMBER.id,
    val typeface: ThemeTypeface = ThemeTypeface.SANS,
    val sizeMode: ThemeSizeMode = ThemeSizeMode.AUTO,
    val creditMode: ThemeCreditMode = ThemeCreditMode.NONE,
    val useAlbumArtBackdrop: Boolean = false,
    val showRefreshButton: Boolean = true
) {
    val themeSpec: ThemeSpec get() = ThemeSpecs.findById(themeId)
}
