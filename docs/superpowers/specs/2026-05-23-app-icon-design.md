# App Icon Design — Runner

**Date:** 2026-05-23  
**Status:** Approved

## Concept: Pulse

An ECG heartbeat line that terminates in a GPS location dot. No text. The shape communicates both fitness tracking and GPS routing in a single mark.

## Visual Specification

### Foreground (adaptive icon foreground layer)

- **ECG polyline** spanning the full width of the canvas (108×108dp safe zone, drawn from ~8dp to ~88dp on the X axis), vertically centered with a dramatic peak and valley.
- **Line color:** `#ff6a00` (orange), stroke width 4.5dp, round caps and joins.
- **Glow pass:** same polyline at stroke-width 2dp, color `#ffaa60`, opacity 0.35 — renders above the main line for a soft bloom effect.
- **GPS dot** at the right terminus (88, 54): filled circle r=6dp in `#ff6a00`.
- **GPS ring** around the dot: stroke circle r=10dp, `#ff6a00`, stroke-width 2dp, opacity 0.35.

### Background (adaptive icon background layer)

- **Dark gradient:** `#1a0808` → `#2d1206` (top-left to bottom-right, linear).
- Solid fill only — no grid lines, no texture.

### Color Palette

| Role            | Hex       |
|-----------------|-----------|
| Background top  | `#1a0808` |
| Background btm  | `#2d1206` |
| ECG / dot       | `#ff6a00` |
| Glow pass       | `#ffaa60` |

## Adaptive Icon Structure

Android adaptive icons require two layers:

- `res/drawable/ic_launcher_background.xml` — gradient background (108×108dp)
- `res/drawable/ic_launcher_foreground.xml` — ECG + GPS dot (108×108dp, art centered in safe zone 72×72dp)

Legacy bitmap fallbacks (`.webp`) are generated at:

| Density   | Size    |
|-----------|---------|
| mdpi      | 48×48   |
| hdpi      | 72×72   |
| xhdpi     | 96×96   |
| xxhdpi    | 144×144 |
| xxxhdpi   | 192×192 |

The `mipmap-anydpi-v26/` XMLs reference both drawable layers and remain unchanged in structure.

## Out of Scope

- Notification / status bar icon (separate monochrome asset, not part of this task)
- Splash screen branding
- Play Store feature graphic
