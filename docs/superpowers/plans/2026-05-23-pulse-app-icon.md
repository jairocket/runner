# Pulse App Icon Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the default Android robot icon with the Pulse design — an orange ECG heartbeat line ending in a GPS dot on a dark gradient background.

**Architecture:** Android adaptive icons use two drawable layers (`ic_launcher_background.xml` and `ic_launcher_foreground.xml`) referenced from `mipmap-anydpi-v26/` XML files (API 26+). Legacy WebP bitmaps in `mipmap-*/` folders serve API 24–25 devices. Only the two drawable XMLs and the legacy bitmaps need to change; the `mipmap-anydpi-v26/` XMLs already reference the right drawables and require no edits.

**Tech Stack:** Android VectorDrawable XML, rsvg-convert (SVG→PNG), cwebp (PNG→WebP)

---

### Task 1: Update background drawable

**Files:**
- Modify: `app/src/main/res/drawable/ic_launcher_background.xml`

No unit test — visual resource. Lint validates the XML structure.

- [ ] **Step 1: Replace file contents**

Replace the entire file with:

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path android:pathData="M0,0h108v108h-108z">
        <aapt:attr name="android:fillColor">
            <gradient
                android:type="linear"
                android:startX="0"
                android:startY="0"
                android:endX="108"
                android:endY="108">
                <item android:offset="0" android:color="#FF1a0808"/>
                <item android:offset="1" android:color="#FF2d1206"/>
            </gradient>
        </aapt:attr>
    </path>
</vector>
```

- [ ] **Step 2: Run lint to verify XML is valid**

```bash
./gradlew lint 2>&1 | grep -i "ic_launcher_background\|error" | head -20
```

Expected: no errors referencing `ic_launcher_background`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/drawable/ic_launcher_background.xml
git commit -m "feat: update launcher background to dark gradient"
```

---

### Task 2: Update foreground drawable

**Files:**
- Modify: `app/src/main/res/drawable/ic_launcher_foreground.xml`

No unit test — visual resource. Lint validates the XML structure.

- [ ] **Step 1: Replace file contents**

Replace the entire file with:

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">

    <!-- ECG main line -->
    <path
        android:pathData="M8,54 L20,54 L26,36 L32,68 L40,20 L48,54 L56,48 L62,54 L70,42 L78,54 L88,54"
        android:fillColor="#00000000"
        android:strokeColor="#ff6a00"
        android:strokeWidth="4.5"
        android:strokeLineCap="round"
        android:strokeLineJoin="round"/>

    <!-- ECG glow pass: same path, thinner, lighter, semi-transparent -->
    <path
        android:pathData="M8,54 L20,54 L26,36 L32,68 L40,20 L48,54 L56,48 L62,54 L70,42 L78,54 L88,54"
        android:fillColor="#00000000"
        android:strokeColor="#ffaa60"
        android:strokeWidth="2"
        android:strokeLineCap="round"
        android:strokeLineJoin="round"
        android:strokeAlpha="0.35"/>

    <!-- GPS dot: filled circle at right terminus (88,54) r=6, drawn as two half-arcs -->
    <path
        android:pathData="M82,54 A6,6 0 1,0 94,54 A6,6 0 1,0 82,54 Z"
        android:fillColor="#ff6a00"/>

    <!-- GPS ring: semi-transparent stroke circle r=10 around the dot -->
    <path
        android:pathData="M78,54 A10,10 0 1,0 98,54 A10,10 0 1,0 78,54 Z"
        android:fillColor="#00000000"
        android:strokeColor="#ff6a00"
        android:strokeWidth="2"
        android:strokeAlpha="0.35"/>
</vector>
```

- [ ] **Step 2: Run lint**

```bash
./gradlew lint 2>&1 | grep -i "ic_launcher_foreground\|error" | head -20
```

Expected: no errors referencing `ic_launcher_foreground`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/drawable/ic_launcher_foreground.xml
git commit -m "feat: update launcher foreground to Pulse ECG icon"
```

---

### Task 3: Generate legacy WebP bitmaps

These replace the default Android robot bitmaps used on API 24–25 (Android 7.x) devices. Devices on API 26+ use the adaptive icon XML and never read these files.

**Files:**
- Create: `scripts/generate_icon_bitmaps.sh`
- Modify: `app/src/main/res/mipmap-mdpi/ic_launcher.webp`
- Modify: `app/src/main/res/mipmap-mdpi/ic_launcher_round.webp`
- Modify: `app/src/main/res/mipmap-hdpi/ic_launcher.webp`
- Modify: `app/src/main/res/mipmap-hdpi/ic_launcher_round.webp`
- Modify: `app/src/main/res/mipmap-xhdpi/ic_launcher.webp`
- Modify: `app/src/main/res/mipmap-xhdpi/ic_launcher_round.webp`
- Modify: `app/src/main/res/mipmap-xxhdpi/ic_launcher.webp`
- Modify: `app/src/main/res/mipmap-xxhdpi/ic_launcher_round.webp`
- Modify: `app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp`
- Modify: `app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.webp`

