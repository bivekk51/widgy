# Widgy UI/UX Hill-Climbing Plan & Council Framework

## Overview
Inspired by Theo's (t3.gg) multi-agent hill climbing methodology and Damir Sokolovsky's `unslop` design craft principles, this initiative audits and iteratively refines every screen and widget in Widgy until achieving a world-class score ($\ge 9.5 / 10.0$).

---

## The Council of Evaluators
The Council consists of three distinct AI agent perspectives:
1. **Dieter (Craft & Unslopping Critic):** Enforces quiet chrome, single bright moments, zero AI slop (no em dashes, no emoji labels, no purple/cyan glows, no floating pills), unified neutral ramps, and nested radii.
2. **Material (Android UX & HIG Specialist):** Enforces 48dp touch targets, strict 4px spacing grid, thumb zone ergonomics, clear empty states, consistent action hierarchy, and seamless navigation.
3. **Kerning (Typography & Widget Engineer):** Enforces optical readability, static layout line heights, Devanagari/Latin script harmony, dynamic widget auto-scaling across 2x2, 4x2, and 4x3 cells, and backdrop contrast safety.

---

## Scoring Rubric (1.0 to 10.0 Scale)
Each evaluator scores across 5 dimensions:
- **Dimension 1: Visual Restraint & Slop Purge (Weight: 20%)**
- **Dimension 2: Typography & Script Harmony (Weight: 20%)**
- **Dimension 3: Widget Glanceability & Scalability (Weight: 20%)**
- **Dimension 4: Layout Rhythm, Spacing (4px) & Radii (Weight: 20%)**
- **Dimension 5: Ergonomics, Touch Targets & Micro-interactions (Weight: 20%)**

**Target Convergence Goal:** Overall Composite Score $\ge 9.5 / 10.0$.

---

## Hill-Climbing Iteration Protocol
- **Iteration 0 (Baseline Audit):** Full audit of Dashboard, Theme Studio, Widget, Lyrics Viewer, Scrapbook, and Search. Score and list all defects.
- **Iteration 1 (Pass 1 - Slop Elimination & Foundation):**
  - Purge em dashes (`—`), unicode emoji symbols in buttons, and irregular radii.
  - Standardize on a strict 4px grid and uniform neutral ramp in `WidgyTheme.kt`.
  - Fix button styling and header hierarchy across all screens.
- **Iteration 2 (Pass 2 - Screen-by-Screen Deep Redesign):**
  - Re-architect Theme Studio dock controls (replace awkward pill chips, ensure contrast on light/dark themes).
  - Elevate Dashboard live card, curation cards, and rotation frequency selector.
  - Polish Scrapbook timeline cards and Search screen track items.
  - Refine Glance widget rendering bounds and contrast scrim.
- **Iteration 3 (Pass 3 - Micro-interactions, Touch Targets & Polish):**
  - Verify every interactive element has $\ge 48$dp touch target.
  - Ensure optical centering and smooth transition states.
  - Council re-evaluates until score $\ge 9.5 / 10.0$.
- **Verification:**
  - `./gradlew testDebugUnitTest` and `./gradlew assembleDebug` must pass 100% cleanly.
