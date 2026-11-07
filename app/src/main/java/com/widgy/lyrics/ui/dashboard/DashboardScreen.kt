package com.widgy.lyrics.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onAddSongClick: () -> Unit,
    onSongClick: (Long) -> Unit,
    onChangeThemeClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.reloadConfig(context)
        viewModel.loadTodayLyric()
    }

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
                    shape = RoundedCornerShape(14.dp),
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
    themeConfig: com.widgy.lyrics.domain.theme.ThemeConfig,
    isRefreshing: Boolean,
    isPinned: Boolean = false,
    onTogglePin: () -> Unit = {},
    onRefreshClick: () -> Unit,
    onClick: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val artworkManager = remember { com.widgy.lyrics.data.image.ArtworkManager(context) }
    var albumArtBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(dailyLyric?.songId, themeConfig.useAlbumArtBackdrop) {
        if (themeConfig.useAlbumArtBackdrop && dailyLyric != null && dailyLyric.songId > 0) {
            val art = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                artworkManager.getOrFetchArtworkBitmap(
                    dailyLyric.songId,
                    dailyLyric.songTitle,
                    dailyLyric.artistName,
                    maxDimension = 200
                )
            }
            albumArtBitmap = if (art != null) com.widgy.lyrics.data.image.StackBlur.blur(art, 22) else null
        } else {
            albumArtBitmap = null
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color(themeConfig.themeSpec.backgroundColor)
        ),
        shape = RoundedCornerShape(22.dp)
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
                        text = "\u201cNo lyrics in rotation yet\u201d",
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
                }
            }

            // Top-Left: Locked Badge
            if (isPinned) {
                Box(
                    modifier = Modifier
                        .padding(12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xCC000000))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFFFFB454),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Locked",
                            color = Color(0xFFFFB454),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Action buttons top-right (Lock, Share, Shuffle)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (dailyLyric != null) {
                        IconButton(onClick = onTogglePin) {
                            Icon(
                                imageVector = if (isPinned) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = if (isPinned) "Unlock rotation" else "Lock lyric on widget",
                                tint = if (isPinned) Color(0xFFFFB454) else Color(0xCCFFFFFF)
                            )
                        }

                        IconButton(
                            onClick = {
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
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = "Share Story Card",
                                tint = Color(0xCCFFFFFF)
                            )
                        }
                    }

                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(8.dp).size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = onRefreshClick) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Shuffle Lyric",
                                tint = Color(0xCCFFFFFF)
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
