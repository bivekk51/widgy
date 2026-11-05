# Widget Theme Studio & Readymade Templates Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

## Goal
Replace the inline dashboard settings with a dedicated **Theme Studio** screen featuring an interactive live widget preview, a visual gallery of ready-made widget templates (Genius, Spotify Glow, Classic Serif, Retro Terminal, Clean OLED), and fine-tuning customization controls for typography, font size, and footer attribution.

## Proposed Changes

### 1. Data & Configuration Layer
- **File**: `app/src/main/java/com/widgy/lyrics/data/preferences/WidgetPreferences.kt`
- Add `WidgetFontFamily` enum (`SANS`, `SERIF`, `MONO`).
- Expand `WidgetTemplate` with 5 ready-made themes:
  - `GENIUS` ("Genius Style", `#0A0A0A`, bold uppercase, white quotes)
  - `SPOTIFY` ("Spotify Glow", `#121212`, accent `#1ED760`, modern bold sans)
  - `CLASSIC` ("Classic Serif", `#18181A`, elegant serif italic)
  - `TERMINAL` ("Retro Terminal", `#0D1117`, monospace phosphor amber `#FFB454`)
  - `MINIMAL` ("Clean OLED", `#000000`, crisp minimalist medium sans)
- Update `WidgetConfig` to include `fontFamily: WidgetFontFamily`.

### 2. Widget Rendering Layer
- **File**: `app/src/main/java/com/widgy/lyrics/widget/DailyLyricWidget.kt`
- Update Glance widget composition to render all 5 templates with custom background colors, font colors, font families, font sizes, and attribution formatting.

### 3. Dedicated Theme Studio Screen
- **File**: `app/src/main/java/com/widgy/lyrics/ui/theme/ThemeStudioScreen.kt`
- Top App Bar with back navigation: "Theme Studio".
- Live interactive Widget Preview card at the top.
- Visual gallery of ready-made template cards.
- Fine-tuning controls for Font Family, Font Size, and Footer Attribution.
- "Apply to Home Screen" action button that saves config and calls `WidgetUpdateManager.updateAllWidgets(context)`.

### 4. Navigation & Dashboard Refactoring
- **File**: `app/src/main/java/com/widgy/lyrics/ui/navigation/WidgyNavGraph.kt`
  - Add `Route.ThemeStudio = "theme_studio"`.
- **File**: `app/src/main/java/com/widgy/lyrics/ui/dashboard/DashboardScreen.kt`
  - Remove inline customizer chips.
  - Add "🎨 Change Theme & Customize" button.

### 5. Verification & Testing
- Unit tests in `app/src/test/java/com/widgy/lyrics/`.
- Gradle build: `./gradlew testDebugUnitTest` and `./gradlew assembleDebug`.
- Install APK to device `889bbe77` and verify on screen.

## Tasks

- [ ] Task 1: Update `WidgetPreferences.kt` with expanded templates and font family
- [ ] Task 2: Update `DailyLyricWidget.kt` to render all 5 templates and fonts
- [ ] Task 3: Create `ThemeStudioScreen.kt` with visual template gallery and live preview
- [ ] Task 4: Connect navigation and update `DashboardScreen.kt` with "Change Theme" button
- [ ] Task 5: Run tests, assemble APK, install to device `889bbe77`, and verify
