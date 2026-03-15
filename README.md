# Robot Simulator

Java/Gradle warehouse simulator for the 2026 multi-agent programming project.

## What Is Implemented

The project currently includes:

- `Reference` simulation mode matching the project baseline idea:
  - pallets are delivered directly to exit zones
  - no AMR communication
  - no battery management
  - no intermediate storage usage
- `Optimized` simulation mode with:
  - fixed AMR fleet
  - decentralized bid/claim task allocation
  - battery tracking and recharge area constraints
  - intermediate storage areas
  - local movement conflict resolution
  - metrics export
- Live warehouse UI showing:
  - entry zones
  - exit zones
  - intermediate zones and occupancy
  - recharge zone occupancy
  - robots
  - pallets and their destination exits
  - fixed obstacles and moving humans

## Current Functional Flow

At each simulation step the optimized simulator does:

1. Generate new pallets in entry zones.
2. Move human obstacles.
3. Let idle AMRs evaluate known pallets.
4. Broadcast bids and resolve claims.
5. Move robots one step with local conflict handling.
6. Deliver pallets to exits or store them in intermediate areas.
7. Update battery, recharge state, and metrics.

## Main Project Files

- `src/main/java/fr/emse/Main.java`: application entry point
- `src/main/java/fr/emse/warehouse/WarehouseSimulator.java`: core warehouse logic
- `src/main/java/fr/emse/warehouse/WarehouseConfig.java`: INI config parsing and validation
- `src/main/java/fr/emse/warehouse/WarehouseUiApp.java`: live UI renderer
- `configuration.ini`: scenario and runtime configuration

## Run Commands

Build:

```bash
./gradlew build
```

Run one simulation:

```bash
./gradlew run
```

Run the live UI:

```bash
./gradlew run --args="--ui"
```

Run the comparison suite:

```bash
./gradlew run --args="--suite"
```

Run tests:

```bash
./gradlew test
```

On Windows use `gradlew.bat` instead of `./gradlew`.

## Configuration

The simulator reads `configuration.ini`.

Important sections:

- `[simulation]`: mode, steps, seed, AMR count
- `[warehouse]`: grid size
- `[arrivals]`: pallet distribution and rate
- `[battery]`: battery, thresholds, safe margin, recharge duration/capacity
- `[zones]`: entry, exit, intermediate, recharge zones
- `[obstacles]`: fixed obstacles and humans
- `[output]`: metrics file
- `[ui]`: UI animation delay

## Metrics Output

The simulator exports metrics to `simulation-metrics.csv`.

Tracked indicators include:

- delivered pallet count
- total delivery time
- average delivery time
- total AMR travel distance
- recharge count and recharge waiting
- blocked movement conflicts
- message count
- average intermediate occupancy

## Known Limitations

These are known issues in the current implementation and are intentionally left visible for comparison before the next fix iteration:

- battery feasibility is not yet enforced as a hard constraint before pickup
- robots can still accept tasks that later become unsafe
- intermediate retrieval needs refinement in some scenarios
- dynamic battery threshold logic for safe fallback to intermediate/recharge is not yet implemented

## Tests

Current automated coverage includes:

- config parsing
- grid pathfinding behavior
- simulator mode smoke tests

## Repo Hygiene

The repository ignores generated/local artifacts such as:

- `build/`
- `.gradle/`
- `simulation.log`
- `simulation-metrics.csv`
- local `.class` files

If generated files are already tracked in git, remove them from the repository once and keep them ignored afterward.

## Suggested Next Work

- enforce battery-feasible task acceptance
- compute dynamic critical threshold based on safe fallback path
- fix any pallet stranding edge cases
- compare current behavior against the stricter battery-safe version
