# Initial Optimized Coordination Model

## 1. Objective of the Studied Model

This document describes the first optimized warehouse-control model developed as an extension of the reference simulation. Its purpose is to move from the idealized baseline toward a more realistic multi-agent setting by introducing a persistent AMR fleet, decentralized coordination, battery-aware routing, recharge constraints, and the use of intermediate storage areas. The model represents the first complete attempt to coordinate a limited number of AMRs under operational constraints rather than assuming one virtual robot per pallet.

The analysis below is based on the archived comparison-suite outputs, the current simulator logic for optimized mode, and the automated validation tests available in the project.

## 2. Reference Model and Studied Variant

The reference model acts as an unconstrained baseline in which pallets are delivered directly to their destination exits without persistent robot management, while the studied variant introduces a shared robot fleet and decentralized control under charging, congestion, and storage constraints. The comparison is summarized below.

| Criterion                                 | Reference model                                                                                                             | Studied variant                                                                                   |
| ----------------------------------------- | --------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------- |
| Number of AMRs equal to number of pallets | Each waiting pallet is handled independently and virtually scheduled for direct delivery, which emulates one AMR per pallet | No; a fixed shared fleet is used                                                                  |
| Robot persistence                         | No persistent robot state                                                                                                   | Persistent AMR fleet                                                                              |
| AMR communication                         | None                                                                                                                        | Communication-based bid and claim coordination                                                    |
| AMR disappears when pallet is delivered   | Effectively yes, because no persistent robot state is maintained                                                            | No                                                                                                |
| Use of intermediate areas                 | No                                                                                                                          | Yes, intermediate areas may be used as temporary storage                                          |
| Battery management                        | No                                                                                                                          | Enabled                                                                                           |
| Recharging area                           | Not used                                                                                                                    | Enabled                                                                                           |
| Battery update                            | Not used                                                                                                                    | Enabled                                                                                           |
| Main design intention                     | Provide an idealized baseline for comparison                                                                                | Coordinate a constrained fleet using communication, recharge management, and intermediate storage |

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

| Parameter            | Value                                             |
| -------------------- | ------------------------------------------------- |
| Base simulation mode | Optimized                                         |
| Base fleet size      | 6 AMRs                                            |
| Communication mode   | Broadcast                                         |
| Arrival distribution | Poisson                                           |
| Base arrival rate    | 0.40 pallets per step                             |
| Battery capacity     | 80                                                |
| Critical threshold   | 12                                                |
| Warning threshold    | 24                                                |
| Safe battery margin  | 8                                                 |
| Recharge duration    | 12 steps                                          |
| Recharge capacity    | 2 robots                                          |
| Main routing options | Direct delivery or temporary intermediate storage |

### 3.3 Suite Protocol

The analyzed suite performs a controlled comparison across multiple seeds, arrival rates, and fleet sizes.

| Parameter                    | Value                             |
| ---------------------------- | --------------------------------- |
| Number of seeds              | 3                                 |
| Seeds used                   | 150 to 152                        |
| Steps per run                | 1500                              |
| Arrival-rate tags            | 300, 400, 500                     |
| Interpreted arrival rates    | 0.30, 0.40, 0.50 pallets per step |
| Optimized fleet sizes tested | 4, 5, 6                           |
| Reference runs               | 9                                 |
| Optimized runs               | 27                                |
| Total runs                   | 36                                |

## 4. Assumed Evaluation Criteria

The following criteria were used to assess the first optimized model.

| Criterion               | Interpretation                                           |
| ----------------------- | -------------------------------------------------------- |
| Throughput              | Number of pallets delivered within the 1500-step horizon |
| Backlog                 | Number of pallets left undelivered at the end of the run |
| Responsiveness          | Average pallet delivery time                             |
| Travel effort           | Total AMR travel distance                                |
| Energy pressure         | Number of recharge events and recharge waiting steps     |
| Coordination overhead   | Number of messages sent                                  |
| Congestion              | Number of blocked movement conflicts                     |
| Intermediate-area usage | Average intermediate occupancy                           |

For this first optimized model, successful behavior is not defined by suppressing intermediate usage. On the contrary, intermediate storage is an intended coordination mechanism and should therefore be interpreted as part of the design rather than as a failure mode.

