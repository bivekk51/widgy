# Widgy

A minimal Android app for curating song lyrics and displaying them as a home screen widget.

## What it does

- **Search lyrics** from [LRCLIB](https://lrclib.net) — no API key needed
- **Curate 1–2 lines** from any track as your personal snippet
- **Daily rotation** — a fresh lyric shows on your home screen every day
- **Theme Studio** — 5 visual themes with live preview matching exactly what the widget renders

## Themes

| Theme | Style |
|---|---|
| **Amber** | Warm amber on deep navy (default) |
| **Terminal** | Green monospace with `>` prompt marker |
| **Genius** | Pitch-black bold all-caps |
| **Spotify glow** | Dark with green accent bar |
| **Classic** | Warm paper, italic serif |

## Stack

- Kotlin + Jetpack Compose
- Jetpack Glance (widget)
- Room (local DB)
- WorkManager (midnight rotation)
- LRCLIB API (lyrics fetch)
- Noto Sans/Serif Devanagari (bundled fonts)

## Build

```bash
./gradlew assembleDebug
./gradlew installDebug   # requires connected device
```

## Releases & CI

Automated builds via GitHub Actions:
- **Release an APK**: push a version tag (e.g. `git tag v1.0.0 && git push origin v1.0.0`) to build and attach the APK to the GitHub **Releases** tab.
- **Manual Build**: Go to **Actions** → **Build & Release APK** → **Run workflow** on GitHub to get a downloadable APK artifact without creating a release.

## Notes

- No background services — zero battery drain
- Widget and in-app preview use the same `LyricRenderer` (Canvas + StaticLayout) so output is pixel-identical
- Supports Nepali / Hindi lyrics via script-run font splitting
