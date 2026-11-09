# Design Specification: Album Art Blur Intensity Adjustment & Presets

**Date:** 2026-11-09  
**Branch:** `feat/album-art-blur-adjustment`  
**Status:** In Review  

---

## 1. Context & Motivation

Widgy enables users to set a blurred album art backdrop for lyrics on the home screen widget and story card share sheets. Currently, the blur radius is hardcoded to a fixed value (radius 20–22 in `StackBlur.blur()`).

Users want the ability to adjust blur intensity:
- Some users prefer a **soft blur** (radius ~10) where album art cover contours remain partially legible behind lyrics.
- Some users prefer a **deep blur** (radius ~36) that creates an ultra-smooth, ambient color wash for maximum typographic legibility.
- Others want fine-grained slider control between these extremes.

---

## 2. Approaches Considered

1. **Approach 1: Dedicated "Blur" Dock Row with Horizontal Slider & Preset Chips (Selected)**
   - Renders a horizontal slider (range 6 to 38) with live radius indicator.
   - Places quick preset chips directly below: **Soft (10)**, **Medium (22)**, **Deep (36)**.
   - Snaps to presets on tap; allows continuous real-time sliding.
   - *Rationale:* Conforms to Widgy's 4-tier design system (`8dp` chips, `68dp` label lockup) and the dock's existing spatial rhythm.
2. **Approach 2: Expandable Backdrop Card Module**
   - Encapsulates the art toggle, slider, and presets into a nested card.
   - *Drawback:* Adds unnecessary container nesting and vertical bulk to the dock.

---

## 3. Data Model & Persistence

### 3.1 `ThemeConfig` Updates
```kotlin
data class ThemeConfig(
    val themeId: String = ThemeSpecs.AMBER.id,
    val typeface: ThemeTypeface = ThemeTypeface.SANS,
    val sizeMode: ThemeSizeMode = ThemeSizeMode.AUTO,
    val creditMode: ThemeCreditMode = ThemeCreditMode.NONE,
    val useAlbumArtBackdrop: Boolean = false,
    val blurRadius: Int = DEFAULT_BLUR_RADIUS,
    val showRefreshButton: Boolean = true
) {
    companion object {
        const val DEFAULT_BLUR_RADIUS = 22
        const val MIN_BLUR_RADIUS = 6
        const val MAX_BLUR_RADIUS = 38
        const val BLUR_SOFT = 10
        const val BLUR_MEDIUM = 22
        const val BLUR_DEEP = 36
    }
    val themeSpec: ThemeSpec get() = ThemeSpecs.findById(themeId)
}
```

### 3.2 `WidgetPreferences` Persistence
- Key: `"theme_blur_radius"` (`Int`, default: `ThemeConfig.DEFAULT_BLUR_RADIUS`).
- Persisted in `saveThemeConfig()`, restored in `getThemeConfig()`.
- Backward compatible: legacy stored configurations without the key default seamlessly to 22.

---

## 4. UI / UX Design in `ThemeStudioScreen`

When `config.useAlbumArtBackdrop == true`:
A new animated row appears right below the "Art" row:
- **Label:** `Blur` (aligned with standard 68dp label column).
- **Control Layout:**
  - **Top:** Horizontal `Slider` (value range `6f..38f`), with live indicator badge `${config.blurRadius}px`.
  - **Bottom:** Row of 3 preset chips (`PillChip`, 8dp corners, 40dp touch target):
    - `Soft` (10)
    - `Medium` (22)
    - `Deep` (36)
- **Live Stage Preview:**
  - `LaunchedEffect(dailyLyric?.songId, config.useAlbumArtBackdrop, config.blurRadius)` re-computes blurred bitmap via `StackBlur.blur(art, config.blurRadius)`.
  - Debounced with coroutines so smooth dragging does not cause frame drops.

---

## 5. Consumers of `blurRadius`

1. **`DailyLyricWidget.kt`**: Passes `themeConfig.blurRadius` to `StackBlur.blur()`.
2. **`DashboardScreen.kt`**: Passes `themeConfig.blurRadius` to `StackBlur.blur()`.
3. **`ThemeStudioScreen.kt`**: Passes `config.blurRadius` to `StackBlur.blur()`.
4. **`DailyWidgetWorker.kt`**: Uses `prefs.getThemeConfig().blurRadius`.
5. **`ShareCardHelper.kt`**: Respects the blurred bitmap generated with the configured radius.

---

## 6. Review & Quality Gates

- **Unit Testing:** `WidgetPreferencesTest` (persistence), `StackBlurTest` (radius boundaries 6, 22, 38).
- **Senior Android Code Reviewer:** Inspects bitmap lifecycle, performance, and memory safety during sliding.
- **Council of Agents UI/UX Review Board:** Evaluates visual restraint (Dieter), ergonomics and touch targets (Material), and typography/contrast legibility (Kerning).
