# Widgy Feature Expansion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement 7 high-impact features in Widgy: Album Art Backdrop widget style, Instagram Story / Wallpaper card exporter, Pin/Lock lyric rotation, Archive/Scrapbook history timeline, flexible rotation frequency (1h–24h), manual song/lyric entry with editable snippets, and widget refresh button polish.

**Architecture:** 
- Keep the zero continuous background service architecture intact.
- Image compositing happens inside the unified `LyricRenderer.kt` on-demand 2D Canvas bitmap, keeping Glance RemoteViews IPC payload below 2MB and avoiding any multi-view Glance latency.
- Album artwork is fetched asynchronously via the free, keyless iTunes Search API and cached locally to internal disk (`files/album_art/`).
- Room database is updated cleanly with backward-compatible migrations.

**Tech Stack:** Kotlin, Jetpack Compose, Jetpack Glance, Room Database, Android WorkManager, OkHttp / HttpURLConnection, Android FileProvider (Sharesheet).

**Spec:** Derived from competitive research (Verlix, LyricGlow, Lyrify, Musixmatch) and user-selected priorities.

## Global Constraints
- Strictly NO git releases or tags unless explicitly instructed by the user.
- Strictly NO adb install unless explicitly confirmed by the user.
- Preserve 100% test pass rate (`./gradlew testDebugUnitTest`).
- Preserve WCAG AAA contrast ratio on text over any blurred album art background.
- Keep widget memory consumption strictly below 500k pixels (~2MB ARGB) to prevent Binder IPC transaction errors.

## Review Focus
1. **Offline Album Art:** If the network is unavailable or a song has no iTunes match, the widget and preview must seamlessly fallback to theme background colors without flickering or crashing.
2. **Glance RemoteViews Binder Limit:** Blurred background bitmap rendering must not bloat memory beyond the 500,000 pixel cap.
3. **FileProvider Permissions:** Exported Instagram Story card PNGs must have proper `FLAG_GRANT_READ_URI_PERMISSION` via Android `FileProvider` so third-party apps (Instagram, WhatsApp) can read the cached file.
4. **Pin State Invalidation:** When a lyric is pinned, `DailyRotationEngine` and `DailyWidgetWorker` must never overwrite the displayed lyric until the user explicitly unpins it.
5. **WorkManager Interval Floors:** Android WorkManager enforces a minimum periodic interval of 15 minutes; interval options (1h, 3h, 6h, 12h, 24h) must respect `ExistingPeriodicWorkPolicy.UPDATE` without duplicate workers.

---

### Task 1: Manual Song Entry & Snippet Text Editing

**Files:**
- Modify: `app/src/main/java/com/widgy/lyrics/ui/search/SearchScreen.kt`
- Modify: `app/src/main/java/com/widgy/lyrics/ui/lyrics/LyricsViewerScreen.kt`
- Modify: `app/src/main/java/com/widgy/lyrics/ui/lyrics/LyricsViewerViewModel.kt`
- Test: `app/src/test/java/com/widgy/lyrics/ui/lyrics/LyricsViewerViewModelTest.kt`

**Interfaces:**
- `LyricsViewerViewModel.initManualEntry()`: sets state for custom song authoring.
- `LyricsViewerViewModel.updateEditedSnippet(text: String)`: allows custom edits to the chosen snippet.
- `LyricsViewerViewModel.saveCustomSongWithSnippet(title: String, artist: String, snippet: String)`: persists custom lyrics to Room.

- [ ] **Step 1: Write unit tests for manual entry and snippet editing in `LyricsViewerViewModelTest.kt`**
- [ ] **Step 2: Run test to verify it fails (`./gradlew testDebugUnitTest --tests com.widgy.lyrics.ui.lyrics.LyricsViewerViewModelTest`)**
- [ ] **Step 3: Implement manual song creation flow and snippet editing dialog in `LyricsViewerViewModel` & `LyricsViewerScreen`**
- [ ] **Step 4: Add "Create Custom Track / Paste Lyrics" entry button on `SearchScreen.kt`**
- [ ] **Step 5: Run tests to verify they pass**
- [ ] **Step 6: Commit changes (`feat: add manual song entry and snippet editing`)**

---

### Task 2: Album Art Fetcher, Disk Cache & Blurred Canvas Compositing

**Files:**
- Create: `app/src/main/java/com/widgy/lyrics/data/api/ArtworkClient.kt`
- Create: `app/src/main/java/com/widgy/lyrics/data/image/StackBlur.kt`
- Modify: `app/src/main/java/com/widgy/lyrics/data/db/SongEntity.kt`
- Modify: `app/src/main/java/com/widgy/lyrics/data/db/AppDatabase.kt`
- Modify: `app/src/main/java/com/widgy/lyrics/ui/theme/LyricRenderer.kt`
- Modify: `app/src/main/java/com/widgy/lyrics/domain/theme/ThemeSpec.kt`
- Modify: `app/src/main/java/com/widgy/lyrics/ui/theme/ThemeStudioScreen.kt`
- Test: `app/src/test/java/com/widgy/lyrics/data/api/ArtworkClientTest.kt`
- Test: `app/src/test/java/com/widgy/lyrics/ui/theme/LyricRendererTest.kt`

