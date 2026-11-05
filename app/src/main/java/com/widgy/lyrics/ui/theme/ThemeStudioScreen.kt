package com.widgy.lyrics.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.widgy.lyrics.data.preferences.WidgetAttribution
import com.widgy.lyrics.data.preferences.WidgetConfig
import com.widgy.lyrics.data.preferences.WidgetFontFamily
import com.widgy.lyrics.data.preferences.WidgetFontSize
import com.widgy.lyrics.data.preferences.WidgetTemplate
import com.widgy.lyrics.domain.model.DailyLyric

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeStudioScreen(
    initialConfig: WidgetConfig,
    dailyLyric: DailyLyric?,
    onConfigChanged: (WidgetConfig) -> Unit = {},
    onApplyConfig: (WidgetConfig) -> Unit,
    onBack: () -> Unit
) {
    var config by remember { mutableStateOf(initialConfig) }

    fun update(newConfig: WidgetConfig) {
        config = newConfig
        onConfigChanged(newConfig)
    }

    // Fallback sample lyric if none curated yet
    val previewLyric = dailyLyric ?: DailyLyric(
        date = "2026-11-05",
        songId = 1L,
        snippetId = 1L,
        text = "WHAT IF TODAY WAS THE RAPTURE\nAND YOU COMPLETELY TARNISHED",
        songTitle = "Euphoria",
        artistName = "Kendrick Lamar"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Theme Studio",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Button(
                        onClick = { onApplyConfig(config) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Apply to Home Screen Widget",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Live Preview Section
            item {
                Text(
                    text = "LIVE WIDGET PREVIEW",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                LiveWidgetPreviewCard(config = config, lyric = previewLyric)
            }

            // 2. Ready-Made Templates Gallery
            item {
                Text(
                    text = "READY-MADE THEMES",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(WidgetTemplate.values()) { template ->
                        TemplateGalleryCard(
                            template = template,
                            isSelected = config.template == template,
                            onClick = {
                                val autoFont = when (template) {
                                    WidgetTemplate.CLASSIC -> WidgetFontFamily.SERIF
                                    WidgetTemplate.TERMINAL -> WidgetFontFamily.MONOSPACE
                                    else -> WidgetFontFamily.SYSTEM
                                }
                                update(
                                    config.copy(
                                        template = template,
                                        fontFamily = autoFont
                                    )
                                )
                            }
                        )
                    }
                }
            }

            // 3. Fine-Tune Customization Section
            item {
                Text(
                    text = "FINE-TUNE CUSTOMIZATION",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Control A: Typography Typeface
                    CustomizationCard(
                        title = "Typeface",
                        subtitle = "Font family rendered across the quotes"
                    ) {
                        SegmentedControl(
                            items = WidgetFontFamily.values().toList(),
                            selectedItem = config.fontFamily,
                            onItemSelected = { font -> update(config.copy(fontFamily = font)) }
                        ) { font, isSelected ->
                            val fontType = when (font) {
                                WidgetFontFamily.SERIF -> FontFamily.Serif
                                WidgetFontFamily.MONOSPACE -> FontFamily.Monospace
                                WidgetFontFamily.SYSTEM -> FontFamily.SansSerif
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Aa",
                                    fontSize = 17.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontFamily = fontType,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF90909A)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = when (font) {
                                        WidgetFontFamily.SYSTEM -> "Sans"
                                        WidgetFontFamily.SERIF -> "Serif"
                                        WidgetFontFamily.MONOSPACE -> "Mono"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else Color(0xFF70707A)
                                )
                            }
                        }
                    }

                    // Control B: Typography Size Scale
                    CustomizationCard(
                        title = "Font Size",
                        subtitle = "Visual scale from subtle to bold hero statement"
                    ) {
                        SegmentedControl(
                            items = WidgetFontSize.values().toList(),
                            selectedItem = config.fontSize,
                            onItemSelected = { size -> update(config.copy(fontSize = size)) }
                        ) { size, isSelected ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "A",
                                    fontSize = when (size) {
                                        WidgetFontSize.SMALL -> 13.sp
                                        WidgetFontSize.MEDIUM -> 16.sp
                                        WidgetFontSize.LARGE -> 20.sp
                                        WidgetFontSize.EXTRA_LARGE -> 24.sp
                                    },
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF90909A)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = when (size) {
                                        WidgetFontSize.SMALL -> "16sp"
                                        WidgetFontSize.MEDIUM -> "22sp"
                                        WidgetFontSize.LARGE -> "30sp"
                                        WidgetFontSize.EXTRA_LARGE -> "40sp"
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else Color(0xFF70707A)
                                )
                            }
                        }
                    }

                    // Control C: Attribution Format
                    CustomizationCard(
                        title = "Footer Attribution",
                        subtitle = "Track metadata credit displayed below the lyric"
                    ) {
                        SegmentedControl(
                            items = WidgetAttribution.values().toList(),
                            selectedItem = config.attribution,
                            onItemSelected = { attr -> update(config.copy(attribution = attr)) }
                        ) { attr, isSelected ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = when (attr) {
                                        WidgetAttribution.NONE -> "None"
                                        WidgetAttribution.ARTIST_ONLY -> "Artist"
                                        WidgetAttribution.SONG_AND_ARTIST -> "Song & Artist"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else Color(0xFF90909A),
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = when (attr) {
                                        WidgetAttribution.NONE -> "Lyric only"
                                        WidgetAttribution.ARTIST_ONLY -> "— Artist"
                                        WidgetAttribution.SONG_AND_ARTIST -> "— Song • Artist"
                                    },
                                    fontSize = 9.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF60606A),
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun CustomizationCard(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, Color(0xFF26262E))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun <T> SegmentedControl(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    itemLabel: @Composable (T, Boolean) -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF141418),
        border = BorderStroke(1.dp, Color(0xFF24242C))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items.forEach { item ->
                val isSelected = item == selectedItem
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) Color(0xFF262630) else Color.Transparent,
                    label = "segmentBg"
                )
                val borderColor by animateColorAsState(
                    targetValue = if (isSelected) Color(0xFF3E3E4C) else Color.Transparent,
                    label = "segmentBorder"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(bgColor)
                        .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                        .clickable { onItemSelected(item) }
                        .padding(vertical = 10.dp, horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    itemLabel(item, isSelected)
                }
            }
        }
    }
}

@Composable
private fun LiveWidgetPreviewCard(
    config: WidgetConfig,
    lyric: DailyLyric
) {
    val bgColor = when (config.template) {
        WidgetTemplate.GENIUS -> Color(0xFF0A0A0A)
        WidgetTemplate.SPOTIFY -> Color(0xFF121212)
        WidgetTemplate.CLASSIC -> Color(0xFF18181A)
        WidgetTemplate.TERMINAL -> Color(0xFF0D1117)
        WidgetTemplate.MINIMAL -> Color(0xFF000000)
    }

    val finalFontFamily = when (config.fontFamily) {
        WidgetFontFamily.SERIF -> FontFamily.Serif
        WidgetFontFamily.MONOSPACE -> FontFamily.Monospace
        WidgetFontFamily.SYSTEM -> when (config.template) {
            WidgetTemplate.CLASSIC -> FontFamily.Serif
            WidgetTemplate.TERMINAL -> FontFamily.Monospace
            else -> FontFamily.SansSerif
        }
    }

    val clean = lyric.text.trim().trim('"', '“', '”', '‘', '’')
    val formatted = when (config.template) {
        WidgetTemplate.GENIUS -> "“${clean.uppercase()}”"
        WidgetTemplate.SPOTIFY -> "“$clean”"
        WidgetTemplate.CLASSIC -> "“$clean”"
        WidgetTemplate.TERMINAL -> "> $clean"
        WidgetTemplate.MINIMAL -> clean
    }

    val textColor = when (config.template) {
        WidgetTemplate.SPOTIFY -> Color.White
        WidgetTemplate.TERMINAL -> Color(0xFFFFB454)
        WidgetTemplate.CLASSIC -> Color(0xFFF2F2F7)
        WidgetTemplate.MINIMAL -> Color(0xFFEBEBF0)
        else -> Color.White
    }

    val fontStyle = when (config.template) {
        WidgetTemplate.CLASSIC -> FontStyle.Italic
        else -> FontStyle.Normal
    }

    val fontWeight = when (config.template) {
        WidgetTemplate.GENIUS, WidgetTemplate.SPOTIFY -> FontWeight.Bold
        WidgetTemplate.MINIMAL -> FontWeight.Normal
        else -> FontWeight.Medium
    }

    val attributionColor = when (config.template) {
        WidgetTemplate.SPOTIFY -> Color(0xFF1ED760)
        WidgetTemplate.TERMINAL -> Color(0xFFE5A93C)
        WidgetTemplate.MINIMAL -> Color(0xFF636366)
        else -> Color(0xFF8E8E93)
    }

    val attributionText = when (config.attribution) {
        WidgetAttribution.NONE -> null
        WidgetAttribution.ARTIST_ONLY -> when (config.template) {
            WidgetTemplate.SPOTIFY -> "♫ ${lyric.artistName}"
            WidgetTemplate.TERMINAL -> "$ ${lyric.artistName}"
            else -> "— ${lyric.artistName}"
        }
        WidgetAttribution.SONG_AND_ARTIST -> when (config.template) {
            WidgetTemplate.SPOTIFY -> "♫ ${lyric.songTitle} • ${lyric.artistName}"
            WidgetTemplate.TERMINAL -> "$ ${lyric.songTitle} :: ${lyric.artistName}"
            else -> "— ${lyric.songTitle} • ${lyric.artistName}"
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp)),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, Color(0xFF26262E))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = formatted,
                color = textColor,
                fontSize = config.fontSize.sizeSp.sp,
                lineHeight = config.fontSize.lineHeightSp.sp,
                fontWeight = fontWeight,
                fontStyle = fontStyle,
                fontFamily = finalFontFamily,
                textAlign = TextAlign.Center
            )

            if (attributionText != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = attributionText,
                    color = attributionColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = if (config.template == WidgetTemplate.TERMINAL) FontFamily.Monospace else FontFamily.SansSerif,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun TemplateGalleryCard(
    template: WidgetTemplate,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF26262E),
        label = "border"
    )

    val previewBg = when (template) {
        WidgetTemplate.GENIUS -> Color(0xFF0A0A0A)
        WidgetTemplate.SPOTIFY -> Color(0xFF121212)
        WidgetTemplate.CLASSIC -> Color(0xFF18181A)
        WidgetTemplate.TERMINAL -> Color(0xFF0D1117)
        WidgetTemplate.MINIMAL -> Color(0xFF000000)
    }

    val previewText = when (template) {
        WidgetTemplate.GENIUS -> "“LYRIC HERE”"
        WidgetTemplate.SPOTIFY -> "“Lyric here”"
        WidgetTemplate.CLASSIC -> "“Lyric here”"
        WidgetTemplate.TERMINAL -> "> lyric"
        WidgetTemplate.MINIMAL -> "lyric"
    }

    val previewColor = when (template) {
        WidgetTemplate.TERMINAL -> Color(0xFFFFB454)
        WidgetTemplate.CLASSIC -> Color(0xFFF2F2F7)
        else -> Color.White
    }

    val previewFont = when (template) {
        WidgetTemplate.CLASSIC -> FontFamily.Serif
        WidgetTemplate.TERMINAL -> FontFamily.Monospace
        else -> FontFamily.SansSerif
    }

    val accent = when (template) {
        WidgetTemplate.SPOTIFY -> Color(0xFF1ED760)
        WidgetTemplate.TERMINAL -> Color(0xFFE5A93C)
        else -> null
    }

    Card(
        modifier = Modifier
            .width(160.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF141418)
        )
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Miniature preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(82.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(previewBg)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = previewText,
                        color = previewColor,
                        fontSize = 11.sp,
                        fontWeight = if (template == WidgetTemplate.GENIUS || template == WidgetTemplate.SPOTIFY) FontWeight.Bold else FontWeight.Normal,
                        fontStyle = if (template == WidgetTemplate.CLASSIC) FontStyle.Italic else FontStyle.Normal,
                        fontFamily = previewFont,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                    if (accent != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .size(width = 16.dp, height = 3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(accent)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = template.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = template.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        maxLines = 2
                    )
                }
                if (isSelected) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
