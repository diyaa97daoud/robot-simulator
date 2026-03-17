# Robot Simulator

Java/Gradle warehouse simulator for the 2026 multi-agent programming project.

## Model Branch Map

This `main` branch now corresponds to **Model 2**, the direct-delivery controller with carry-through recharge, after merging `feature/carrying-pallet-to-charging-zones`.

The three reported optimized models are linked to branches as follows:

- **Model 1**: `feature/warehouse-simulator-ui-baseline`
  - Heuristic bid/claim coordination with intermediate-area fallback.
- **Model 2**: `main`
  - Current default branch.
  - Direct delivery with carry-through recharge and no intermediate usage in practice.
- **Model 3**: `feature/low-threshold-with-hard-pickup-feasibility`
  - Hard battery-feasible direct-delivery bidding that removes intermediate usage by rejecting risky assignments before pickup.

If you want to compare the controller variants directly, switch branches and run the same suite configuration on each branch.

## Current Branch Performance Snapshot

The following results summarize the last suite run recorded on this branch using suite defaults (10 seeds, 300 steps per run). Re-run the suite after the latest carry-to-charge direct-delivery changes to refresh these numbers.

### Reference vs Best Optimized (Mean Over Seeds)

| Arrival Rate | Reference Delivered | Best Optimized Delivered | Best Optimized Fleet | Reference Avg Delivery Time | Best Optimized Avg Delivery Time |
| ------------ | ------------------: | -----------------------: | -------------------: | --------------------------: | -------------------------------: |
| 300          |               74.00 |                    14.60 |                    6 |                       48.05 |                            97.44 |
| 400          |              101.80 |                    15.50 |                    6 |                       47.58 |                            91.21 |
| 500          |              132.00 |                    15.30 |                    6 |                       47.77 |                            92.08 |

### Analysis

- The optimized strategy remains substantially below reference throughput in this short-horizon setting.
- Relative optimized throughput decreases as arrival rate increases (about 19.7% at rate 300, 15.2% at rate 400, 11.6% at rate 500).
- Fleet size 6 is consistently the best optimized fleet, but delivered pallets still saturate around 15 in 300 steps.
- Average optimized delivery time is roughly double the reference value, which aligns with more conservative battery behavior and more frequent charging.

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
  - direct-delivery assignments
  - recharge while carrying pallet support
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
6. Deliver pallets to exits, or temporarily recharge while still carrying the pallet if needed.
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
- the controller is still heuristic-based and does not enforce hard pre-bid battery feasibility
- recharge-zone contention may increase because carrying robots keep ownership of pallets while charging

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
