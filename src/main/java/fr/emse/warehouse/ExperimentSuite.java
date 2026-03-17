package fr.emse.warehouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ExperimentSuite {
    private static final int DEFAULT_SUITE_SEED_COUNT = 10;
    private static final int DEFAULT_SUITE_STEPS = 300;

    private final WarehouseConfig baseConfig;

    public ExperimentSuite(WarehouseConfig baseConfig) {
        this.baseConfig = baseConfig;
    }

    public List<SimulationResult> runDefaultComparison() throws IOException {
        List<SimulationResult> results = new ArrayList<SimulationResult>();
        String runTag = "run-" + System.currentTimeMillis();
        String suiteDir = "build/suite/" + runTag;
        int[] seeds = buildSeedRange(baseConfig.getSeed(), DEFAULT_SUITE_SEED_COUNT);
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
                String referenceMetricsFile = suiteDir + "/reference-seed-" + seed + "-rate-" + rateTag + ".csv";
                WarehouseConfig reference = baseConfig.copyWith(
                    SimulationMode.REFERENCE,
                    seed,
                    baseConfig.getAmrCount(),
                    rate,
                    DEFAULT_SUITE_STEPS,
                    referenceMetricsFile
                );
                results.add(new WarehouseSimulator(reference).run());

                for (int fleet : fleets) {
                    String optimizedMetricsFile = suiteDir + "/optimized-seed-" + seed + "-rate-" + rateTag + "-fleet-" + fleet + ".csv";
                    WarehouseConfig optimized = baseConfig.copyWith(
                        SimulationMode.OPTIMIZED,
                        seed,
                        fleet,
                        rate,
                        DEFAULT_SUITE_STEPS,
                        optimizedMetricsFile
                    );
                    results.add(new WarehouseSimulator(optimized).run());
                }
            }
        }

        int expectedScenarios = seeds.length * rates.length * (1 + fleets.length);
        if (results.size() != expectedScenarios) {
            throw new IOException("Suite completed " + results.size() + " scenarios, expected " + expectedScenarios);
        }
        return results;
    }

    private int[] buildSeedRange(int startSeed, int count) {
        int safeCount = Math.max(1, count);
        int[] seeds = new int[safeCount];
        for (int i = 0; i < safeCount; i++) {
            seeds[i] = startSeed + i;
        }
        return seeds;
    }

    public MinimumAmrSearchResult findMinimumFleetForTarget(double targetAverageDeliveryTime, int minFleet, int maxFleet) throws IOException {
        int low = Math.max(1, minFleet);
        int high = Math.max(low, maxFleet);
        String runTag = "run-" + System.currentTimeMillis();
        String searchDir = "build/min-amr-search/" + runTag;
        int[] seeds = new int[] { baseConfig.getSeed(), baseConfig.getSeed() + 1, baseConfig.getSeed() + 2 };

        int selectedFleet = high;
        double selectedAverage = Double.MAX_VALUE;
        boolean targetReached = false;

        for (int fleet = low; fleet <= high; fleet++) {
            double sum = 0.0d;
            int count = 0;
            for (int seed : seeds) {
                String metricsFile = searchDir + "/metrics-fleet-" + fleet + "-seed-" + seed + ".csv";
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
