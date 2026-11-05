package com.widgy.lyrics.data.preferences

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class WidgetPreferencesTest {

    private val context: Context = mockk(relaxed = true)
    private val prefs: SharedPreferences = mockk(relaxed = true)
    private val editor: SharedPreferences.Editor = mockk(relaxed = true)

    @Before
    fun setup() {
        every { context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE) } returns prefs
        every { prefs.edit() } returns editor
        every { editor.putString(any(), any()) } returns editor
    }

    @Test
    fun defaultWidgetConfig_hasExpectedValues() {
        val config = WidgetConfig()
        assertEquals(WidgetTemplate.GENIUS, config.template)
        assertEquals(WidgetFontFamily.SYSTEM, config.fontFamily)
        assertEquals(WidgetAttribution.ARTIST_ONLY, config.attribution)
        assertEquals(WidgetFontSize.LARGE, config.fontSize)
    }

    @Test
    fun getConfig_readsStoredValues() {
        every { prefs.getString("template", any()) } returns WidgetTemplate.SPOTIFY.name
        every { prefs.getString("font_family", any()) } returns WidgetFontFamily.SERIF.name
        every { prefs.getString("attribution", any()) } returns WidgetAttribution.NONE.name
        every { prefs.getString("font_size", any()) } returns WidgetFontSize.EXTRA_LARGE.name

        val widgetPrefs = WidgetPreferences(context)
        val loaded = widgetPrefs.getConfig()

        assertEquals(WidgetTemplate.SPOTIFY, loaded.template)
        assertEquals(WidgetFontFamily.SERIF, loaded.fontFamily)
        assertEquals(WidgetAttribution.NONE, loaded.attribution)
        assertEquals(WidgetFontSize.EXTRA_LARGE, loaded.fontSize)
    }

    @Test
    fun saveConfig_writesAllValues() {
        val widgetPrefs = WidgetPreferences(context)
        val config = WidgetConfig(
            template = WidgetTemplate.TERMINAL,
            fontFamily = WidgetFontFamily.MONOSPACE,
            attribution = WidgetAttribution.SONG_AND_ARTIST,
            fontSize = WidgetFontSize.REGULAR
        )

        widgetPrefs.saveConfig(config)

        verify {
            editor.putString("template", WidgetTemplate.TERMINAL.name)
            editor.putString("font_family", WidgetFontFamily.MONOSPACE.name)
            editor.putString("attribution", WidgetAttribution.SONG_AND_ARTIST.name)
            editor.putString("font_size", WidgetFontSize.REGULAR.name)
            editor.apply()
        }
    }
}
