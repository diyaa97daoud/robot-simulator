# Model 1 Report (First Optimized Idea)

## Possible model name
- **Model 1: Initial Optimized Coordination Model**
- Alternative names: **First Optimized Prototype**, **Bid/Claim Optimized Model v1**

## Model description
This model is the first implemented extension of the reference idea, represented by **OPTIMIZED** mode (`SimulationMode.OPTIMIZED`).  
It introduces a fixed AMR fleet, communication-based task assignment, intermediate storage handling, battery/recharge behavior, and local motion conflict handling.

## Static configuration (used for this model)
Based on `configuration.ini`:

```ini
[simulation]
mode = optimized
steps = 1500
seed = 150
amrCount = 6

[warehouse]
rows = 30
columns = 45

[arrivals]
distribution = poisson
rate = 0.40

[communication]
mode = broadcast

[battery]
maxBattery = 80
criticalThreshold = 12
warningThreshold = 24
safeMargin = 8
rechargeDuration = 12
rechargeCapacity = 2

[zones]
entries = A1:3,2|A2:3,14|A3:3,24
exits = Z1:40,3|Z2:40,25
intermediates = I1:22,11:3|I2:22,20:3
recharge = R1:21,16:2
```

## What changed vs the reference model
- Fleet changed from "one AMR per pallet" to a **fixed shared fleet** (`amrCount`).
- AMRs now **communicate** through bid/claim messages (`BROADCAST` and `DYADIC` support in code).
- **Intermediate zones** are used to temporarily store pallets when beneficial.
- **Battery is tracked** and robots can move to recharge with capacity and duration constraints.
- **Local movement conflict resolution** is applied each step to avoid collisions.

## Main assumptions
- AMRs make local bidding decisions, while assignment arbitration is executed in simulator logic.
- Path cost and travel are computed on a discrete grid with static + human dynamic obstacles.
- Recharge is limited by recharge zone capacity and recharge duration.
- Intermediate zones have finite capacities and occupancy penalties.
- In this branch, battery-feasibility is not yet a strict hard constraint before accepting tasks.

## Algorithms used
- **Arrival generation:** stochastic pallet generation (`poisson`, `binomial`, `geometric`) via `PalletGenerator`.
- **Pathfinding:** BFS shortest-path (`GridPathfinder.shortestDistance`) and next-step routing (`nextStep`).
- **Task allocation (broadcast path):**
  - Each idle robot computes a best bid using score:
  - `score = pickupCost + deliveryCost + congestionPenalty + batteryPenalty + queuePenalty`
  - Winning tie-break uses score, then task load, then robot ID.
- **Task allocation (dyadic path):**
  - Idle robot picks local best pallet and sends a targeted claim to nearest idle peer.
- **Motion conflict handling:** same-cell contention resolved by priority (carrying > recharge urgency > lower load > lower ID).
- **Battery/recharge handling:** threshold-based transition to recharge, capacity-limited charging slots, timed full recharge.

## Performance and results (suite evidence)
### How suite results are derived
`--suite` runs `ExperimentSuite.runDefaultComparison()` with:
- **Seeds**: `baseSeed`, `baseSeed+1`, `baseSeed+2` → from config seed 150: **150, 151, 152**
- **Arrival rates**: `0.75x`, `1.00x`, `1.25x` of base rate 0.40, clipped to `[0.1, 0.95]`  
  → **0.30, 0.40, 0.50**, shown in README as **300, 400, 500** (`rate * 1000`)
- **Optimized fleets tested**: `amrCount-2`, `amrCount-1`, `amrCount` with minimum 2  
  → from base `amrCount=6`: **4, 5, 6**
- **Scenarios**: `3 seeds x 3 rates x (1 reference + 3 optimized fleets) = 36` runs total.

The README table reports, for each arrival rate:
- mean reference metrics over seeds,
- mean optimized metrics over seeds for the **best optimized fleet** (here fleet 6).

### Detailed reported means (from README snapshot)
| Arrival Rate (x1000) | Reference Delivered | Best Optimized Delivered | Best Optimized Fleet | Reference Backlog | Best Optimized Backlog |
|---|---:|---:|---:|---:|---:|
| 300 | 437.00 | 80.33 | 6 | 18.00 | 374.67 |
| 400 | 580.00 | 81.33 | 6 | 15.00 | 513.67 |
| 500 | 716.33 | 82.33 | 6 | 30.00 | 664.00 |

### Meaning of suite variables/metrics
- **mode**: simulation mode (`REFERENCE` or `OPTIMIZED`).
- **seed**: random seed controlling arrivals and stochastic choices.
- **amrCount**: number of robots used in the scenario (optimized fleet size in optimized runs).
- **delivered**: number of pallets delivered before simulation end.
- **undeliveredBacklog**: number of pallets not delivered at simulation end.
- **totalDeliveryTime**: sum of `(deliveryStep - arrivalStep)` over delivered pallets.
- **avgDeliveryTime**: `totalDeliveryTime / delivered`.
- **totalDistance**: total robot movement distance (grid steps).
- **rechargeCount**: number of recharge sessions started.
- **rechargeWaitSteps**: cumulative steps spent waiting for recharge availability.
- **blockedConflicts**: number of movement conflicts where a robot was blocked by conflict resolution.
- **messagesSent**: number of bid/claim messages emitted.
- **avgIntermediateOccupancy**: average occupancy sampled over intermediate zones across steps.

Interpretation:
- Delivered count saturates around ~80 for the first optimized model even as demand rises.
- Backlog increases sharply with demand, showing the model is implemented but currently underperforming versus reference in this branch.
