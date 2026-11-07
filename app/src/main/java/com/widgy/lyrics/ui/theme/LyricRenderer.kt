package com.widgy.lyrics.ui.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.text.style.LeadingMarginSpan
import android.text.style.MetricAffectingSpan
import androidx.core.content.res.ResourcesCompat
import com.widgy.lyrics.R
import com.widgy.lyrics.domain.theme.ThemeConfig
import com.widgy.lyrics.domain.theme.ThemeCreditMode
import com.widgy.lyrics.domain.theme.ThemeSizeMode
import com.widgy.lyrics.domain.theme.ThemeSpec
import com.widgy.lyrics.domain.theme.ThemeSpecs
import com.widgy.lyrics.domain.theme.ThemeTypeface
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

class CustomTypefaceSpan(private val typeface: Typeface) : MetricAffectingSpan() {
    override fun updateDrawState(ds: TextPaint) {
        ds.typeface = typeface
    }

    override fun updateMeasureState(paint: TextPaint) {
        paint.typeface = typeface
    }
}

object LyricRenderer {

    // Cached typefaces
    private var cachedSansMedium: Typeface? = null
    private var cachedSansBold: Typeface? = null
    private var cachedSerifMedium: Typeface? = null
    private var cachedSerifBold: Typeface? = null

    fun loadTypefaces(context: Context) {
        if (cachedSansMedium == null) {
            cachedSansMedium = runCatching {
                ResourcesCompat.getFont(context, R.font.noto_sans_devanagari_medium)
            }.getOrNull() ?: Typeface.SANS_SERIF
        }
        if (cachedSansBold == null) {
            cachedSansBold = runCatching {
                ResourcesCompat.getFont(context, R.font.noto_sans_devanagari_bold)
            }.getOrNull() ?: Typeface.DEFAULT_BOLD
        }
        if (cachedSerifMedium == null) {
            cachedSerifMedium = runCatching {
                ResourcesCompat.getFont(context, R.font.noto_serif_devanagari_medium)
            }.getOrNull() ?: Typeface.SERIF
        }
        if (cachedSerifBold == null) {
            cachedSerifBold = runCatching {
                ResourcesCompat.getFont(context, R.font.noto_serif_devanagari_bold)
            }.getOrNull() ?: Typeface.SERIF
        }
    }

    fun isDevanagariChar(c: Char): Boolean {
        val block = Character.UnicodeBlock.of(c)
        return block == Character.UnicodeBlock.DEVANAGARI ||
                block == Character.UnicodeBlock.DEVANAGARI_EXTENDED ||
                block == Character.UnicodeBlock.VEDIC_EXTENSIONS
    }

