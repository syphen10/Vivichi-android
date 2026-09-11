# Vivichi (Android / Kotlin)

Native Kotlin + Jetpack Compose rewrite of the original single-file HTML app, ready to build into
an APK/AAB for the Play Store.

## What's here

- **Kotlin + Jetpack Compose (Material 3)** UI — all 7 tabs (Home, Habits, Stats, Style, Playground,
  Cemetery, Settings) plus onboarding, rewritten natively, not a WebView wrapper.
- **State & persistence**: `AppState` (kotlinx.serialization) stored as JSON in Jetpack DataStore —
  the same "one JSON blob" persistence strategy the original used with `localStorage`.
- **Game logic**: `domain/GameLogic.kt` — habit unlock/expiry windows, XP/leveling, streaks, health
  decay, day rollover, mood, titles — ported 1:1 from the original's JS functions.
- **Notifications**: `AlarmManager`-based daily reminders per enabled habit, re-armed on boot.

## What was intentionally simplified (and why)

The original embeds large Lottie JSON animations and base64-encoded WebM videos per species/mood —
hundreds of KB of animation data per character. Porting that byte-for-byte isn't practical as a
Kotlin/Compose rewrite, so:

- **Characters** (`ui/components/CharacterView.kt`) are rendered as emoji with native Compose
  animations (float/rotate/pulse by mood, desaturate by health) instead of Lottie/video. Same
  species/mood/outfit/health logic drives it — just a lighter-weight renderer. Swap in a Lottie
  library (`com.airbnb.android:lottie-compose`) later if you want to reuse the original JSON assets.
- **Playground sounds** use `ToneGenerator` beeps instead of the original's synthesized Web Audio
  animal sounds.
- **Streak share** isn't implemented as a canvas-drawn image card (that's a fair chunk of extra
  work — Canvas → Bitmap → FileProvider → share intent). Worth adding if you want it; happy to build
  it out.
- I also fixed one latent bug found while porting: the original's day-rollover death handling never
  reset the day's log on the death path, so it could append duplicate cemetery entries every minute
  until the death screen was dismissed. Fixed here.

Everything else — habit CRUD, scheduling, XP/leveling, streak math, health, titles, outfit
unlocking, cemetery, settings — is fully native and functionally equivalent to the original.

## Before you publish

- **App icon** (`res/drawable/ic_launcher_foreground.xml`) is a simple placeholder vector — replace
  with real branded artwork (use Android Studio's Image Asset Studio for adaptive icons).
- **Notification icon** currently uses a system placeholder — add a proper monochrome status-bar icon.
- **Package name** is `com.vivichi.app` — must be globally unique on Play; change
  `applicationId`/`namespace` in `app/build.gradle.kts` if taken.
- **Play Store assets** (feature graphic, screenshots, privacy policy) aren't part of this repo.

## Build & run

1. Open this folder in **Android Studio** (Koala+ recommended). It will offer to generate the Gradle
   wrapper automatically the first time — accept it (or run `gradle wrapper` yourself if you have
   Gradle installed).
2. Let Gradle sync, then **Run** on a device/emulator (API 26+) to try it.

## Build a release AAB for Play Store

1. **Build > Generate Signed Bundle / APK** in Android Studio → choose **Android App Bundle**.
2. Create (or reuse) a signing keystore — **back this up**, you need the same key for every future update.
3. Build the release variant → produces `app/release/app-release.aab`.
4. Upload that `.aab` to the Play Console under your app's release track.

Command-line equivalent, once you have a keystore and the wrapper generated:

```bash
./gradlew bundleRelease
```
