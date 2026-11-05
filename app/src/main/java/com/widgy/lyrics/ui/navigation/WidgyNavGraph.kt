package com.widgy.lyrics.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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

object NavRoutes {
    const val DASHBOARD = "dashboard"
    const val SEARCH = "search"
    const val LYRICS_LOCAL = "lyrics_local/{songId}"

    fun lyricsLocal(songId: Long) = "lyrics_local/$songId"
}

@Composable
fun WidgyNavGraph(
    navController: NavHostController,
    repository: LyricsRepository,
    initialSongId: Long? = null
) {
    var selectedOnlineTrack = remember { mutableListOf<LrcTrackDto>() }

    val startDestination = if (initialSongId != null && initialSongId > 0) {
        NavRoutes.lyricsLocal(initialSongId)
    } else {
        NavRoutes.DASHBOARD
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(NavRoutes.DASHBOARD) {
            val dashboardViewModel = remember { DashboardViewModel(repository) }
            DashboardScreen(
                viewModel = dashboardViewModel,
                onAddSongClick = { navController.navigate(NavRoutes.SEARCH) },
                onSongClick = { songId -> navController.navigate(NavRoutes.lyricsLocal(songId)) }
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
                lyricsViewModel.loadLocalSong(songId)
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
