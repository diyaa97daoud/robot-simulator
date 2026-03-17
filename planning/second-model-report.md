# Direct-Delivery Model With Carry-Through Recharge

## 1. Objective of the Studied Model

This document describes the second optimized warehouse-control model. Its purpose is to keep the decentralized, battery-aware AMR coordination of the first optimized model while removing operational dependence on intermediate storage. Instead of dropping or temporarily storing a pallet when battery becomes critical during transport, the robot keeps the pallet with it, moves to the recharge zone, recharges, and then resumes the same delivery task.

The intention of this model is therefore to preserve task continuity without adopting the much stricter pre-bid battery-feasibility filter of the third model. In conceptual terms, it stands between the first optimized model and the hard-feasibility direct-delivery model.

## 2. Reference Model and Studied Variant

The implemented reference model is an idealized baseline with direct pallet-to-exit transport and no persistent AMR constraints, whereas the studied variant uses a persistent shared AMR fleet with decentralized communication, explicit battery management, direct-delivery task assignment, and recharge-while-carrying recovery. Their main differences are summarized below.

| Criterion                                 | Reference model                                                                                                             | Studied variant                                                                                  |
| ----------------------------------------- | --------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------ |
| Number of AMRs equal to number of pallets | Each waiting pallet is handled independently and virtually scheduled for direct delivery, which emulates one AMR per pallet | No; a persistent shared fleet is used                                                            |
| Robot persistence                         | No persistent robot state                                                                                                   | Persistent AMR fleet                                                                             |
| AMR communication                         | None                                                                                                                        | Broadcast-based decentralized bidding and claim exchange                                         |
| AMR disappears when pallet is delivered   | Effectively yes, because no persistent robot state is maintained                                                            | No                                                                                               |
| Use of intermediate areas                 | No                                                                                                                          | No in practice; intermediate zones remain configured but are not used by the controller          |
| Battery management                        | No                                                                                                                          | Enabled                                                                                          |
| Recharging area                           | Not used                                                                                                                    | Enabled                                                                                          |
| Battery update                            | Not used                                                                                                                    | Enabled                                                                                          |
| Main design intention                     | Provide an idealized unconstrained baseline                                                                                 | Preserve direct delivery and keep pallet ownership through recharge when energy becomes critical |

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
| Routing policy       | Direct delivery only  |

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

The following criteria were used to assess the second model.

| Criterion               | Interpretation                                                              |
| ----------------------- | --------------------------------------------------------------------------- |
| Throughput              | Number of delivered pallets in the 300-step horizon                         |
| Responsiveness          | Average pallet delivery time                                                |
| Travel effort           | Total AMR travel distance                                                   |
| Energy pressure         | Number of recharge events and recharge waiting steps                        |
| Coordination overhead   | Number of messages sent                                                     |
| Congestion              | Number of blocked movement conflicts                                        |
| Intermediate-area usage | Average intermediate occupancy                                              |
| Task continuity         | Whether a robot keeps ownership of a pallet even when it must recharge      |
| Recovery robustness     | Ability to recover from low battery without switching to intermediate usage |

A successful implementation of this model should show zero intermediate-area usage while maintaining throughput much closer to the first optimized model than to the hard-feasibility third model.

## 5. Implemented Changes Relative to the Reference Model

| Aspect                 | Reference model           | Studied variant                                                        |
| ---------------------- | ------------------------- | ---------------------------------------------------------------------- |
| Task allocation        | Immediate direct handling | Decentralized bidding and claiming                                     |
| Robot persistence      | No persistent fleet       | Persistent fleet of AMRs                                               |
| Communication          | None                      | Broadcast bid and claim exchange                                       |
| Battery                | Ignored                   | Tracked at every robot move                                            |
| Recharge               | Absent                    | Dedicated recharge area with limited capacity                          |
| Routing                | Direct only               | Direct only                                                            |
| Low-battery recovery   | Not applicable            | Robot keeps carrying the pallet and recharges before resuming delivery |
| Intermediate retrieval | Not applicable            | Disabled in practice                                                   |

The central change of this variant is therefore not a higher battery threshold, but a different recovery policy: once a robot has committed to a pallet, it keeps that pallet through the recharge process instead of falling back to an intermediate-area transfer.

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

In this model, bid scoring remains heuristic-based, but delivery targets are restricted to exit zones. The scoring form is:

$$
\text{score} =
\text{pickupCost} +
\text{directDeliveryCost} +
\text{congestionPenalty} +
\text{batteryPenalty}
$$

where `pickupCost` is the shortest-path distance from robot to pallet, `directDeliveryCost` is the path cost from pallet to its destination exit, `congestionPenalty` penalizes blocked pickup positions, and `batteryPenalty` penalizes risky assignments under the current battery level.

