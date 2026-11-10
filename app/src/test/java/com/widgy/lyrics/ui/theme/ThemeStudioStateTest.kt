package com.widgy.lyrics.ui.theme

import com.widgy.lyrics.domain.theme.ThemeConfig
import com.widgy.lyrics.domain.theme.ThemeCreditMode
import com.widgy.lyrics.domain.theme.ThemeSizeMode
import com.widgy.lyrics.domain.theme.ThemeSpecs
import com.widgy.lyrics.domain.theme.ThemeTypeface
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeStudioStateTest {

    @Test
    fun themeCategories_hasExactlyFourCategoriesWithDisplayNames() {
        val categories = ThemeCategory.values()
        assertEquals(4, categories.size)
        assertEquals("Appearance", ThemeCategory.APPEARANCE.displayName)
        assertEquals("Typography", ThemeCategory.TYPOGRAPHY.displayName)
        assertEquals("Credits", ThemeCategory.CREDITS.displayName)
        assertEquals("Advanced", ThemeCategory.ADVANCED.displayName)
    }

    @Test
    fun themeConfig_resetRestoresDefaultsWithoutMutatingOthers() {
        val customized = ThemeConfig(
            themeId = ThemeSpecs.TERMINAL.id,
            typeface = ThemeTypeface.MONO,
            sizeMode = ThemeSizeMode.SP_30,
            creditMode = ThemeCreditMode.SONG_AND_ARTIST,
            useAlbumArtBackdrop = true,
            blurRadius = 35,
            showRefreshButton = false
        )

        assertNotEquals(ThemeSpecs.AMBER.id, customized.themeId)
        assertTrue(customized.useAlbumArtBackdrop)

        val resetConfig = ThemeConfig()
        assertEquals(ThemeSpecs.AMBER.id, resetConfig.themeId)
        assertEquals(ThemeTypeface.SANS, resetConfig.typeface)
        assertEquals(ThemeSizeMode.AUTO, resetConfig.sizeMode)
        assertEquals(ThemeCreditMode.NONE, resetConfig.creditMode)
        assertFalse(resetConfig.useAlbumArtBackdrop)
        assertEquals(ThemeConfig.DEFAULT_BLUR_RADIUS, resetConfig.blurRadius)
        assertTrue(resetConfig.showRefreshButton)
    }

    @Test
    fun themeConfig_categoryOptionsArePreservedAcrossUpdates() {
        var config = ThemeConfig()

        // 1. Appearance update
        config = config.copy(themeId = ThemeSpecs.KYOTO.id, useAlbumArtBackdrop = true, blurRadius = 28)
        assertEquals("kyoto", config.themeId)
        assertTrue(config.useAlbumArtBackdrop)
        assertEquals(28, config.blurRadius)

        // 2. Typography update - should preserve Appearance
        config = config.copy(typeface = ThemeTypeface.SERIF, sizeMode = ThemeSizeMode.SP_22)
        assertEquals("kyoto", config.themeId)
        assertEquals(ThemeTypeface.SERIF, config.typeface)
        assertEquals(ThemeSizeMode.SP_22, config.sizeMode)

        // 3. Credits update - should preserve Appearance & Typography
        config = config.copy(creditMode = ThemeCreditMode.ARTIST)
        assertEquals("kyoto", config.themeId)
        assertEquals(ThemeTypeface.SERIF, config.typeface)
        assertEquals(ThemeCreditMode.ARTIST, config.creditMode)

        // 4. Advanced update - should preserve all previous
        config = config.copy(showRefreshButton = false)
        assertEquals("kyoto", config.themeId)
        assertEquals(ThemeCreditMode.ARTIST, config.creditMode)
        assertFalse(config.showRefreshButton)
    }
}
