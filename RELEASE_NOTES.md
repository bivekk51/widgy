# Widgy v1.6.1 – Charles Unslop Design System Refinements & AAA Contrast

Widgy v1.6.1 applies the comprehensive `charles-unslop` design craft principles across the core Android interface, removing visual line noise, subtracting unnecessary containers, enforcing a strict 4px/8px rhythm, and elevating contrast ratios to WCAG AAA.

---

### ✨ What's New in v1.6.1

#### 🎨 Charles Unslop Craft & Subtraction
* **Dividers Subtracted**: Removed redundant visual dividers in Theme Studio—proximity, whitespace, and natural hierarchy now group categories, dock controls, and actions without unnecessary line clutter.
* **Surface Nesting Removed**: Subtracted redundant Card wrappers around the Rotation Frequency selector; controls now sit directly on the surface canvas grouped by clean proximity.
* **Editorial Typographic Restraint**: Removed arbitrary letter spacing on top app bars and enforced natural sentence case across headers, chips, and past lyric scrapbooks.

#### 📐 Strict 4px/8px Grid Spacing & Unified Tokens
* Snapped all off-grid paddings (such as `10.dp` and `6.dp`) to strict `8.dp` tokens across `ThemeStudioScreen`, `DashboardScreen`, and `LyricsViewerScreen`.
* Unified hardcoded radii throughout the app to semantic `WidgyShapes` design tokens (`small` 8dp, `medium` 12dp, `large` 16dp).

#### 👁️ WCAG AAA Contrast Accessibility
* Replaced low-contrast border outlines (`outline`, 1.38:1) on secondary labels, delete icons, and helper texts with semantic `onSurfaceVariant` (~7.5:1 AAA contrast), ensuring crystal-clear legibility under any lighting conditions.
* Replaced hardcoded hex colors with theme-aware semantic color scheme tokens.