## 5. Implemented Changes Relative to the Reference Model

| Aspect              | Reference model            | Studied variant                                                   |
| ------------------- | -------------------------- | ----------------------------------------------------------------- |
| Task allocation     | Immediate direct handling  | Decentralized bid and claim assignment                            |
| Robot persistence   | No persistent fleet        | Persistent fleet of AMRs                                          |
| Communication       | None                       | Broadcast coordination, with dyadic support in the implementation |
| Battery             | Ignored                    | Tracked at each robot move                                        |
| Recharge            | Absent                     | Dedicated recharge area with limited capacity                     |
| Routing             | Direct only                | Direct delivery or intermediate routing                           |
| Congestion handling | Not modeled at robot level | Local movement conflict resolution                                |

The key innovation of this first model is the introduction of a realistic constrained AMR fleet that must balance task assignment, charging, and path conflicts while still attempting to maintain throughput.

## 6. Algorithms Used

### 6.1 Path Planning

Path planning is based on breadth-first search over a 4-neighbor grid. The same mechanism is used both to estimate shortest travel distances and to select the next movement step.

### 6.2 Decentralized Task Allocation

The optimized mode uses a decentralized bid and claim mechanism:

1. Idle robots evaluate waiting pallets.
2. Each robot computes its best bid according to a local score.
3. Bids are broadcast.
4. Winning bids are resolved per pallet.
5. Claims assign selected robots to pallets.

The bid score combines distance, congestion, battery, and storage terms:

$$
	ext{score} =
	ext{pickupCost} +
	ext{deliveryCost} +
	ext{congestionPenalty} +
	ext{batteryPenalty} +
	ext{queuePenalty}
$$

where the queue term penalizes occupied intermediate zones and the battery term penalizes assignments that appear unsafe under the current charge level.

### 6.3 Intermediate Storage Logic

For each pallet, the controller compares direct delivery with routing through the best intermediate zone. Intermediate storage is selected when it is estimated to be more advantageous or when battery pressure makes direct delivery less desirable. Stored pallets can later be reclaimed by nearby idle robots.

### 6.4 Battery and Recharge Policy

The battery-management policy uses two thresholds.

| Threshold               | Effect                                                                |
| ----------------------- | --------------------------------------------------------------------- |
| Critical threshold = 12 | Idle robots at or below this level are redirected to recharge         |
| Warning threshold = 24  | Idle or waiting robots below this level are also directed to recharge |

Charging is capacity-limited, and a recharge requires 12 simulation steps. The first optimized model does not enforce battery feasibility as a strict hard constraint before every pickup, which is an important limitation when interpreting the results.

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

| Test scope              | Purpose                                                                  |
| ----------------------- | ------------------------------------------------------------------------ |
| Configuration parsing   | Ensures scenario parameters are read correctly                           |
| Grid pathfinding        | Verifies obstacle-aware shortest-path behavior                           |
| Reference mode behavior | Confirms the absence of communication in reference mode                  |
| Optimized mode behavior | Confirms that communication and recharge mechanisms are active           |
| Recharge behavior       | Confirms that optimized-mode recharge handling is exercised by the tests |

All currently available automated tests passed successfully:

| Metric          | Result |
| --------------- | ------ |
| Total tests run | 8      |
| Passed          | 8      |
| Failed          | 0      |

### 7.2 Experimental Validation

The main empirical validation is the archived comparison suite used for this report. Metrics were computed from the exported CSV files generated by the suite rather than from UI inspection.

## 8. Experimental Results

### 8.1 Reference Model Performance

Values are reported as means over 3 seeds.

| Arrival-rate tag | Effective rate | Delivered pallets | Undelivered backlog | Average delivery time | Total distance |
| ---------------- | -------------- | ----------------- | ------------------- | --------------------- | -------------- |
| 300              | 0.30           | 437.00            | 18.00               | 48.51                 | 21198.00       |
| 400              | 0.40           | 580.00            | 15.00               | 48.08                 | 27882.00       |
| 500              | 0.50           | 716.33            | 30.00               | 48.34                 | 34630.67       |

The reference model maintains very high throughput because it does not include fleet-sharing, charging, congestion resolution, or communication overhead.

