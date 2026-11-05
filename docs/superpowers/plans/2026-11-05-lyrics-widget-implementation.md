# Widgy: Daily Lyric Widget Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a battery-efficient, minimalist Android app and Jetpack Glance home screen widget that fetches song lyrics from LRCLIB, enables granular line curation, and deterministically rotates a daily quote with zero background CPU/RAM waste.

**Architecture:** A native Kotlin Android app with Jetpack Compose for UI, Room SQLite database for offline storage, and a Retrofit/OkHttp client for the public LRCLIB API. A Jetpack Glance widget presents the daily lyric with zero persistent background services, supported by a <100ms midnight WorkManager one-shot worker and deterministic fallback calculation.

**Tech Stack:** Kotlin 2.0, Android Gradle Plugin 8.7, Jetpack Compose (BOM 2024.10+), Jetpack Glance 1.1.0, Room 2.6.1, WorkManager 2.9.1, Retrofit 2.11.0 / OkHttp 4.12.0, Kotlinx Serialization, JUnit 4, Robolectric.

**Spec:** `docs/superpowers/specs/2026-11-05-lyrics-widget-design.md`

## Global Constraints
- Native Android with minimum SDK 26 (Android 8.0) and target/compile SDK 35.
- Absolute zero persistent background services or long-running daemons.
- LRCLIB API (`https://lrclib.net/api`) consumed with custom User-Agent header `WidgyAndroid/1.0`.
- All lyrics and user curations must be fully cached locally in SQLite (Room) for offline widget operation.
- Home screen widget built strictly using Google's declarative Jetpack Glance framework with Material You dynamic color support.

## Review Focus
1. Empty active pool (user installs app and adds widget before selecting any songs): widget must show a welcoming, clean placeholder prompting them to tap and pick a song without crashing.
2. LRCLIB network failure or invalid lyrics format: search/fetch errors must be caught gracefully and show a clear offline/error banner, never freezing the UI.
3. Song with timestamps (`[00:12.34]` synced lyrics) vs plain lyrics: lyrics parser must cleanly strip timestamp brackets and section metadata (`[Chorus]`, `[Verse]`) so widget displays pure readable lyric text.
4. Deep Doze / battery saver mode preventing the midnight WorkManager from firing: widget must fall back to lazy on-render deterministic day hash calculation upon morning view so the quote updates reliably.
5. Large lyrics selection (user selects a 5-line stanza): widget layout must dynamically scale typography or truncate cleanly with an ellipsis without overflowing or breaking layout boundaries in compact 2x2 widget slots.

---

### Task 1: Project Scaffolding & Gradle Setup

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle/libs.versions.toml`
- Create: `app/build.gradle.kts`
- Create: `local.properties`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/com/widgy/lyrics/WidgyApp.kt`

**Interfaces:**
- Consumes: Android SDK 35 at `/home/bivss/Android/Sdk`, OpenJDK 21, Gradle 8.12
- Produces: Compilable Android project structure with Compose, Glance, Room, WorkManager, and Retrofit dependencies

- [ ] **Step 1: Create Gradle version catalog and build scripts**

Configure `gradle/libs.versions.toml`, root `build.gradle.kts`, `settings.gradle.kts`, and `app/build.gradle.kts` with target SDK 35, min SDK 26, Compose BOM, Glance 1.1.0, Room 2.6.1, WorkManager 2.9.1, and Retrofit 2.11.0. Set `sdk.dir=/home/bivss/Android/Sdk` in `local.properties`.

- [ ] **Step 2: Create AndroidManifest.xml and Application class**

Declare `com.widgy.lyrics.WidgyApp` extending `android.app.Application`, with `android:allowBackup="true"`, theme settings, and `android.permission.INTERNET`.

- [ ] **Step 3: Verify build and test configuration**

Run: `/home/bivss/.gradle/wrapper/dists/gradle-8.12-bin/cetblhg4pflnnks72fxwobvgv/gradle-8.12/bin/gradle assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add gradle/ build.gradle.kts settings.gradle.kts app/ local.properties
git commit -m "chore: scaffold native Android project with Compose, Glance, and Room"
```

---

### Task 2: LRCLIB API Client & Lyrics Parser

