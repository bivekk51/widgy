package com.widgy.lyrics.ui.share

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.widgy.lyrics.domain.theme.ThemeConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ShareCardHelperTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun generateStoryCard_createsVertical9By16Bitmap() {
        val bitmap = ShareCardHelper.generateStoryCard(
            context = context,
            config = ThemeConfig(),
            lines = listOf("You only get one shot", "Do not miss your chance to blow"),
            songTitle = "Lose Yourself",
            artistName = "Eminem",
            isStoryFormat = true
        )

        assertNotNull(bitmap)
        assertEquals(1080, bitmap.width)
        assertEquals(1920, bitmap.height)
    }

    @Test
    fun generateSquareCard_creates1By1Bitmap() {
        val bitmap = ShareCardHelper.generateStoryCard(
            context = context,
            config = ThemeConfig(),
            lines = listOf("You only get one shot"),
            songTitle = "Lose Yourself",
            artistName = "Eminem",
            isStoryFormat = false
        )

        assertNotNull(bitmap)
        assertEquals(1080, bitmap.width)
        assertEquals(1080, bitmap.height)
    }

    @Test
    fun saveBitmapToShareCache_savesValidPngFile() {
        val bitmap = android.graphics.Bitmap.createBitmap(100, 100, android.graphics.Bitmap.Config.ARGB_8888)
        val file = ShareCardHelper.saveBitmapToShareCache(context, bitmap)

        assertNotNull(file)
        assertTrue(file.exists())
        assertTrue(file.length() > 0)
        assertTrue(file.name.endsWith(".png"))
    }
}
