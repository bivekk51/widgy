package com.widgy.lyrics.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.widgy.lyrics.data.db.SnippetEntity
import com.widgy.lyrics.data.db.SongWithSnippets
import com.widgy.lyrics.domain.model.DailyLyric
import com.widgy.lyrics.ui.theme.WidgyShapes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onAddSongClick: () -> Unit,
    onSongClick: (Long) -> Unit,
    onChangeThemeClick: () -> Unit,
    onScrapbookClick: () -> Unit = {},
    onImportPlaylistClick: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.reloadConfig(context)
        viewModel.loadTodayLyric(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Widgy",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                actions = {
                    IconButton(onClick = onScrapbookClick) {
                        Icon(Icons.Default.DateRange, contentDescription = "Lyric Scrapbook")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddSongClick,
                shape = WidgyShapes.large,
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
                    themeConfig = state.themeConfig,
                    isRefreshing = state.isRefreshingLyric,
                    isPinned = state.isLyricPinned,
                    onTogglePin = { viewModel.toggleLyricPin(context) },
                    onRefreshClick = { viewModel.refreshTodayLyric(context) },
                    onClick = {
                        state.todayLyric?.let { onSongClick(it.songId) }
                    }
                )
            }

            item {
                Button(
                    onClick = onChangeThemeClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = WidgyShapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(
                        Icons.Default.Palette,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Customize Widget & Themes",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Rotation frequency",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (state.rotationIntervalHours >= 24) "Daily at midnight" else "Every ${state.rotationIntervalHours}h",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(1 to "1h", 3 to "3h", 6 to "6h", 12 to "12h", 24 to "Daily").forEach { (hours, label) ->
                            val isSelected = state.rotationIntervalHours == hours
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.updateRotationInterval(hours, context) },
                                label = { Text(label, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                                modifier = Modifier.weight(1f),
                                shape = WidgyShapes.small
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "My Curation",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(
                        onClick = onImportPlaylistClick,
                        modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    ) {
                        Text(
                            text = "Import Playlist",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
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
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Tap + to search lyrics and curate 1 or 2 lines",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
    themeConfig: com.widgy.lyrics.domain.theme.ThemeConfig,
    isRefreshing: Boolean,
    isPinned: Boolean = false,
    onTogglePin: () -> Unit = {},
    onRefreshClick: () -> Unit,
    onClick: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val artworkManager = remember { com.widgy.lyrics.data.image.ArtworkManager(context) }
    var albumArtBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(dailyLyric?.songId, themeConfig.useAlbumArtBackdrop, themeConfig.blurRadius) {
        if (themeConfig.useAlbumArtBackdrop && dailyLyric != null && dailyLyric.songId > 0) {
            val art = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                artworkManager.getOrFetchArtworkBitmap(
                    dailyLyric.songId,
                    dailyLyric.songTitle,
                    dailyLyric.artistName,
                    maxDimension = 200
                )
            }
            albumArtBitmap = if (art != null) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                    com.widgy.lyrics.data.image.StackBlur.blur(art, themeConfig.blurRadius)
                }
            } else null
        } else {
            albumArtBitmap = null
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color(themeConfig.themeSpec.backgroundColor)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            if (dailyLyric != null) {
                // Live bitmap preview using the shared LyricRenderer — same output as widget
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val density = androidx.compose.ui.platform.LocalDensity.current.density
                    val widthPx = (maxWidth.value * density).toInt()
                    val heightPx = (maxHeight.value * density).toInt()

                    val lines = dailyLyric.text.trim()
                        .lines().map { it.trim() }.filter { it.isNotBlank() }

                    val bitmap = remember(themeConfig, lines, dailyLyric.songTitle, dailyLyric.artistName, widthPx, heightPx, albumArtBitmap) {
                        if (widthPx > 50 && heightPx > 50) {
                            com.widgy.lyrics.ui.theme.LyricRenderer.render(
                                context = context,
                                config = themeConfig,
                                lines = lines,
                                songTitle = dailyLyric.songTitle,
                                artistName = dailyLyric.artistName,
                                widthPx = widthPx,
                                heightPx = heightPx,
                                padTopDp = 18f,
                                padBottomDp = if (themeConfig.creditMode != com.widgy.lyrics.domain.theme.ThemeCreditMode.NONE) 36f else 18f,
                                padHorizontalDp = 16f,
                                albumArtBitmap = albumArtBitmap
                            )
                        } else null
                    }

                    if (bitmap != null) {
                        androidx.compose.foundation.Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Today's lyric preview",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            } else {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "No lyrics in rotation yet",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap + below to add your first song",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Contrast-safe action buttons top-right (Lock, Share, Shuffle)
            val isCardLight = remember(themeConfig.themeSpec.backgroundColor, themeConfig.useAlbumArtBackdrop) {
                if (themeConfig.useAlbumArtBackdrop) {
                    false
                } else {
                    val bg = themeConfig.themeSpec.backgroundColor
                    val r = android.graphics.Color.red(bg)
                    val g = android.graphics.Color.green(bg)
                    val b = android.graphics.Color.blue(bg)
                    (0.299 * r + 0.587 * g + 0.114 * b) / 255.0 > 0.6
                }
            }
            val cardActionTint = if (isCardLight) Color(0xFF1C1A17) else Color.White
            val cardActionButtonBg = if (isCardLight) Color(0x1F000000) else Color(0x33000000)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (dailyLyric != null) {
                        IconButton(
                            onClick = onTogglePin,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(cardActionButtonBg)
                        ) {
                            Icon(
                                imageVector = if (isPinned) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = if (isPinned) "Unlock rotation" else "Lock lyric on widget",
                                tint = if (isPinned) MaterialTheme.colorScheme.primary else cardActionTint,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                    val lines = dailyLyric.text.trim().lines().map { it.trim() }.filter { it.isNotBlank() }
                                    val storyBitmap = com.widgy.lyrics.ui.share.ShareCardHelper.generateStoryCard(
                                        context = context,
                                        config = themeConfig,
                                        lines = lines,
                                        songTitle = dailyLyric.songTitle,
                                        artistName = dailyLyric.artistName,
                                        albumArtBitmap = albumArtBitmap,
                                        isStoryFormat = true
                                    )
                                    com.widgy.lyrics.ui.share.ShareCardHelper.shareLyricCard(
                                        context = context,
                                        bitmap = storyBitmap,
                                        songTitle = dailyLyric.songTitle,
                                        artistName = dailyLyric.artistName
                                    )
                                }
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(cardActionButtonBg)
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = "Share Story Card",
                                tint = cardActionTint,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    if (isRefreshing) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(cardActionButtonBg),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = cardActionTint,
                                strokeWidth = 2.dp
                            )
                        }
                    } else {
                        IconButton(
                            onClick = onRefreshClick,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(cardActionButtonBg)
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Shuffle Lyric",
                                tint = cardActionTint,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                    shape = WidgyShapes.small,
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = if (song.isRandomPool) "Random pool" else "${snippets.size} lines",
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
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
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
                        text = "Select lines",
                        fontSize = 13.sp,
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
            text = snippet.content,
            fontSize = 13.sp,
            fontFamily = FontFamily.Serif,
            fontStyle = FontStyle.Italic,
            color = if (snippet.isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
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
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
