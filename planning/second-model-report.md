# Conservative Battery-Threshold Model With Intended Suppression of Intermediate-Area Usage

## 1. Objective of the Studied Model

This document describes the second optimized warehouse-control model. Its purpose is to extend the reference warehouse simulation with decentralized AMR coordination and battery management while making the battery policy more conservative, so that robots are less likely to abandon a pallet or route it through an intermediate area because of low battery. In practical terms, this variant aims to approximate a no-intermediate-transfer behavior by forcing earlier recharge decisions.

The analysis below is based on the archived suite outputs, the implemented simulator logic, and the automated validation tests available in the project.

## 2. Reference Model and Studied Variant

The implemented reference model is an idealized baseline with direct pallet-to-exit transport and no resource constraints related to robot persistence, charging, or coordination, whereas the studied model is an optimized decentralized variant with a conservative battery policy intended to prevent pallet stranding and reduce reliance on intermediate storage. Their main differences are summarized below.

| Criterion                                 | Reference model                                                                                                                                                   | Studied variant                                                                                             |
| ----------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------- |
| Number of AMRs equal to number of pallets | Each waiting pallet is handled independently and virtually scheduled for direct delivery, which emulates one AMR per pallet rather than a persistent shared fleet | No; a persistent shared fleet is used                                                                       |
| Robot persistence                         | No persistent robot state                                                                                                                                         | Persistent AMR fleet                                                                                        |
| AMR communication                         | None                                                                                                                                                              | Broadcast-based decentralized bidding and claim exchange                                                    |
| AMR disappears when pallet is delivered   | Effectively yes, because no persistent robot state is maintained in reference mode                                                                                | No                                                                                                          |
| Use of intermediate areas                 | No                                                                                                                                                                | Still configured in the environment, but intended to be avoided through more conservative recharge behavior |
| Battery management                        | No                                                                                                                                                                | Enabled                                                                                                     |
| Recharging area                           | Not used                                                                                                                                                          | Enabled                                                                                                     |
| Battery update                            | Not used                                                                                                                                                          | Enabled                                                                                                     |
| Main design intention                     | Provide an idealized unconstrained baseline                                                                                                                       | Force robots to recharge earlier so they do not drop or temporarily store pallets mid-route                 |

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

| Parameter            | Value                 |
| -------------------- | --------------------- |
| Base simulation mode | Optimized             |
| Base fleet size      | 6 AMRs                |
| Communication mode   | Broadcast             |
| Arrival distribution | Poisson               |
| Base arrival rate    | 0.40 pallets per step |
| Battery capacity     | 80                    |
| Critical threshold   | 12                    |
| Warning threshold    | 24                    |
| Safe battery margin  | 8                     |
| Recharge duration    | 12 steps              |
| Recharge capacity    | 2 robots              |

### 3.3 Suite Protocol

The analyzed suite performs a controlled comparison over multiple seeds, arrival rates, and fleet sizes.

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

The following criteria were used to interpret the model academically.

| Criterion                | Interpretation                                                     |
| ------------------------ | ------------------------------------------------------------------ |
| Throughput               | Number of delivered pallets in the 300-step horizon                |
| Responsiveness           | Average pallet delivery time                                       |
| Travel effort            | Total AMR travel distance                                          |
| Energy pressure          | Number of recharge events and recharge waiting steps               |
| Coordination overhead    | Number of messages sent                                            |
| Congestion               | Number of blocked movement conflicts                               |
| Intermediate-area usage  | Average intermediate occupancy                                     |
| Safety-oriented behavior | Reduced risk of pallet stranding due to earlier recharge decisions |

A key assumption in this analysis is that a successful conservative-threshold variant should show very low intermediate-area usage, because recharge is triggered earlier and robots are expected to avoid taking pallets into unsafe battery states.

## 5. Implemented Changes Relative to the Reference Model

