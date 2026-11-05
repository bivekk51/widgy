package com.widgy.lyrics.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.widgy.lyrics.data.db.SnippetEntity
import com.widgy.lyrics.data.db.SongWithSnippets
import com.widgy.lyrics.data.preferences.WidgetAttribution
import com.widgy.lyrics.data.preferences.WidgetConfig
import com.widgy.lyrics.data.preferences.WidgetFontSize
import com.widgy.lyrics.data.preferences.WidgetTemplate
import com.widgy.lyrics.domain.model.DailyLyric

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onAddSongClick: () -> Unit,
    onSongClick: (Long) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Widgy",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddSongClick,
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Song")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                TodayLyricLiveCard(
                    dailyLyric = state.todayLyric,
                    config = state.widgetConfig,
                    isRefreshing = state.isRefreshingLyric,
                    onRefreshClick = { viewModel.refreshTodayLyric(context) },
                    onClick = {
                        state.todayLyric?.let { onSongClick(it.songId) }
                    }
                )
            }

            item {
                WidgetTemplateCustomizer(
                    config = state.widgetConfig,
                    onConfigChanged = { updated ->
                        viewModel.updateWidgetConfig(updated, context)
                    }
                )
            }

            item {
                Text(
                    text = "My Curation",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (state.savedSongs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "No songs saved yet",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Tap + to search lyrics and curate 1 or 2 lines",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            } else {
                items(state.savedSongs, key = { it.song.id }) { songWithSnippets ->
                    SongCurationCard(
                        songWithSnippets = songWithSnippets,
                        onSongClick = { onSongClick(songWithSnippets.song.id) },
                        onToggleSnippet = { snippetId, isActive ->
                            viewModel.toggleSnippetActive(snippetId, isActive, context)
                        },
                        onDeleteSong = { viewModel.deleteSong(songWithSnippets.song.id, context) },
                        onDeleteSnippet = { snippetId -> viewModel.deleteSnippet(snippetId, context) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }
}

@Composable
private fun TodayLyricLiveCard(
    dailyLyric: DailyLyric?,
    config: WidgetConfig,
    isRefreshing: Boolean,
    onRefreshClick: () -> Unit,
    onClick: () -> Unit
) {
    val cardBg = when (config.template) {
        WidgetTemplate.GENIUS -> Color(0xFF0C0C0C)
        WidgetTemplate.CLASSIC -> Color(0xFF161618)
        WidgetTemplate.MINIMAL -> Color(0xFF1E1E22)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(22.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Top Right Refresh Button
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopEnd) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .height(18.dp)
                            .width(18.dp),
                        color = Color.White
                    )
                } else {
                    IconButton(onClick = onRefreshClick) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Shuffle Lyric",
                            tint = Color(0x99FFFFFF)
                        )
                    }
                }
            }

            // Center Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (dailyLyric == null) {
                    Text(
                        text = "“No lyrics in rotation yet”",
                        color = Color(0xFF8E8E93),
                        fontSize = 17.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tap + below to add your first song",
                        color = Color(0xFF636366),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                } else {
                    val formatted = when (config.template) {
                        WidgetTemplate.GENIUS -> "“${dailyLyric.text.uppercase()}”"
                        else -> "“${dailyLyric.text}”"
                    }

                    val fontStyle = when (config.template) {
                        WidgetTemplate.GENIUS -> MaterialTheme.typography.titleLarge.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            fontSize = config.fontSize.sizeSp.sp,
                            textAlign = TextAlign.Center
                        )
                        WidgetTemplate.CLASSIC -> MaterialTheme.typography.titleLarge.copy(
                            color = Color(0xFFF2F2F7),
                            fontStyle = FontStyle.Italic,
                            fontFamily = FontFamily.Serif,
                            fontSize = config.fontSize.sizeSp.sp,
                            textAlign = TextAlign.Center
                        )
                        WidgetTemplate.MINIMAL -> MaterialTheme.typography.titleLarge.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.SansSerif,
                            fontSize = config.fontSize.sizeSp.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    Text(
                        text = formatted,
                        style = fontStyle,
                        textAlign = TextAlign.Center
                    )

                    val attribution = when (config.attribution) {
                        WidgetAttribution.NONE -> null
                        WidgetAttribution.ARTIST_ONLY -> "— ${dailyLyric.artistName}"
                        WidgetAttribution.SONG_AND_ARTIST -> "— ${dailyLyric.songTitle} • ${dailyLyric.artistName}"
                    }

                    if (attribution != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = attribution,
                            color = Color(0xFF8E8E93),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetTemplateCustomizer(
    config: WidgetConfig,
    onConfigChanged: (WidgetConfig) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Widget Style & Template",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Template Style Row
            Text(
                text = "Template",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(WidgetTemplate.values()) { template ->
                    FilterChip(
                        selected = config.template == template,
                        onClick = { onConfigChanged(config.copy(template = template)) },
                        label = { Text(template.displayName) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Attribution Row
            Text(
                text = "Footer Display",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(WidgetAttribution.values()) { attr ->
                    FilterChip(
                        selected = config.attribution == attr,
                        onClick = { onConfigChanged(config.copy(attribution = attr)) },
                        label = { Text(attr.displayName) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Font Size Row
            Text(
                text = "Font Size",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(WidgetFontSize.values()) { size ->
                    FilterChip(
                        selected = config.fontSize == size,
                        onClick = { onConfigChanged(config.copy(fontSize = size)) },
                        label = { Text(size.displayName) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SongCurationCard(
    songWithSnippets: SongWithSnippets,
    onSongClick: () -> Unit,
    onToggleSnippet: (Long, Boolean) -> Unit,
    onDeleteSong: () -> Unit,
    onDeleteSnippet: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val song = songWithSnippets.song
    val snippets = songWithSnippets.snippets

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = if (song.isRandomPool) "Random Pool" else "${snippets.size} lines",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(onClick = onDeleteSong) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete Song",
                        tint = MaterialTheme.colorScheme.outline
                    )
                }

                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    if (song.isRandomPool) {
                        Text(
                            text = "Every day, a random 1 or 2 lines from this track will be pulled into the daily pool.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        snippets.forEach { snippet ->
                            SnippetRow(
                                snippet = snippet,
                                onToggle = { onToggleSnippet(snippet.id, it) },
                                onDelete = { onDeleteSnippet(snippet.id) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "View & pick lines →",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { onSongClick() }
                            .padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SnippetRow(
    snippet: SnippetEntity,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "“${snippet.content}”",
            fontSize = 13.sp,
            fontFamily = FontFamily.Serif,
            fontStyle = FontStyle.Italic,
            color = if (snippet.isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = snippet.isActive,
            onCheckedChange = onToggle,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete Line",
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}
