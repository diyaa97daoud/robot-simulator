# Strict Battery-Feasible Direct-Delivery Model

## 1. Objective of the Studied Model

This document describes the third warehouse-control model currently implemented in optimized mode. Its purpose is to make task allocation more conservative by accepting only pallet assignments that are immediately battery-feasible for direct delivery. In contrast with the earlier optimized variants, this model disables effective use of intermediate storage and focuses on preventing unsafe assignments before pickup rather than recovering from them later.

The analysis below is based on the archived comparison-suite outputs in the analyzed run, the current simulator logic for optimized mode, and the automated validation tests available in the project.

## 2. Reference Model and Studied Variant

The reference model remains an idealized baseline in which each pallet is virtually handled without persistent robot constraints, whereas the studied variant introduces a persistent shared fleet with decentralized communication, explicit battery feasibility checks, recharge handling, and effectively direct-only delivery behavior. The comparison is summarized below.

| Criterion                                 | Reference model                                                                                                             | Studied variant                                                                                   |
| ----------------------------------------- | --------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------- |
| Number of AMRs equal to number of pallets | Each waiting pallet is handled independently and virtually scheduled for direct delivery, which emulates one AMR per pallet | No; a fixed shared fleet is used                                                                  |
| Robot persistence                         | No persistent robot state                                                                                                   | Persistent AMR fleet                                                                              |
| AMR communication                         | None                                                                                                                        | Broadcast-based decentralized bidding and claim exchange                                          |
| AMR disappears when pallet is delivered   | Effectively yes, because no persistent robot state is maintained                                                            | No                                                                                                |
| Use of intermediate areas                 | No                                                                                                                          | Intermediate areas remain configured, but the current control logic effectively does not use them |
| Battery management                        | No                                                                                                                          | Enabled with stricter feasibility filtering                                                       |
| Recharging area                           | Not used                                                                                                                    | Enabled                                                                                           |
| Battery update                            | Not used                                                                                                                    | Enabled                                                                                           |
| Main design intention                     | Provide an idealized unconstrained baseline                                                                                 | Accept only battery-safe direct deliveries and avoid unsafe task commitments                      |

## 3. Configuration of the Analyzed Experiments

### 3.1 Environment Configuration

| Parameter          | Value                                                                    |
| ------------------ | ------------------------------------------------------------------------ |
| Grid size          | 30 rows x 45 columns                                                     |
| Entry zones        | A1 `(3,2)`, A2 `(3,14)`, A3 `(3,24)`                                     |
| Exit zones         | Z1 `(40,3)`, Z2 `(40,25)`                                                |
| Intermediate zones | I1 `(22,11)` capacity 3, I2 `(22,20)` capacity 3                         |
| Recharge zone      | R1 `(21,16)` capacity 2                                                  |
| Fixed obstacles    | `(10,7)`, `(14,7)`, `(18,10)`, `(10,18)`, `(16,21)`, `(30,7)`, `(30,24)` |
| Human obstacles    | `(21,8)`, `(24,10)`, `(17,24)`                                           |

### 3.2 Operational Configuration

| Parameter                  | Value                 |
| -------------------------- | --------------------- |
| Base simulation mode       | Optimized             |
| Base fleet size            | 6 AMRs                |
| Communication mode         | Broadcast             |
| Arrival distribution       | Poisson               |
| Base arrival rate          | 0.40 pallets per step |
| Battery capacity           | 80                    |
| Critical threshold         | 12                    |
| Warning threshold          | 24                    |
| Safe battery margin        | 8                     |
| Recharge duration          | 12 steps              |
| Recharge capacity          | 2 robots              |
| Routing policy in practice | Direct delivery only  |

### 3.3 Suite Protocol

The analyzed suite performs a controlled comparison across multiple seeds, arrival rates, and fleet sizes.

| Parameter                    | Value                             |
| ---------------------------- | --------------------------------- |
| Number of seeds              | 10                                |
| Seeds used                   | 150 to 159                        |
| Steps per run                | 300                               |
| Arrival-rate tags            | 300, 400, 500                     |
| Interpreted arrival rates    | 0.30, 0.40, 0.50 pallets per step |
| Optimized fleet sizes tested | 4, 5, 6                           |
| Reference runs               | 30                                |
| Optimized runs               | 90                                |
| Total runs                   | 120                               |

## 4. Assumed Evaluation Criteria

The following criteria were used to assess the third model.

