from __future__ import annotations

import csv
import math
import re
import statistics
from dataclasses import dataclass
from pathlib import Path

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt


ROOT = Path(__file__).resolve().parents[1]
OUTPUT_DIR = ROOT / "planning" / "figures"

MODEL_RUNS = {
    "Model 1": ROOT / "build" / "suite" / "run-1773757946775",
    "Model 2": ROOT / "build" / "suite" / "run-1773760411373",
    "Model 3": ROOT / "build" / "suite" / "run-1773692677043",
}

MODEL_COLORS = {
    "Reference": "#222222",
    "Model 1": "#1f77b4",
    "Model 2": "#ff7f0e",
    "Model 3": "#2ca02c",
}

RATES = [300, 400, 500]
FLEETS = [4, 5, 6]
RATE_LABELS = ["0.30", "0.40", "0.50"]


@dataclass
class Record:
    kind: str
    seed: int
    rate: int
    fleet: int
    delivered: float
    avg_delivery_time: float
    total_distance: float
    recharge_count: float
    recharge_wait_steps: float
    blocked_conflicts: float
    messages_sent: float
    avg_intermediate_occupancy: float


FILE_PATTERN = re.compile(r"^(optimized|reference)-seed-(\d+)-rate-(\d+)(?:-fleet-(\d+))?\.csv$")


def load_records(run_dir: Path) -> list[Record]:
    records: list[Record] = []
    for path in sorted(run_dir.glob("*.csv")):
        match = FILE_PATTERN.match(path.name)
        if not match:
            continue
        kind, seed_text, rate_text, fleet_text = match.groups()
        seed = int(seed_text)
        rate = int(rate_text)
        fleet = int(fleet_text) if fleet_text else 0
        with path.open(newline="", encoding="utf-8") as handle:
            reader = csv.DictReader(handle)
            for row in reader:
                records.append(
                    Record(
                        kind=kind,
                        seed=seed,
                        rate=rate,
                        fleet=fleet,
                        delivered=float(row["delivered"]),
                        avg_delivery_time=float(row["avgDeliveryTime"]),
                        total_distance=float(row["totalDistance"]),
                        recharge_count=float(row["rechargeCount"]),
                        recharge_wait_steps=float(row["rechargeWaitSteps"]),
                        blocked_conflicts=float(row["blockedConflicts"]),
                        messages_sent=float(row["messagesSent"]),
                        avg_intermediate_occupancy=float(row["avgIntermediateOccupancy"]),
                    )
                )
    return records


def mean(values: list[float]) -> float:
    return sum(values) / len(values)


def sd(values: list[float]) -> float:
    return statistics.stdev(values) if len(values) > 1 else 0.0


def grouped(records: list[Record], *, kind: str, rate: int, fleet: int | None = None) -> list[Record]:
    filtered = [record for record in records if record.kind == kind and record.rate == rate]
    if fleet is not None:
        filtered = [record for record in filtered if record.fleet == fleet]
    return filtered


def best_fleet(records: list[Record], rate: int) -> int:
    candidates = []
    for fleet in FLEETS:
        rows = grouped(records, kind="optimized", rate=rate, fleet=fleet)
        delivered_values = [row.delivered for row in rows]
        avg_time_values = [row.avg_delivery_time for row in rows]
        candidates.append((mean(delivered_values), -mean(avg_time_values), fleet))
    candidates.sort(reverse=True)
    return candidates[0][2]


def build_model_stats() -> dict[str, dict[str, object]]:
    stats: dict[str, dict[str, object]] = {}
    for model_name, run_dir in MODEL_RUNS.items():
        records = load_records(run_dir)
        best_by_rate = {rate: best_fleet(records, rate) for rate in RATES}
        stats[model_name] = {
            "records": records,
            "best_by_rate": best_by_rate,
        }
    return stats