**Files:**
- Create: `app/src/main/java/com/widgy/lyrics/data/model/LrcLibDto.kt`
- Create: `app/src/main/java/com/widgy/lyrics/data/api/LyricsCleaner.kt`
- Create: `app/src/main/java/com/widgy/lyrics/data/api/LrcLibApi.kt`
- Create: `app/src/main/java/com/widgy/lyrics/data/api/LrcLibClient.kt`
- Test: `app/src/test/java/com/widgy/lyrics/data/api/LyricsCleanerTest.kt`

**Interfaces:**
- Consumes: LRCLIB JSON responses (`https://lrclib.net/api/search`, `/api/get`)
- Produces:
  - `LyricsCleaner.cleanLyrics(rawLyrics: String): List<LyricLine>`
  - `LyricsCleaner.detectHooks(lines: List<String>): Set<Int>`
  - `LrcLibClient.searchTracks(query: String): Result<List<LrcTrackDto>>`
  - `LrcLibClient.getLyrics(trackName: String, artistName: String): Result<LrcTrackDto>`

- [ ] **Step 1: Write failing unit test for `LyricsCleaner`**

```kotlin
class LyricsCleanerTest {
    @Test
    fun cleanLyrics_stripsTimestampsAndMetadataHeaders() {
        val raw = "[00:12.34]First line\n[Chorus]\n[00:18.00]Second line"
        val cleaned = LyricsCleaner.cleanLyrics(raw)
        assertEquals(listOf("First line", "Second line"), cleaned.map { it.text })
        assertTrue(cleaned[1].isHook)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests com.widgy.lyrics.data.api.LyricsCleanerTest`
Expected: FAIL with compilation error (LyricsCleaner not defined)

- [ ] **Step 3: Implement `LyricsCleaner` and DTOs**

Implement `LyricsCleaner.cleanLyrics`: regex `\\[\\d{2}:\\d{2}\\.\\d{2,3}\\]` removal, trim whitespace, detect `[Chorus]`/`[Hook]` markers to flag following lines as hooks. Implement `LrcTrackDto` with `@Serializable`.

- [ ] **Step 4: Implement `LrcLibApi` and `LrcLibClient`**

Define Retrofit interface with `@GET("api/search")` and `@GET("api/get")`. Configure OkHttp with custom `User-Agent: WidgyAndroid/1.0 (https://github.com/widgy)`.

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew testDebugUnitTest --tests com.widgy.lyrics.data.api.LyricsCleanerTest`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/widgy/lyrics/data/ app/src/test/
git commit -m "feat: implement LRCLIB API client and lyrics timestamp/hook parser"
```

---

### Task 3: Room Database & Persistence Layer

**Files:**
- Create: `app/src/main/java/com/widgy/lyrics/data/db/SongEntity.kt`
- Create: `app/src/main/java/com/widgy/lyrics/data/db/SnippetEntity.kt`
- Create: `app/src/main/java/com/widgy/lyrics/data/db/DailyHistoryEntity.kt`
- Create: `app/src/main/java/com/widgy/lyrics/data/db/SongDao.kt`
- Create: `app/src/main/java/com/widgy/lyrics/data/db/SnippetDao.kt`
- Create: `app/src/main/java/com/widgy/lyrics/data/db/DailyHistoryDao.kt`
- Create: `app/src/main/java/com/widgy/lyrics/data/db/AppDatabase.kt`
- Test: `app/src/test/java/com/widgy/lyrics/data/db/AppDatabaseTest.kt`

**Interfaces:**
- Consumes: SQLite via AndroidX Room
- Produces:
  - `SongDao`: `insertSong()`, `deleteSong()`, `getAllSongsWithSnippets(): Flow<List<SongWithSnippets>>`, `getSongById(id: Long): SongEntity?`
  - `SnippetDao`: `insertSnippets()`, `updateSnippetActive()`, `getActiveSnippets(): List<SnippetEntity>`, `deleteSnippet()`
  - `DailyHistoryDao`: `getDailyLyric(date: String): DailyHistoryEntity?`, `insertOrUpdate(entity: DailyHistoryEntity)`

- [ ] **Step 1: Write failing unit test for `AppDatabase` (Robolectric)**

```kotlin
@RunWith(RobolectricTestRunner::class)
class AppDatabaseTest {
    @Test
    fun insertingSongAndSnippets_cascadesOnDelete() = runBlocking {
        val songId = db.songDao().insertSong(SongEntity(title = "Sing About Me", artist = "Kendrick Lamar", plainLyrics = "...", isRandomPool = false))
        db.snippetDao().insertSnippet(SnippetEntity(songId = songId, content = "Promise that you will sing about me"))
        db.songDao().deleteSongById(songId)
        assertTrue(db.snippetDao().getSnippetsForSong(songId).isEmpty())
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests com.widgy.lyrics.data.db.AppDatabaseTest`
Expected: FAIL (entities/DAOs not defined)