| Criterion                   | Interpretation                                                   |
| --------------------------- | ---------------------------------------------------------------- |
| Throughput                  | Number of pallets delivered within the 300-step horizon          |
| Responsiveness              | Average pallet delivery time                                     |
| Travel effort               | Total AMR travel distance                                        |
| Energy pressure             | Number of recharge events and recharge waiting steps             |
| Coordination overhead       | Number of messages sent                                          |
| Congestion                  | Number of blocked movement conflicts                             |
| Intermediate-area usage     | Average intermediate occupancy                                   |
| Task-admission conservatism | Extent to which the controller rejects risky tasks before pickup |

For this model, success is defined less by maximizing throughput and more by ensuring that only battery-feasible tasks are admitted. This makes the design intentionally conservative.

## 5. Implemented Changes Relative to the Reference Model

| Aspect                 | Reference model            | Studied variant                                                                   |
| ---------------------- | -------------------------- | --------------------------------------------------------------------------------- |
| Task allocation        | Immediate direct handling  | Decentralized bid and claim assignment with battery-feasibility filtering         |
| Robot persistence      | No persistent fleet        | Persistent fleet of AMRs                                                          |
| Communication          | None                       | Broadcast coordination                                                            |
| Battery                | Ignored                    | Tracked at each robot move and checked before assignment                          |
| Recharge               | Absent                     | Dedicated recharge area with limited capacity                                     |
| Routing                | Direct only                | Direct delivery only in practice, despite configured intermediates                |
| Intermediate retrieval | Not applicable             | Disabled in practice because idle robots do not claim stored intermediate pallets |
| Congestion handling    | Not modeled at robot level | Local movement conflict resolution                                                |

The main change introduced by this third model is the replacement of heuristic battery penalties with a stricter admission rule. A robot only bids for a pallet if it already has enough battery to reach the pickup point and then either complete the direct delivery or at least reach the recharge zone safely, while still respecting the configured safety margin.

## 6. Algorithms Used

### 6.1 Path Planning

Path planning is based on breadth-first search over a 4-neighbor grid. The same mechanism is used both to estimate shortest travel distances and to select the next movement step.

### 6.2 Decentralized Task Allocation

The optimized mode uses a decentralized bid and claim mechanism:

1. Idle robots evaluate waiting pallets.
2. Each robot computes a feasible bid only if the battery condition is satisfied.
3. Bids are broadcast.
4. Winning bids are resolved per pallet.
5. Claims assign selected robots to pallets.

The current bid score is simplified to a direct-delivery estimate:

$$
\text{score} = \text{pickupCost} + \text{directDeliveryCost} + \text{congestionPenalty}
$$

However, before a bid is accepted, the robot must satisfy the feasibility rule:

$$
\text{battery} \ge \text{pickupCost} + \min(\text{directDeliveryCost}, \text{carryToRechargeCost}) + \text{safeMargin}
$$

If this condition is not met, the robot does not bid for that pallet.

### 6.3 Intermediate Storage Logic

Although intermediate zones still exist in the configuration, the current optimized logic no longer uses them in practice. The bid computation only targets exit zones, and the idle-state intermediate reclamation function immediately returns to idle without assigning stored pallets. Empirically, this produces zero intermediate occupancy across all optimized runs in the analyzed suite.

### 6.4 Battery and Recharge Policy

The battery-management policy still uses the configured thresholds.

| Threshold               | Effect                                                                                                                                                        |
| ----------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Critical threshold = 12 | Robots at or below this level are redirected to recharge                                                                                                      |
| Warning threshold = 24  | Present in configuration, but the dominant effect in this model comes from pre-assignment feasibility checks rather than warning-based opportunistic recovery |

Charging is capacity-limited and takes 12 simulation steps. Unlike earlier variants, this model tries to prevent unsafe commitments before they occur instead of depending on intermediate storage or post-hoc correction.

### 6.5 Conflict Resolution

If several robots propose the same next cell, priority is assigned according to the following order:

1. Robots carrying a pallet.
2. Robots moving to recharge with lower battery level.
3. Robots with lower current task load.
4. Lower robot identifier.

## 7. Validation and Testing

Two validation layers support this analysis.

### 7.1 Automated Unit Tests

The project contains automated JUnit tests covering:

| Test scope              | Purpose                                                        |
| ----------------------- | -------------------------------------------------------------- |
| Configuration parsing   | Ensures scenario parameters are read correctly                 |
| Grid pathfinding        | Verifies obstacle-aware shortest-path behavior                 |
| Reference mode behavior | Confirms the absence of communication in reference mode        |
| Optimized mode behavior | Confirms that communication and recharge mechanisms are active |
| Recharge while carrying | Verifies that carrying-state recharge behavior is supported    |

All currently available automated tests passed successfully:

| Metric          | Result |
| --------------- | ------ |
| Total tests run | 9      |
| Passed          | 9      |
| Failed          | 0      |

### 7.2 Experimental Validation

