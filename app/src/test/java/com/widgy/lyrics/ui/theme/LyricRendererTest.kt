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
    fun buildScriptStyledText_splitsDevanagariAndLatinRuns() {
        val mixed = "> Timi Ra Ma तिमी र म"
        val styled = LyricRenderer.buildScriptStyledText(mixed, Typeface.DEFAULT_BOLD, Typeface.DEFAULT)
        assertNotNull(styled)
        assertEquals(mixed, styled.toString())
    }

    @Test
    fun buildScriptStyledText_absorbsNepaliPunctuationIntoDevanagariRun() {
        val nepaliWithPunctuation = "तिमी कता छौ?"
        val styled = LyricRenderer.buildScriptStyledText(nepaliWithPunctuation, Typeface.DEFAULT_BOLD, Typeface.DEFAULT)
        assertNotNull(styled)
        assertEquals(nepaliWithPunctuation, styled.toString())

        val spanned = styled as android.text.Spanned
        val spans = spanned.getSpans(0, spanned.length, CustomTypefaceSpan::class.java)
        // Entire phrase including trailing question mark is absorbed into 1 Devanagari span
        assertEquals(1, spans.size)
    }

    @Test
    fun autoSizeSearch_coupletPreservesLineCadence() {
        // Multi-line stanza should not break into 4 fragmented lines
        val couplet = listOf("तिमी नभए पनि बाटो चल्छ", "तर आज किन यो मन आफैँलाई सोध्दैन")
        val sizeSp = LyricRenderer.findBestAutoSizeSp(
            context = context,
            spec = ThemeSpecs.AMBER,
            typefaceChoice = ThemeTypeface.SANS,
            lines = couplet,
            availableWidthPx = 400,
            availableHeightPx = 250,
            density = 2.0f,
            hasAccentBar = false
        )
        assertTrue("AutoSize should calculate valid size", sizeSp >= 13f)
    }

    @Test
    fun themeSpecs_allThemesPresentWithAmberAsDefault() {
        assertEquals(11, ThemeSpecs.ALL_THEMES.size)
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

        val kyotoSpec = ThemeSpecs.findById("kyoto")
        assertEquals("Kyoto", kyotoSpec.name)
        assertEquals("※", kyotoSpec.swatchGlyph)
        assertEquals(ThemeTypeface.SERIF, kyotoSpec.defaultTypeface)

        val olivettiSpec = ThemeSpecs.findById("olivetti")
        assertEquals("Olivetti", olivettiSpec.name)
        assertEquals("¶", olivettiSpec.swatchGlyph)
        assertEquals(ThemeTypeface.MONO, olivettiSpec.defaultTypeface)
        assertTrue(olivettiSpec.isLeftAligned)

        val bordeauxSpec = ThemeSpecs.findById("bordeaux")
        assertEquals("Bordeaux", bordeauxSpec.name)
        assertEquals("§", bordeauxSpec.swatchGlyph)
        assertTrue(bordeauxSpec.italic)

        val editorialSpec = ThemeSpecs.findById("editorial")
        assertEquals("Editorial", editorialSpec.name)
        assertTrue(editorialSpec.uppercase)
        assertEquals(700, editorialSpec.fontWeight)

        val nordicSpec = ThemeSpecs.findById("nordic")
        assertEquals("Nordic", nordicSpec.name)
        assertEquals("Ø", nordicSpec.swatchGlyph)

        val botanicalSpec = ThemeSpecs.findById("botanical")
        assertEquals("Botanical", botanicalSpec.name)
        assertEquals(ThemeTypeface.SERIF, botanicalSpec.defaultTypeface)
    }

    @Test
    fun render_allElevenThemesProduceValidBitmaps() {
        val sampleLines = listOf("तिमी नभए पनि बाटो चल्छ", "Every moment is a poetry")
        for (spec in ThemeSpecs.ALL_THEMES) {
            val config = ThemeConfig(
                themeId = spec.id,
                typeface = spec.defaultTypeface,
                sizeMode = ThemeSizeMode.AUTO,
                creditMode = ThemeCreditMode.SONG_AND_ARTIST
            )
            val bitmap = LyricRenderer.render(
                context = context,
                config = config,
                lines = sampleLines,
                songTitle = "Test Song",
                artistName = "Test Artist",
                widthPx = 400,
                heightPx = 300
            )
            assertNotNull("Theme ${spec.id} must render valid bitmap", bitmap)
            assertEquals(400, bitmap.width)
            assertEquals(300, bitmap.height)
        }
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

        assertTrue("Auto size should be at least 13sp", sizeSp >= 13f)
    }

    @Test
    fun autoSizeSearch_scalesUpOnLargeWidget() {
        val shortLines = listOf("Timi Ra Ma")
        val sizeSp = LyricRenderer.findBestAutoSizeSp(
            context = context,
            spec = ThemeSpecs.AMBER,
            typefaceChoice = ThemeTypeface.SANS,
            lines = shortLines,
            availableWidthPx = 1000,
            availableHeightPx = 600,
            density = 2.0f,
            hasAccentBar = false
        )

        assertTrue("Short lyrics on large space should scale above 36sp, got $sizeSp", sizeSp > 36f)
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

    @Test
    fun render_withCustomPaddings_drawsCorrectly() {
        val lines = listOf("तिमी नभए पनि बाटो चल्छ")
        val config = ThemeConfig(
            themeId = ThemeSpecs.TERMINAL.id,
            typeface = ThemeTypeface.MONO,
            sizeMode = ThemeSizeMode.AUTO,
            creditMode = ThemeCreditMode.ARTIST
        )

        val bitmap = LyricRenderer.render(
            context = context,
            config = config,
            lines = lines,
            songTitle = "Timi",
            artistName = "Pariwartan",
            widthPx = 400,
            heightPx = 300,
            padTopDp = 20f,
            padBottomDp = 36f,
            padHorizontalDp = 18f
        )

        assertNotNull(bitmap)
        assertEquals(400, bitmap.width)
        assertEquals(300, bitmap.height)
    }

    @Test
    fun render_withAlbumArtBackdrop_drawsWithoutCrashing() {
        val lines = listOf("We gon' be alright")
        val config = ThemeConfig(
            themeId = ThemeSpecs.GENIUS.id,
            typeface = ThemeTypeface.SANS,
            sizeMode = ThemeSizeMode.AUTO,
            creditMode = ThemeCreditMode.SONG_AND_ARTIST,
            useAlbumArtBackdrop = true
        )
        val dummyArt = android.graphics.Bitmap.createBitmap(50, 50, android.graphics.Bitmap.Config.ARGB_8888)

        val bitmap = LyricRenderer.render(
            context = context,
            config = config,
            lines = lines,
            songTitle = "Alright",
            artistName = "Kendrick Lamar",
            widthPx = 500,
            heightPx = 350,
            albumArtBitmap = dummyArt
        )

        assertNotNull(bitmap)
        assertEquals(500, bitmap.width)
        assertEquals(350, bitmap.height)
    }

    @Test
    fun render_capPixels_limitsTotalPixelsForRemoteViewsSafety() {
        val lines = listOf("High resolution render")
        val config = ThemeConfig(
            themeId = ThemeSpecs.AMBER.id,
            typeface = ThemeTypeface.SANS,
            sizeMode = ThemeSizeMode.AUTO,
            creditMode = ThemeCreditMode.SONG_AND_ARTIST
        )

        val cappedBitmap = LyricRenderer.render(
            context = context,
            config = config,
            lines = lines,
            songTitle = "Title",
            artistName = "Artist",
            widthPx = 2000,
            heightPx = 2000,
            capPixels = true
        )
        assertNotNull(cappedBitmap)
        assertTrue(
            "Capped bitmap must have <= 500,000 pixels, was ${cappedBitmap.width * cappedBitmap.height}",
            cappedBitmap.width.toLong() * cappedBitmap.height.toLong() <= 500_000L
        )

        val uncappedBitmap = LyricRenderer.render(
            context = context,
            config = config,
            lines = lines,
            songTitle = "Title",
            artistName = "Artist",
            widthPx = 2000,
            heightPx = 2000,
            capPixels = false
        )
        assertNotNull(uncappedBitmap)
        assertEquals(2000, uncappedBitmap.width)
        assertEquals(2000, uncappedBitmap.height)
    }
}