| Aspect            | Reference model           | Studied variant                                                         |
| ----------------- | ------------------------- | ----------------------------------------------------------------------- |
| Task allocation   | Immediate direct handling | Decentralized bidding and claiming                                      |
| Robot persistence | No persistent fleet       | Persistent fleet of AMRs                                                |
| Communication     | None                      | Broadcast claim and bid exchanges                                       |
| Battery           | Ignored                   | Tracked at every robot move                                             |
| Recharge          | Absent                    | Dedicated recharge area with limited capacity                           |
| Routing           | Direct only               | Direct delivery preferred, intermediate routing available as fallback   |
| Safety policy     | Not applicable            | More conservative battery thresholds to reduce unsafe carrying behavior |

The principal design change of this variant is therefore not the removal of intermediate zones from the environment, but the introduction of a more restrictive battery policy intended to make their use unnecessary in most situations.

## 6. Algorithms Used

### 6.1 Path Planning

Shortest-path computation on the warehouse grid is performed with a breadth-first search over 4-neighbor moves. This pathfinder is used both to estimate delivery costs and to choose the next robot step.

### 6.2 Decentralized Task Allocation

The optimized mode uses a decentralized bid and claim mechanism:

1. Idle robots evaluate waiting pallets.
2. Each robot computes its best bid.
3. Bids are broadcast.
4. Winning bids are resolved per pallet.
5. A claim is issued and the selected robot is assigned the pallet.

The bid score combines multiple factors:

$$
\text{score} =
\text{pickupCost} +
\text{deliveryCost} +
\text{congestionPenalty} +
\text{batteryPenalty} +
\text{queuePenalty}
$$

where `pickupCost` is the shortest-path distance from robot to pallet, `deliveryCost` is the path cost to the chosen destination, `congestionPenalty` penalizes blocked pickup positions, `batteryPenalty` penalizes assignments that are risky under the current battery level, and `queuePenalty` penalizes the use of intermediate areas with occupancy.

### 6.3 Battery and Recharge Policy

The model applies two battery thresholds.

| Threshold               | Effect                                                                    |
| ----------------------- | ------------------------------------------------------------------------- |
| Critical threshold = 12 | If battery is at or below this level, the robot is redirected to recharge |
| Warning threshold = 24  | Idle or waiting robots under this level are also sent to recharge         |

This policy is conservative because it triggers recharge relatively early compared with the total battery capacity of 80. The intended effect is to reduce risky task acceptance and avoid mid-route pallet abandonment.

### 6.4 Conflict Resolution

If multiple robots want to move into the same next position, a local priority rule is used:

1. Robots carrying pallets are prioritized.
2. Robots moving to recharge are prioritized by lower battery level.
3. Lower task-load robots are preferred.
4. Remaining ties are broken by robot identifier.

## 7. Validation and Testing

Two complementary validation layers were used.

### 7.1 Automated Unit Tests

The project contains automated JUnit tests covering:

| Test scope              | Purpose                                                           |
| ----------------------- | ----------------------------------------------------------------- |
| Configuration parsing   | Ensures scenario parameters are read correctly                    |
| Grid pathfinding        | Verifies obstacle-aware shortest-path behavior                    |
| Reference mode behavior | Confirms no communication traffic in reference mode               |
| Optimized mode behavior | Confirms communication and battery features are active            |
| Recharge while carrying | Verifies that recharging while transporting a pallet is supported |

All automated tests passed successfully:

| Metric          | Result |
| --------------- | ------ |
| Total tests run | 9      |
| Passed          | 9      |
| Failed          | 0      |

### 7.2 Experimental Validation

The principal empirical validation is the 120-run comparison suite summarized in this section. Results were computed from the archived CSV outputs rather than from manual observation.

## 8. Experimental Results

### 8.1 Reference Model Performance

Values are reported as mean +- standard deviation over 10 seeds.

| Arrival-rate tag | Effective rate | Delivered pallets | Average delivery time |
| ---------------- | -------------- | ----------------- | --------------------- |
| 300              | 0.30           | 74.00 +- 9.72     | 48.05 +- 0.87         |
| 400              | 0.40           | 101.80 +- 7.41    | 47.58 +- 0.59         |
| 500              | 0.50           | 132.00 +- 9.60    | 47.77 +- 0.71         |