- [ ] **Step 3: Implement Room entities, DAOs, and `AppDatabase`**

Define `SongEntity`, `SnippetEntity` (foreignKey onDelete = CASCADE), and `DailyHistoryEntity`. Implement DAOs with Room annotations and `AppDatabase` with singleton builder.

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew testDebugUnitTest --tests com.widgy.lyrics.data.db.AppDatabaseTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/widgy/lyrics/data/db/ app/src/test/
git commit -m "feat: implement Room database entities, DAOs, and cascading relationships"
```

---

### Task 4: Repository Layer & Balanced Daily Rotation Engine

**Files:**
- Create: `app/src/main/java/com/widgy/lyrics/domain/model/Candidate.kt`
- Create: `app/src/main/java/com/widgy/lyrics/domain/model/DailyLyric.kt`
- Create: `app/src/main/java/com/widgy/lyrics/domain/engine/DailyRotationEngine.kt`
- Create: `app/src/main/java/com/widgy/lyrics/data/repository/LyricsRepository.kt`
- Test: `app/src/test/java/com/widgy/lyrics/domain/engine/DailyRotationEngineTest.kt`

**Interfaces:**
- Consumes: `AppDatabase` DAOs, `LyricsCleaner`
- Produces:
  - `DailyRotationEngine.resolveDailyLyric(date: LocalDate, forceRefresh: Boolean = false): DailyLyric?`
  - `LyricsRepository.saveSongWithSnippets(song: SongEntity, snippets: List<String>, isRandomPool: Boolean): Long`
  - `LyricsRepository.refreshTodayLyric(): DailyLyric?`

- [ ] **Step 1: Write failing unit tests for `DailyRotationEngine`**

```kotlin
class DailyRotationEngineTest {
    @Test
    fun resolveDailyLyric_balancedCandidates_randomPoolDoesNotOverwhelmSnippets() {
        // Given 1 curated snippet from Song A and 1 random-pool Song B with 50 lines
        // Verify both have 50% candidate probability, not 1 vs 50
    }
    
    @Test
    fun resolveDailyLyric_deterministicAcrossMultipleCallsOnSameDate() {
        val lyric1 = engine.resolveDailyLyric(LocalDate.of(2026, 11, 5), forceRefresh = false)
        val lyric2 = engine.resolveDailyLyric(LocalDate.of(2026, 11, 5), forceRefresh = false)
        assertEquals(lyric1, lyric2)
    }

