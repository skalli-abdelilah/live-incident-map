# Live Incident Map — Android

An operations tool that plots ~1,500 incidents across Morocco on an interactive map, with a
list view, reactive filtering, a simulated live feed, and offline support.

Built for the Frontend Technical Challenge (mobile track, Android).

| | |
|---|---|
| **Language / UI** | Kotlin + Jetpack Compose (Material 3) |
| **Architecture** | MVVM + unidirectional data flow |
| **Async** | Coroutines + Flow |
| **DI** | Hilt |
| **Map** | MapLibre GL Native |
| **Cache** | Room |
| **Min / Compile SDK** | 26 / 35 |

## Run

Open the project in Android Studio (Ladybug or newer) and press **Run**, or:

```bash
./gradlew installDebug
```

Requires **JDK 17 or 21** — the Android Gradle Plugin does not support JDK 23 yet. If your
`JAVA_HOME` points at 23, select a 21 JDK under *Settings → Build Tools → Gradle → Gradle JDK*.
No API keys or extra setup: the basemap and dataset need no credentials.

---

## Key technical decisions

### MapLibre, with clustering on the source
Clustering is configured on the **GeoJSON source**, not in application code, so MapLibre
groups points inside the map engine as the camera moves. Nothing allocates one Android view
per marker, and marker styling is a data-driven expression evaluated by the renderer rather
than a JVM round-trip. Cost is effectively independent of the number of incidents.

MapLibre also needs no API key and imposes no vendor lock-in.

### Room as the single source of truth
Every screen observes `Flow<List<Incident>>` from the database. Both the initial load and
live arrivals are *writes into that cache* — no code path pushes data into the UI. This is
what makes offline behaviour fall out for free: a failed refresh leaves the cache intact, so
the last good data stays on screen.

### One app-scoped filter holder
`FilterRepository` is a `@Singleton`. Both the map and list ViewModels `combine` their data
with its `StateFlow`, so changing a filter re-emits and both screens re-render. Had each
screen owned its own copy, the two views could silently disagree about what is displayed —
this makes cross-screen consistency structural instead of something to remember.

Filter semantics are **AND across facets, OR within a facet**. An empty facet means
"unrestricted" rather than "match nothing", which keeps the default state trivially
representable and makes *clear all* a single assignment.

### Preserving the user's position
The requirement that new incidents must not disrupt the operator is handled mostly by what
the code **deliberately doesn't do**:

- The camera is never moved programmatically. Data updates only call `setGeoJson`, swapping
  rendered features.
- The list is never scrolled programmatically. Rows are keyed by incident id so `LazyColumn`
  keeps its anchor; the only `animateScrollToItem` call sits inside the pill's `onClick`.
- The "N new incidents" pill **informs, it doesn't gate** — the data is already rendered.

`MapView` can't survive leaving composition, so the **camera itself** is hoisted into
`rememberSaveable` and recorded on camera-idle. Returning from the detail screen, or rotating
the device, restores the viewport instead of re-framing the country. The underlying principle:
*viewport and scroll offset are user state — the app reads them, it doesn't write them.*

---

## Tradeoffs I deliberately accepted

**1. Filtering happens in memory, not in SQL.**
At 1,500–5,000 incidents, filtering an in-memory list inside `combine` is simpler, keeps the
predicate logic pure and unit-testable, and avoids query-building complexity. It does mean the
whole dataset lives in memory and every filter change re-filters the full list. Past ~50k I'd
push the predicates into the Room query (`WHERE` + an index on `reportedAtEpochMs`) and return
a filtered `Flow` — the ViewModels wouldn't change, only the repository.

**2. Refresh merges instead of replacing.**
Refresh upserts by id rather than clearing the table first. A clear-then-insert is atomic and
would prune records deleted upstream, but once the live feed shared that table it also deleted
every live incident the operator had already seen. Merging keeps live data intact; the cost is
that upstream deletions would linger, which a production sync would solve with a
server-supplied deletion list rather than inferring removals from a snapshot.

