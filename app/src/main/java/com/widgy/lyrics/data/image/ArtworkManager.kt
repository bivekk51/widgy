package com.widgy.lyrics.data.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.widgy.lyrics.data.api.ArtworkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class ArtworkManager(
    private val context: Context,
    private val client: ArtworkClient = ArtworkClient()
) {
    private val artDir: File by lazy {
        File(context.filesDir, "album_art").apply { if (!exists()) mkdirs() }
    }

    fun getLocalArtFile(songId: Long): File {
        return File(artDir, "song_$songId.jpg")
    }

    suspend fun getOrFetchArtworkBitmap(
        songId: Long,
        title: String,
        artist: String,
        maxDimension: Int = 300
    ): Bitmap? = withContext(Dispatchers.IO) {
        val file = getLocalArtFile(songId)
        if (file.exists() && file.length() > 0) {
            val cached = decodeSampledBitmap(file.absolutePath, maxDimension)
            if (cached != null) {
                return@withContext cached
            }
            file.delete()
        }

        val urlResult = client.fetchArtworkUrl(title, artist)
        val url = urlResult.getOrNull() ?: return@withContext null

        val bytesResult = client.fetchArtworkBytes(url)
        val bytes = bytesResult.getOrNull() ?: return@withContext null

        runCatching {
            FileOutputStream(file).use { it.write(bytes) }
        }

        val decoded = decodeSampledBitmap(file.absolutePath, maxDimension)
        if (decoded == null && file.exists()) {
            file.delete()
        }
        decoded
    }

    fun loadCachedArtworkBitmap(songId: Long, maxDimension: Int = 300): Bitmap? {
        val file = getLocalArtFile(songId)
        if (!file.exists() || file.length() == 0L) return null
        val decoded = decodeSampledBitmap(file.absolutePath, maxDimension)
        if (decoded == null && file.exists()) {
            file.delete()
        }
        return decoded
    }

    companion object {
        fun decodeSampledBitmap(filePath: String, maxDimension: Int): Bitmap? {
            return runCatching {
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(filePath, options)

                var inSampleSize = 1
                while (options.outWidth / (inSampleSize * 2) >= maxDimension &&
                    options.outHeight / (inSampleSize * 2) >= maxDimension
                ) {
                    inSampleSize *= 2
                }

                val decodeOptions = BitmapFactory.Options().apply {
                    this.inSampleSize = inSampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                BitmapFactory.decodeFile(filePath, decodeOptions)
            }.getOrNull()
        }
    }
}
