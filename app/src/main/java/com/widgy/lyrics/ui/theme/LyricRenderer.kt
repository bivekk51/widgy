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
        applyCustomTypeFace(ds, typeface)
    }

    override fun updateMeasureState(paint: TextPaint) {
        applyCustomTypeFace(paint, typeface)
    }

    private fun applyCustomTypeFace(paint: Paint, tf: Typeface) {
        paint.typeface = tf
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
     * Splits text into script runs when Mono typeface is selected.
     * Latin and numbers use System Monospace; Devanagari characters use Noto Sans Devanagari.
     */
    fun buildMonoStyledText(text: String, devanagariTf: Typeface): CharSequence {
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
                    CustomTypefaceSpan(Typeface.MONOSPACE),
                    start,
                    i,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
        return ssb
    }

    fun resolveTypeface(context: Context, spec: ThemeSpec, typefaceChoice: ThemeTypeface): Typeface {
        loadTypefaces(context)
        val isBold = spec.fontWeight >= 700
        return when (typefaceChoice) {
            ThemeTypeface.SANS -> if (isBold) cachedSansBold ?: Typeface.DEFAULT_BOLD else cachedSansMedium ?: Typeface.SANS_SERIF
            ThemeTypeface.SERIF -> if (isBold) cachedSerifBold ?: Typeface.SERIF else cachedSerifMedium ?: Typeface.SERIF
            ThemeTypeface.MONO -> Typeface.MONOSPACE
        }
    }

    /**
     * Binary search to find largest font size between 13sp and 36sp where text fits.
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
        var low = 13f
        var high = 36f
        var best = 13f

        val extraReservedPx = if (hasAccentBar) (24f * density).roundToInt() else 0
        val targetHeightPx = max(10, availableHeightPx - extraReservedPx)

        for (step in 0..10) {
            val mid = (low + high) / 2f
            val layout = createTextLayout(
                context = context,
                spec = spec,
                typefaceChoice = typefaceChoice,
                lines = lines,
                textSizeSp = mid,
                targetWidthPx = availableWidthPx,
                density = density
            )

            if (layout.height <= targetHeightPx) {
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
        val baseTf = resolveTypeface(context, spec, typefaceChoice)
        val devanagariTf = cachedSansMedium ?: Typeface.SANS_SERIF
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = spec.foregroundColor
            textSize = textSizeSp * density
            typeface = baseTf
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
            val styledLine = if (typefaceChoice == ThemeTypeface.MONO) {
                buildMonoStyledText(line, devanagariTf)
            } else {
                line
            }
            ssb.append(styledLine)
            val end = ssb.length

            if (spec.promptMarker && (index == 0 || spec.promptOnEveryLine)) {
                ssb.setSpan(
                    LeadingMarginSpan.Standard(0, markerWidth.roundToInt()),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
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
                .setLineSpacing(0f, 1.55f)
                .setIncludePad(true)
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
                1.55f,
                0f,
                true
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
        displayMetricsWidth: Int = context.resources.displayMetrics.widthPixels,
        displayMetricsHeight: Int = context.resources.displayMetrics.heightPixels
    ): Bitmap {
        var w = max(widthPx, 100)
        var h = max(heightPx, 100)

        // Safety downscaling for RemoteViews binder limits (1.5x screen area cap)
        val screenW = max(displayMetricsWidth, 720)
        val screenH = max(displayMetricsHeight, 1280)
        val maxPixels = (screenW.toLong() * screenH.toLong() * 1.5).toLong()
        val currentPixels = w.toLong() * h.toLong()
        if (currentPixels > maxPixels && currentPixels > 0) {
            val scale = sqrt(maxPixels.toDouble() / currentPixels.toDouble()).toFloat()
            w = max((w * scale).roundToInt(), 100)
            h = max((h * scale).roundToInt(), 100)
        }

        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val density = context.resources.displayMetrics.density
        val spec = config.themeSpec

        // 1. Draw edge-to-edge theme background
        canvas.drawColor(spec.backgroundColor)

        // 2. Padding bounds
        val padHorizontal = (20f * density).roundToInt()
        val padTop = (72f * density).roundToInt() // Minimum 72dp top padding to never overlap overlay controls
        val hasFooter = config.creditMode != ThemeCreditMode.NONE && (artistName.isNotBlank() || songTitle.isNotBlank())
        val padBottom = if (hasFooter) (52f * density).roundToInt() else (40f * density).roundToInt()

        val contentWidth = max(w - padHorizontal * 2, 40)
        val contentHeight = max(h - padTop - padBottom, 40)

        // 3. Size determination (Auto binary search or fixed)
        val finalSp = when (config.sizeMode) {
            ThemeSizeMode.AUTO -> findBestAutoSizeSp(
                context = context,
                spec = spec,
                typefaceChoice = config.typeface,
                lines = lines,
                availableWidthPx = contentWidth,
                availableHeightPx = contentHeight,
                density = density,
                hasAccentBar = spec.accentBar
            )
            ThemeSizeMode.SP_16 -> 16f
            ThemeSizeMode.SP_22 -> 22f
            ThemeSizeMode.SP_30 -> 30f
            ThemeSizeMode.SP_40 -> 40f
        }

        // 4. Create and center the text layout
        val layout = createTextLayout(
            context = context,
            spec = spec,
            typefaceChoice = config.typeface,
            lines = lines,
            textSizeSp = finalSp,
            targetWidthPx = contentWidth,
            density = density
        )

        val accentExtra = if (spec.accentBar) (22f * density).roundToInt() else 0
        val totalBlockHeight = layout.height + accentExtra

        // Vertically center inside the safe stage text area
        val startY = padTop + max(0, (contentHeight - totalBlockHeight) / 2)
        val startX = padHorizontal.toFloat()

        canvas.save()
        canvas.translate(startX, startY.toFloat())
        layout.draw(canvas)
        canvas.restore()

        // 5. Spotify Accent Bar if enabled
        if (spec.accentBar) {
            val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.parseColor("#1ED760")
                style = Paint.Style.FILL
            }
            val barW = 36f * density
            val barH = 4f * density
            val barX = (w - barW) / 2f
            val barY = startY + layout.height + (16f * density)
            val rect = RectF(barX, barY, barX + barW, barY + barH)
            canvas.drawRoundRect(rect, 2f * density, 2f * density, barPaint)
        }

        // 6. Footer credit: 14dp from bottom, centered, 12sp, 65% opacity
        if (hasFooter) {
            val creditText = when (config.creditMode) {
                ThemeCreditMode.NONE -> null
                ThemeCreditMode.ARTIST -> "— $artistName"
                ThemeCreditMode.SONG_AND_ARTIST -> if (songTitle.isNotBlank()) "— $songTitle • $artistName" else "— $artistName"
            }

            if (!creditText.isNullOrBlank()) {
                val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = spec.foregroundColor
                    alpha = (255 * 0.65f).roundToInt()
                    textSize = 12f * density
                    typeface = resolveTypeface(context, spec, config.typeface)
                    textAlign = Paint.Align.CENTER
                }
                val footerY = h - (14f * density)
                val footerX = w / 2f
                canvas.drawText(creditText, footerX, footerY, footerPaint)
            }
        }

        return bitmap
    }
}

private object LineBreakStrategyCompat {
    const val SIMPLE = 0
    const val BALANCED = 2
}