**3. OpenStreetMap raster tiles.**
Keyless and instantly runnable by a reviewer, at the cost of raster tiles being heavier and
less crisp than vector, and OSM's public tile server being unsuitable for production traffic.
In production I'd use a keyed vector provider (MapTiler, Protomaps) or self-hosted tiles —
a one-line style URL change.

---

## A note on the dataset

Some incidents plot in the Atlantic. That is in the source data, not the rendering: the
dataset jitters coordinates ±0.05–0.10° around each city centre with no land/water check, so
the jitter circle around every coastal city spills into the sea (e.g. `INC-00628` at
`33.62618, -7.68736`, north-west of Casablanca's coastline). The overall bounding box —
lat `23.61…35.82`, lng `-16.04…-1.85` — confirms lat/lng ordering is correct.

I chose to **render the data faithfully rather than silently correct it**: quietly mutating
source data hides a real quality problem from the people who need to know about it. In
production I'd validate coordinates at the ingestion boundary and flag suspect records,
because an operator dispatching a unit to a point at sea is a genuine failure mode.

---

## What I'd do next

1. **Instrumented tests** for the map interactions (cluster tap, marker tap) and a Room
   migration test — the unit tests currently cover the pure logic only.
2. **Persist filters** across launches with DataStore; the shape is already a single
   serializable object.
3. **Push filtering into SQL** as described above, once the dataset justifies it.
4. **Tile caching** for genuine offline maps. Incident data is cached, but the basemap still
   needs a network on first view of an area.
5. **Heat/density view** — the web brief asks for one; on mobile it maps cleanly onto a
   MapLibre `HeatmapLayer` over the same GeoJSON source, toggled from the existing UI.
6. **Accessibility pass** — content descriptions on map affordances, and verification with
   TalkBack and large font scales.

---

## How I'd approach the web (Angular) version

**Mapping:** MapLibre GL JS — the same style JSON, the same clustered GeoJSON source, the
same expressions. The map configuration would port almost verbatim.

**State:** Angular signals for view state, RxJS where streams compose. The core would mirror
the mobile design: an `IncidentStore` exposing incidents, a `FilterStore` holding the
selection, and a derived `computed()` combining them — the direct analogue of `combine`.
The live feed would be a `WebSocketSubject` (or an `interval` simulation) whose emissions
update the store, so the map re-renders from state exactly as it does here.

**Data model reuse:** the domain types (`Incident`, `Severity`, `IncidentCategory`) and the
filter predicate are pure logic and would translate one-to-one into TypeScript — the same
"empty facet means unrestricted" semantics and the same AND/OR rules, with the same unit
tests. That shared model is what keeps the two platforms behaving identically.

**What I'd rebuild:** everything lifecycle-shaped — the `MapView` lifecycle bridge and
`rememberSaveable` camera persistence are Android-specific concerns. On the web the
equivalent problem is preserving the viewport across route changes, solved by keeping the map
instance alive outside the routed component, or storing the camera in the URL so a view is
shareable — which is arguably better UX for a control room.

---

## Project structure

```
data/
  model/        Incident, Severity, IncidentCategory, IncidentFilters
  remote/       Asset data source (simulated network) + simulated live feed
  local/        Room entity, DAO, database
  network/      Connectivity monitor
  repository/   IncidentRepository, FilterRepository, LiveUpdateManager
ui/
  map/          MapLibre integration, clustering layers, camera persistence
  list/         Paged list with pull-to-refresh
  detail/       Single-incident screen
  filters/      Filter bottom sheet
  common/       Skeletons, states, severity badge, banners
  navigation/   Type-safe routes and NavHost
di/             Hilt modules
```

Built feature by feature, each on its own branch and pull request — see the
[commit history](../../commits/main) and [pull requests](../../pulls?q=is%3Apr).