**Interfaces:**
- `ArtworkClient.fetchArtworkUrl(title: String, artist: String): Result<String?>`: searches iTunes public API (`https://itunes.apple.com/search?term=...`) for high-res cover art.
- `StackBlur.blurBitmap(bitmap: Bitmap, radius: Int): Bitmap`: fast, memory-safe in-memory blur.
- `LyricRenderer.render(..., albumArtBitmap: Bitmap? = null, useAlbumArtBackdrop: Boolean = false)`: composites downscaled blurred album art with dark scrim before text rendering.
- `ThemeConfig.useAlbumArt: Boolean`: user preference toggle.

- [ ] **Step 1: Write unit test for `ArtworkClient` parsing and `StackBlur`**
- [ ] **Step 2: Run test to verify failure**
- [ ] **Step 3: Implement `ArtworkClient` using standard HttpURLConnection / OkHttp**
- [ ] **Step 4: Implement `StackBlur` algorithm for deterministic software blur**
- [ ] **Step 5: Integrate album art backdrop rendering and scrim into `LyricRenderer.kt`**
- [ ] **Step 6: Add "Album Art Backdrop" switch and darkness slider into `ThemeStudioScreen.kt`**
- [ ] **Step 7: Run tests to verify they pass**
- [ ] **Step 8: Commit changes (`feat: add album art fetcher and blurred canvas backdrop`)**

---

### Task 3: Social & Sharing: Instagram Story & Wallpaper Card Exporter

**Files:**
- Create: `app/src/main/java/com/widgy/lyrics/ui/share/ShareCardHelper.kt`
- Modify: `app/src/main/AndroidManifest.xml` (add `androidx.core.content.FileProvider`)
- Create: `app/src/main/res/xml/file_paths.xml`
- Modify: `app/src/main/java/com/widgy/lyrics/ui/dashboard/DashboardScreen.kt`
- Modify: `app/src/main/java/com/widgy/lyrics/ui/theme/ThemeStudioScreen.kt`
- Test: `app/src/test/java/com/widgy/lyrics/ui/share/ShareCardHelperTest.kt`

**Interfaces:**
- `ShareCardHelper.generateStoryCardBitmap(context: Context, lines: List<String>, title: String, artist: String, config: ThemeConfig, albumArt: Bitmap?): Bitmap`: generates 1080x1920 (9:16) high-res image.
- `ShareCardHelper.shareImage(context: Context, bitmap: Bitmap)`: writes to app cache, retrieves Content URI via FileProvider, and starts `Intent.ACTION_SEND` chooser.

- [ ] **Step 1: Configure `FileProvider` in `AndroidManifest.xml` and `res/xml/file_paths.xml`**
- [ ] **Step 2: Write tests for bitmap dimensions and file provider cache path generation**
- [ ] **Step 3: Implement `ShareCardHelper` with 9:16 Story format and 1:1 Square format options**
- [ ] **Step 4: Add "Share Lyric Card" icon button to `DashboardScreen.kt` and `ThemeStudioScreen.kt`**
- [ ] **Step 5: Run tests and verify image generation passes**
- [ ] **Step 6: Commit changes (`feat: add Instagram Story and wallpaper card exporter`)**

---

### Task 4: "Pin / Lock" Today's Lyric

**Files:**
- Modify: `app/src/main/java/com/widgy/lyrics/data/preferences/WidgetPreferences.kt`
- Modify: `app/src/main/java/com/widgy/lyrics/domain/engine/DailyRotationEngine.kt`
- Modify: `app/src/main/java/com/widgy/lyrics/ui/dashboard/DashboardViewModel.kt`
- Modify: `app/src/main/java/com/widgy/lyrics/ui/dashboard/DashboardScreen.kt`
- Test: `app/src/test/java/com/widgy/lyrics/domain/engine/DailyRotationEngineTest.kt`

**Interfaces:**
- `WidgetPreferences.isPinned(): Boolean`
- `WidgetPreferences.setPinned(pinned: Boolean, lyric: DailyLyric? = null)`
- `WidgetPreferences.getPinnedLyric(): DailyLyric?`
- `DailyRotationEngine.resolveDailyLyric(date: LocalDate, forceRefresh: Boolean = false)`: returns pinned lyric if pinned, ignoring date hash.