### 8.2 Optimized Variant Performance by Fleet Size

Values are mean values over 3 seeds.

| Arrival-rate tag | Effective rate | Fleet | Delivered pallets | Undelivered backlog | Average delivery time | Total distance | Recharge count | Blocked conflicts | Messages sent | Avg. intermediate occupancy |
| ---------------- | -------------- | ----- | ----------------- | ------------------- | --------------------- | -------------- | -------------- | ----------------- | ------------- | --------------------------- |
| 300              | 0.30           | 4     | 55.67             | 399.33              | 239.89                | 5222.33        | 54.33          | 143.67            | 208.33        | 0.320                       |
| 300              | 0.30           | 5     | 68.00             | 387.00              | 263.02                | 6505.33        | 66.67          | 202.33            | 248.00        | 0.640                       |
| 300              | 0.30           | 6     | 80.33             | 374.67              | 288.96                | 7748.33        | 79.33          | 306.33            | 284.33        | 0.640                       |
| 400              | 0.40           | 4     | 55.67             | 539.33              | 161.25                | 5219.67        | 55.33          | 131.67            | 228.33        | 0.320                       |
| 400              | 0.40           | 5     | 69.33             | 525.67              | 215.55                | 6492.67        | 67.33          | 226.33            | 265.67        | 0.319                       |
| 400              | 0.40           | 6     | 81.33             | 513.67              | 244.05                | 7750.33        | 80.00          | 314.33            | 302.33        | 0.319                       |
| 500              | 0.50           | 4     | 55.67             | 690.67              | 170.28                | 5211.33        | 55.67          | 138.33            | 235.00        | 0.320                       |
| 500              | 0.50           | 5     | 69.33             | 677.00              | 175.96                | 6481.33        | 67.67          | 210.00            | 277.00        | 0.320                       |
| 500              | 0.50           | 6     | 82.33             | 664.00              | 229.69                | 7744.67        | 80.33          | 285.00            | 316.00        | 0.320                       |

### 8.3 Best Optimized Configuration Compared With the Reference

For every tested arrival rate, the best optimized throughput is obtained with fleet size 6.

| Arrival-rate tag | Best optimized fleet | Optimized delivered | Reference delivered | Throughput vs. reference | Optimized backlog | Reference backlog | Optimized avg. delivery time | Reference avg. delivery time | Time ratio |
| ---------------- | -------------------- | ------------------- | ------------------- | ------------------------ | ----------------- | ----------------- | ---------------------------- | ---------------------------- | ---------- |
| 300              | 6                    | 80.33               | 437.00              | 18.38%                   | 374.67            | 18.00             | 288.96                       | 48.51                        | 5.96x      |
| 400              | 6                    | 81.33               | 580.00              | 14.02%                   | 513.67            | 15.00             | 244.05                       | 48.08                        | 5.08x      |
| 500              | 6                    | 82.33               | 716.33              | 11.49%                   | 664.00            | 30.00             | 229.69                       | 48.34                        | 4.75x      |

### 8.4 Statistical View of the Best Optimized Configuration

Values are mean +- standard deviation over 3 seeds.

| Arrival-rate tag | Fleet | Delivered pallets | Undelivered backlog | Average delivery time | Recharge count | Blocked conflicts | Messages sent   | Avg. intermediate occupancy |
| ---------------- | ----- | ----------------- | ------------------- | --------------------- | -------------- | ----------------- | --------------- | --------------------------- |
| 300              | 6     | 80.33 +- 0.58     | 374.67 +- 5.13      | 288.96 +- 18.01       | 79.33 +- 0.58  | 306.33 +- 73.36   | 284.33 +- 10.07 | 0.64 +- 0.55                |
| 400              | 6     | 81.33 +- 0.58     | 513.67 +- 30.14     | 244.05 +- 23.72       | 80.00 +- 0.00  | 314.33 +- 6.11    | 302.33 +- 11.02 | 0.32 +- 0.55                |
| 500              | 6     | 82.33 +- 1.15     | 664.00 +- 51.42     | 229.69 +- 28.60       | 80.33 +- 1.53  | 285.00 +- 59.92   | 316.00 +- 7.55  | 0.32 +- 0.55                |

