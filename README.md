# Live Incident Map — Android

A production-minded Android app that plots real-time incident data across Morocco on an
interactive map, with a list view, filtering, live updates and offline support.

Built for the Frontend Technical Challenge (mobile track).

## Stack

| Concern | Choice |
|---|---|
| Language / UI | Kotlin + Jetpack Compose |
| Architecture | MVVM + unidirectional data flow (`StateFlow`) |
| Async | Kotlin Coroutines + Flow |
| DI | Hilt |
| Map | MapLibre GL Native (open-source, GPU clustering, no API key) |
| Local cache | Room |
| Min / Target SDK | 26 / 35 |

## Requirements

- Android Studio (Ladybug or newer)
- JDK 17 or 21 (the project targets JVM 17; the Android Gradle Plugin does not yet
  support JDK 23 — if your `JAVA_HOME` points at 23, select JDK 21 in
  *Settings → Build Tools → Gradle → Gradle JDK*)
- An emulator or device on API 26+

## Run

```bash
# from Android Studio: open the project and press Run, or from the CLI:
./gradlew installDebug   # builds and installs on a running emulator/device
```

`local.properties` (pointing at your Android SDK) is generated automatically by Android
Studio and is intentionally git-ignored.

## Project status

Built feature-by-feature on separate branches / PRs. See the commit history and the
"What's next" section (added as the app grows).

---

_Full architecture notes, tradeoffs and "what I'd do next" live at the bottom of this
README and are expanded as each feature lands._