- [ ] **Step 1: Install required tools (if not already present)**

```bash
sudo apt-get install -y librsvg2-bin webp
```

Expected: both `rsvg-convert` and `cwebp` commands become available.

- [ ] **Step 2: Create the generation script**

Create `scripts/generate_icon_bitmaps.sh`:

```bash
#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
RES_DIR="$PROJECT_DIR/app/src/main/res"
TMP_SVG="/tmp/runner_icon_flat.svg"
TMP_PNG="/tmp/runner_icon.png"

cat > "$TMP_SVG" << 'SVGEOF'
<svg xmlns="http://www.w3.org/2000/svg" width="512" height="512" viewBox="0 0 108 108">
  <defs>
    <linearGradient id="bg" x1="0" y1="0" x2="108" y2="108" gradientUnits="userSpaceOnUse">
      <stop offset="0%" stop-color="#1a0808"/>
      <stop offset="100%" stop-color="#2d1206"/>
    </linearGradient>
  </defs>
  <rect width="108" height="108" fill="url(#bg)"/>
  <polyline points="8,54 20,54 26,36 32,68 40,20 48,54 56,48 62,54 70,42 78,54 88,54"
    fill="none" stroke="#ff6a00" stroke-width="4.5" stroke-linecap="round" stroke-linejoin="round"/>
  <polyline points="8,54 20,54 26,36 32,68 40,20 48,54 56,48 62,54 70,42 78,54 88,54"
    fill="none" stroke="#ffaa60" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" opacity="0.35"/>
  <circle cx="88" cy="54" r="6" fill="#ff6a00"/>
  <circle cx="88" cy="54" r="10" fill="none" stroke="#ff6a00" stroke-width="2" opacity="0.35"/>
</svg>
SVGEOF

declare -A SIZES=(
  [mdpi]=48
  [hdpi]=72
  [xhdpi]=96
  [xxhdpi]=144
  [xxxhdpi]=192
)

for density in "${!SIZES[@]}"; do
  size="${SIZES[$density]}"
  rsvg-convert -w "$size" -h "$size" "$TMP_SVG" -o "$TMP_PNG"
  cwebp -q 100 "$TMP_PNG" -o "$RES_DIR/mipmap-${density}/ic_launcher.webp"
  cwebp -q 100 "$TMP_PNG" -o "$RES_DIR/mipmap-${density}/ic_launcher_round.webp"
  echo "Generated ${density} (${size}x${size})"
done

rm -f "$TMP_SVG" "$TMP_PNG"
echo "Done."
```

- [ ] **Step 3: Make script executable and run it**

```bash
chmod +x scripts/generate_icon_bitmaps.sh
./scripts/generate_icon_bitmaps.sh
```

Expected output:
```
Generated mdpi (48x48)
Generated hdpi (72x72)
Generated xhdpi (96x96)
Generated xxhdpi (144x144)
Generated xxxhdpi (192x192)
Done.
```

- [ ] **Step 4: Verify files were updated**

```bash
ls -lh app/src/main/res/mipmap-xxhdpi/ic_launcher.webp
```

Expected: file timestamp is current (just replaced).

- [ ] **Step 5: Commit**

```bash
git add scripts/generate_icon_bitmaps.sh \
  app/src/main/res/mipmap-mdpi/ic_launcher.webp \
  app/src/main/res/mipmap-mdpi/ic_launcher_round.webp \
  app/src/main/res/mipmap-hdpi/ic_launcher.webp \
  app/src/main/res/mipmap-hdpi/ic_launcher_round.webp \
  app/src/main/res/mipmap-xhdpi/ic_launcher.webp \
  app/src/main/res/mipmap-xhdpi/ic_launcher_round.webp \
  app/src/main/res/mipmap-xxhdpi/ic_launcher.webp \
  app/src/main/res/mipmap-xxhdpi/ic_launcher_round.webp \
  app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp \
  app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.webp
git commit -m "feat: generate Pulse icon legacy WebP bitmaps"
```

---

### Task 4: Build and smoke test

- [ ] **Step 1: Build debug APK**

```bash
./gradlew assembleDebug
```

Expected: `BUILD SUCCESSFUL` with no resource errors.

- [ ] **Step 2: Install on device**

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Expected: `Success`

- [ ] **Step 3: Verify icon on launcher**

On the device, navigate to the launcher/home screen. Long-press to go to the app drawer if needed. Confirm:
- The Runner icon shows an orange ECG line on a dark background.
- No text is visible on the icon.
- The GPS dot appears at the right end of the line.
- The icon looks sharp (not blurry) at launcher size.

- [ ] **Step 4: Confirm with user and close**

Report smoke test result to user before pushing.