def reference_stats(records: list[Record], metric: str) -> tuple[list[float], list[float]]:
    means = []
    errors = []
    for rate in RATES:
        values = [getattr(row, metric) for row in grouped(records, kind="reference", rate=rate)]
        means.append(mean(values))
        errors.append(sd(values))
    return means, errors


def best_model_stats(model_info: dict[str, object], metric: str) -> tuple[list[float], list[float]]:
    records: list[Record] = model_info["records"]  # type: ignore[assignment]
    best_by_rate: dict[int, int] = model_info["best_by_rate"]  # type: ignore[assignment]
    means = []
    errors = []
    for rate in RATES:
        fleet = best_by_rate[rate]
        values = [getattr(row, metric) for row in grouped(records, kind="optimized", rate=rate, fleet=fleet)]
        means.append(mean(values))
        errors.append(sd(values))
    return means, errors


def save_figure(fig: plt.Figure, name: str) -> None:
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    fig.tight_layout()
    fig.savefig(OUTPUT_DIR / name, dpi=220, bbox_inches="tight")
    plt.close(fig)


def plot_throughput(stats: dict[str, dict[str, object]]) -> None:
    fig, ax = plt.subplots(figsize=(8.2, 4.8))
    ref_records: list[Record] = stats["Model 2"]["records"]  # type: ignore[index]
    ref_means, ref_errors = reference_stats(ref_records, "delivered")
    ax.errorbar(RATE_LABELS, ref_means, yerr=ref_errors, label="Reference", color=MODEL_COLORS["Reference"], marker="o", linewidth=2)

    for model_name in ("Model 1", "Model 2", "Model 3"):
        means, errors = best_model_stats(stats[model_name], "delivered")
        ax.errorbar(RATE_LABELS, means, yerr=errors, label=model_name, color=MODEL_COLORS[model_name], marker="o", linewidth=2)

    ax.set_title("Delivered Pallets vs Arrival Rate")
    ax.set_xlabel("Arrival rate")
    ax.set_ylabel("Mean delivered pallets")
    ax.grid(True, axis="y", alpha=0.25)
    ax.legend(frameon=False)
    save_figure(fig, "01-throughput-vs-arrival-rate.png")


def plot_delivery_time(stats: dict[str, dict[str, object]]) -> None:
    fig, ax = plt.subplots(figsize=(8.2, 4.8))
    ref_records: list[Record] = stats["Model 2"]["records"]  # type: ignore[index]
    ref_means, ref_errors = reference_stats(ref_records, "avg_delivery_time")
    ax.errorbar(RATE_LABELS, ref_means, yerr=ref_errors, label="Reference", color=MODEL_COLORS["Reference"], marker="o", linewidth=2)

    for model_name in ("Model 1", "Model 2", "Model 3"):
        means, errors = best_model_stats(stats[model_name], "avg_delivery_time")
        ax.errorbar(RATE_LABELS, means, yerr=errors, label=model_name, color=MODEL_COLORS[model_name], marker="o", linewidth=2)

    ax.set_title("Average Delivery Time vs Arrival Rate")
    ax.set_xlabel("Arrival rate")
    ax.set_ylabel("Mean average delivery time")
    ax.grid(True, axis="y", alpha=0.25)
    ax.legend(frameon=False)
    save_figure(fig, "02-delivery-time-vs-arrival-rate.png")


