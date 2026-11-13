package com.widgy.lyrics.ui.theme

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.UnfoldLess
import androidx.compose.material.icons.outlined.UnfoldMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.widgy.lyrics.data.image.ArtworkManager
import com.widgy.lyrics.data.image.StackBlur
import com.widgy.lyrics.domain.model.DailyLyric
import com.widgy.lyrics.domain.theme.ThemeConfig
import com.widgy.lyrics.domain.theme.ThemeCreditMode
import com.widgy.lyrics.domain.theme.ThemeSizeMode
import com.widgy.lyrics.domain.theme.ThemeSpec
import com.widgy.lyrics.domain.theme.ThemeSpecs
import com.widgy.lyrics.domain.theme.ThemeTypeface
import com.widgy.lyrics.ui.share.ShareCardHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

enum class ThemeCategory(
    val displayName: String,
    val icon: ImageVector
) {
    APPEARANCE("Appearance", Icons.Outlined.Palette),
    TYPOGRAPHY("Typography", Icons.Outlined.TextFields),
    CREDITS("Credits", Icons.Outlined.Person),
    ADVANCED("Advanced", Icons.Outlined.Tune)
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
    var selectedCategory by remember { mutableStateOf(ThemeCategory.APPEARANCE) }
    var currentLyricIndex by remember { mutableStateOf(0) }
    var customLinesText by remember {
        val initialLines = dailyLyric?.text?.lines()?.filter { it.isNotBlank() }
            ?: SAMPLE_LYRICS_POOL[0]
        mutableStateOf(initialLines.joinToString("\n"))
    }
    var isEditingLines by remember { mutableStateOf(false) }
    var isPreviewExpanded by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

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
            delay(16) // 1 frame debounce for silky slider scrub
            val blurred = withContext(Dispatchers.Default) {
                StackBlur.blur(art, config.blurRadius)
            }
            albumArtBitmap = blurred
        } else {
            albumArtBitmap = null
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

    val stageWeight by animateFloatAsState(
        targetValue = if (isPreviewExpanded) 1.55f else 1.0f,
        animationSpec = tween(250),
        label = "stageWeight"
    )

    // Reset Confirmation Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Text(
                    text = "Reset theme styling?",
                    color = colors.textPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "This will restore all appearance, typography, and credit customizations to default values.",
                    color = colors.textSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val resetCfg = ThemeConfig()
                        update(resetCfg)
                        showResetDialog = false
                        feedbackMessage = "Settings reset to defaults"
                        scope.launch {
                            delay(2000)
                            feedbackMessage = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accentButtonBg,
                        contentColor = colors.accentText
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Reset", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetDialog = false }
                ) {
                    Text("Cancel", color = colors.textSecondary)
                }
            },
            containerColor = colors.dockBg,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(config.themeSpec.backgroundColor))
    ) {
        // ==========================================
        // 1. LIVE PREVIEW STAGE
        // ==========================================
        Box(
            modifier = Modifier
                .weight(stageWeight)
                .fillMaxWidth()
                .background(Color(config.themeSpec.backgroundColor))
        ) {
            // Live Stage Preview Canvas
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize()
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

            // Minimal Header Overlay: Back, Title, Reset
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Back Button (48dp accessible touch target)
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

                // Reset Action (48dp accessible touch target)
                IconButton(
                    onClick = { showResetDialog = true },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(stageButtonBg)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.RestartAlt,
                        contentDescription = "Reset theme",
                        tint = stageContentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Expand / Collapse Preview Pill
            IconButton(
                onClick = { isPreviewExpanded = !isPreviewExpanded },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(stageButtonBg)
            ) {
                Icon(
                    imageVector = if (isPreviewExpanded) Icons.Outlined.UnfoldLess else Icons.Outlined.UnfoldMore,
                    contentDescription = if (isPreviewExpanded) "Collapse preview" else "Expand preview",
                    tint = stageContentColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Feedback Banner (e.g. "Widget updated", "Settings reset")
            androidx.compose.animation.AnimatedVisibility(
                visible = feedbackMessage != null,
                enter = fadeIn(tween(180)),
                exit = fadeOut(tween(180)),
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
                        text = feedbackMessage ?: "",
                        color = Color(0xFFFFB454),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // ==========================================
        // 2. CATEGORY-BASED CUSTOMIZATION DOCK
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
            ) {
                // CATEGORY NAVIGATION BAR (Directly below preview)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ThemeCategory.values().forEach { category ->
                        val isSelected = selectedCategory == category
                        CategoryTabItem(
                            category = category,
                            isSelected = isSelected,
                            colors = colors,
                            onClick = {
                                if (isEditingLines) isEditingLines = false
                                selectedCategory = category
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // DOCK CONTENT (Scrollable, Contextual)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isPreviewExpanded) 140.dp else 210.dp)
                ) {
                    if (isEditingLines) {
                        // Inline Lyric Lines Editor
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Lyric Lines",
                                    color = colors.textPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                TextButton(
                                    onClick = {
                                        customLinesText = SAMPLE_LYRICS_POOL[currentLyricIndex].joinToString("\n")
                                    }
                                ) {
                                    Text("Reset to Sample", color = colors.accent, fontSize = 12.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            BasicTextField(
                                value = customLinesText,
                                onValueChange = { customLinesText = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
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
                        // Customization Panels per Category
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            when (selectedCategory) {
                                ThemeCategory.APPEARANCE -> {
                                    AppearancePanel(
                                        config = config,
                                        colors = colors,
                                        onUpdate = { update(it) }
                                    )
                                }
                                ThemeCategory.TYPOGRAPHY -> {
                                    TypographyPanel(
                                        config = config,
                                        colors = colors,
                                        onUpdate = { update(it) }
                                    )
                                }
                                ThemeCategory.CREDITS -> {
                                    CreditsPanel(
                                        config = config,
                                        activeTitle = activeTitle,
                                        activeArtist = activeArtist,
                                        colors = colors,
                                        onUpdate = { update(it) }
                                    )
                                }
                                ThemeCategory.ADVANCED -> {
                                    AdvancedPanel(
                                        config = config,
                                        currentLyricIndex = currentLyricIndex,
                                        totalSamples = SAMPLE_LYRICS_POOL.size,
                                        colors = colors,
                                        onCycleSample = {
                                            currentLyricIndex = (currentLyricIndex + 1) % SAMPLE_LYRICS_POOL.size
                                            customLinesText = SAMPLE_LYRICS_POOL[currentLyricIndex].joinToString("\n")
                                        },
                                        onUpdate = { update(it) }
                                    )
                                }
                            }
                        }
                    }
                }

                // BOTTOM ACTION AREA (Sticky, 48dp minimum targets)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Secondary Button: "Edit Lines" / "Done"
                    Button(
                        onClick = { isEditingLines = !isEditingLines },
                        modifier = Modifier.height(48.dp),
                        shape = WidgyShapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isEditingLines) colors.accentButtonBg else colors.chipBg,
                            contentColor = if (isEditingLines) colors.accentText else colors.textPrimary
                        )
                    ) {
                        Text(
                            text = if (isEditingLines) "Done" else "Edit lines",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Share Story Card Button (Compact Icon Button)
                    IconButton(
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                val storyBitmap = ShareCardHelper.generateStoryCard(
                                    context = context,
                                    config = config,
                                    lines = activeLines,
                                    songTitle = activeTitle,
                                    artistName = activeArtist,
                                    albumArtBitmap = albumArtBitmap,
                                    isStoryFormat = true
                                )
                                ShareCardHelper.shareLyricCard(
                                    context = context,
                                    bitmap = storyBitmap,
                                    songTitle = activeTitle,
                                    artistName = activeArtist
                                )
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(WidgyShapes.medium)
                            .background(colors.chipBg)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Story Card",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Primary Button: "Apply to widget"
                    Button(
                        onClick = {
                            onApplyConfig(config)
                            feedbackMessage = "Widget updated"
                            scope.launch {
                                delay(2200)
                                feedbackMessage = null
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

// ==========================================
// CATEGORY NAVIGATION TAB
// ==========================================
@Composable
private fun CategoryTabItem(
    category: ThemeCategory,
    isSelected: Boolean,
    colors: ThemeStudioColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = category.icon,
            contentDescription = category.displayName,
            tint = if (isSelected) colors.accent else colors.textSecondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = category.displayName,
            color = if (isSelected) colors.accent else colors.textSecondary,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(2.dp))
        // Indicator dot / line
        Box(
            modifier = Modifier
                .width(16.dp)
                .height(2.dp)
                .clip(CircleShape)
                .background(if (isSelected) colors.accent else Color.Transparent)
        )
    }
}

// ==========================================
// CATEGORY 1: APPEARANCE PANEL
// ==========================================
@Composable
private fun AppearancePanel(
    config: ThemeConfig,
    colors: ThemeStudioColors,
    onUpdate: (ThemeConfig) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Theme Swatches: Compact visual color previews
        Text(
            text = "Themes",
            color = colors.textSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(ThemeSpecs.ALL_THEMES, key = { it.id }) { themeSpec ->
                val isSelected = config.themeId.equals(themeSpec.id, ignoreCase = true)

                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .then(
                            if (isSelected) {
                                Modifier.border(2.5.dp, colors.accent, CircleShape)
                            } else {
                                Modifier.border(1.dp, colors.divider, CircleShape)
                            }
                        )
                        .clickable {
                            onUpdate(
                                config.copy(
                                    themeId = themeSpec.id,
                                    typeface = themeSpec.defaultTypeface
                                )
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Predefined Color Combination Preview (Background + Foreground Disc)
                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 36.dp else 42.dp)
                            .clip(CircleShape)
                            .background(Color(themeSpec.backgroundColor)),
                        contentAlignment = Alignment.Center
                    ) {
                        // Inner foreground representation
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(Color(themeSpec.foregroundColor)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (themeSpec.accentBar) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1ED760))
                                )
                            }
                        }
                    }
                }
            }
        }

        // Active Theme Description
        Text(
            text = "${config.themeSpec.name} · ${config.themeSpec.description}",
            color = colors.textSecondary,
            fontSize = 12.sp,
            maxLines = 2
        )

        // Artwork Mode: Solid Theme vs Blurred Art
        Text(
            text = "Backdrop",
            color = colors.textSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PillChip(
                text = "Solid Theme",
                isSelected = !config.useAlbumArtBackdrop,
                colors = colors,
                onClick = { onUpdate(config.copy(useAlbumArtBackdrop = false)) }
            )
            PillChip(
                text = "Blurred Art",
                isSelected = config.useAlbumArtBackdrop,
                colors = colors,
                onClick = { onUpdate(config.copy(useAlbumArtBackdrop = true)) }
            )
        }

        // Blur Intensity: Contextually visible ONLY when Blurred Art is enabled
        AnimatedVisibility(visible = config.useAlbumArtBackdrop) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Blur Intensity",
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${config.blurRadius}px",
                        color = colors.accent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Slider(
                    value = config.blurRadius.toFloat(),
                    onValueChange = { newRadius ->
                        val intRadius = newRadius.roundToInt().coerceIn(
                            ThemeConfig.MIN_BLUR_RADIUS,
                            ThemeConfig.MAX_BLUR_RADIUS
                        )
                        if (intRadius != config.blurRadius) {
                            onUpdate(config.copy(blurRadius = intRadius))
                        }
                    },
                    valueRange = ThemeConfig.MIN_BLUR_RADIUS.toFloat()..ThemeConfig.MAX_BLUR_RADIUS.toFloat(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = colors.accent,
                        activeTrackColor = colors.accent,
                        inactiveTrackColor = colors.chipBg
                    )
                )
            }
        }
    }
}

// ==========================================
// CATEGORY 2: TYPOGRAPHY PANEL
// ==========================================
@Composable
private fun TypographyPanel(
    config: ThemeConfig,
    colors: ThemeStudioColors,
    onUpdate: (ThemeConfig) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Typeface selector with actual typography rendering
        Text(
            text = "Typeface",
            color = colors.textSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeTypeface.values().forEach { tf ->
                val isSelected = config.typeface == tf
                val sampleFontFamily = when (tf) {
                    ThemeTypeface.SANS -> FontFamily.SansSerif
                    ThemeTypeface.SERIF -> FontFamily.Serif
                    ThemeTypeface.MONO -> FontFamily.Monospace
                }

                Box(
                    modifier = Modifier
                        .defaultMinSize(minHeight = 40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) colors.accentButtonBg else colors.chipBg)
                        .clickable { onUpdate(config.copy(typeface = tf)) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tf.displayName,
                        fontFamily = sampleFontFamily,
                        color = if (isSelected) colors.accentText else colors.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        // Font Size selector: Auto, 16, 22, 30, 40
        Text(
            text = "Font Size",
            color = colors.textSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ThemeSizeMode.values()) { size ->
                PillChip(
                    text = size.displayName,
                    isSelected = config.sizeMode == size,
                    colors = colors,
                    onClick = { onUpdate(config.copy(sizeMode = size)) }
                )
            }
        }

        // Contextual Explanation for Size Mode
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = colors.chipBg.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (config.sizeMode == ThemeSizeMode.AUTO) {
                    "Auto: Dynamically calculates optimal typography to fit any widget size without clipping."
                } else {
                    "Manual: Fixed ${config.sizeMode.fixedSp?.toInt()}sp text size across all widget cells."
                },
                color = colors.textSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(10.dp)
            )
        }

        // Text Alignment Note
        Text(
            text = "Alignment: ${if (config.themeSpec.isLeftAligned) "Left-aligned" else "Centered"} (set by ${config.themeSpec.name} theme)",
            color = colors.textSecondary,
            fontSize = 12.sp
        )
    }
}

// ==========================================
// CATEGORY 3: CREDITS PANEL
// ==========================================
@Composable
private fun CreditsPanel(
    config: ThemeConfig,
    activeTitle: String,
    activeArtist: String,
    colors: ThemeStudioColors,
    onUpdate: (ThemeConfig) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Attribution Display",
            color = colors.textSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ThemeCreditMode.values()) { credit ->
                PillChip(
                    text = credit.displayName,
                    isSelected = config.creditMode == credit,
                    colors = colors,
                    onClick = { onUpdate(config.copy(creditMode = credit)) }
                )
            }
        }

        // Contextual Example Preview Card
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = colors.chipBg,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Widget Footer Preview",
                    color = colors.textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                val previewText = when (config.creditMode) {
                    ThemeCreditMode.NONE -> "No attribution shown"
                    ThemeCreditMode.ARTIST -> activeArtist
                    ThemeCreditMode.SONG_AND_ARTIST -> "$activeTitle · $activeArtist"
                }
                Text(
                    text = previewText,
                    color = if (config.creditMode == ThemeCreditMode.NONE) colors.textSecondary else colors.accent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ==========================================
// CATEGORY 4: ADVANCED PANEL
// ==========================================
@Composable
private fun AdvancedPanel(
    config: ThemeConfig,
    currentLyricIndex: Int,
    totalSamples: Int,
    colors: ThemeStudioColors,
    onCycleSample: () -> Unit,
    onUpdate: (ThemeConfig) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Widget Shuffle Button Setting
        Text(
            text = "Widget Shuffle Control",
            color = colors.textSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PillChip(
                text = "Visible",
                isSelected = config.showRefreshButton,
                colors = colors,
                onClick = { onUpdate(config.copy(showRefreshButton = true)) }
            )
            PillChip(
                text = "Hidden",
                isSelected = !config.showRefreshButton,
                colors = colors,
                onClick = { onUpdate(config.copy(showRefreshButton = false)) }
            )
        }

        Text(
            text = "Shows a quick shuffle icon on the home screen widget to cycle lyrics instantly.",
            color = colors.textSecondary,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Sample Lyric Switcher
        Text(
            text = "Preview Verse",
            color = colors.textSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )

        Button(
            onClick = onCycleSample,
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.chipBg,
                contentColor = colors.textPrimary
            )
        ) {
            Text(
                text = "Cycle Sample Lyric (${currentLyricIndex + 1}/$totalSamples)",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ==========================================
// REUSABLE PILL CHIP
// ==========================================
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
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1
        )
    }
}