### 6.3 Battery and Recharge Policy

The model applies the same configured thresholds as the first optimized model.

| Threshold               | Effect                                                                    |
| ----------------------- | ------------------------------------------------------------------------- |
| Critical threshold = 12 | If battery is at or below this level, the robot is redirected to recharge |
| Warning threshold = 24  | Idle or waiting robots under this level are also sent to recharge         |

The crucial behavioral change is what happens while a robot is already carrying a pallet. In this model, reaching the recharge zone does not imply dropping the task. The robot can enter the recharge state while still carrying the pallet and then restore its delivery state once charging is complete.

### 6.4 Conflict Resolution

If multiple robots want to move into the same next position, a local priority rule is used:

1. Robots carrying pallets are prioritized.
2. Robots moving to recharge are prioritized by lower battery level.
3. Lower task-load robots are preferred.
4. Remaining ties are broken by robot identifier.

Because carrying robots keep task ownership during recharge, this rule has greater practical importance than in the first model.

## 7. Validation and Testing

Two complementary validation layers were used.

### 7.1 Automated Unit Tests

The project contains automated JUnit tests covering:

| Test scope                              | Purpose                                                                  |
| --------------------------------------- | ------------------------------------------------------------------------ |
| Configuration parsing                   | Ensures scenario parameters are read correctly                           |
| Grid pathfinding                        | Verifies obstacle-aware shortest-path behavior                           |
| Reference mode behavior                 | Confirms no communication traffic in reference mode                      |
| Optimized mode behavior                 | Confirms communication and battery features are active                   |
| Recharge while carrying without storage | Verifies that robots can recharge while carrying and avoid intermediates |

All automated tests passed successfully on the current branch:

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

| Arrival-rate tag | Effective rate | Fleet | Delivered pallets | Average delivery time | Total distance | Recharge count | Recharge wait steps | Blocked conflicts | Messages sent | Avg. intermediate occupancy |
| ---------------- | -------------- | ----- | ----------------- | --------------------- | -------------- | -------------- | ------------------- | ----------------- | ------------- | --------------------------- |
| 300              | 0.30           | 4     | 11.10             | 99.53                 | 1042.60        | 9.10           | 0.50                | 52.10             | 48.80         | 0.000                       |
| 300              | 0.30           | 5     | 12.90             | 92.83                 | 1271.50        | 11.00          | 2.10                | 100.40            | 60.70         | 0.000                       |
| 300              | 0.30           | 6     | 14.70             | 95.88                 | 1492.60        | 12.90          | 2.30                | 142.60            | 72.60         | 0.000                       |
| 400              | 0.40           | 4     | 11.50             | 99.16                 | 1043.50        | 9.10           | 0.10                | 51.30             | 51.40         | 0.000                       |
| 400              | 0.40           | 5     | 13.50             | 89.75                 | 1271.80        | 10.80          | 1.00                | 95.90             | 63.90         | 0.000                       |
| 400              | 0.40           | 6     | 15.40             | 92.10                 | 1495.20        | 12.80          | 0.40                | 140.20            | 76.80         | 0.000                       |
| 500              | 0.50           | 4     | 11.20             | 106.30                | 1041.60        | 9.00           | 0.20                | 53.40             | 51.40         | 0.000                       |
| 500              | 0.50           | 5     | 13.60             | 91.71                 | 1277.20        | 11.00          | 1.10                | 91.20             | 65.50         | 0.000                       |
| 500              | 0.50           | 6     | 15.20             | 93.68                 | 1502.70        | 13.00          | 0.70                | 137.40            | 77.40         | 0.000                       |

### 8.3 Best Optimized Configuration Compared With the Reference

For all three arrival rates, the best optimized throughput is obtained with fleet size 6.

| Arrival-rate tag | Best optimized fleet | Optimized delivered | Reference delivered | Throughput vs. reference | Optimized avg. delivery time | Reference avg. delivery time | Time ratio |
| ---------------- | -------------------- | ------------------- | ------------------- | ------------------------ | ---------------------------- | ---------------------------- | ---------- |
| 300              | 6                    | 14.70               | 74.00               | 19.86%                   | 95.88                        | 48.05                        | 2.00x      |
| 400              | 6                    | 15.40               | 101.80              | 15.13%                   | 92.10                        | 47.58                        | 1.94x      |
| 500              | 6                    | 15.20               | 132.00              | 11.52%                   | 93.68                        | 47.77                        | 1.96x      |

### 8.4 Statistical View of the Best Optimized Configuration

Values are mean +- standard deviation over 10 seeds.

