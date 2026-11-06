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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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

                val previewBitmap = remember(config, activeLines, activeTitle, activeArtist, widthPx, heightPx) {
                    if (widthPx > 50 && heightPx > 50) {
                        LyricRenderer.render(
                            context = context,
                            config = config,
                            lines = activeLines,
                            songTitle = activeTitle,
                            artistName = activeArtist,
                            widthPx = widthPx,
                            heightPx = heightPx
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

            // Top Overlay Row (12dp padding): Back, Title, Refresh
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Circular back button (40dp, 18% gray fill)
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0x2EFFFFFF))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Title "Theme studio"
                Text(
                    text = "Theme studio",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                // Circular refresh button (40dp, 18% gray fill)
                IconButton(
                    onClick = {
                        currentLyricIndex = (currentLyricIndex + 1) % SAMPLE_LYRICS_POOL.size
                        customLinesText = SAMPLE_LYRICS_POOL[currentLyricIndex].joinToString("\n")
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0x2EFFFFFF))
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Random lyric",
                        tint = Color.White,
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
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xDD1A1A1E),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF)),
                    shadowElevation = 8.dp
                ) {
                    Text(
                        text = "✓  Widget updated",
                        color = Color(0xFFFFB454),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // ==========================================
        // 2. DOCK (overlaps stage by 20dp, radius 24dp)
        // ==========================================
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-20).dp), // Stage extends 20dp behind dock
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = colors.dockBg,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
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
                    maxLines = 1,
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
                        modifier = Modifier.height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.chipBg,
                            contentColor = colors.textPrimary
                        )
                    ) {
                        Text(
                            text = if (isEditingLines) "Done" else "Edit lines",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Primary Button: "Apply to widget" (flex 1, fill #FFB454, text #2A1700, 52dp height, 26dp radius)
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
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp),
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
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(thickness = 1.dp, color = colors.divider)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = colors.textSecondary,
                fontSize = 13.sp,
                modifier = Modifier.width(52.dp)
            )
            Box(modifier = Modifier.weight(1f)) {
                content()
            }
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
            .height(48.dp) // Accessible 48dp touch target
            .clip(CircleShape)
            .background(if (isSelected) colors.accentButtonBg else colors.chipBg)
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) colors.accentText else colors.textPrimary,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
            maxLines = 1
        )
    }
}
