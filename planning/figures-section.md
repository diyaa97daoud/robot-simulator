# Visual Comparison of the Three Optimized Models

## 1. Role of the Visual Analysis

The figures complement the numerical tables by making the relative behavior of the three optimized models immediately visible. Taken together, they show not only which model performs better, but also why the performance differs. The visual comparison is organized around five dimensions: throughput, delivery time, fleet scalability, intermediate-area usage, and operational cost in terms of recharge pressure and movement conflicts. This sequence is appropriate for an academic report because it begins with the main outcome variables and then moves toward the explanatory variables that account for the observed differences.

## 2. Throughput Comparison

**Figure 1** compares the mean number of delivered pallets as a function of the arrival rate for the reference model and the best fleet-size configuration of the three optimized models. This is the central performance figure because it summarizes the global ranking of the models and makes the scale of the gap between the idealized baseline and the constrained decentralized strategies immediately apparent.

The figure shows three clear results. First, the reference model dominates all optimized models across all tested arrival rates, confirming that robot persistence, charging constraints, and decentralized coordination impose a substantial productivity cost. Second, Model 1 is the strongest of the three optimized strategies in terms of raw throughput, although it still remains far below the reference baseline. Third, Model 3 is the weakest throughput model, which indicates that strict battery-feasibility enforcement greatly reduces the number of accepted and completed tasks. Model 2 occupies an intermediate position, demonstrating that a more conservative battery policy reduces unsafe behavior without reducing throughput as drastically as Model 3.

**Figure 1.** Mean number of delivered pallets as a function of the arrival rate for the reference model and the best fleet-size configuration of the three optimized models. Error bars represent the standard deviation across seeds.

## 3. Delivery-Time Comparison

**Figure 2** presents the mean average delivery time as a function of the arrival rate for the same set of models. This figure is essential because throughput alone does not distinguish between a model that accepts many tasks but executes them slowly and a model that accepts only a small number of tasks but completes those accepted tasks relatively efficiently.

The figure shows that Model 1 has the highest delivery times, reflecting the combined effect of recharge pressure, congestion, communication overhead, and the active management of intermediate storage. Model 2 improves upon Model 1 because its more conservative battery policy reduces some unstable behaviors that increase delay, although it still remains clearly above the reference model. Model 3 exhibits comparatively moderate delivery times despite its very low throughput. This indicates that the main limitation of the third model is not inefficient execution of accepted tasks, but overly restrictive task admission. The visual contrast between Figures 1 and 2 is therefore important: low throughput may arise either from slow execution or from conservative filtering, and Model 3 belongs to the second case.

**Figure 2.** Mean average delivery time as a function of the arrival rate for the reference model and the best fleet-size configuration of the three optimized models. Error bars represent the standard deviation across seeds.

## 4. Fleet-Size Sensitivity

**Figure 3** compares delivered pallets for fleet sizes 4, 5, and 6 across the three optimized models and the tested arrival rates. This figure is particularly useful for evaluating the quality of the decentralized coordination mechanism, because a well-performing multi-agent strategy should benefit meaningfully from additional robots.

The figure shows that Model 1 benefits the most from larger fleets, although the gains diminish as fleet size increases. Model 2 also responds positively to fleet growth, but the improvement is smaller, indicating that its battery policy reduces the ability to convert additional robots into proportional throughput gains. Model 3 shows the weakest fleet-size sensitivity, which demonstrates that its main bottleneck is not the number of available robots but the strict feasibility criterion used before task allocation. In academic terms, this figure supports the conclusion that larger fleets are not sufficient when coordination policies remain too conservative or too interference-prone.

**Figure 3.** Mean delivered pallets for fleet sizes 4, 5, and 6 across the three optimized models and the tested arrival rates, showing the effect of fleet scaling on decentralized coordination performance.

## 5. Intermediate-Area Usage

**Figure 4** shows mean average intermediate occupancy across the three optimized models. This figure is especially important because the progression from Model 1 to Model 3 is closely related to how intermediate storage is treated. The figure therefore captures one of the most significant design transitions in the project.

The figure shows a clear transition in the operational role of intermediate areas across the three models. Model 1 exhibits visible intermediate-area usage because temporary storage is an active part of its coordination strategy. Model 2 shows a much lower occupancy level, but not a complete disappearance of intermediate use, because it suppresses transfers through a higher battery threshold rather than by removing the mechanism entirely. Model 3 shows zero occupancy, demonstrating that in practice it behaves as a strict direct-delivery variant despite the continued presence of intermediate zones in the environment configuration. This figure therefore provides direct behavioral evidence that the control policies materially altered the role of intermediate storage from one model to the next.

**Figure 4.** Mean average intermediate occupancy across the three optimized models, highlighting the transition from explicit intermediate usage in Model 1 to practical elimination of intermediate storage in Model 3.

## 6. Recharge Pressure and Congestion

**Figure 5** combines two related indicators of operational cost: recharge count and blocked movement conflicts. The recharge chart captures the extent to which each model is constrained by battery management, while the conflict chart captures the amount of local interference produced by robot movement and coordination.

The figure shows that Model 1 combines high recharge activity with substantial movement conflicts, reflecting its attempt to maintain throughput while operating under strong battery and congestion pressure. Model 2 retains non-negligible recharge activity, but at a lower scale than Model 1, while also showing a lower though still important conflict burden. Model 3 exhibits very low recharge counts because it avoids risky assignments before pickup, yet still displays very high blocked-conflict values relative to its extremely low productivity. This is a significant result because it demonstrates that stronger safety constraints do not automatically eliminate coordination inefficiency. Instead, they may reduce productive work while leaving contention effects largely intact.

**Figure 5.** Mean recharge count and blocked movement conflicts across the optimized models, illustrating the operational cost associated with battery management and robot interference.

## 7. Optional Communication Figure

If the communication-overhead figure is included, it shows the mean number of coordination messages sent across the optimized models. This figure is secondary to the main five figures, but it is useful for clarifying that communication cost alone does not explain the performance differences. Rather, the degradation in throughput is more convincingly interpreted as the combined result of battery policy, task-admission logic, recharge behavior, and congestion.

**Figure 6.** Mean number of coordination messages sent across the optimized models and tested arrival rates, showing the communication overhead induced by decentralized task allocation.

## 8. Concluding Interpretation

Taken together, the figures establish a coherent comparative narrative. Model 1 is the most productive of the three optimized strategies, but it achieves this at the cost of high delivery times, substantial recharge pressure, and active reliance on intermediate storage. Model 2 introduces a safer and more conservative battery policy, which reduces intermediate-area usage and improves delivery-time behavior relative to Model 1, but still leaves the model far below the reference baseline in throughput. Model 3 completes the progression by enforcing strict battery-feasible direct delivery, thereby eliminating intermediate usage entirely and minimizing recharge pressure, but at the cost of a dramatic collapse in throughput. The visual comparison therefore confirms the central trade-off observed throughout the report: increasing operational safety and delivery feasibility improves behavioral robustness, but excessive conservatism can severely degrade decentralized coordination performance.
