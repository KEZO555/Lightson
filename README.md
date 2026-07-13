# Lightson

A tiny Android app that replicates the **colour filter** feature from
[Luma](https://github.com/vandamd/luma), the minimal launcher for the Light
Phone III — but as a standalone app that works with any launcher on any
Android device (8.0+).

## What it does

Your phone stays in grayscale (Android's built-in colour-correction /
daltonizer — the same mechanism the Light Phone uses; no overlays, no
root). Lightson only pauses the grayscale per app:

- **Colour apps** — pick apps that should run in full colour. When one of
  them comes to the foreground the grayscale filter switches off
  automatically, and it switches back on the moment you leave the app.
- **Keymap** — give the *current* app colour from a hardware key; grayscale
  returns by itself when you leave the app (or press again). Options:
  long-press the camera button (Light Phone III's side shutter key), volume
  up + down together, or double-press volume up/down. With the camera-button
  keymap a short press still opens the camera, and the key is left untouched
  while a camera app is in the foreground so the shutter keeps working.
- **Toggle shortcut** — an exported `ToggleFilterActivity`
  (`app.lightson.action.TOGGLE_FILTER`) plus a launcher long-press shortcut,
  so key-mapper apps (Key Mapper, Button Mapper), launcher gestures, or
  `adb shell am start -n app.lightson/.ToggleFilterActivity` can toggle
  colour for the current app too.

## Install with Obtainium

Add this repo to [Obtainium](https://github.com/ImranR98/Obtainium):
*Add App* → paste `https://github.com/KEZO555/Lightson` → *Add*. Obtainium
picks up the APK attached to the latest GitHub Release and notifies you of
updates. Releases are published automatically whenever a `v*` tag is pushed.

Note: release APKs are signed with a keystore committed to this repo so
every CI build has the same signature (required for clean updates). That
key is for personal sideloading only — anyone can sign with it, so don't
trust the signature as proof of origin, and never reuse the key elsewhere.

## Setup

1. Install the APK.
2. Grant the secure-settings permission once via adb (this is the only way
   Android allows an app to control colour correction; it survives reboots):

   ```
   adb shell pm grant app.lightson android.permission.WRITE_SECURE_SETTINGS
   ```

3. To use automatic colour switching, tap **Auto colour switching** and
   enable the *Lightson colour switching* accessibility service. The service
   only listens for foreground-app changes — it cannot and does not read
   screen content (`canRetrieveWindowContent` is off).
4. Pick your **Colour apps** (e.g. Camera, Photos, Maps).
5. Optionally pick a **Keymap** (e.g. *Long-press camera button*). Key
   gestures are detected by the same accessibility service, so it must be
   enabled for the keymap to work.

## How it works

The grayscale effect is Android's accessibility colour correction set to
monochromacy, controlled through two `Settings.Secure` keys:

- `accessibility_display_daltonizer_enabled` — filter on/off
- `accessibility_display_daltonizer` — `0` = grayscale (monochromacy)

Writing those keys requires `WRITE_SECURE_SETTINGS`, which can only be
granted over adb. An `AccessibilityService` subscribed to
`TYPE_WINDOW_STATE_CHANGED` events detects which app is in the foreground:
entering a colour app disables the filter, leaving it restores the filter.
The "we disabled it" flag is persisted so the filter is restored even if the
process is killed mid-swap. Events from the system UI and keyboards are
ignored to avoid the filter flickering during app transitions.

## Building

```
./gradlew assembleDebug
```

The APK lands in `app/build/outputs/apk/debug/`. A GitHub Actions workflow
also builds the APK on every push and attaches it as a build artifact.
