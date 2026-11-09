package com.widgy.lyrics.ui.theme

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import com.widgy.lyrics.data.image.ArtworkManager
import com.widgy.lyrics.data.image.StackBlur
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.widgy.lyrics.domain.model.DailyLyric
import com.widgy.lyrics.domain.theme.ThemeConfig
import com.widgy.lyrics.domain.theme.ThemeCreditMode
import com.widgy.lyrics.domain.theme.ThemeSizeMode
import com.widgy.lyrics.domain.theme.ThemeSpec
import com.widgy.lyrics.domain.theme.ThemeSpecs
import com.widgy.lyrics.domain.theme.ThemeTypeface
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class ThemeStudioColors(
    val dockBg: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val divider: Color,
    val chipBg: Color,
    val accent: Color,
    val accentText: Color = Color(0xFF2A1700),
    val accentButtonBg: Color = Color(0xFFFFB454)
)

object ThemeStudioPalette {
    val Dark = ThemeStudioColors(
        dockBg = Color(0xFF141416),
        textPrimary = Color(0xFFF3EFE8),
        textSecondary = Color(0xFF9B958A),
        divider = Color(0xFF26262B),
        chipBg = Color(0xFF222227),
        accent = Color(0xFFFFB454)
    )

    val Light = ThemeStudioColors(
        dockBg = Color(0xFFFBF8F3),
        textPrimary = Color(0xFF1C1A17),
        textSecondary = Color(0xFF6F685D),
        divider = Color(0xFFE3DCCF),
        chipBg = Color(0xFFEFE9DE),
        accent = Color(0xFFC27400)
    )
}

private val SAMPLE_LYRICS_POOL = listOf(
    listOf("तिमी नभए पनि बाटो चल्छ, उज्यालो साथै हुन्छ", "तर आज किन यो मन आफैँलाई सोध्दैन?"),
    listOf("WHAT IF TODAY WAS THE RAPTURE", "AND YOU COMPLETELY TARNISHED"),
    listOf("माया नै माया थ्यो, नछुट्टिने साथ थ्यो", "हामी भन्ने शब्द छुट्टिँदै भयो किन तिमी र म?"),
    listOf("I WANNA BE YOUR VACUUM CLEANER", "BREATHING IN YOUR DUST")
)

