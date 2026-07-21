# Live Incident Map — Android

An operations tool that plots incidents across Morocco on an interactive map, with a list
view, filtering, a live feed and offline support.

Built for the Frontend Technical Challenge (mobile track).

## Features

- **Map** — full-screen, centred on Morocco, markers coloured and sized by severity, with
  clustering so the view stays legible across the whole dataset
- **List** — newest first, pull-to-refresh, infinite scroll
- **Detail** — every field for a single incident
- **Filters** — category and severity (multi-select) and a date range, applied to both the
  map and the list at once
- **Live updates** — new incidents arrive continuously without moving the user's map view or
  scroll position
- **Offline** — cached incidents remain available with no connection
- Loading skeletons, empty and error states; portrait and landscape

## Stack

Kotlin · Jetpack Compose · MVVM with unidirectional data flow · Coroutines & Flow · Hilt ·
Room · MapLibre GL Native · min SDK 26, compile SDK 35

## Running the app

**Requires JDK 17 or 21** — the Android Gradle Plugin does not support JDK 23 yet. No API
keys or additional setup: the basemap and dataset need no credentials.

### Android Studio
Open the project (Ladybug or newer) and press **Run**.

### Command line

```bash
# Start an emulator, if one isn't already running
emulator -avd <your-avd-name>       # `emulator -list-avds` to see them

# Build, install and launch
./gradlew installDebug
adb shell am start -n com.livemap.incidents/.MainActivity
```

On Windows, use `gradlew.bat` in place of `./gradlew`.

Other tasks:

```bash
./gradlew assembleDebug   # build the APK → app/build/outputs/apk/debug/
./gradlew test            # run unit tests
./gradlew clean           # clean build outputs
```

If Gradle picks the wrong JDK, point it at a supported one — in Android Studio via
*Settings → Build Tools → Gradle → Gradle JDK*, or on the command line by setting `JAVA_HOME`.

## Architecture

```
Data sources ──► Repository ──► Room (single source of truth) ──► ViewModel ──► Compose UI
```

Room is the single source of truth. The initial load and every live arrival are writes into
that cache, and each screen renders as a pure function of it. Nothing pushes data into the
UI, which is why offline behaviour, live updates and keeping the map and list in agreement
all follow from one decision rather than three separate mechanisms.

```
data/    models, data sources, Room, connectivity, repositories
ui/      map, list, detail, filters, shared components, navigation
di/      Hilt modules
```

## Key decisions

**MapLibre, clustering on the source.** Clustering is configured on the GeoJSON source rather
than in application code, so the map engine groups points itself and marker styling is
evaluated by the renderer. Nothing allocates a view per marker, so cost stays roughly flat as
the dataset grows. It also needs no API key.

**One shared filter holder.** The active filters live in a single application-scoped object
that both screens observe, so they cannot drift apart. Cross-screen consistency is structural
rather than something to remember.

**The user's position is never overwritten.** The camera and scroll offset are treated as user
state: the app reads them but never moves them on the user's behalf. New incidents are
rendered where they fall, and a small "N new incidents" indicator says something arrived —
jumping to it is always an explicit choice.

## Tradeoffs accepted

**Filtering happens in memory rather than in SQL.** At this dataset size it keeps the filter
logic pure and easy to test, at the cost of holding everything in memory. Past a much larger
dataset the predicates would move into the database query — the screens would not change.

**Refresh merges rather than replaces.** Replacing the cache wholesale would prune records
deleted upstream, but would also discard live incidents the operator had already seen.
Merging keeps live data intact; reconciling deletions would need the server to say what was
removed.

**OpenStreetMap raster tiles.** Keyless and immediately runnable by a reviewer, at the cost of
being heavier than vector tiles and unsuitable for production traffic. Changing to a keyed or
self-hosted provider is a one-line style change.

## A note on the dataset

Some incidents plot in the sea. That is in the source data, not the rendering: coordinates are
jittered around city centres with no land/water check, so the area around each coastal city
spills offshore.

I chose to render the data faithfully rather than quietly correct it — hiding a data quality
problem from an operations tool is worse than showing it. In production I would validate
coordinates when data is ingested and flag suspect records, since dispatching a unit to a
point at sea is a real failure mode.

## What I'd do next

- Instrumented tests for the map interactions; unit tests currently cover the pure logic
- Persist the selected filters between launches
- Move filtering into the database query once the dataset justifies it
- Cache map tiles, so the basemap is available offline as well as the incident data
- A heat/density view, which maps cleanly onto the same clustered source
- An accessibility pass with TalkBack and large font scales

## How I'd approach the web (Angular) version

**Mapping:** MapLibre GL JS — the same style, the same clustered source, the same expressions.
The map configuration would port over almost unchanged.

**State:** signals for view state, RxJS where streams compose, mirroring the structure here —
a store for incidents, a store for filters, and a derived value combining them.

**Reuse:** the domain model and the filter logic are pure and would translate directly to
TypeScript, along with their tests. Sharing that model is what would keep the two platforms
behaving identically.

**Rebuild:** everything lifecycle-shaped. Preserving the map viewport is an Android-specific
problem here; on the web the equivalent is surviving route changes, which I would solve by
keeping the map instance outside the routed component — or by putting the camera in the URL,
so a view can be shared with a colleague.

---

Built feature by feature, each on its own branch and pull request — see the
[commit history](../../commits/main) and [pull requests](../../pulls?q=is%3Apr).
