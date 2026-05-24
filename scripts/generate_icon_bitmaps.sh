#!/usr/bin/env bash
# generate_icon_bitmaps.sh
# Renders the Pulse icon SVG at 5 Android densities and writes WebP files
# into the appropriate mipmap resource directories.
#
# Requirements: rsvg-convert (librsvg) and ImageMagick convert with libwebp
# No new packages are installed by this script.

set -euo pipefail

CONVERT=$(command -v magick 2>/dev/null || command -v convert 2>/dev/null || true)

for cmd in rsvg-convert "$CONVERT"; do
  if [[ -z "$cmd" ]] || ! command -v "$cmd" &>/dev/null; then
    echo "Error: required tool not found: ${cmd:-convert/magick}" >&2
    echo "Install: sudo apt-get install librsvg2-bin imagemagick" >&2
    exit 1
  fi
done

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
RES_DIR="$PROJECT_ROOT/app/src/main/res"
TMP_SVG="$(mktemp /tmp/pulse_icon_XXXXXX.svg)"
TMP_PNG="$(mktemp /tmp/pulse_icon_XXXXXX.png)"

cleanup() {
  rm -f "$TMP_SVG" "$TMP_PNG"
}
trap cleanup EXIT

# ---------------------------------------------------------------------------
# Pulse icon SVG (ECG line + GPS dot on dark gradient background)
# ---------------------------------------------------------------------------
cat > "$TMP_SVG" << 'SVG_EOF'
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
SVG_EOF

# ---------------------------------------------------------------------------
# Density → pixel size mapping
# ---------------------------------------------------------------------------
declare -A SIZES=(
  [mdpi]=48
  [hdpi]=72
  [xhdpi]=96
  [xxhdpi]=144
  [xxxhdpi]=192
)

echo "Generating Pulse icon WebP bitmaps..."

for DENSITY in mdpi hdpi xhdpi xxhdpi xxxhdpi; do
  SIZE="${SIZES[$DENSITY]}"
  OUT_DIR="$RES_DIR/mipmap-${DENSITY}"

  if [[ ! -d "$OUT_DIR" ]]; then
    echo "ERROR: directory not found: $OUT_DIR" >&2
    exit 1
  fi

  # Step 1: render SVG → PNG at exact pixel size (rsvg-convert gives accurate SVG rendering)
  rsvg-convert -w "$SIZE" -h "$SIZE" "$TMP_SVG" -o "$TMP_PNG"

  # Step 2: convert PNG → WebP
  for NAME in ic_launcher ic_launcher_round; do
    OUT_FILE="$OUT_DIR/${NAME}.webp"
    $CONVERT "$TMP_PNG" -define webp:lossless=true "$OUT_FILE"
    echo "  wrote $OUT_FILE (${SIZE}x${SIZE})"
  done
done

echo "Done. All 10 WebP files updated."