@Composable
fun ThemeStudioScreen(
    initialConfig: ThemeConfig,
    dailyLyric: DailyLyric?,
    onConfigChanged: (ThemeConfig) -> Unit = {},
    onApplyConfig: (ThemeConfig) -> Unit,
    onBack: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val colors = if (isDark) ThemeStudioPalette.Dark else ThemeStudioPalette.Light
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var config by remember { mutableStateOf(initialConfig) }
    var currentLyricIndex by remember { mutableStateOf(0) }
    var customLinesText by remember {
        val initialLines = dailyLyric?.text?.lines()?.filter { it.isNotBlank() }
            ?: SAMPLE_LYRICS_POOL[0]
        mutableStateOf(initialLines.joinToString("\n"))
    }
    var isEditingLines by remember { mutableStateOf(false) }
    var showUpdatedSnackbar by remember { mutableStateOf(false) }

    fun update(newConfig: ThemeConfig) {
        config = newConfig
        onConfigChanged(newConfig)
    }

    val activeLines = remember(customLinesText) {
        customLinesText.lines().map { it.trim() }.filter { it.isNotEmpty() }.ifEmpty {
            listOf("Add lyrics to see your widget")
        }
    }

    val activeTitle = dailyLyric?.songTitle ?: "Timi Ra Ma"
    val activeArtist = dailyLyric?.artistName ?: "Pariwartan"

    val artworkManager = remember { ArtworkManager(context) }
    var rawArtworkBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var albumArtBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(dailyLyric?.songId, config.useAlbumArtBackdrop) {
        if (config.useAlbumArtBackdrop) {
            val songId = dailyLyric?.songId ?: 0L
            val art = withContext(Dispatchers.IO) {
                artworkManager.getOrFetchArtworkBitmap(
                    songId = songId,
                    title = activeTitle,
                    artist = activeArtist,
                    maxDimension = 200
                )
            }
            rawArtworkBitmap = art
        } else {
            rawArtworkBitmap = null
            albumArtBitmap = null
        }
    }

    LaunchedEffect(rawArtworkBitmap, config.useAlbumArtBackdrop, config.blurRadius) {
        val art = rawArtworkBitmap
        if (config.useAlbumArtBackdrop && art != null) {
            delay(16) // 1 frame debounce for silky 60fps slider drag
            val blurred = withContext(Dispatchers.Default) {
                StackBlur.blur(art, config.blurRadius)
            }
            albumArtBitmap = blurred
        } else {
            albumArtBitmap = null
        }
    }

    // Single non-scrolling full-height container
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(config.themeSpec.backgroundColor))
    ) {
        // ==========================================
        // 1. STAGE (weight 1, min height 240dp)
        // ==========================================
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(config.themeSpec.backgroundColor))
        ) {
            // Live Stage Preview Canvas (fills whole stage)
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                val density = LocalDensity.current.density
                val widthPx = (maxWidth.value * density).toInt()
                val heightPx = (maxHeight.value * density).toInt()

                val previewBitmap = remember(
                    config.themeId,
                    config.typeface,
                    config.sizeMode,
                    config.creditMode,
                    config.useAlbumArtBackdrop,
                    activeLines,
                    activeTitle,
                    activeArtist,
                    widthPx,
                    heightPx,
                    albumArtBitmap
                ) {
                    if (widthPx > 50 && heightPx > 50) {
                        LyricRenderer.render(
                            context = context,
                            config = config,
                            lines = activeLines,
                            songTitle = activeTitle,
                            artistName = activeArtist,
                            widthPx = widthPx,
                            heightPx = heightPx,
                            padTopDp = 76f,
                            padBottomDp = if (config.creditMode != ThemeCreditMode.NONE) 56f else 40f,
                            albumArtBitmap = albumArtBitmap
                        )
                    } else null
                }

                if (previewBitmap != null) {
                    Image(
                        bitmap = previewBitmap.asImageBitmap(),
                        contentDescription = "Widget Live Preview",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            val isStageLight = remember(config.themeSpec.backgroundColor, config.useAlbumArtBackdrop) {
                if (config.useAlbumArtBackdrop) {
                    false
                } else {
                    val bg = config.themeSpec.backgroundColor
                    val r = android.graphics.Color.red(bg)
                    val g = android.graphics.Color.green(bg)
                    val b = android.graphics.Color.blue(bg)
                    (0.299 * r + 0.587 * g + 0.114 * b) / 255.0 > 0.6
                }
            }
            val stageContentColor = if (isStageLight) Color(0xFF1C1A17) else Color.White
            val stageButtonBg = if (isStageLight) Color(0x1F000000) else Color(0x2EFFFFFF)

            // Top Overlay Row (12dp padding): Back, Title, Refresh
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Circular back button (48dp accessible touch target)
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(stageButtonBg)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = stageContentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Title "Theme Studio"
                Text(
                    text = "Theme Studio",
                    color = stageContentColor,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                // Circular refresh button (48dp accessible touch target)
                IconButton(
                    onClick = {
                        currentLyricIndex = (currentLyricIndex + 1) % SAMPLE_LYRICS_POOL.size
                        customLinesText = SAMPLE_LYRICS_POOL[currentLyricIndex].joinToString("\n")
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(stageButtonBg)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Random lyric",
                        tint = stageContentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // "Widget updated" Snackbar near top of stage
            androidx.compose.animation.AnimatedVisibility(
                visible = showUpdatedSnackbar,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(200)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 64.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xDD1A1A1E),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF)),
                    shadowElevation = 8.dp
                ) {
                    Text(
                        text = "Widget updated",
                        color = Color(0xFFFFB454),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // ==========================================
        // 2. DOCK (24dp top corner radius)
        // ==========================================
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = colors.dockBg,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // A. Theme Swatches (horizontally scrollable row of 48dp circles with 12dp gaps)
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(ThemeSpecs.ALL_THEMES, key = { it.id }) { themeSpec ->
                        val isSelected = config.themeId.equals(themeSpec.id, ignoreCase = true)

                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .then(
                                    if (isSelected) {
                                        Modifier.border(2.5.dp, colors.accent, CircleShape)
                                    } else {
                                        Modifier
                                    }
                                )
                                .clickable {
                                    update(
                                        config.copy(
                                            themeId = themeSpec.id,
                                            typeface = themeSpec.defaultTypeface
                                        )
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            // Inner circle with 2dp gap when selected
                            Box(
                                modifier = Modifier
                                    .size(if (isSelected) 39.dp else 46.dp)
                                    .clip(CircleShape)
                                    .background(Color(themeSpec.backgroundColor)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = themeSpec.swatchGlyph,
                                    color = Color(themeSpec.foregroundColor),
                                    fontSize = if (themeSpec.swatchGlyph == ">") 18.sp else 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // B. Swatch Caption: "{Theme name} · {description}"
                Text(
                    text = "${config.themeSpec.name} · ${config.themeSpec.description}",
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    maxLines = 2,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )

                // C. Three Labeled Rows (or Inline Editor when open)
                if (isEditingLines) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "Lyric Lines (one per line)",
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        BasicTextField(
                            value = customLinesText,
                            onValueChange = { customLinesText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.chipBg)
                                .padding(12.dp),
                            textStyle = TextStyle(
                                color = colors.textPrimary,
                                fontSize = 14.sp
                            ),
                            cursorBrush = SolidColor(colors.accent)
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Row 1: Typeface
                        ConfigRow(
                            label = "Typeface",
                            colors = colors
                        ) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(ThemeTypeface.values()) { tf ->
                                    PillChip(
                                        text = tf.displayName,
                                        isSelected = config.typeface == tf,
                                        colors = colors,
                                        onClick = { update(config.copy(typeface = tf)) }
                                    )
                                }
                            }
                        }

                        // Row 2: Size
                        ConfigRow(
                            label = "Size",
                            colors = colors
                        ) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(ThemeSizeMode.values()) { size ->
                                    PillChip(
                                        text = size.displayName,
                                        isSelected = config.sizeMode == size,
                                        colors = colors,
                                        onClick = { update(config.copy(sizeMode = size)) }
                                    )
                                }
                            }
                        }

                        // Row 3: Credit
                        ConfigRow(
                            label = "Credit",
                            colors = colors
                        ) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(ThemeCreditMode.values()) { credit ->
                                    PillChip(
                                        text = credit.displayName,
                                        isSelected = config.creditMode == credit,
                                        colors = colors,
                                        onClick = { update(config.copy(creditMode = credit)) }
                                    )
                                }
                            }
                        }

                        // Row 4: Album Art Backdrop
                        ConfigRow(
                            label = "Art",
                            colors = colors
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                PillChip(
                                    text = "Solid Theme",
                                    isSelected = !config.useAlbumArtBackdrop,
                                    colors = colors,
                                    onClick = { update(config.copy(useAlbumArtBackdrop = false)) }
                                )
                                PillChip(
                                    text = "Blurred Art",
                                    isSelected = config.useAlbumArtBackdrop,
                                    colors = colors,
                                    onClick = { update(config.copy(useAlbumArtBackdrop = true)) }
                                )
                            }
                        }

                        // Row 4b: Blur Range & Presets (Visible only when Blurred Art is enabled)
                        AnimatedVisibility(visible = config.useAlbumArtBackdrop) {
                            ConfigRow(
                                label = "Blur",
                                colors = colors
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Range scroll (horizontal slider + px badge)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Slider(
                                            value = config.blurRadius.toFloat(),
                                            onValueChange = { newRadius ->
                                                val intRadius = newRadius.roundToInt().coerceIn(
                                                    ThemeConfig.MIN_BLUR_RADIUS,
                                                    ThemeConfig.MAX_BLUR_RADIUS
                                                )
                                                if (intRadius != config.blurRadius) {
                                                    update(config.copy(blurRadius = intRadius))
                                                }
                                            },
                                            valueRange = ThemeConfig.MIN_BLUR_RADIUS.toFloat()..ThemeConfig.MAX_BLUR_RADIUS.toFloat(),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(28.dp),
                                            colors = SliderDefaults.colors(
                                                thumbColor = colors.accent,
                                                activeTrackColor = colors.accent,
                                                inactiveTrackColor = colors.chipBg
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "${config.blurRadius}px",
                                            color = colors.textSecondary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.width(36.dp),
                                            textAlign = TextAlign.End
                                        )
                                    }

                                    // Quick preset chips below: Soft (10), Medium (22), Deep (36)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        PillChip(
                                            text = "Soft (10)",
                                            isSelected = config.blurRadius == ThemeConfig.BLUR_SOFT,
                                            colors = colors,
                                            onClick = { update(config.copy(blurRadius = ThemeConfig.BLUR_SOFT)) }
                                        )
                                        PillChip(
                                            text = "Medium (22)",
                                            isSelected = config.blurRadius == ThemeConfig.BLUR_MEDIUM,
                                            colors = colors,
                                            onClick = { update(config.copy(blurRadius = ThemeConfig.BLUR_MEDIUM)) }
                                        )
                                        PillChip(
                                            text = "Deep (36)",
                                            isSelected = config.blurRadius == ThemeConfig.BLUR_DEEP,
                                            colors = colors,
                                            onClick = { update(config.copy(blurRadius = ThemeConfig.BLUR_DEEP)) }
                                        )
                                    }
                                }
                            }
                        }

                        // Row 5: Shuffle Button on Widget
                        ConfigRow(
                            label = "Shuffle",
                            colors = colors
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                PillChip(
                                    text = "Visible",
                                    isSelected = config.showRefreshButton,
                                    colors = colors,
                                    onClick = { update(config.copy(showRefreshButton = true)) }
                                )
                                PillChip(
                                    text = "Hidden",
                                    isSelected = !config.showRefreshButton,
                                    colors = colors,
                                    onClick = { update(config.copy(showRefreshButton = false)) }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // D. Button Row: "Edit lines" / "Done" & "Apply to widget"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Secondary Button: "Edit lines" / "Done"
                    Button(
                        onClick = { isEditingLines = !isEditingLines },
                        modifier = Modifier.height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.chipBg,
                            contentColor = colors.textPrimary
                        )
                    ) {
                        Text(
                            text = if (isEditingLines) "Done" else "Edit lines",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Share Story Card Button
                    IconButton(
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                val storyBitmap = com.widgy.lyrics.ui.share.ShareCardHelper.generateStoryCard(
                                    context = context,
                                    config = config,
                                    lines = activeLines,
                                    songTitle = activeTitle,
                                    artistName = activeArtist,
                                    albumArtBitmap = albumArtBitmap,
                                    isStoryFormat = true
                                )
                                com.widgy.lyrics.ui.share.ShareCardHelper.shareLyricCard(
                                    context = context,
                                    bitmap = storyBitmap,
                                    songTitle = activeTitle,
                                    artistName = activeArtist
                                )
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.chipBg)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Story Card",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Primary Button: "Apply to widget" (flex 1, fill #FFB454, text #2A1700, 48dp height, 12dp radius)
                    Button(
                        onClick = {
                            onApplyConfig(config)
                            showUpdatedSnackbar = true
                            scope.launch {
                                delay(2200)
                                showUpdatedSnackbar = false
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.accentButtonBg,
                            contentColor = colors.accentText
                        )
                    ) {
                        Text(
                            text = "Apply to widget",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfigRow(
    label: String,
    colors: ThemeStudioColors,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = colors.textSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(68.dp)
        )
        Box(modifier = Modifier.weight(1f)) {
            content()
        }
    }
}

@Composable
private fun PillChip(
    text: String,
    isSelected: Boolean,
    colors: ThemeStudioColors,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .defaultMinSize(minHeight = 40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) colors.accentButtonBg else colors.chipBg)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) colors.accentText else colors.textPrimary,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1
        )
    }
}