| Arrival-rate tag | Fleet | Delivered pallets | Average delivery time | Recharge count | Recharge wait steps | Blocked conflicts | Messages sent | Avg. intermediate occupancy |
| ---------------- | ----- | ----------------- | --------------------- | -------------- | ------------------- | ----------------- | ------------- | --------------------------- |
| 300              | 6     | 14.70 +- 0.48     | 95.88 +- 11.45        | 12.90 +- 0.57  | 2.30 +- 3.43        | 142.60 +- 29.64   | 72.60 +- 2.80 | 0.00 +- 0.00                |
| 400              | 6     | 15.40 +- 0.70     | 92.10 +- 13.35        | 12.80 +- 0.63  | 0.40 +- 1.26        | 140.20 +- 17.89   | 76.80 +- 5.77 | 0.00 +- 0.00                |
| 500              | 6     | 15.20 +- 0.79     | 93.68 +- 13.21        | 13.00 +- 0.47  | 0.70 +- 1.64        | 137.40 +- 18.76   | 77.40 +- 3.89 | 0.00 +- 0.00                |

### 8.5 Evidence Concerning Intermediate-Area Usage

This model eliminates intermediate-area usage in all optimized runs.

| Indicator                                           | Value   |
| --------------------------------------------------- | ------- |
| Optimized runs with zero intermediate occupancy     | 90 / 90 |
| Optimized runs with non-zero intermediate occupancy | 0 / 90  |
| Maximum observed average intermediate occupancy     | 0.000   |

This is strong empirical evidence that the controller behaves as a direct-delivery carry-through-recharge model rather than as a conservative-threshold variant with residual storage fallback.

## 9. Analysis and Discussion

Several conclusions follow from the experimental results.

First, the new recovery policy succeeds in its main design objective: intermediate storage disappears entirely from the optimized runs. This is the clearest behavioral difference relative to the first optimized model, which still used intermediate areas in a subset of runs.

Second, this elimination of intermediate usage does not come with a dramatic throughput penalty. The second model remains very close to the first optimized model in delivered pallets and average delivery time. In other words, preserving task continuity through recharge is enough to remove intermediate usage without collapsing system productivity.

Third, the model does incur somewhat more recharge waiting than the first optimized model. This is expected. If a robot keeps a pallet while charging, the recharge zone becomes the recovery bottleneck instead of the intermediate area. The data therefore shows a transfer of operational pressure from temporary storage usage toward recharge-zone contention.

Fourth, fleet enlargement continues to help only modestly. Increasing the fleet from 4 to 6 robots improves throughput from roughly 11 delivered pallets to roughly 15 delivered pallets, while blocked conflicts and message counts rise substantially. This means that the main structural bottleneck remains shared movement and coordination overhead rather than simple fleet size.

Finally, the reference model remains much faster because it is idealized and unconstrained. It ignores fleet sharing, communication, congestion, and charging. The relevant comparison is therefore not whether the second model beats the reference, but whether it provides a cleaner operational policy than the first model without becoming as restrictive as the third model. On that criterion, the results are favorable.

## 10. Main Findings

| Finding                                                                   | Interpretation                                                        |
| ------------------------------------------------------------------------- | --------------------------------------------------------------------- |
| Fleet size 6 is consistently the best optimized fleet                     | More robots still help, but only modestly                             |
| Throughput remains close to the first optimized model                     | Carry-through recharge avoids a major performance collapse            |
| Intermediate usage is zero in all optimized runs                          | The model successfully removes storage fallback from actual execution |
| Recharge waiting increases relative to the first model                    | Recovery pressure shifts from intermediates to the recharge zone      |
| Delivery times remain far above reference                                 | Charging and decentralized coordination still limit responsiveness    |
| The model is much less conservative than the hard-feasibility third model | It preserves productivity while still eliminating intermediate use    |

## 11. Conclusion

The second model can be characterized as a decentralized, battery-aware direct-delivery coordination model with carry-through recharge. Its defining property is that robots keep ownership of a pallet when they must recharge, instead of dropping the task into an intermediate area or relying on stricter pre-bid rejection.

The experimental results show that this policy achieves a meaningful compromise. It completely eliminates intermediate-area usage, preserves throughput close to that of the first optimized model, and remains far more productive than the hard-feasibility third model. The main tradeoff is increased recharge-zone pressure, visible in recharge waiting steps. Academically, this makes the second model a genuine middle design between a flexible heuristic controller with storage fallback and a strict feasibility-filtering controller.

## 12. Suggested Report Positioning

> The studied variant extends the reference model with persistent AMRs, broadcast-based decentralized coordination, and explicit battery management, but unlike the first optimized model it enforces direct delivery in practice and allows robots to recharge while continuing to carry their assigned pallet. Experimental results show that this policy eliminates intermediate-area usage completely while preserving throughput close to the first optimized controller. Compared with the stricter third model, it achieves substantially better productivity at the cost of higher recharge-zone pressure rather than temporary storage usage.
