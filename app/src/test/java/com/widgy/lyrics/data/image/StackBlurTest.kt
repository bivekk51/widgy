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

    @Test
    fun blurBitmap_recycledBitmap_returnsOriginalBitmap() {
        val src = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        src.recycle()
        val blurred = StackBlur.blur(src, 5)
        assertEquals(src, blurred)
    }

    @Test
    fun blurBitmap_boundaryMinRadius6_succeeds() {
        val src = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888)
        val blurred = StackBlur.blur(src, 6)
        assertNotNull(blurred)
        assertEquals(50, blurred.width)
    }

    @Test
    fun blurBitmap_boundaryDefaultRadius22_succeeds() {
        val src = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888)
        val blurred = StackBlur.blur(src, 22)
        assertNotNull(blurred)
        assertEquals(50, blurred.width)
    }

    @Test
    fun blurBitmap_boundaryMaxRadius38_succeeds() {
        val src = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888)
        val blurred = StackBlur.blur(src, 38)
        assertNotNull(blurred)
        assertEquals(50, blurred.width)
    }
}
