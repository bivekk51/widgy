package com.widgy.lyrics.ui.theme

import android.content.Context
import android.graphics.Typeface
import androidx.test.core.app.ApplicationProvider
import com.widgy.lyrics.domain.theme.ThemeConfig
import com.widgy.lyrics.domain.theme.ThemeCreditMode
import com.widgy.lyrics.domain.theme.ThemeSizeMode
import com.widgy.lyrics.domain.theme.ThemeSpecs
import com.widgy.lyrics.domain.theme.ThemeTypeface
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LyricRendererTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun isDevanagariChar_identifiesDevanagariAndLatin() {
        assertTrue(LyricRenderer.isDevanagariChar('क'))
        assertTrue(LyricRenderer.isDevanagariChar('ि'))
        assertTrue(LyricRenderer.isDevanagariChar('ा'))
        assertTrue(LyricRenderer.isDevanagariChar('अ'))

        assertFalse(LyricRenderer.isDevanagariChar('a'))
        assertFalse(LyricRenderer.isDevanagariChar('Z'))
        assertFalse(LyricRenderer.isDevanagariChar('1'))
        assertFalse(LyricRenderer.isDevanagariChar('>'))
    }

    @Test
    fun buildMonoStyledText_splitsDevanagariAndLatinRuns() {
        val mixed = "> Timi Ra Ma तिमी र म"
        val styled = LyricRenderer.buildMonoStyledText(mixed, Typeface.DEFAULT)
        assertNotNull(styled)
        assertEquals(mixed, styled.toString())
    }

    @Test
    fun themeSpecs_allThemesPresentWithAmberAsDefault() {
        assertEquals(5, ThemeSpecs.ALL_THEMES.size)
        val defaultSpec = ThemeSpecs.findById("amber")
        assertEquals("Amber", defaultSpec.name)
        assertEquals(ThemeTypeface.SANS, defaultSpec.defaultTypeface)

        val terminalSpec = ThemeSpecs.findById("terminal")
        assertTrue(terminalSpec.promptMarker)
        assertTrue(terminalSpec.isLeftAligned)

        val spotifySpec = ThemeSpecs.findById("spotify")
        assertTrue(spotifySpec.accentBar)

        val geniusSpec = ThemeSpecs.findById("genius")
        assertTrue(geniusSpec.uppercase)
        assertEquals(700, geniusSpec.fontWeight)

        val classicSpec = ThemeSpecs.findById("classic")
        assertTrue(classicSpec.italic)
        assertEquals(ThemeTypeface.SERIF, classicSpec.defaultTypeface)
    }

    @Test
    fun autoSizeSearch_picksSizeFittingAvailableHeight() {
        val lines = listOf("तिमी नभए पनि बाटो चल्छ, उज्यालो साथै हुन्छ", "तर आज किन यो मन आफैँलाई सोध्दैन?")
        val sizeSp = LyricRenderer.findBestAutoSizeSp(
            context = context,
            spec = ThemeSpecs.AMBER,
            typefaceChoice = ThemeTypeface.SANS,
            lines = lines,
            availableWidthPx = 800,
            availableHeightPx = 300,
            density = 2.0f,
            hasAccentBar = false
        )

        assertTrue("Auto size should be between 13sp and 36sp", sizeSp in 13f..36f)
    }

    @Test
    fun render_producesValidBitmap() {
        val lines = listOf("WHAT IF TODAY WAS THE RAPTURE", "AND YOU COMPLETELY TARNISHED")
        val config = ThemeConfig(
            themeId = ThemeSpecs.AMBER.id,
            typeface = ThemeTypeface.SANS,
            sizeMode = ThemeSizeMode.AUTO,
            creditMode = ThemeCreditMode.SONG_AND_ARTIST
        )

        val bitmap = LyricRenderer.render(
            context = context,
            config = config,
            lines = lines,
            songTitle = "Euphoria",
            artistName = "Kendrick Lamar",
            widthPx = 600,
            heightPx = 400
        )

        assertNotNull(bitmap)
        assertEquals(600, bitmap.width)
        assertEquals(400, bitmap.height)
    }
}