    /**
     * Splits text into script runs:
     * Devanagari characters receive [devanagariTf], while Latin characters, digits,
     * and punctuation receive [latinTf]. This avoids missing-glyph tofu boxes and
     * font metric corruption when mixing English and Nepali lyrics.
     */
    fun buildScriptStyledText(
        text: String,
        latinTf: Typeface,
        devanagariTf: Typeface
    ): CharSequence {
        val ssb = SpannableStringBuilder(text)
        var i = 0
        while (i < text.length) {
            if (isDevanagariChar(text[i])) {
                val start = i
                while (i < text.length && (isDevanagariChar(text[i]) || text[i] == ' ' || text[i] == '।' || text[i] == '॥')) {
                    i++
                }
                ssb.setSpan(
                    CustomTypefaceSpan(devanagariTf),
                    start,
                    i,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            } else {
                val start = i
                while (i < text.length && !isDevanagariChar(text[i])) {
                    i++
                }
                ssb.setSpan(
                    CustomTypefaceSpan(latinTf),
                    start,
                    i,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
        return ssb
    }

    /** Legacy helper preserved for unit tests */
    fun buildMonoStyledText(text: String, devanagariTf: Typeface): CharSequence {
        return buildScriptStyledText(text, Typeface.MONOSPACE, devanagariTf)
    }

    fun resolveTypeface(context: Context, spec: ThemeSpec, typefaceChoice: ThemeTypeface): Typeface {
        loadTypefaces(context)
        val isBold = spec.fontWeight >= 700
        return when (typefaceChoice) {
            ThemeTypeface.SANS -> if (isBold) cachedSansBold ?: Typeface.DEFAULT_BOLD else cachedSansMedium ?: Typeface.SANS_SERIF
            ThemeTypeface.SERIF -> if (isBold) cachedSerifBold ?: Typeface.SERIF else cachedSerifMedium ?: Typeface.SERIF
            ThemeTypeface.MONO -> if (isBold) Typeface.create(Typeface.MONOSPACE, Typeface.BOLD) else Typeface.MONOSPACE
        }
    }

    private fun resolveLatinTypeface(spec: ThemeSpec, typefaceChoice: ThemeTypeface): Typeface {
        val isBold = spec.fontWeight >= 700
        return when (typefaceChoice) {
            ThemeTypeface.SANS -> if (isBold) Typeface.DEFAULT_BOLD else Typeface.SANS_SERIF
            ThemeTypeface.SERIF -> if (isBold) Typeface.create(Typeface.SERIF, Typeface.BOLD) else Typeface.SERIF
            ThemeTypeface.MONO -> if (isBold) Typeface.create(Typeface.MONOSPACE, Typeface.BOLD) else Typeface.MONOSPACE
        }
    }

    private fun resolveDevanagariTypeface(spec: ThemeSpec, typefaceChoice: ThemeTypeface): Typeface {
        val isBold = spec.fontWeight >= 700
        return when (typefaceChoice) {
            ThemeTypeface.SERIF -> if (isBold) cachedSerifBold ?: Typeface.SERIF else cachedSerifMedium ?: Typeface.SERIF
            else -> if (isBold) cachedSansBold ?: Typeface.DEFAULT_BOLD else cachedSansMedium ?: Typeface.SANS_SERIF
        }
    }

    /**
     * Binary search to find optimal font size.
     * Scales dynamically up to 54sp on large widgets while checking height, line width,
     * and line count to preserve poetic line structure.
     */
    fun findBestAutoSizeSp(
        context: Context,
        spec: ThemeSpec,
        typefaceChoice: ThemeTypeface,
        lines: List<String>,
        availableWidthPx: Int,
        availableHeightPx: Int,
        density: Float,
        hasAccentBar: Boolean
    ): Float {
        val extraReservedPx = if (hasAccentBar) (22f * density).roundToInt() else 0
        val targetHeightPx = max(10, availableHeightPx - extraReservedPx)
        val safeWidth = max(availableWidthPx, 40)

        // Dynamic ceiling based on available height: allows large widgets (4x3, 4x4)
        // to scale up gracefully instead of being artificially stuck at 36sp
        val highCap = max(36f, min(54f, targetHeightPx / (max(lines.size, 1) * 1.55f * density)))
        var low = 12f
        var high = highCap
        var best = 13f

        for (step in 0..12) {
            val mid = (low + high) / 2f
            val layout = createTextLayout(
                context = context,
                spec = spec,
                typefaceChoice = typefaceChoice,
                lines = lines,
                textSizeSp = mid,
                targetWidthPx = safeWidth,
                density = density
            )

            // Check: height fits AND lines don't break into excessive fragments
            val maxAllowedLines = max(lines.size * 2, 2)
            var fitsHorizontally = true
            for (lineIdx in 0 until layout.lineCount) {
                if (layout.getLineWidth(lineIdx) > safeWidth + 1) {
                    fitsHorizontally = false
                    break
                }
            }

            if (layout.height <= targetHeightPx && layout.lineCount <= maxAllowedLines && fitsHorizontally) {
                best = mid
                low = mid + 0.5f
            } else {
                high = mid - 0.5f
            }
            if (high - low < 0.25f) break
        }
        return best
    }

    fun createTextLayout(
        context: Context,
        spec: ThemeSpec,
        typefaceChoice: ThemeTypeface,
        lines: List<String>,
        textSizeSp: Float,
        targetWidthPx: Int,
        density: Float
    ): StaticLayout {
        loadTypefaces(context)
        val latinTf = resolveLatinTypeface(spec, typefaceChoice)
        val devanagariTf = resolveDevanagariTypeface(spec, typefaceChoice)

        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = spec.foregroundColor
            textSize = textSizeSp * density
            typeface = latinTf
            if (spec.italic) {
                textSkewX = -0.22f
            }
        }

        val formattedLines = lines.mapIndexed { index, rawLine ->
            var line = rawLine
            if (spec.uppercase) {
                line = line.uppercase()
            }
            if (spec.promptMarker) {
                if (index == 0 || spec.promptOnEveryLine) {
                    "> $line"
                } else {
                    line
                }
            } else {
                line
            }
        }

        val ssb = SpannableStringBuilder()
        val markerWidth = paint.measureText("> ")

        for ((index, line) in formattedLines.withIndex()) {
            val start = ssb.length
            val styledLine = buildScriptStyledText(line, latinTf, devanagariTf)
            ssb.append(styledLine)
            val end = ssb.length

            if (spec.promptMarker) {
                if (index == 0 || spec.promptOnEveryLine) {
                    ssb.setSpan(
                        LeadingMarginSpan.Standard(0, markerWidth.roundToInt()),
                        start,
                        end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                } else {
                    // Align subsequent lines under the text of Line 0
                    ssb.setSpan(
                        LeadingMarginSpan.Standard(markerWidth.roundToInt(), markerWidth.roundToInt()),
                        start,
                        end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }

            if (index < formattedLines.size - 1) {
                ssb.append("\n")
            }
        }

        val alignment = if (spec.isLeftAligned) Layout.Alignment.ALIGN_NORMAL else Layout.Alignment.ALIGN_CENTER
        val safeWidth = max(targetWidthPx, 40)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val breakStrategy = if (spec.isLeftAligned) {
                LineBreakStrategyCompat.SIMPLE
            } else {
                LineBreakStrategyCompat.BALANCED
            }

            StaticLayout.Builder.obtain(ssb, 0, ssb.length, paint, safeWidth)
                .setAlignment(alignment)
                .setLineSpacing(0f, 1.30f)
                .setIncludePad(false)
                .setBreakStrategy(breakStrategy)
                .setHyphenationFrequency(StaticLayout.HYPHENATION_FREQUENCY_NONE)
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(
                ssb,
                paint,
                safeWidth,
                alignment,
                1.30f,
                0f,
                false
            )
        }
    }

    fun render(
        context: Context,
        config: ThemeConfig,
        lines: List<String>,
        songTitle: String,
        artistName: String,
        widthPx: Int,
        heightPx: Int,
        padTopDp: Float? = null,
        padBottomDp: Float? = null,
        padHorizontalDp: Float = 20f,
        albumArtBitmap: Bitmap? = null,
        capPixels: Boolean = true,
        displayMetricsWidth: Int = context.resources.displayMetrics.widthPixels,
        displayMetricsHeight: Int = context.resources.displayMetrics.heightPixels
    ): Bitmap {
        var w = max(widthPx, 100)
        var h = max(heightPx, 100)

        // Safety downscaling for RemoteViews binder limits (cap total pixels at 500,000 ~2MB ARGB)
        val maxAllowedPixels = 500_000L
        val currentPixels = w.toLong() * h.toLong()
        var effectiveDensity = context.resources.displayMetrics.density
        if (capPixels && currentPixels > maxAllowedPixels && currentPixels > 0) {
            val scale = sqrt(maxAllowedPixels.toDouble() / currentPixels.toDouble()).toFloat()
            w = max((w * scale).roundToInt(), 100)
            h = max((h * scale).roundToInt(), 100)
            effectiveDensity *= scale
        }

        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val density = effectiveDensity
        val spec = config.themeSpec

        // 1. Draw edge-to-edge background (either blurred album art with dark scrim or solid theme color)
        if (config.useAlbumArtBackdrop && albumArtBitmap != null && !albumArtBitmap.isRecycled) {
            val srcW = albumArtBitmap.width
            val srcH = albumArtBitmap.height
            val srcAspect = srcW.toFloat() / srcH.toFloat()
            val dstAspect = w.toFloat() / h.toFloat()

            val srcRect = if (srcAspect > dstAspect) {
                val cropW = (srcH * dstAspect).toInt()
                val left = (srcW - cropW) / 2
                android.graphics.Rect(left, 0, left + cropW, srcH)
            } else {
                val cropH = (srcW / dstAspect).toInt()
                val top = (srcH - cropH) / 2
                android.graphics.Rect(0, top, srcW, top + cropH)
            }
            val dstRect = android.graphics.Rect(0, 0, w, h)
            val blurPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
            canvas.drawBitmap(albumArtBitmap, srcRect, dstRect, blurPaint)

            // Scrim: 58% black overlay ensures white/amber lyrics maintain high contrast
            canvas.drawColor(AndroidColor.argb((255 * 0.58f).roundToInt(), 0, 0, 0))
        } else {
            canvas.drawColor(spec.backgroundColor)
        }

        // 2. Padding bounds (decoupled: caller can specify top/bottom for Stage vs Widget)
        val hasFooter = config.creditMode != ThemeCreditMode.NONE && (artistName.isNotBlank() || songTitle.isNotBlank())
        val resolvedPadTopDp = padTopDp ?: 20f
        val resolvedPadBottomDp = padBottomDp ?: (if (hasFooter) 38f else 20f)

        val padHorizontal = (padHorizontalDp * density).roundToInt()
        val padTop = (resolvedPadTopDp * density).roundToInt()
        val padBottom = (resolvedPadBottomDp * density).roundToInt()

        val contentWidth = max(w - padHorizontal * 2, 40)
        val contentHeight = max(h - padTop - padBottom, 40)

        // 3. Compute footer layout first to prevent vertical collision
        var footerLayout: StaticLayout? = null
        var footerTopPx = h.toFloat()

        if (hasFooter) {
            val creditText = when (config.creditMode) {
                ThemeCreditMode.NONE -> null
                ThemeCreditMode.ARTIST -> "— $artistName"
                ThemeCreditMode.SONG_AND_ARTIST -> if (songTitle.isNotBlank()) "— $songTitle · $artistName" else "— $artistName"
            }

            if (!creditText.isNullOrBlank()) {
                val footerLatinTf = if (spec.fontWeight >= 700) Typeface.DEFAULT_BOLD else Typeface.SANS_SERIF
                val footerDevanagariTf = cachedSansMedium ?: Typeface.SANS_SERIF
                val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = spec.foregroundColor
                    alpha = (255 * 0.65f).roundToInt()
                    textSize = 12f * density
                    typeface = footerLatinTf
                }
                val footerWidth = max(w - padHorizontal * 2, 40)
                val styledFooter = buildScriptStyledText(creditText, footerLatinTf, footerDevanagariTf)

                footerLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    StaticLayout.Builder.obtain(styledFooter, 0, styledFooter.length, footerPaint, footerWidth)
                        .setAlignment(Layout.Alignment.ALIGN_CENTER)
                        .setLineSpacing(0f, 1.15f)
                        .setIncludePad(false)
                        .setMaxLines(1)
                        .setEllipsize(TextUtils.TruncateAt.END)
                        .build()
                } else {
                    @Suppress("DEPRECATION")
                    StaticLayout(styledFooter, footerPaint, footerWidth, Layout.Alignment.ALIGN_CENTER, 1.15f, 0f, false)
                }

                // If caller gave high bottom padding (e.g. 56dp on Stage to clear the 20dp dock overlap),
                // ensure footer sits cleanly 14dp above the dock!
                val footerBottomMarginDp = if (padBottomDp != null && padBottomDp >= 50f) 34f else 16f
                val footerBottom = h - (footerBottomMarginDp * density)
                footerTopPx = footerBottom - footerLayout.height
            }
        }

        // 4. Calculate available height for lyrics avoiding footer collision
        val availableLyricHeight = if (footerLayout != null) {
            max(20, (footerTopPx - padTop - (8f * density)).roundToInt())
        } else {
            contentHeight
        }

        // 5. Size determination
        val finalSp = when (config.sizeMode) {
            ThemeSizeMode.AUTO -> findBestAutoSizeSp(
                context = context,
                spec = spec,
                typefaceChoice = config.typeface,
                lines = lines,
                availableWidthPx = contentWidth,
                availableHeightPx = availableLyricHeight,
                density = density,
                hasAccentBar = spec.accentBar
            )
            ThemeSizeMode.SP_16 -> 16f
            ThemeSizeMode.SP_22 -> 22f
            ThemeSizeMode.SP_30 -> 30f
            ThemeSizeMode.SP_40 -> 40f
        }

        // 6. Create text layout
        val layout = createTextLayout(
            context = context,
            spec = spec,
            typefaceChoice = config.typeface,
            lines = lines,
            textSizeSp = finalSp,
            targetWidthPx = contentWidth,
            density = density
        )

        val accentExtra = if (spec.accentBar) (20f * density).roundToInt() else 0
        val totalBlockHeight = layout.height + accentExtra

        // Vertically center inside the safe text area above the footer
        val verticalOffset = max(0f, (availableLyricHeight - totalBlockHeight) / 2f)
        val startY = padTop + verticalOffset
        val startX = padHorizontal.toFloat()

        canvas.save()
        canvas.translate(startX, startY)
        layout.draw(canvas)
        canvas.restore()

        // 7. Spotify Accent Bar
        if (spec.accentBar) {
            val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.parseColor("#1ED760")
                style = Paint.Style.FILL
            }
            val barW = 36f * density
            val barH = 4f * density
            val barX = (w - barW) / 2f
            val barY = startY + layout.height + (14f * density)
            val rect = RectF(barX, barY, barX + barW, barY + barH)
            canvas.drawRoundRect(rect, 2f * density, 2f * density, barPaint)
        }

        // 8. Draw Footer
        if (footerLayout != null) {
            canvas.save()
            canvas.translate(padHorizontal.toFloat(), footerTopPx)
            footerLayout.draw(canvas)
            canvas.restore()
        }

        return bitmap
    }
}

private object LineBreakStrategyCompat {
    const val SIMPLE = 0
    const val BALANCED = 2
}
