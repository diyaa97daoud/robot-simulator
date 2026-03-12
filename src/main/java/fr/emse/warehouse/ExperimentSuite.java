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
                WarehouseConfig reference = baseConfig.copyWith(
                    SimulationMode.REFERENCE,
                    seed,
                    baseConfig.getAmrCount(),
                    rate,
                    baseConfig.getMetricsOutputFile()
                );
                results.add(new WarehouseSimulator(reference).run());

                for (int fleet : fleets) {
                    WarehouseConfig optimized = baseConfig.copyWith(
                        SimulationMode.OPTIMIZED,
                        seed,
                        fleet,
                        rate,
                        baseConfig.getMetricsOutputFile()
                    );
                    results.add(new WarehouseSimulator(optimized).run());
                }
            }
        }
        return results;
    }
}