def plot_fleet_sensitivity(stats: dict[str, dict[str, object]]) -> None:
    fig, axes = plt.subplots(1, 3, figsize=(13.5, 4.2), sharey=True)
    width = 0.22
    x_positions = list(range(len(FLEETS)))

    for axis, rate in zip(axes, RATES):
        for offset, model_name in zip((-width, 0.0, width), ("Model 1", "Model 2", "Model 3")):
            records: list[Record] = stats[model_name]["records"]  # type: ignore[index]
            means = [mean([row.delivered for row in grouped(records, kind="optimized", rate=rate, fleet=fleet)]) for fleet in FLEETS]
            axis.bar([x + offset for x in x_positions], means, width=width, label=model_name if rate == RATES[0] else None, color=MODEL_COLORS[model_name])
        axis.set_title(f"Arrival rate {rate / 1000:.2f}")
        axis.set_xticks(x_positions, [str(fleet) for fleet in FLEETS])
        axis.set_xlabel("Fleet size")
        axis.grid(True, axis="y", alpha=0.25)

    axes[0].set_ylabel("Mean delivered pallets")
    handles, labels = axes[0].get_legend_handles_labels()
    fig.legend(handles, labels, loc="upper center", ncol=3, frameon=False)
    fig.suptitle("Fleet-Size Sensitivity of Delivered Pallets", y=1.03)
    save_figure(fig, "03-delivered-by-fleet-size.png")


def plot_intermediate_usage(stats: dict[str, dict[str, object]]) -> None:
    fig, ax = plt.subplots(figsize=(8.8, 4.8))
    width = 0.22
    x_positions = list(range(len(RATES)))
    offsets = {"Model 1": -width, "Model 2": 0.0, "Model 3": width}

    for model_name in ("Model 1", "Model 2", "Model 3"):
        means, _ = best_model_stats(stats[model_name], "avg_intermediate_occupancy")
        ax.bar([x + offsets[model_name] for x in x_positions], means, width=width, label=model_name, color=MODEL_COLORS[model_name])

    ax.set_title("Intermediate-Area Usage Across Models")
    ax.set_xlabel("Arrival rate")
    ax.set_ylabel("Mean average intermediate occupancy")
    ax.set_xticks(x_positions, RATE_LABELS)
    ax.grid(True, axis="y", alpha=0.25)
    ax.legend(frameon=False)
    save_figure(fig, "04-intermediate-usage.png")


def plot_recharge_and_conflicts(stats: dict[str, dict[str, object]]) -> None:
    fig, axes = plt.subplots(1, 2, figsize=(12.2, 4.8))
    width = 0.22
    x_positions = list(range(len(RATES)))
    offsets = {"Model 1": -width, "Model 2": 0.0, "Model 3": width}

    for model_name in ("Model 1", "Model 2", "Model 3"):
        recharge_wait_means, _ = best_model_stats(stats[model_name], "recharge_wait_steps")
        conflict_means, _ = best_model_stats(stats[model_name], "blocked_conflicts")
        axes[0].bar([x + offsets[model_name] for x in x_positions], recharge_wait_means, width=width, label=model_name, color=MODEL_COLORS[model_name])
        axes[1].bar([x + offsets[model_name] for x in x_positions], conflict_means, width=width, label=model_name, color=MODEL_COLORS[model_name])

    axes[0].set_title("Recharge Waiting Steps")
    axes[0].set_xlabel("Arrival rate")
    axes[0].set_ylabel("Mean recharge waiting steps")
    axes[0].set_xticks(x_positions, RATE_LABELS)
    axes[0].grid(True, axis="y", alpha=0.25)

    axes[1].set_title("Blocked Conflicts")
    axes[1].set_xlabel("Arrival rate")
    axes[1].set_ylabel("Mean blocked conflicts")
    axes[1].set_xticks(x_positions, RATE_LABELS)
    axes[1].grid(True, axis="y", alpha=0.25)

    handles, labels = axes[0].get_legend_handles_labels()
    fig.legend(handles, labels, loc="upper center", ncol=3, frameon=False)
    fig.suptitle("Recharge Pressure and Congestion", y=1.03)
    save_figure(fig, "05-recharge-and-conflicts.png")


def main() -> None:
    stats = build_model_stats()
    plot_throughput(stats)
    plot_delivery_time(stats)
    plot_fleet_sensitivity(stats)
    plot_intermediate_usage(stats)
    plot_recharge_and_conflicts(stats)
    print(f"Generated figures in {OUTPUT_DIR}")


if __name__ == "__main__":
    main()