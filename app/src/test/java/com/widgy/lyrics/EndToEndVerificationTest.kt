package com.widgy.lyrics

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.widgy.lyrics.data.api.LyricsCleaner
import com.widgy.lyrics.widget.DailyLyricWidgetReceiver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class EndToEndVerificationTest {

    @Test
    fun zeroProcessContract_noPersistentServicesDeclared() {
        val manifestFile = File("src/main/AndroidManifest.xml")
        val manifestContent = if (manifestFile.exists()) {
            manifestFile.readText()
        } else {
            File("app/src/main/AndroidManifest.xml").readText()
        }

        // Verify there is NO <service> declared in AndroidManifest
        assertFalse(
            "AndroidManifest should never declare background services to ensure 0% background RAM drain",
            manifestContent.contains("<service")
        )
        // Verify receiver is declared
        assertTrue(
            "AndroidManifest must declare DailyLyricWidgetReceiver",
            manifestContent.contains("DailyLyricWidgetReceiver")
        )
    }

    @Test
    fun reviewFocus_syncedTimestampStripping_handlesBracketsAndHeaders() {
        val rawLrc = """
            [00:04.12]Ayy, I remember syrup sandwiches and crime allowances
            [00:08.55]Finesse a nigga with some counterfeits, but now I’m countin' this
            [Chorus]
            [00:14.30]Sit down, be humble
            [00:16.80]Sit down, be humble
        """.trimIndent()

        val lines = LyricsCleaner.cleanLyrics(rawLrc)
        assertEquals(4, lines.size)
        assertEquals("Ayy, I remember syrup sandwiches and crime allowances", lines[0].text)
        assertFalse(lines[0].isHook)
        assertEquals("Sit down, be humble", lines[2].text)
        assertTrue(lines[2].isHook)
    }

    @Test
    fun reviewFocus_receiverInstantiation_succeeds() {
        val receiver = DailyLyricWidgetReceiver()
        assertNotNull(receiver.glanceAppWidget)
    }
}
