# Circuit Trainer

A lightweight, privacy-friendly interval/circuit training app for Android, inspired by
[Privacy Friendly Circuit Training](https://f-droid.org/en/packages/org.secuso.privacyfriendlycircuittraining/)
but designed around a friendlier UI and a few fixes for its rough edges.

No ads, no tracking, no network access. Workouts are stored on-device as a small JSON file.

## Features

- **Per-exercise durations** — every exercise has its own work time *and* its own rest
  time, instead of one static duration for the whole session.
- **Rounds** — repeat the whole circuit any number of times, with a configurable break
  between rounds.
- **Exercise library** — create reusable exercises (with default work/rest times) in a
  dedicated screen, then drop them into any workout with one tap from the "Add exercise"
  sheet. Exercises in a workout can also be bookmarked back into the library.
- **Big, clear workout player** — color-coded phases (get ready / work / rest), a large
  countdown ring, "up next" preview, and oversized controls that are easy to hit
  mid-burpee. Skip forward/back, add 10 seconds, pause any time.
- **Back button can't kill your session** — pressing back during a workout pauses the
  timer and asks for confirmation instead of silently ending the workout.
- **Auto-pause when you leave the app** — switching to another app (e.g. to change a
  song) pauses the workout exactly where it is; it resumes only when you say so. No more
  timers running blind in the background or a frozen UI when you come back.
- **Robust timer** — remaining time is computed from the monotonic clock on every tick,
  so the display can never drift, freeze, or get stuck at 0.
- Sound (3-2-1 beeps, phase-change tones) and vibration cues, each individually
  toggleable, plus a configurable get-ready countdown.
- Keeps the screen on during a session; survives screen rotation without losing progress.
- Material 3 UI with dark theme and dynamic color (Android 12+).

## Building

Requires JDK 17+ and the Android SDK (set `ANDROID_HOME`, or build in Android Studio).

```sh
./gradlew assembleDebug
# APK lands in app/build/outputs/apk/debug/app-debug.apk
```

You don't need a local setup to get an APK: every push builds one in CI and publishes it
to the rolling [**latest** release](../../releases/tag/latest) (`circuit-trainer.apk`).
Download it on your phone, allow installs from your browser, and you're set. Each CI run
also keeps the APK as a `circuit-trainer-debug` artifact under the Actions tab.

## Project layout

- `app/src/main/java/io/github/jreyn419/circuittrainer/data` — models and JSON/file
  persistence (no database, keeps the app small).
- `.../ui/home` — workout list.
- `.../ui/library` — reusable exercise library.
- `.../ui/edit` — workout editor (per-exercise durations, reordering, rounds, library picker).
- `.../ui/play` — the workout player: timer engine (`PlayerViewModel`), screen, and
  sound/vibration cues.
- `.../ui/settings` — sound/vibration/get-ready settings.
