package com.widgy.lyrics.data.image

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StackBlurTest {

    @Test
    fun blurBitmap_preservesDimensionsAndProducesValidBitmap() {
        val src = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888)
        src.eraseColor(Color.RED)

        val blurred = StackBlur.blur(src, 5)

        assertNotNull(blurred)
        assertEquals(20, blurred.width)
        assertEquals(20, blurred.height)
    }

    @Test
    fun blurBitmap_radiusZero_returnsOriginalBitmap() {
        val src = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        val blurred = StackBlur.blur(src, 0)
        assertEquals(src, blurred)
    }
}