### 8.5 Evidence Concerning Intermediate-Area Usage

Intermediate storage is part of the model design, so its presence is expected rather than anomalous.

| Indicator                                           | Value   |
| --------------------------------------------------- | ------- |
| Optimized runs with zero intermediate occupancy     | 16 / 27 |
| Optimized runs with non-zero intermediate occupancy | 11 / 27 |
| Maximum observed average intermediate occupancy     | 0.961   |

These values indicate that the first optimized model uses intermediate areas opportunistically rather than continuously. Occupancy remains moderate, which suggests that intermediates are used as occasional relief points instead of becoming the dominant routing mode.

## 9. Analysis and Discussion

Several observations emerge from the suite results.

First, the first optimized model clearly improves realism relative to the reference baseline, but it does so at a very large performance cost. Even with 6 AMRs, the optimized configuration delivers only about 80 pallets across all tested arrival rates, whereas the reference model delivers between 437 and 716.33 pallets over the same horizon.

Second, increasing the fleet from 4 to 6 AMRs improves throughput, but the gain is modest relative to the demand increase. This indicates that the system bottleneck is not merely fleet size. Recharge behavior, communication overhead, and motion conflicts also limit overall productivity.

Third, undelivered backlog grows dramatically as the arrival rate increases. For the best fleet, backlog rises from 374.67 at rate 0.30 to 664.00 at rate 0.50. This shows that the first optimized model does not scale adequately under higher demand.

Fourth, the average delivery time is much larger than in the reference model, even when throughput slightly increases with fleet size. This indicates that the optimized controller spends significant time in non-productive states such as repositioning, charging, waiting, or resolving congestion.

Fifth, recharge counts remain extremely high at approximately 80 recharge sessions for the best fleet regardless of arrival rate. This suggests that battery management is a persistent operational constraint and likely one of the main causes of throughput saturation.

Finally, message counts and blocked conflicts both increase with fleet size. This means that adding robots also increases coordination and congestion costs. The first optimized model therefore demonstrates the classic multi-agent trade-off between parallelism and interference.

## 10. Main Findings

| Finding                                                  | Interpretation                                                                          |
| -------------------------------------------------------- | --------------------------------------------------------------------------------------- |
| Fleet size 6 is consistently the best optimized fleet    | More robots improve throughput, but only moderately                                     |
| Throughput saturates around 80 delivered pallets         | The first optimized controller does not scale with demand                               |
| Backlog grows sharply with arrival rate                  | Demand outpaces the capacity of the coordinated fleet                                   |
| Delivery times are 4.75x to 5.96x larger than reference  | Realistic coordination costs are substantial in this version                            |
| Recharge counts remain near 80 for the best fleet        | Battery management is a dominant bottleneck                                             |
| Intermediate occupancy remains moderate and intermittent | Intermediate areas are used as auxiliary storage rather than as the main transport path |
| Conflict and message counts increase with fleet size     | More robots create more interference and communication overhead                         |

## 11. Conclusion

The first optimized model is an important step beyond the reference simulation because it introduces persistent AMRs, decentralized task allocation, explicit battery management, recharging constraints, and intermediate storage. From a modeling perspective, it succeeds in representing a substantially more realistic warehouse-control problem.

However, the experimental results show that this first design remains far less efficient than the reference baseline. Throughput saturates at roughly 80 delivered pallets, backlog grows sharply with demand, average delivery times are several times larger than in the reference model, and recharge and congestion costs remain high. The first optimized model therefore demonstrates the feasibility of decentralized coordination under realistic constraints, but not yet an effective coordination mechanism for minimizing total pallet delivery time.

## 12. Suggested Report Positioning

A precise academic wording for this model is the following:

> The first optimized model extends the reference warehouse simulation by replacing the idealized one-pallet-one-robot assumption with a persistent shared AMR fleet coordinated through decentralized bidding and claiming. The model introduces battery-aware operation, recharge constraints, intermediate storage areas, and local conflict resolution. Experimental results show that these additions substantially increase realism but also impose severe throughput and delay penalties. The model therefore serves as a foundational constrained coordination baseline rather than as a high-performance decentralized solution.
