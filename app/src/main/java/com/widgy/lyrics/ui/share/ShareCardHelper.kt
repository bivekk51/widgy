package com.widgy.lyrics.ui.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import com.widgy.lyrics.domain.theme.ThemeConfig
import com.widgy.lyrics.ui.theme.LyricRenderer
import java.io.File
import java.io.FileOutputStream

object ShareCardHelper {

    fun generateStoryCard(
        context: Context,
        config: ThemeConfig,
        lines: List<String>,
        songTitle: String,
        artistName: String,
        albumArtBitmap: Bitmap? = null,
        isStoryFormat: Boolean = true
    ): Bitmap {
        val width = 1080
        val height = if (isStoryFormat) 1920 else 1080

        val padTop = if (isStoryFormat) 180f else 60f
        val padBottom = if (isStoryFormat) 140f else 60f
        val padHorizontal = 48f

        return LyricRenderer.render(
            context = context,
            config = config,
            lines = lines,
            songTitle = songTitle,
            artistName = artistName,
            widthPx = width,
            heightPx = height,
            padTopDp = padTop,
            padBottomDp = padBottom,
            padHorizontalDp = padHorizontal,
            albumArtBitmap = albumArtBitmap,
            capPixels = false
        )
    }

    fun saveBitmapToShareCache(context: Context, bitmap: Bitmap): File {
        val shareDir = File(context.cacheDir, "share").apply { if (!exists()) mkdirs() }
        val shareFile = File(shareDir, "widgy_lyric_card.png")
        FileOutputStream(shareFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return shareFile
    }

    fun shareLyricCard(
        context: Context,
        bitmap: Bitmap,
        songTitle: String = "",
        artistName: String = ""
    ) {
        val file = saveBitmapToShareCache(context, bitmap)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            val caption = if (songTitle.isNotBlank() && artistName.isNotBlank()) {
                "\"$songTitle\" by $artistName · Widgy"
            } else {
                "Daily Lyric via Widgy"
            }
            putExtra(Intent.EXTRA_TEXT, caption)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(shareIntent, "Share Lyric Card").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
