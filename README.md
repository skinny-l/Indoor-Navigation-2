# Indoor Navigation

An Android indoor-navigation app built with Kotlin and Jetpack Compose. It implements
BLE-beacon trilateration for positioning and A* pathfinding for turn-by-turn routing
across multiple floors, with wheelchair-accessible route options.

**Status: prototype.** It runs against a simulated beacon environment and mock floor-plan
data — no backend, no live beacon deployment, authentication is stubbed. What's real is
the positioning maths, the pathfinding, and the UI.

## What it does

- Renders multi-floor building plans and searches points of interest
- Estimates position by converting beacon RSSI to distance via a path-loss model, then
  trilaterating from three or more beacons
- Smooths the result using accelerometer and gyroscope data
- Routes between any two points with A*, handling floor changes via stairs and lifts
- Filters routes for wheelchair accessibility

## Running it

Android Studio, SDK 24+ (Android 7.0), Kotlin 2.1.20+.

    git clone https://github.com/skinny-l/Indoor-Navigation-2.git

Open in Android Studio, sync Gradle, run. No configuration needed — it starts on mock data.

## Structure

- `positioning/` — beacon scanning, RSSI-to-distance conversion, trilateration, sensor fusion
- `navigation/` — navigation graph and A* implementation
- `domain/model/` — Position, FloorPlan, PointOfInterest, Beacon, NavigationPath
- `presentation/` — Compose screens, theming, ViewModels
- `data/repository/` — data access, currently backed by mock sources

## What real deployment would need

Surveyed beacon positions with configured UUIDs, actual floor-plan data, a real auth
implementation, and a backend serving building data. The repository interfaces are the
seams where those plug in.

## Licence

MIT
