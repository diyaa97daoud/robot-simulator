# Robot Simulator

Java/Gradle warehouse simulator for the 2026 multi-agent programming project.

## Current Branch Performance Snapshot

The latest suite execution in this branch was interrupted before completion.

### Completed Coverage

- Expected files: 120
- Generated files: 13
- Completion: 10.83%
- Seeds covered so far: 150, 151
- Rates covered so far: 300, 400, 500

### Partial Results Collected So Far

| Mode | Rate | Fleet | Runs | Delivered Avg | Avg Delivery Time | Recharge Avg |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| OPTIMIZED | 300 | 4 | 1 | 8.00 | 112.38 | 4.00 |
| OPTIMIZED | 300 | 5 | 1 | 7.00 | 101.29 | 3.00 |
| OPTIMIZED | 300 | 6 | 1 | 8.00 | 86.00 | 3.00 |
| OPTIMIZED | 400 | 4 | 1 | 8.00 | 121.62 | 4.00 |
| OPTIMIZED | 400 | 5 | 1 | 7.00 | 87.71 | 2.00 |
| OPTIMIZED | 400 | 6 | 1 | 8.00 | 75.00 | 2.00 |
| OPTIMIZED | 500 | 4 | 1 | 8.00 | 123.50 | 4.00 |
| OPTIMIZED | 500 | 5 | 1 | 7.00 | 88.43 | 2.00 |
| OPTIMIZED | 500 | 6 | 1 | 8.00 | 76.88 | 2.00 |
| REFERENCE | 300 | 6 | 2 | 76.00 | 47.88 | 0.00 |
| REFERENCE | 400 | 6 | 1 | 114.00 | 46.95 | 0.00 |
| REFERENCE | 500 | 6 | 1 | 147.00 | 47.40 | 0.00 |

### Analysis

- These numbers are provisional because only 13/120 files were generated.
- Even in the partial sample, reference remains much higher in delivered count, with lower average delivery time.
- Optimized runs show non-zero recharging in all collected cases, while reference stays at zero by design.

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
  - recharge while carrying pallet support
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

Default suite settings in this branch:

- 10 seeds (`baseSeed` to `baseSeed + 9`)
- 300 simulation steps per run

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
- `[communication]`: communication mode (`broadcast` or `dyadic`)
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
- dynamic battery threshold logic for best fallback routing is still heuristic-based

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
