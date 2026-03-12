# Missing Points and Gaps Analysis

## 1. Purpose

This document lists the remaining gaps between the current implementation and the project specification, with concrete actions to close each gap.

---

## 2. Current Alignment Snapshot

### Implemented and aligned

- Configurable environment (zones, arrivals, obstacles, humans, battery, recharge parameters).
- Intermediate zones with capacity and pallet storage.
- Battery usage and recharge handling with capacity constraints.
- Basic AMR communication artifacts (bid/claim message model).
- Metrics export and experiment runner for comparison scenarios.

### Partially aligned

- Reference mode exists, but it is a simplified analytical delivery model.
- Optimized mode includes communication and coordination logic, but arbitration remains simulator-driven.

---

## 3. Missing Points and Gaps

## Gap A — True decentralized decision process is not fully implemented

**Spec expectation**

- No centralization of information or decision process.

**Current state**

- Bid collection and winner selection are centrally executed in simulator logic (`createBidsAndClaims`).

**Why this is a gap**

- The simulator currently acts as a coordinator/arbiter, which is central decision-making.

**Required fix**

- Move task-assignment protocol to robot-local behavior:
  - each robot computes local bids,
  - robots exchange messages,
  - winner emerges via distributed tie-break rule (for example, lower score then lower robot id),
  - simulator only delivers messages and updates environment state.

---

## Gap B — Dyadic communication mode is not explicitly supported as an alternative

**Spec expectation**

- Communication can be dyadic or broadcast.

**Current state**

- Message model has receiver field support, but workflow behaves mainly as broadcast bidding.

**Why this is a gap**

- No explicit runtime mode or protocol path demonstrates dyadic-only coordination.

**Required fix**

- Add a communication mode config option (`broadcast` or `dyadic`).
- Implement dyadic path (direct negotiation / direct claim confirmation).
- Keep broadcast mode for comparison.

---

## Gap C — Reference model behavior does not strictly match section 1.4

**Spec expectation**

- AMR count equals number of pallets.
- AMR does not communicate.
- AMR disappears when pallet is delivered.
- AMR does not use intermediate areas.
- AMR does not manage battery/recharge.

**Current state**

- Reference mode computes delivery by shortest-distance approximation without explicit AMR lifecycle.

**Why this is a gap**

- Baseline is not behaviorally equivalent to required reference model assumptions.

**Required fix**

- Implement explicit reference AMR entities and lifecycle:
  - spawn per pallet,
  - no messages,
  - direct entry-to-exit route,
  - remove AMR after delivery,
  - no battery/recharge/intermediate logic.

---

## Gap D — Objective of minimum AMR count is explored but not solved explicitly

**Spec expectation**

- Decrease total delivery time with the minimum number of AMRs.

**Current state**

- Experiment suite compares fixed fleet sizes, but no automatic search for minimum feasible AMR count for target performance.

**Why this is a gap**

- Analysis exists, but optimization target is not formally computed.

**Required fix**

- Add fleet-size search procedure:
  - define target KPI threshold (for example max average delivery time),
  - scan fleet sizes with multiple seeds,
  - return smallest AMR count meeting target.

---

## Gap E — Metrics set can better support report conclusions

**Spec expectation**

- Propose indicators to assess performance.

**Current state**

- Core metrics exist, but some report-oriented indicators are missing.

**Why this is a gap**

- Harder to clearly justify trade-offs in final report.

**Required fix**

- Add/derive additional indicators:
  - throughput (delivered pallets per 100 steps),
  - undelivered backlog at end,
  - robot utilization ratio,
  - energy efficiency proxy (distance per recharge or per delivered pallet).

---

## 4. Priority Plan (Recommended Order)

1. Gap C (strict reference model) — baseline must be correct first.
2. Gap A (decentralized assignment) — core scientific objective.
3. Gap B (dyadic vs broadcast switch) — protocol completeness.
4. Gap D (minimum AMR search) — objective formalization.
5. Gap E (extra indicators) — strengthen experiments/report.

---

## 5. Definition of Done

- [ ] Reference mode strictly matches section 1.4 behavior.
- [ ] Optimized mode uses decentralized assignment (no central winner computation).
- [ ] Communication mode is configurable (`dyadic` and `broadcast`).
- [ ] Minimum-AMR search output is produced and reproducible.
- [ ] Metrics file includes enough indicators for report comparison.
- [ ] Experiment section compares reference and optimized models across multiple seeds.

---

## 6. Suggested Evidence for Final Report

- Side-by-side protocol diagram: reference vs decentralized optimized.
- Table of fleet-size vs KPI to justify minimum AMR choice.
- Sensitivity plots for arrival rate and battery settings.
- Discussion of failure cases (congestion, recharge bottleneck, blocked paths).

---

## 7. Implementation Progress (Current)

- ✅ Added strict reference-mode delivery lifecycle (scheduled delivery over time, not instant completion).
- ✅ Added configurable communication mode in `configuration.ini` and parser (`broadcast` / `dyadic`).
- ✅ Split optimized assignment into two paths:
  - `broadcast`: bid aggregation + best-score claim.
  - `dyadic`: direct local claim with peer-targeted claim message.
- ⏳ Remaining: stronger fully distributed arbitration and explicit minimum-AMR search algorithm.
