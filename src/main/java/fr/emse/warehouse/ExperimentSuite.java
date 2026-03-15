package fr.emse.warehouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ExperimentSuite {
    private final WarehouseConfig baseConfig;

    public ExperimentSuite(WarehouseConfig baseConfig) {
        this.baseConfig = baseConfig;
    }

    public List<SimulationResult> runDefaultComparison() throws IOException {
        List<SimulationResult> results = new ArrayList<SimulationResult>();
        int[] seeds = new int[] { baseConfig.getSeed(), baseConfig.getSeed() + 1, baseConfig.getSeed() + 2 };
        double[] rates = new double[] {
            Math.max(0.1d, baseConfig.getPalletRate() * 0.75d),
            baseConfig.getPalletRate(),
            Math.min(0.95d, baseConfig.getPalletRate() * 1.25d)
        };
        int[] fleets = new int[] {
            Math.max(2, baseConfig.getAmrCount() - 2),
            Math.max(2, baseConfig.getAmrCount() - 1),
            Math.max(2, baseConfig.getAmrCount())
        };

        for (int seed : seeds) {
            for (double rate : rates) {
                String rateTag = String.valueOf(Math.round(rate * 1000));
                String referenceMetricsFile = "build/suite/reference-seed-" + seed + "-rate-" + rateTag + ".csv";
                WarehouseConfig reference = baseConfig.copyWith(
                    SimulationMode.REFERENCE,
                    seed,
                    baseConfig.getAmrCount(),
                    rate,
                    referenceMetricsFile
                );
                results.add(new WarehouseSimulator(reference).run());

                for (int fleet : fleets) {
                    String optimizedMetricsFile = "build/suite/optimized-seed-" + seed + "-rate-" + rateTag + "-fleet-" + fleet + ".csv";
                    WarehouseConfig optimized = baseConfig.copyWith(
                        SimulationMode.OPTIMIZED,
                        seed,
                        fleet,
                        rate,
                        optimizedMetricsFile
                    );
                    results.add(new WarehouseSimulator(optimized).run());
                }
            }
        }
        return results;
    }

    public MinimumAmrSearchResult findMinimumFleetForTarget(double targetAverageDeliveryTime, int minFleet, int maxFleet) throws IOException {
        int low = Math.max(1, minFleet);
        int high = Math.max(low, maxFleet);
        int[] seeds = new int[] { baseConfig.getSeed(), baseConfig.getSeed() + 1, baseConfig.getSeed() + 2 };

        int selectedFleet = high;
        double selectedAverage = Double.MAX_VALUE;
        boolean targetReached = false;

        for (int fleet = low; fleet <= high; fleet++) {
            double sum = 0.0d;
            int count = 0;
            for (int seed : seeds) {
                String metricsFile = "build/min-amr-search/metrics-fleet-" + fleet + "-seed-" + seed + ".csv";
                WarehouseConfig optimized = baseConfig.copyWith(
                    SimulationMode.OPTIMIZED,
                    seed,
                    fleet,
                    baseConfig.getPalletRate(),
                    metricsFile
                );
                SimulationResult result = new WarehouseSimulator(optimized).run();
                sum += result.getMetrics().getAveragePalletDeliveryTime();
                count++;
            }
            double avg = count == 0 ? Double.MAX_VALUE : sum / count;

            if (avg < selectedAverage) {
                selectedAverage = avg;
                selectedFleet = fleet;
            }

            if (avg <= targetAverageDeliveryTime) {
                selectedFleet = fleet;
                selectedAverage = avg;
                targetReached = true;
                break;
            }
        }

        return new MinimumAmrSearchResult(
            low,
            high,
            selectedFleet,
            targetAverageDeliveryTime,
            selectedAverage,
            targetReached
        );
    }
}