### 8.2 Optimized Variant Performance by Fleet Size

Values are mean values over 10 seeds.

| Arrival-rate tag | Effective rate | Fleet | Delivered pallets | Average delivery time | Total distance | Recharge count | Blocked conflicts | Messages sent | Avg. intermediate occupancy |
| ---------------- | -------------- | ----- | ----------------- | --------------------- | -------------- | -------------- | ----------------- | ------------- | --------------------------- |
| 300              | 0.30           | 4     | 11.20             | 99.69                 | 1043.40        | 9.10           | 51.70             | 49.30         | 0.081                       |
| 300              | 0.30           | 5     | 12.90             | 93.43                 | 1270.10        | 10.90          | 101.20            | 61.30         | 0.240                       |
| 300              | 0.30           | 6     | 14.60             | 97.44                 | 1489.50        | 12.90          | 145.70            | 73.60         | 0.392                       |
| 400              | 0.40           | 4     | 11.50             | 97.89                 | 1040.20        | 9.00           | 51.20             | 52.20         | 0.159                       |
| 400              | 0.40           | 5     | 13.50             | 91.12                 | 1272.60        | 10.80          | 95.20             | 64.10         | 0.079                       |
| 400              | 0.40           | 6     | 15.50             | 91.21                 | 1495.30        | 12.90          | 139.80            | 77.30         | 0.079                       |
| 500              | 0.50           | 4     | 11.40             | 102.96                | 1043.10        | 9.10           | 52.40             | 52.90         | 0.244                       |
| 500              | 0.50           | 5     | 13.70             | 91.92                 | 1278.40        | 11.00          | 90.50             | 65.90         | 0.106                       |
| 500              | 0.50           | 6     | 15.30             | 92.08                 | 1503.10        | 12.90          | 138.00            | 79.00         | 0.278                       |

### 8.3 Best Optimized Configuration Compared With the Reference

For all three arrival rates, the best optimized throughput is obtained with fleet size 6.

| Arrival-rate tag | Best optimized fleet | Optimized delivered | Reference delivered | Throughput vs. reference | Optimized avg. delivery time | Reference avg. delivery time | Time ratio |
| ---------------- | -------------------- | ------------------- | ------------------- | ------------------------ | ---------------------------- | ---------------------------- | ---------- |
| 300              | 6                    | 14.60               | 74.00               | 19.73%                   | 97.44                        | 48.05                        | 2.03x      |
| 400              | 6                    | 15.50               | 101.80              | 15.23%                   | 91.21                        | 47.58                        | 1.92x      |
| 500              | 6                    | 15.30               | 132.00              | 11.59%                   | 92.08                        | 47.77                        | 1.93x      |

### 8.4 Statistical View of the Best Optimized Configuration

Values are mean +- standard deviation over 10 seeds.

| Arrival-rate tag | Fleet | Delivered pallets | Average delivery time | Recharge count | Blocked conflicts | Messages sent | Avg. intermediate occupancy |
| ---------------- | ----- | ----------------- | --------------------- | -------------- | ----------------- | ------------- | --------------------------- |
| 300              | 6     | 14.60 +- 0.52     | 97.44 +- 9.29         | 12.90 +- 0.57  | 145.70 +- 27.53   | 73.60 +- 2.50 | 0.39 +- 0.55                |
| 400              | 6     | 15.50 +- 0.53     | 91.21 +- 11.57        | 12.90 +- 0.57  | 139.80 +- 15.35   | 77.30 +- 4.99 | 0.08 +- 0.25                |
| 500              | 6     | 15.30 +- 0.95     | 92.08 +- 14.25        | 12.90 +- 0.32  | 138.00 +- 19.42   | 79.00 +- 3.02 | 0.28 +- 0.46                |

### 8.5 Evidence Concerning Intermediate-Area Usage

