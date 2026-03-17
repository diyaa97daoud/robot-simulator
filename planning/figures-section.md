# Visual Comparison of the Three Optimized Models

## 1. Role of the Visual Analysis

The figures complement the numerical tables by making the relative behavior of the three optimized models immediately visible. Taken together, they show not only which model performs better, but also why the performance differs. The visual comparison is organized around five dimensions: throughput, delivery time, fleet scalability, intermediate-area usage, and operational cost in terms of recharge pressure and movement conflicts. This sequence is appropriate for an academic report because it begins with the main outcome variables and then moves toward the explanatory variables that account for the observed differences.

## 2. Throughput Comparison

**Figure 1** compares the mean number of delivered pallets as a function of the arrival rate for the reference model and the best fleet-size configuration of the three optimized models. This is the central performance figure because it summarizes the global ranking of the models and makes the scale of the gap between the idealized baseline and the constrained decentralized strategies immediately apparent.

The figure shows three clear results. First, the reference model dominates all optimized models across all tested arrival rates, confirming that robot persistence, charging constraints, and decentralized coordination impose a substantial productivity cost. Second, Models 1 and 2 are very close in raw throughput, which indicates that removing intermediate storage from the controller does not by itself produce a major productivity collapse when pallet ownership is preserved through recharge. Third, Model 3 is the weakest throughput model, which indicates that strict battery-feasibility enforcement greatly reduces the number of accepted and completed tasks.

**Figure 1.** Mean number of delivered pallets as a function of the arrival rate for the reference model and the best fleet-size configuration of the three optimized models. Error bars represent the standard deviation across seeds.

## 3. Delivery-Time Comparison

**Figure 2** presents the mean average delivery time as a function of the arrival rate for the same set of models. This figure is essential because throughput alone does not distinguish between a model that accepts many tasks but executes them slowly and a model that accepts only a small number of tasks but completes those accepted tasks relatively efficiently.

The figure shows that Models 1 and 2 again remain close, with Model 2 sometimes slightly better and sometimes slightly worse depending on the arrival rate. This confirms that the second model preserves the execution profile of the first optimized controller while changing its recovery policy. Model 3 exhibits comparatively moderate delivery times despite its very low throughput. This indicates that the main limitation of the third model is not inefficient execution of accepted tasks, but overly restrictive task admission. The visual contrast between Figures 1 and 2 is therefore important: low throughput may arise either from slow execution or from conservative filtering, and Model 3 belongs to the second case.

**Figure 2.** Mean average delivery time as a function of the arrival rate for the reference model and the best fleet-size configuration of the three optimized models. Error bars represent the standard deviation across seeds.

## 4. Fleet-Size Sensitivity

**Figure 3** compares delivered pallets for fleet sizes 4, 5, and 6 across the three optimized models and the tested arrival rates. This figure is particularly useful for evaluating the quality of the decentralized coordination mechanism, because a well-performing multi-agent strategy should benefit meaningfully from additional robots.

The figure shows that both Models 1 and 2 benefit similarly from larger fleets, with fleet size 6 consistently producing the best optimized throughput. This means that the second model does not lose the basic scaling behavior of the first controller even though it removes intermediate usage in practice. Model 3 shows the weakest fleet-size sensitivity, which demonstrates that its main bottleneck is not the number of available robots but the strict feasibility criterion used before task allocation. In academic terms, this figure supports the conclusion that larger fleets are not sufficient when coordination policies become too conservative.

**Figure 3.** Mean delivered pallets for fleet sizes 4, 5, and 6 across the three optimized models and the tested arrival rates, showing the effect of fleet scaling on decentralized coordination performance.

## 5. Intermediate-Area Usage

**Figure 4** shows mean average intermediate occupancy across the three optimized models. This figure is especially important because the progression from Model 1 to Model 3 is closely related to how intermediate storage is treated. The figure therefore captures one of the most significant design transitions in the project.

The figure shows a clear transition in the operational role of intermediate areas across the three models. Model 1 exhibits visible intermediate-area usage because temporary storage is an active part of its coordination strategy. Model 2 shows zero occupancy across all tested arrival rates, demonstrating that direct delivery with carry-through recharge is sufficient to eliminate storage fallback in practice. Model 3 also shows zero occupancy, but it reaches this outcome through a much stricter pre-assignment feasibility rule. This figure therefore provides direct behavioral evidence that Models 2 and 3 both remove intermediate storage, but through very different control policies.

**Figure 4.** Mean average intermediate occupancy across the three optimized models, highlighting the transition from explicit intermediate usage in Model 1 to complete elimination of intermediate storage in Models 2 and 3.

## 6. Recharge Pressure and Congestion

**Figure 5** combines two related indicators of operational cost: recharge waiting steps and blocked movement conflicts. The recharge-waiting chart captures the extent to which each model transfers recovery pressure toward the charging infrastructure, while the conflict chart captures the amount of local interference produced by robot movement and coordination.

The figure shows that Model 2 generally incurs more recharge waiting than Model 1, especially at the lower arrival rate. This is consistent with the intended behavior of the controller: instead of using intermediate storage as a recovery mechanism, robots preserve pallet ownership and carry that recovery burden into the recharge zone. At the same time, blocked-conflict levels remain close to those of Model 1, which explains why throughput also remains close. Model 3 exhibits almost no recharge waiting because it prevents risky assignments before pickup, yet still displays very high blocked-conflict values relative to its extremely low productivity. This is a significant result because it demonstrates that stronger safety constraints do not automatically eliminate coordination inefficiency.

**Figure 5.** Mean recharge waiting steps and blocked movement conflicts across the optimized models, illustrating the operational cost associated with carry-through recovery and robot interference.

## 7. Optional Communication Figure

If the communication-overhead figure is included, it shows the mean number of coordination messages sent across the optimized models. This figure is secondary to the main five figures, but it is useful for clarifying that communication cost alone does not explain the performance differences. Rather, the degradation in throughput is more convincingly interpreted as the combined result of battery policy, task-admission logic, recharge behavior, and congestion.

**Figure 6.** Mean number of coordination messages sent across the optimized models and tested arrival rates, showing the communication overhead induced by decentralized task allocation.

## 8. Concluding Interpretation

Taken together, the figures establish a clearer comparative narrative than before. Model 1 is the flexible heuristic controller that still relies on intermediate storage in some runs. Model 2 removes intermediate storage entirely while preserving throughput and delivery-time behavior close to Model 1, but it shifts recovery pressure toward recharge waiting. Model 3 also removes intermediate usage, yet does so through strict pre-bid battery-feasibility filtering that sharply reduces throughput. The visual comparison therefore confirms that the new second model is a genuine middle design: it preserves the productivity level of the first controller much better than Model 3, while achieving the storage-free behavior that Model 1 did not fully attain.
