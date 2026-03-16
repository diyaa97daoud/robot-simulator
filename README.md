# Robot Simulator

Java/Gradle warehouse simulator for the 2026 multi-agent programming project.

## Current Branch Performance Snapshot

This branch evaluates a direct-only, battery-safe policy intended to reduce decision overhead and shorten delivery paths.

### Suite Setup

- Expected files: 120
- Generated files: 120
- Completion: 100%
- Seeds covered: 150 to 159
- Rates covered: 300, 400, 500

### Reference vs Best Optimized (Mean Over Seeds)

| Arrival Rate | Reference Delivered | Best Optimized Delivered | Best Optimized Fleet | Reference Avg Delivery Time | Best Optimized Avg Delivery Time |
| ------------ | ------------------: | -----------------------: | -------------------: | --------------------------: | -------------------------------: |
| 300          |               74.00 |                     6.20 |                    6 |                       48.05 |                            59.77 |
| 400          |              101.80 |                     6.20 |                    6 |                       47.58 |                            56.55 |
| 500          |              132.00 |                     6.20 |                    6 |                       47.77 |                            60.14 |

### Best Optimized Operational Metrics

| Arrival Rate | Fleet | Recharge Avg | Blocked Conflicts Avg | Messages Avg | Intermediate Occupancy Avg |
| ------------ | ----: | -----------: | --------------------: | -----------: | -------------------------: |
| 300          |     6 |         0.70 |                917.30 |        27.00 |                      0.000 |
| 400          |     6 |         0.30 |                957.20 |        26.10 |                      0.000 |
| 500          |     6 |         0.60 |                938.40 |        26.50 |                      0.000 |

### Analysis

- This policy did reduce recharge usage and kept average delivery time closer to the reference model than previous optimized variants.
- However, throughput collapsed: delivered pallets stayed around 6.2 regardless of arrival rate.
- The main failure mode is congestion. Blocked movement conflicts are extremely high, which means robots spend much of the run blocking one another rather than completing deliveries.
- Intermediate occupancy stayed at zero, confirming that this branch behaves as a direct-only policy.
- For report comparison, this branch is a useful negative result: removing intermediate handling and aggressively simplifying dispatch reduced recharge overhead but significantly worsened throughput.

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
  - direct-only battery-safe task acceptance in this branch
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
6. Deliver pallets directly to exits.
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
- local conflict handling is still too weak under dense direct-only dispatch
- robots can converge on the same corridors and lose throughput through repeated blocking
- this branch does not exploit intermediate buffering, which limits its ability to reduce congestion

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

- add anti-congestion task selection or corridor-aware dispatch
- reintroduce selective intermediate usage only when it reduces conflicts
- compare this direct-only policy against the safer hybrid policy and the no-intermediate baseline
