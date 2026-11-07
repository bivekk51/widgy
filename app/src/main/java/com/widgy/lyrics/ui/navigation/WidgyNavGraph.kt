package com.widgy.lyrics.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.widgy.lyrics.data.model.LrcTrackDto
import com.widgy.lyrics.data.repository.LyricsRepository
import com.widgy.lyrics.ui.dashboard.DashboardScreen
import com.widgy.lyrics.ui.dashboard.DashboardViewModel
import com.widgy.lyrics.ui.lyrics.LyricsViewerScreen
import com.widgy.lyrics.ui.lyrics.LyricsViewerViewModel
import com.widgy.lyrics.ui.search.SearchScreen
import com.widgy.lyrics.ui.search.SearchViewModel
import com.widgy.lyrics.ui.theme.ThemeStudioScreen

object NavRoutes {
    const val DASHBOARD = "dashboard"
    const val SEARCH = "search"
    const val THEME_STUDIO = "theme_studio"
    const val LYRICS_LOCAL = "lyrics_local/{songId}"
    const val MANUAL_ENTRY = "manual_entry"

    fun lyricsLocal(songId: Long) = "lyrics_local/$songId"
}

@Composable
fun WidgyNavGraph(
    navController: NavHostController,
    repository: LyricsRepository,
    targetSongId: Long? = null,
    onTargetSongConsumed: () -> Unit = {},
    initialSnippetText: String? = null
) {
    val context = LocalContext.current
    val dashboardViewModel = remember { DashboardViewModel(repository, context) }
    var selectedOnlineTrack = remember { mutableListOf<LrcTrackDto>() }

    // Reactively navigate when targetSongId is provided (cold launch or onNewIntent from widget)
    LaunchedEffect(targetSongId) {
        if (targetSongId != null && targetSongId > 0) {
            onTargetSongConsumed()
            navController.navigate(NavRoutes.lyricsLocal(targetSongId))
        }
    }

    // Start at DASHBOARD so back press always brings user to Dashboard
    NavHost(
        navController = navController,
        startDestination = NavRoutes.DASHBOARD
    ) {
        composable(NavRoutes.DASHBOARD) {
            DashboardScreen(
                viewModel = dashboardViewModel,
                onAddSongClick = { navController.navigate(NavRoutes.SEARCH) },
                onSongClick = { songId -> navController.navigate(NavRoutes.lyricsLocal(songId)) },
                onChangeThemeClick = { navController.navigate(NavRoutes.THEME_STUDIO) }
            )
        }

        composable(NavRoutes.THEME_STUDIO) {
            val uiState by dashboardViewModel.uiState.collectAsState()

            ThemeStudioScreen(
                initialConfig = uiState.themeConfig,
                dailyLyric = uiState.todayLyric,
                onConfigChanged = { /* Live preview in ThemeStudio; only persist on apply */ },
                onApplyConfig = { newConfig ->
                    dashboardViewModel.updateThemeConfig(newConfig, context)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.SEARCH) {
            val searchViewModel = remember { SearchViewModel() }
            SearchScreen(
                viewModel = searchViewModel,
                onTrackSelected = { track ->
                    selectedOnlineTrack.clear()
                    selectedOnlineTrack.add(track)
                    navController.navigate("lyrics_online")
                },
                onManualEntryClick = {
                    navController.navigate(NavRoutes.MANUAL_ENTRY)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.MANUAL_ENTRY) {
            val lyricsViewModel = remember { LyricsViewerViewModel(repository) }
            LaunchedEffect(Unit) {
                lyricsViewModel.initManualEntry()
            }
            LyricsViewerScreen(
                viewModel = lyricsViewModel,
                onSaved = {
                    navController.popBackStack(NavRoutes.DASHBOARD, inclusive = false)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable("lyrics_online") {
            val lyricsViewModel = remember { LyricsViewerViewModel(repository) }
            val track = selectedOnlineTrack.firstOrNull()
            LaunchedEffect(track) {
                if (track != null) {
                    lyricsViewModel.loadTrack(track)
                }
            }
            LyricsViewerScreen(
                viewModel = lyricsViewModel,
                onSaved = {
                    navController.popBackStack(NavRoutes.DASHBOARD, inclusive = false)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = NavRoutes.LYRICS_LOCAL,
            arguments = listOf(navArgument("songId") { type = NavType.LongType })
        ) { backStackEntry ->
            val songId = backStackEntry.arguments?.getLong("songId") ?: -1L
            val lyricsViewModel = remember { LyricsViewerViewModel(repository) }
            LaunchedEffect(songId) {
                lyricsViewModel.loadLocalSong(songId, initialSnippetText)
            }
            LyricsViewerScreen(
                viewModel = lyricsViewModel,
                onSaved = {
                    navController.popBackStack(NavRoutes.DASHBOARD, inclusive = false)
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