The main empirical validation is the archived comparison suite used for this report. Metrics were computed from the exported CSV files generated by the suite rather than from UI inspection.

## 8. Experimental Results

### 8.1 Reference Model Performance

Values are reported as means over 10 seeds.

| Arrival-rate tag | Effective rate | Delivered pallets | Average delivery time | Total distance | Recharge count | Messages sent | Avg. intermediate occupancy |
| ---------------- | -------------- | ----------------- | --------------------- | -------------- | -------------- | ------------- | --------------------------- |
| 300              | 0.30           | 74.00             | 48.05                 | 3556.80        | 0.00           | 0.00          | 0.000                       |
| 400              | 0.40           | 101.80            | 47.58                 | 4842.60        | 0.00           | 0.00          | 0.000                       |
| 500              | 0.50           | 132.00            | 47.77                 | 6307.40        | 0.00           | 0.00          | 0.000                       |

The reference model again outperforms the constrained optimized model because it ignores fleet sharing, charging, and robot-level congestion.

### 8.2 Optimized Variant Performance by Fleet Size

Values are mean values over 10 seeds.

| Arrival-rate tag | Effective rate | Fleet | Delivered pallets | Average delivery time | Total distance | Recharge count | Recharge wait steps | Blocked conflicts | Messages sent | Avg. intermediate occupancy |
| ---------------- | -------------- | ----- | ----------------- | --------------------- | -------------- | -------------- | ------------------- | ----------------- | ------------- | --------------------------- |
| 300              | 0.30           | 4     | 4.00              | 54.00                 | 215.20         | 0.10           | 0.00                | 524.70            | 14.00         | 0.000                       |
| 300              | 0.30           | 5     | 5.10              | 57.06                 | 283.00         | 0.40           | 0.00                | 705.40            | 20.10         | 0.000                       |
| 300              | 0.30           | 6     | 6.20              | 59.77                 | 352.60         | 0.70           | 0.00                | 917.30            | 27.00         | 0.000                       |
| 400              | 0.40           | 4     | 4.00              | 56.50                 | 223.60         | 0.20           | 0.00                | 498.40            | 13.80         | 0.000                       |
| 400              | 0.40           | 5     | 5.20              | 57.06                 | 285.00         | 0.30           | 0.00                | 741.80            | 20.10         | 0.000                       |
| 400              | 0.40           | 6     | 6.20              | 56.55                 | 337.00         | 0.30           | 0.00                | 957.20            | 26.10         | 0.000                       |
| 500              | 0.50           | 4     | 4.00              | 58.35                 | 229.60         | 0.30           | 0.00                | 483.30            | 13.80         | 0.000                       |
| 500              | 0.50           | 5     | 5.20              | 57.85                 | 290.00         | 0.40           | 0.00                | 711.80            | 20.10         | 0.000                       |
| 500              | 0.50           | 6     | 6.20              | 60.14                 | 354.00         | 0.60           | 0.00                | 938.40            | 26.50         | 0.000                       |

### 8.3 Best Optimized Configuration Compared With the Reference

For every tested arrival rate, the best optimized throughput is obtained with fleet size 6.

| Arrival-rate tag | Best optimized fleet | Optimized delivered | Reference delivered | Throughput vs. reference | Optimized avg. delivery time | Reference avg. delivery time | Time ratio |
| ---------------- | -------------------- | ------------------- | ------------------- | ------------------------ | ---------------------------- | ---------------------------- | ---------- |
| 300              | 6                    | 6.20                | 74.00               | 8.38%                    | 59.77                        | 48.05                        | 1.24x      |
| 400              | 6                    | 6.20                | 101.80              | 6.09%                    | 56.55                        | 47.58                        | 1.19x      |
| 500              | 6                    | 6.20                | 132.00              | 4.70%                    | 60.14                        | 47.77                        | 1.26x      |

### 8.4 Statistical View of the Best Optimized Configuration

Values are mean +- standard deviation over 10 seeds.

| Arrival-rate tag | Fleet | Delivered pallets | Average delivery time | Total distance  | Recharge count | Recharge wait steps | Blocked conflicts | Messages sent | Avg. intermediate occupancy |
| ---------------- | ----- | ----------------- | --------------------- | --------------- | -------------- | ------------------- | ----------------- | ------------- | --------------------------- |
| 300              | 6     | 6.20 +- 0.42      | 59.77 +- 13.18        | 352.60 +- 75.07 | 0.70 +- 0.82   | 0.00 +- 0.00        | 917.30 +- 67.67   | 27.00 +- 0.94 | 0.00 +- 0.00                |
| 400              | 6     | 6.20 +- 0.42      | 56.55 +- 12.21        | 337.00 +- 71.75 | 0.30 +- 0.67   | 0.00 +- 0.00        | 957.20 +- 84.07   | 26.10 +- 1.66 | 0.00 +- 0.00                |
| 500              | 6     | 6.20 +- 0.42      | 60.14 +- 13.87        | 354.00 +- 76.57 | 0.60 +- 0.84   | 0.00 +- 0.00        | 938.40 +- 94.49   | 26.50 +- 1.27 | 0.00 +- 0.00                |

