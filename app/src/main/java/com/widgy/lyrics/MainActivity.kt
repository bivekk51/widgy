package com.widgy.lyrics

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.navigation.compose.rememberNavController
import com.widgy.lyrics.data.api.playlist.SharedHttpClient
import com.widgy.lyrics.data.db.AppDatabase
import com.widgy.lyrics.data.repository.LyricsRepository
import com.widgy.lyrics.ui.navigation.WidgyNavGraph
import com.widgy.lyrics.ui.theme.WidgyTheme
import com.widgy.lyrics.widget.WidgetUpdateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_SONG_ID = "extra_song_id"
        const val EXTRA_SNIPPET_TEXT = "extra_snippet_text"
    }

    private val targetSongId = mutableStateOf<Long?>(null)
    private val targetSnippetText = mutableStateOf<String?>(null)
    private val sharedPlaylistUrl = mutableStateOf<String?>(null)

    override fun onResume() {
        super.onResume()
        WidgetUpdateManager.notifyWidgetUpdate(this@MainActivity)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Schedule once-daily midnight rotation worker (zero-process, <100ms)
        WidgetUpdateManager.scheduleDailyRotation(this)

        val db = AppDatabase.getDatabase(this)
        val prefs = com.widgy.lyrics.data.preferences.WidgetPreferences(this)
        val repository = LyricsRepository(db, pinnedLyricProvider = { prefs.getPinnedLyric() })

        extractIntentData(intent)

        setContent {
            WidgyTheme {
                val navController = rememberNavController()
                WidgyNavGraph(
                    navController = navController,
                    repository = repository,
                    targetSongId = targetSongId.value,
                    onTargetSongConsumed = { targetSongId.value = null },
                    initialSnippetText = targetSnippetText.value,
                    sharedPlaylistUrl = sharedPlaylistUrl.value,
                    onSharedPlaylistUrlConsumed = { sharedPlaylistUrl.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractIntentData(intent)
    }

    private fun extractIntentData(intent: Intent?) {
        val songId = intent?.getLongExtra(EXTRA_SONG_ID, -1L)?.takeIf { it > 0 }
        val snippet = intent?.getStringExtra(EXTRA_SNIPPET_TEXT)
        if (songId != null) {
            targetSongId.value = songId
            targetSnippetText.value = snippet
        }

        if (intent?.action == Intent.ACTION_SEND && intent.type?.startsWith("text/") == true) {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT)
            val extractedUrl = text?.let { SharedHttpClient.extractUrlFromText(it) }
            if (extractedUrl != null) {
                sharedPlaylistUrl.value = extractedUrl
            }
        }
    }
}
