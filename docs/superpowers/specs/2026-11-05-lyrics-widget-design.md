# Design Document: Widgy - Daily Lyric Widget for Android

## 1. Overview & Intent

Widgy is a minimalist, battery-efficient Android application paired with a clean home-screen widget. It enables users to curate a personal collection of meaningful song lyrics and delivers a fresh lyric snippet every day onto their home screen.

Music lovers often have specific lines from songs that deeply resonate with them (e.g. Kendrick Lamar's *"Sing About Me, I'm Dying of Thirst"*). Widgy gives these personal moments a permanent, quiet, and aesthetic presence without the bloat, ads, or battery drain common in typical quote and lyric widgets.

The core guiding principle is **absolute resource efficiency**: zero persistent background services, zero continuous RAM consumption, and zero background network polling.

---

## 2. Goals, Non-Goals, and Anti-Goals

### Goals
* **Frictionless Song & Lyrics Discovery**: Instant search for tracks and retrieve full, accurate lyrics via the open-source LRCLIB API (`https://lrclib.net`) without requiring user sign-ups or API keys.
* **Granular Snippet Curation**: Allow users to select specific lines/verses from a song, or designate whole songs for random daily line extraction.
* **Home Screen Widget (Jetpack Glance)**: A clean, minimal widget displaying the daily lyric snippet, song title, and artist name with full support for Android dynamic theming (Material You) and light/dark modes.
* **Zero-Process Background Architecture**: Ensure no background daemon or lingering process runs in memory. Day changes are handled deterministically on render and/or via a brief (<100ms) midnight `WorkManager` execution that terminates immediately.
* **Offline-First Resilience**: All saved songs, full lyrics, and snippets are stored locally in SQLite (via Room). Network access only happens when actively searching or adding songs.
* **Minimalist UI**: Clean, intentional typography, generous whitespace, and zero unnecessary visual clutter or skeuomorphic effects.

### Non-Goals
* **Audio Playback**: Widgy is a lyric curation and home-screen display tool, not a music streaming player.
* **Cloud Sync / User Accounts (v1)**: All data resides on-device in the local SQLite database.
* **Social Sharing Feeds**: No social media feed or community lyrics crowdsourcing inside the app.

### Anti-Goals
* **No Persistent Background Services**: No `Service` running in the background consuming RAM or battery.
* **No Recurring Background Network Calls**: The widget never makes network requests; it reads exclusively from local storage.
* **No Bloated Widget Aesthetics**: No shiny gradients, skeuomorphic shadows, or distracting animations on the home screen.

---

## 3. Approaches Considered & Selected

During architectural planning, three approaches were evaluated:

1. **Pure Native Android (Kotlin + Jetpack Compose + Jetpack Glance)** — *Selected*
   - *Rationale*: Directly meets the Android-first priority and zero-overhead requirement. Jetpack Glance compiles directly to Android `RemoteViews`, avoiding cross-platform bridge overhead. Room and WorkManager provide battle-tested, standard Android platform primitives.
   - *Trade-off*: iOS support will be built later with Swift/WidgetKit rather than sharing UI code today.

2. **React Native / Expo with Native Widget Extension**
   - *Trade-off*: Home screen widgets cannot run the JavaScript engine, meaning native Kotlin/Swift widget extensions are required anyway. Slower cold start and larger APK footprint.

3. **Kotlin Multiplatform (KMP) + Compose Multiplatform**
   - *Trade-off*: Additional multiplatform build complexity upfront, whereas native Android provides the fastest, cleanest development cycle for Android-first delivery.

---

## 4. System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                       Jetpack Compose UI                    │
│  [Home / Curation View]   [Search Screen]   [Lyrics View]   │
└──────────────────────────────┬──────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────┐
│                    LyricsRepository                         │
│  - Search & fetch from API                                  │
│  - Manage local snippets & rotation pool                    │
└──────────────┬───────────────────────────────┬──────────────┘
               │                               │
┌──────────────▼──────────────┐ ┌──────────────▼──────────────┐
│    LRCLIB Client (Remote)   │ │      Room Database (Local)  │
│  - https://lrclib.net/api/  │ │  - SongEntity               │
│  - Search & exact lyrics    │ │  - SnippetEntity            │
│  - No auth / public REST    │ │  - DailyHistoryEntity       │
└─────────────────────────────┘ └──────────────┬──────────────┘
                                               │
┌──────────────────────────────────────────────▼──────────────┐
│                Zero-Process Widget Subsystem                │
│  ┌─────────────────────────┐   ┌──────────────────────────┐ │
│  │  Jetpack Glance Widget  │   │   Midnight WorkManager   │ │
│  │  - Observes Room state  │   │   - 100ms execution      │ │
│  │  - Reads DailyHistory   │   │   - Updates Glance state │ │
│  │  - Renders RemoteViews  │   │   - Shuts down           │ │
│  └─────────────────────────┘   └──────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

---

## 5. Architectural Calls & Decisions (Builder Review Settle)

### 5.1 Balanced Rotation Pool Design
* **Decision (Builder Call)**: Songs marked for "random rotation" are **not** exploded into dozens of individual rows. Instead, each active curated snippet and each song designated as "random pool" counts as **one balanced candidate** in the pool.
* **Mechanism**: When a random-pool song candidate is selected for the day, a clean lyric line (filtering out empty lines and metadata brackets like `[Verse 1]` or `[Chorus]`) is dynamically sampled from its cached `plainLyrics`. This prevents a single 60-line song from overwhelming handpicked snippets from other songs.

### 5.2 Authoritative Source of Truth & Refresh Action Mechanics
* **Decision (Builder Call)**: `DailyHistoryEntity` (keyed by `date: YYYY-MM-DD`) is the single source of truth for the daily lyric.
* **Mechanism**:
  * Both the Glance widget and the Compose Dashboard observe `DailyHistoryEntity`.
  * If the device boots or the widget renders and today's row does not exist yet (e.g., Doze mode delayed the midnight worker), the widget lazily resolves the day's lyric deterministically, writes the row into `DailyHistoryEntity`, and displays it.
  * Tapping the **Refresh button** (on the widget or in the app) immediately draws another random candidate from the active pool, overwrites today's record in `DailyHistoryEntity`, and triggers `DailyLyricWidget.update()`. This leaves future scheduled rotations intact without corrupting date hashes.

### 5.3 Navigation Hierarchy & Widget Tap Target
* **Decision (Builder Call)**:
  * **Screen 1 (Dashboard)**: Uses clean expandable cards for each saved song, displaying its active snippet count and enabling/disabling individual lines or the entire song.
  * **Widget Tap Target**: Tapping the home-screen widget deep-links directly into **Screen 3 (Lyrics Viewer)** with the song loaded, the active lyric highlighted, and a back arrow returning to Screen 1.
  * **Screen 3 (Lyrics Viewer)**: Reused for both new song imports and re-curating/editing snippets of already saved songs.

---

## 6. Component Breakdown

### 6.1 Remote Data Layer: LRCLIB API Client
* **Base URL**: `https://lrclib.net/api`
* **Endpoints**:
  * Search: `GET /api/search?q={query}` (returns track matches with title, artist, album, duration, and full lyrics).
  * Direct Fetch: `GET /api/get?track_name={track}&artist_name={artist}` (fallback for exact lyric lookup).
* **Headers**: Custom `User-Agent: WidgyAndroid/1.0 (https://github.com/widgy)` per LRCLIB fair-use guidelines.
* **Parsing**: Strips LRC timestamp brackets (`[00:12.34]`) when formatting `syncedLyrics`, or consumes `plainLyrics` directly.

### 6.2 Local Persistence Layer: Room Database
* **`SongEntity`**:
  * `id`: Long (Primary Key, autoincrement)
  * `lrclibId`: Long?
  * `title`: String
  * `artist`: String
  * `album`: String?
  * `plainLyrics`: String (Cached full lyrics for offline browsing and dynamic line picking)
  * `isRandomPool`: Boolean (True if the song participates in random daily line picking)
  * `createdAt`: Long
* **`SnippetEntity`**:
  * `id`: Long (Primary Key, autoincrement)
  * `songId`: Long (Foreign Key -> SongEntity, onDelete = CASCADE)
  * `content`: String (The exact lyric line or multi-line verse selected)
  * `isActive`: Boolean (Allows pausing snippets without deleting them)
  * `createdAt`: Long
* **`DailyHistoryEntity`**:
  * `date`: String (Format: `YYYY-MM-DD`, Primary Key)
  * `songId`: Long
  * `snippetId`: Long? (Null if dynamically extracted from a random-pool song)
  * `lyricText`: String (Cached snapshot of the displayed line)
  * `songTitle`: String
  * `artistName`: String
  * `updatedAt`: Long

### 6.3 Daily Rotation Engine (Zero-Process Design)
* **Midnight Scheduled Worker (`DailyWidgetWorker`)**:
  * `PeriodicWorkRequest` configured with `ExistingPeriodicWorkPolicy.KEEP` to run once every 24 hours around midnight.
  * Runs for < 100 milliseconds, selects the day's lyric, commits it to `DailyHistoryEntity`, calls `DailyLyricWidget().update(context, glanceId)`, and exits cleanly.
* **Deterministic Fallback**:
  * If the device was powered off or in deep Doze at midnight, the widget computes the lyric on first render using `(epochDay + seed) % candidatePool.size` and commits it to `DailyHistoryEntity`.

### 6.4 Home Screen Widget (Jetpack Glance)
* **Sizes**: Responsive layout adapting to 2x2, 3x2, and 4x2 home screen grid cells.
* **Design**:
  * Minimalist surface using Material You dynamic colors (`GlanceTheme.colors.surfaceContainer` / adaptive light-dark).
  * Body: Lyric quote rendered with clean typography.
  * Footer: Song title and artist name in subtle secondary text.
  * Actions: Deep link to song on card tap, discreet shuffle button in top corner.

### 6.5 App UI & UX (Jetpack Compose)
* **Screen 1: Dashboard / Curation**:
  * Today's active lyric preview card with one-tap shuffle.
  * List of saved songs as expandable cards with snippet counts and toggle switches.
  * Floating action button to "+ Search & Add Songs".
* **Screen 2: Song Search**:
  * Live search input with 300ms debounce.
  * Clean track result cards showing title, artist, album.
* **Screen 3: Lyrics Viewer & Line Selector**:
  * Full song lyrics displayed with comfortable line spacing.
  * Tap lines to toggle selection.
  * Selection actions: "Save Selected Lines" or "Add Song to Random Pool".
* **Screen 4: Preferences & Styling**:
  * Widget text alignment, font styling, and background card theme.

---

## 7. Project Structure

```
widgy/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/widgy/lyrics/
│   │   │   │   ├── data/
│   │   │   │   │   ├── api/          # LRCLIB API client & DTOs
│   │   │   │   │   ├── db/           # Room Database, DAOs, Entities
│   │   │   │   │   └── repository/   # LyricsRepository
│   │   │   │   ├── domain/
│   │   │   │   │   └── model/        # Domain models (Song, Snippet, DailyLyric)
│   │   │   │   ├── widget/
│   │   │   │   │   ├── DailyLyricWidget.kt       # GlanceAppWidget
│   │   │   │   │   ├── DailyLyricWidgetReceiver.kt
│   │   │   │   │   └── DailyWidgetWorker.kt      # WorkManager midnight trigger
│   │   │   │   ├── ui/
│   │   │   │   │   ├── theme/        # Material 3 Theme & Typography
│   │   │   │   │   ├── dashboard/    # Dashboard & curated list
│   │   │   │   │   ├── search/       # Song search screen
│   │   │   │   │   ├── lyrics/       # Lyrics viewer & selector
│   │   │   │   │   └── navigation/   # Jetpack Navigation graph
│   │   │   │   └── MainActivity.kt
│   │   │   └── res/
│   │   │       ├── xml/              # Widget provider info XML
│   │   │       └── values/           # Strings, colors
│   │   └── test/                     # Unit tests
│   └── build.gradle.kts
├── gradle/
└── build.gradle.kts
```

---

## 8. Left to Builder (Implementation Decisions)
* Gradle version catalog (`libs.versions.toml`) coordinates for Android Gradle Plugin (8.x), Kotlin (2.x), Compose BOM, Glance (1.1.0), and Room (2.6.x).
* HTTP network client: Retrofit + OkHttp with `kotlinx.serialization` for lightweight, robust JSON parsing.