    @Test
    fun resolveDailyLyric_emptyPool_returnsNullWithoutCrashing() {
        val result = engine.resolveDailyLyric(LocalDate.of(2026, 11, 5))
        assertNull(result)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests com.widgy.lyrics.domain.engine.DailyRotationEngineTest`
Expected: FAIL

- [ ] **Step 3: Implement `DailyRotationEngine` and `LyricsRepository`**

Implement balanced lottery: gather `activeSnippets` (as `Candidate.Curated`) and `randomPoolSongs` (as `Candidate.RandomSong`). If a `Candidate.RandomSong` is picked, randomly sample a non-empty, non-header line from its `plainLyrics`. Compute deterministic index via `(epochDay + seed) % candidates.size`. On `forceRefresh = true`, pick an alternate index from the candidates and commit immediately to `DailyHistoryDao`.

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew testDebugUnitTest --tests com.widgy.lyrics.domain.engine.DailyRotationEngineTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/widgy/lyrics/domain/ app/src/main/java/com/widgy/lyrics/data/repository/ app/src/test/
git commit -m "feat: implement balanced daily rotation engine and repository"
```

---

### Task 5: Jetpack Glance Home Screen Widget & Midnight Worker

**Files:**
- Create: `app/src/main/res/xml/daily_lyric_widget_info.xml`
- Create: `app/src/main/res/layout/glance_default_loading.xml`
- Create: `app/src/main/java/com/widgy/lyrics/widget/DailyLyricWidget.kt`
- Create: `app/src/main/java/com/widgy/lyrics/widget/DailyLyricWidgetReceiver.kt`
- Create: `app/src/main/java/com/widgy/lyrics/widget/DailyWidgetWorker.kt`
- Create: `app/src/main/java/com/widgy/lyrics/widget/WidgetUpdateManager.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Test: `app/src/test/java/com/widgy/lyrics/widget/DailyWidgetWorkerTest.kt`

**Interfaces:**
- Consumes: `DailyRotationEngine`, `WorkManager`, `GlanceAppWidget`
- Produces:
  - `DailyLyricWidget : GlanceAppWidget`: renders adaptive Material You card with lyric, song, artist, and tap actions
  - `DailyLyricWidgetReceiver : GlanceAppWidgetReceiver`
  - `DailyWidgetWorker : CoroutineWorker`: runs at midnight, resolves day's lyric, updates Glance state, shuts down <100ms
  - `WidgetUpdateManager.scheduleDailyRotation(context)`

- [ ] **Step 1: Write failing test for `DailyWidgetWorker`**

```kotlin
@RunWith(RobolectricTestRunner::class)
class DailyWidgetWorkerTest {
    @Test
    fun doWork_resolvesLyricAndReturnsSuccess() = runBlocking {
        val worker = TestListenableWorkerBuilder<DailyWidgetWorker>(context).build()
        val result = worker.doWork()
        assertEquals(ListenableWorker.Result.success(), result)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests com.widgy.lyrics.widget.DailyWidgetWorkerTest`
Expected: FAIL

- [ ] **Step 3: Implement `DailyLyricWidget`, `DailyWidgetWorker`, and XML configs**

Implement `DailyLyricWidget` using `GlanceTheme.colors.surfaceContainer` background, responsive padding, italicized lyric quote text, and subtitle footer. Add tap intent leading to `MainActivity` with `EXTRA_SONG_ID`. Add shuffle button invoking `ActionCallback` that calls `LyricsRepository.refreshTodayLyric()`. Implement `DailyWidgetWorker` with `WorkManager`.

- [ ] **Step 4: Register Receiver and Worker in `AndroidManifest.xml`**

Register `DailyLyricWidgetReceiver` with intent-filter `android.appwidget.action.APPWIDGET_UPDATE` and meta-data `@xml/daily_lyric_widget_info`.

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew testDebugUnitTest --tests com.widgy.lyrics.widget.DailyWidgetWorkerTest`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add app/src/main/res/ app/src/main/java/com/widgy/lyrics/widget/ app/src/main/AndroidManifest.xml app/src/test/
git commit -m "feat: implement Jetpack Glance home-screen widget and midnight WorkManager task"
```

---

### Task 6: Song Search & Lyrics Line Picker UI (Screens 2 & 3)

**Files:**
- Create: `app/src/main/java/com/widgy/lyrics/ui/search/SearchViewModel.kt`
- Create: `app/src/main/java/com/widgy/lyrics/ui/search/SearchScreen.kt`
- Create: `app/src/main/java/com/widgy/lyrics/ui/lyrics/LyricsViewerViewModel.kt`
- Create: `app/src/main/java/com/widgy/lyrics/ui/lyrics/LyricsViewerScreen.kt`
- Test: `app/src/test/java/com/widgy/lyrics/ui/search/SearchViewModelTest.kt`

**Interfaces:**
- Consumes: `LrcLibClient`, `LyricsRepository`, `LyricsCleaner`
- Produces:
  - `SearchScreen(onTrackSelected: (LrcTrackDto) -> Unit, onBack: () -> Unit)`
  - `LyricsViewerScreen(track: LrcTrackDto?, localSongId: Long?, onSaved: () -> Unit, onBack: () -> Unit)`

- [ ] **Step 1: Write failing unit test for `SearchViewModel`**

```kotlin
class SearchViewModelTest {
    @Test
    fun onQueryChanged_debouncesAndEmitsResults() = runTest {
        viewModel.onQueryChanged("Kendrick")
        advanceTimeBy(350)
        assertTrue(viewModel.uiState.value.results.isNotEmpty())
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests com.widgy.lyrics.ui.search.SearchViewModelTest`
Expected: FAIL

- [ ] **Step 3: Implement `SearchViewModel` and `SearchScreen`**

Use Material 3 `SearchBar` or `OutlinedTextField` with leading search icon, clear button, and 300ms debounce flow. Render result cards with track title, artist name, and album.

- [ ] **Step 4: Implement `LyricsViewerViewModel` and `LyricsViewerScreen`**

Parse full lyrics into line items. Render line list with click-to-select toggle. Show subtle `[Hook]` badge for chorus lines. Provide bottom sheet bar: "Save X Selected Lines" and "Add Song to Random Pool".

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew testDebugUnitTest --tests com.widgy.lyrics.ui.search.SearchViewModelTest`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/widgy/lyrics/ui/search/ app/src/main/java/com/widgy/lyrics/ui/lyrics/ app/src/test/
git commit -m "feat: implement song search and interactive lyrics line picker screens"
```

---

### Task 7: Dashboard Screen, Navigation & Deep Linking

**Files:**
- Create: `app/src/main/java/com/widgy/lyrics/ui/dashboard/DashboardViewModel.kt`
- Create: `app/src/main/java/com/widgy/lyrics/ui/dashboard/DashboardScreen.kt`
- Create: `app/src/main/java/com/widgy/lyrics/ui/navigation/WidgyNavGraph.kt`
- Modify: `app/src/main/java/com/widgy/lyrics/MainActivity.kt`
- Test: `app/src/test/java/com/widgy/lyrics/ui/dashboard/DashboardViewModelTest.kt`

**Interfaces:**
- Consumes: `LyricsRepository`, `DailyRotationEngine`
- Produces:
  - `DashboardScreen`: displays active daily lyric card with shuffle button, saved songs list with snippet count badges, active toggles, and delete options
  - `WidgyNavGraph`: handles `dashboard`, `search`, `lyrics/{songId}` routes and widget deep-links

- [ ] **Step 1: Write failing test for `DashboardViewModel`**

```kotlin
class DashboardViewModelTest {
    @Test
    fun toggleSnippetActive_updatesRepository() = runTest {
        viewModel.toggleSnippet(snippetId = 1L, isActive = false)
        verify(repository).setSnippetActive(1L, false)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests com.widgy.lyrics.ui.dashboard.DashboardViewModelTest`
Expected: FAIL

- [ ] **Step 3: Implement `DashboardViewModel` and `DashboardScreen`**

Build clean Material 3 dashboard. Today's lyric card at the top with quick "Shuffle" button. Expandable song cards below showing saved lines, active toggle switches, and delete button. Floating action button "+ Add Song".

- [ ] **Step 4: Implement `WidgyNavGraph` and `MainActivity` deep link handling**

Setup Jetpack Navigation. In `MainActivity`, inspect `intent.getLongExtra(EXTRA_SONG_ID, -1L)`. If present, immediately navigate to `lyrics/{songId}`. Initialize `WidgetUpdateManager.scheduleDailyRotation(this)` in `onCreate`.

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew testDebugUnitTest --tests com.widgy.lyrics.ui.dashboard.DashboardViewModelTest`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/widgy/lyrics/ui/dashboard/ app/src/main/java/com/widgy/lyrics/ui/navigation/ app/src/main/java/com/widgy/lyrics/MainActivity.kt app/src/test/
git commit -m "feat: implement dashboard curation view, navigation, and widget deep linking"
```

---

### Task 8: End-to-End Build Verification & Resource Optimization

**Files:**
- Modify: `app/build.gradle.kts` (R8 / Proguard optimizations, resource shrinking)
- Create: `app/proguard-rules.pro`
- Test: `app/src/test/java/com/widgy/lyrics/EndToEndVerificationTest.kt`

**Interfaces:**
- Consumes: Complete assembled app
- Produces: Validated, production-ready release/debug APK with zero background daemons

- [ ] **Step 1: Verify Zero-Process Contract**

Write test verifying `AndroidManifest.xml` declares NO background `android.app.Service` components, confirming 0% continuous RAM drain.

- [ ] **Step 2: Run full unit test suite**

Run: `/home/bivss/.gradle/wrapper/dists/gradle-8.12-bin/cetblhg4pflnnks72fxwobvgv/gradle-8.12/bin/gradle testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`, all tests passing

- [ ] **Step 3: Assemble debug APK**

Run: `/home/bivss/.gradle/wrapper/dists/gradle-8.12-bin/cetblhg4pflnnks72fxwobvgv/gradle-8.12/bin/gradle assembleDebug`
Expected: `BUILD SUCCESSFUL`, APK generated at `app/build/outputs/apk/debug/app-debug.apk`

- [ ] **Step 4: Commit**

```bash
git add app/build.gradle.kts app/proguard-rules.pro app/src/test/
git commit -m "chore: complete end-to-end build verification and resource optimization"
```
