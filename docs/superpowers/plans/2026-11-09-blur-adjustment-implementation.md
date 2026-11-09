# Album Art Blur Intensity Adjustment & Presets Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Enable users to customize the album art backdrop blur intensity in Theme Studio using an interactive horizontal slider and quick preset chips (Soft, Medium, Deep), persisting the choice across widgets, dashboard, and share sheets.

**Architecture:** Extend `ThemeConfig` with `blurRadius: Int = 22` and persist via `WidgetPreferences`. Add a debounced real-time horizontal `Slider` and `PillChip` preset row in `ThemeStudioScreen`. Route `config.blurRadius` into `StackBlur.blur(bitmap, radius)` across all consumer components.

**Tech Stack:** Kotlin, Jetpack Compose, Jetpack Glance, Room, Android Bitmap / Canvas, JUnit 4, Robolectric.

**Spec:** `docs/superpowers/specs/2026-11-09-blur-adjustment-design.md`

## Global Constraints
- Do NOT push to `main` branch.
- Do NOT create git tags or GitHub releases.
- Do NOT attempt USB installation.
- Adhere to the 4-tier radii system: `8dp` for chips, `12dp` for controls, `16dp` for containers, `24dp` for dock.
- Maintain WCAG AAA contrast and touch targets $\ge 48$dp.

## Review Focus
1. Rapid slider scrubbing triggering excessive `StackBlur` operations: must debounce or cancel stale blur jobs via coroutines to prevent UI stutter.
2. Value boundary extremes: `blurRadius` must be safely clamped within `6..38`.
3. Widget preferences migration: older preference stores missing `theme_blur_radius` must safely fallback to `DEFAULT_BLUR_RADIUS` (22).
4. Slider touch ergonomics: the slider and preset chips must respect thumb clearance and not collide with dock navigation.

---

### Task 1: Model & Preferences Persistence for `blurRadius` (TDD)

**Files:**
- Modify: `app/src/main/java/com/widgy/lyrics/domain/theme/ThemeSpec.kt`
- Modify: `app/src/main/java/com/widgy/lyrics/data/preferences/WidgetPreferences.kt`
- Test: `app/src/test/java/com/widgy/lyrics/data/preferences/WidgetPreferencesTest.kt`

- [ ] **Step 1: Write failing unit test in `WidgetPreferencesTest`**
  Add tests asserting that `ThemeConfig` includes `blurRadius` with default `22`, and `WidgetPreferences.saveThemeConfig()` / `getThemeConfig()` correctly saves and restores custom blur radius values (e.g. 10, 36).
- [ ] **Step 2: Run test to confirm it fails**
  Run `./gradlew testDebugUnitTest --tests com.widgy.lyrics.data.preferences.WidgetPreferencesTest`.
- [ ] **Step 3: Implement `blurRadius` in `ThemeConfig` and `WidgetPreferences`**
  - Add `blurRadius: Int = DEFAULT_BLUR_RADIUS` in `ThemeConfig` with companion constants.
  - Add `"theme_blur_radius"` preference key in `WidgetPreferences`.
- [ ] **Step 4: Run tests to confirm they pass**
  Run `./gradlew testDebugUnitTest --tests com.widgy.lyrics.data.preferences.WidgetPreferencesTest`.
- [ ] **Step 5: Commit Task 1**
  `git commit -m "feat(preferences): add blurRadius to ThemeConfig and WidgetPreferences"`

---

### Task 2: Route `blurRadius` to Widget, Dashboard, and Background Worker

**Files:**
- Modify: `app/src/main/java/com/widgy/lyrics/widget/DailyLyricWidget.kt`
- Modify: `app/src/main/java/com/widgy/lyrics/ui/dashboard/DashboardScreen.kt`
- Modify: `app/src/main/java/com/widgy/lyrics/widget/DailyWidgetWorker.kt`

- [ ] **Step 1: Update `DailyLyricWidget.kt`**
  Replace hardcoded blur radius `20` with `themeConfig.blurRadius`.
- [ ] **Step 2: Update `DashboardScreen.kt`**
  Replace hardcoded blur radius `22` with `themeConfig.blurRadius`.
- [ ] **Step 3: Update `DailyWidgetWorker.kt`**
  Ensure background artwork caching and blurring honors `prefs.getThemeConfig().blurRadius`.
- [ ] **Step 4: Verify test suite**
  Run `./gradlew testDebugUnitTest`.
- [ ] **Step 5: Commit Task 2**
  `git commit -m "feat(blur): route customizable blurRadius to widget and dashboard"`

---

### Task 3: Theme Studio UI: Range Slider & Preset Chips (`Soft`, `Medium`, `Deep`)

**Files:**
- Modify: `app/src/main/java/com/widgy/lyrics/ui/theme/ThemeStudioScreen.kt`

- [ ] **Step 1: Implement Blur Control Row in `ThemeStudioScreen`**
  When `config.useAlbumArtBackdrop == true`, render `ConfigRow(label = "Blur", ...)`:
  - Material 3 `Slider` with range `6f..38f`, updating `config.blurRadius`.
  - Live indicator text showing `${config.blurRadius}px`.
  - Preset chips below slider: `Soft` (10), `Medium` (22), `Deep` (36).
- [ ] **Step 2: Implement debounced stage preview blur**
  In `ThemeStudioScreen`, update `LaunchedEffect(dailyLyric?.songId, config.useAlbumArtBackdrop, config.blurRadius)` to recompute blurred bitmap cleanly.
- [ ] **Step 3: Verify tests and debug build**
  Run `./gradlew testDebugUnitTest && ./gradlew assembleDebug`.
- [ ] **Step 4: Commit Task 3**
  `git commit -m "feat(ui): add horizontal blur slider and presets in Theme Studio"`

---

### Task 4: Senior Android Code Review & Council UI/UX Audit

**Files:**
- Review git diff on branch `feat/album-art-blur-adjustment` vs `main`.

- [ ] **Step 1: Launch Senior Android Code Reviewer subagent**
  Audit memory allocations, bitmap recycling, slider dragging performance, and IPC safety.
- [ ] **Step 2: Launch Council of Agents UI/UX Review Board subagent**
  Audit visual restraint (Dieter), touch targets & ergonomics (Material), typography and contrast (Kerning).
- [ ] **Step 3: Address any findings and re-verify**
  Ensure `./gradlew testDebugUnitTest && ./gradlew assembleDebug` passes with zero errors.
- [ ] **Step 4: Commit and present scorecard to user**
