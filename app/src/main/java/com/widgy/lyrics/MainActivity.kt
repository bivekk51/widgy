package com.widgy.lyrics

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.navigation.compose.rememberNavController
import com.widgy.lyrics.data.db.AppDatabase
import com.widgy.lyrics.data.repository.LyricsRepository
import com.widgy.lyrics.ui.navigation.WidgyNavGraph
import com.widgy.lyrics.widget.WidgetUpdateManager

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_SONG_ID = "extra_song_id"
        const val EXTRA_SNIPPET_TEXT = "extra_snippet_text"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Schedule once-daily midnight rotation worker (zero-process, <100ms)
        WidgetUpdateManager.scheduleDailyRotation(this)

        val db = AppDatabase.getDatabase(this)
        val repository = LyricsRepository(db)

        val initialSongId = intent?.getLongExtra(EXTRA_SONG_ID, -1L)?.takeIf { it > 0 }
        val initialSnippetText = intent?.getStringExtra(EXTRA_SNIPPET_TEXT)

        setContent {
            MaterialTheme {
                val navController = rememberNavController()
                WidgyNavGraph(
                    navController = navController,
                    repository = repository,
                    initialSongId = initialSongId,
                    initialSnippetText = initialSnippetText
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}