### 8.5 Evidence Concerning Intermediate-Area Usage

This model effectively disables intermediate usage in practice.

| Indicator                                           | Value   |
| --------------------------------------------------- | ------- |
| Optimized runs with zero intermediate occupancy     | 90 / 90 |
| Optimized runs with non-zero intermediate occupancy | 0 / 90  |
| Maximum observed average intermediate occupancy     | 0.000   |

This is a strong empirical confirmation that the third model behaves as a strict direct-delivery variant, despite the continued presence of intermediate zones in the environment configuration.

## 9. Analysis and Discussion

Several conclusions follow from the suite results.

First, the third model successfully enforces a very strict safety-oriented admission policy. No optimized run uses intermediate storage, recharge waiting is effectively absent, and recharge counts remain extremely low. This indicates that robots rarely enter risky energy states because the controller filters tasks before they are accepted.

Second, this safety comes at a severe throughput cost. Even with fleet size 6, the model delivers only 6.2 pallets on average in 300 steps, compared with 74.0 to 132.0 for the reference model. Throughput falls to between 4.70% and 8.38% of the baseline, which is substantially worse than the earlier optimized variants.

Third, average delivery time for the few completed tasks remains only moderately above the reference model. This is an important detail: once the model commits to a task, it tends to complete it efficiently. The main weakness is therefore not slow execution of accepted jobs, but excessively restrictive task admission.

Fourth, blocked-conflict counts are extremely high relative to the number of completed deliveries. This suggests that many robot movements still occur without translating into productive throughput. The fleet therefore spends significant effort in contention and repositioning despite accepting very few tasks.

Fifth, adding more robots improves throughput only from roughly 4 delivered pallets to roughly 6.2 delivered pallets, while blocked conflicts increase sharply. The third model therefore demonstrates that a stricter safety rule alone is not sufficient to produce an efficient decentralized coordination mechanism.

Finally, the complete disappearance of intermediate usage shows that the model does achieve strict direct-delivery behavior. However, it does so by becoming too conservative to sustain meaningful warehouse throughput under the tested demand levels.

## 10. Main Findings

| Finding                                                              | Interpretation                                                     |
| -------------------------------------------------------------------- | ------------------------------------------------------------------ |
| Fleet size 6 is consistently the best optimized fleet                | More robots still help slightly                                    |
| Throughput collapses to about 6.2 delivered pallets                  | Strict battery-feasibility filtering is overly conservative        |
| Delivery times remain only modestly above reference                  | Accepted tasks are handled reasonably efficiently                  |
| Recharge counts and waiting are near zero                            | The controller avoids risky energy commitments before assignment   |
| Intermediate occupancy is zero in all optimized runs                 | The model behaves as a strict direct-delivery variant              |
| Blocked conflicts remain very high                                   | Low throughput is accompanied by substantial movement interference |
| Throughput decreases sharply relative to previous optimized variants | Safety improved, but system efficiency degraded drastically        |

## 11. Conclusion

The third model can be characterized as a strict battery-feasible direct-delivery coordination model. Its main contribution is the introduction of a hard pre-assignment feasibility filter that prevents robots from accepting unsafe pallet tasks and, in effect, removes intermediate storage from operational use.

From a safety perspective, the model behaves consistently with its design objective. It eliminates intermediate usage, keeps recharge pressure very low, and avoids the unstable task commitments that motivated the redesign. From a performance perspective, however, it is far too conservative. Throughput collapses, fleet scaling becomes ineffective, and congestion remains high relative to useful work. The third model therefore serves as an important negative result: strict feasibility enforcement can improve operational safety, but by itself it does not yield a competitive decentralized coordination strategy for minimizing total pallet delivery time.

## 12. Suggested Report Positioning

A precise academic wording for this model is the following:

> The third optimized model replaces heuristic battery-aware coordination with a stricter task-admission policy in which robots only accept pallet assignments that are immediately battery-feasible for direct delivery. In practice, this suppresses all intermediate-area usage and greatly reduces recharge activity. Experimental results show that the model is operationally safer and more conservative than earlier optimized variants, but also drastically less productive. It therefore illustrates the trade-off between safety-oriented feasibility enforcement and overall warehouse throughput.