- [ ] **Step 1: Write unit test in `DailyRotationEngineTest` verifying pinned lyric is returned across different dates**
- [ ] **Step 2: Run test to verify failure**
- [ ] **Step 3: Implement pinning methods in `WidgetPreferences` and bypass in `DailyRotationEngine`**
- [ ] **Step 4: Add Pin icon toggle (📌 / 🔓) in `TodayLyricLiveCard` on `DashboardScreen.kt`**
- [ ] **Step 5: Run tests to verify they pass**
- [ ] **Step 6: Commit changes (`feat: add pin and lock lyric rotation`)**

---

### Task 5: Archive / History Log ("Lyric Scrapbook")

**Files:**
- Modify: `app/src/main/java/com/widgy/lyrics/data/db/DailyHistoryDao.kt`
- Create: `app/src/main/java/com/widgy/lyrics/ui/history/ScrapbookScreen.kt`
- Create: `app/src/main/java/com/widgy/lyrics/ui/history/ScrapbookViewModel.kt`
- Modify: `app/src/main/java/com/widgy/lyrics/ui/navigation/WidgyNavGraph.kt`
- Modify: `app/src/main/java/com/widgy/lyrics/ui/dashboard/DashboardScreen.kt`
- Test: `app/src/test/java/com/widgy/lyrics/ui/history/ScrapbookViewModelTest.kt`

**Interfaces:**
- `DailyHistoryDao.getAllHistory(): Flow<List<DailyHistoryEntity>>`
- `ScrapbookViewModel.historyItems: StateFlow<List<DailyHistoryEntity>>`
- `ScrapbookViewModel.pinPastLyric(entity: DailyHistoryEntity)`: pins a past favorite back onto the widget.

- [ ] **Step 1: Write unit test for `ScrapbookViewModel` fetching history from Room**
- [ ] **Step 2: Run test to verify failure**
- [ ] **Step 3: Add `getAllHistory()` to `DailyHistoryDao` and `LyricsRepository`**
- [ ] **Step 4: Implement `ScrapbookViewModel` and `ScrapbookScreen` with timeline cards and "Re-pin" action**
- [ ] **Step 5: Add Scrapbook navigation destination in `WidgyNavGraph.kt` and button on `DashboardScreen.kt`**
- [ ] **Step 6: Run tests to verify they pass**
- [ ] **Step 7: Commit changes (`feat: add lyric scrapbook history screen`)**

---

### Task 6: Flexible Rotation Frequency (1h, 3h, 6h, 12h, 24h)

**Files:**
- Modify: `app/src/main/java/com/widgy/lyrics/data/preferences/WidgetPreferences.kt`
- Modify: `app/src/main/java/com/widgy/lyrics/widget/WidgetUpdateManager.kt`
- Modify: `app/src/main/java/com/widgy/lyrics/ui/dashboard/DashboardScreen.kt`
- Test: `app/src/test/java/com/widgy/lyrics/widget/WidgetUpdateManagerTest.kt`

**Interfaces:**
- `WidgetPreferences.getRotationIntervalHours(): Int`
- `WidgetPreferences.setRotationIntervalHours(hours: Int)`
- `WidgetUpdateManager.scheduleRotation(context: Context, intervalHours: Int)`: cancels or updates `PeriodicWorkRequest` to run every N hours.

- [ ] **Step 1: Write test for `WidgetPreferences` interval storage and interval bounds validation**
- [ ] **Step 2: Run test to verify failure**
- [ ] **Step 3: Implement dynamic `scheduleRotation` in `WidgetUpdateManager.kt`**
- [ ] **Step 4: Add interval selector bottom sheet or menu in `DashboardScreen.kt`**
- [ ] **Step 5: Run tests to verify they pass**
- [ ] **Step 6: Commit changes (`feat: add flexible rotation interval settings`)**

---

### Task 7: Widget Refresh Button Polish & Visibility Toggle

**Files:**
- Modify: `app/src/main/java/com/widgy/lyrics/domain/theme/ThemeSpec.kt`
- Modify: `app/src/main/java/com/widgy/lyrics/widget/DailyLyricWidget.kt`
- Modify: `app/src/main/java/com/widgy/lyrics/ui/theme/ThemeStudioScreen.kt`
- Test: `app/src/test/java/com/widgy/lyrics/widget/DailyLyricWidgetTest.kt`

**Interfaces:**
- `ThemeConfig.showRefreshButton: Boolean = true`
- `DailyLyricWidget`: hides the ↻ button when `showRefreshButton == false` for users wanting a 100% minimalist, buttonless home screen widget.

- [ ] **Step 1: Add `showRefreshButton` boolean to `ThemeConfig`**
- [ ] **Step 2: Update `DailyLyricWidget.kt` to conditionally display the button container**
- [ ] **Step 3: Add "Show Shuffle Button on Widget" switch in `ThemeStudioScreen.kt`**
- [ ] **Step 4: Run all unit tests across the app (`./gradlew testDebugUnitTest`)**
- [ ] **Step 5: Run full app build check (`./gradlew assembleDebug`)**
- [ ] **Step 6: Commit changes (`feat: add widget refresh button toggle in Theme Studio`)**