Because this variant is intended to behave as a no-intermediate model, intermediate occupancy is particularly important.

| Indicator                                           | Value   |
| --------------------------------------------------- | ------- |
| Optimized runs with zero intermediate occupancy     | 70 / 90 |
| Optimized runs with non-zero intermediate occupancy | 20 / 90 |
| Maximum observed average intermediate occupancy     | 1.52    |

This means that the model mostly suppresses intermediate-area usage, but it does not eliminate it completely. Therefore, academically, the implementation should be described as a conservative-threshold model with residual intermediate usage, not as a strict no-intermediate model.

## 9. Analysis and Discussion

Several conclusions follow from the experimental results.

First, the conservative battery policy succeeds in one important respect: intermediate occupancy remains low in most optimized runs. This supports the intuition that earlier recharge decisions reduce the need for temporary pallet storage. The data therefore validates the behavioral intention of the model.

Second, this safety-oriented improvement comes with a substantial performance cost. Even the best optimized configuration, using 6 robots, reaches only 11.59% to 19.73% of the reference throughput, depending on the arrival rate. At the same time, average delivery time is approximately 1.9 to 2.0 times larger than in the reference model.

Third, fleet enlargement improves throughput only marginally. Increasing the optimized fleet from 4 to 6 robots raises delivered pallets from roughly 11 to roughly 15, but blocked conflicts and message volume increase significantly. This indicates that the main bottleneck is not only the number of robots, but also the interaction between charging decisions, movement conflicts, and conservative task acceptance.

Fourth, recharge activity remains almost constant at about 12.9 recharges for the best fleet across all arrival rates. This suggests that the conservative battery rule dominates robot behavior and may be constraining productive movement more strongly than the pallet demand itself.

Finally, the reference model remains much faster because it is idealized and unconstrained. It does not suffer from congestion, charging, or communication overhead. Therefore, the absolute performance gap is expected. However, the gap observed here is large enough to indicate that the current decentralized coordination mechanism and battery thresholds are still too restrictive for high-throughput operation.

## 10. Main Findings

| Finding                                              | Interpretation                                                                    |
| ---------------------------------------------------- | --------------------------------------------------------------------------------- |
| Fleet size 6 is always the best optimized fleet      | More robots help, but only modestly                                               |
| Throughput remains far below reference               | Coordination and battery policies still limit system capacity                     |
| Delivery times nearly double relative to reference   | Conservative safety behavior slows execution                                      |
| Intermediate usage is low but non-zero               | The model approximates no-intermediate behavior, but does not strictly enforce it |
| Recharge count stays high and stable                 | Battery policy is a major operational bottleneck                                  |
| Conflict and message counts increase with fleet size | More robots increase coordination and congestion costs                            |

## 11. Conclusion

The analyzed model can be characterized as a decentralized, battery-aware AMR coordination model with conservative recharge thresholds intended to suppress intermediate-area usage. The experimental results show that this intention is largely achieved behaviorally, since most optimized runs do not use intermediate areas and the observed average occupancy remains very low. Nevertheless, the model does not fully satisfy a strict no-intermediate interpretation because residual intermediate usage is still present in 20 of the 90 optimized runs.

From a performance perspective, the model is robust in the sense that it avoids aggressive risky behavior, but it does so at a significant throughput cost. The best optimized configuration remains substantially below the reference model in delivered pallets and average delivery time. Therefore, the current conservative threshold policy improves operational safety but does not yet provide an efficient decentralized coordination mechanism for minimizing total pallet delivery time.

## 12. Suggested Report Positioning

> The studied variant extends the reference model with persistent AMRs, broadcast-based decentralized coordination, and explicit battery management. A conservative recharge policy was introduced to reduce pallet stranding and to discourage the use of intermediate areas. Experimental evidence shows that this policy keeps intermediate occupancy very low, but does not eliminate it completely. The resulting model is safer operationally than a less conservative battery policy, yet it remains markedly inferior to the reference model in throughput and delivery-time performance under the tested short-horizon conditions.
