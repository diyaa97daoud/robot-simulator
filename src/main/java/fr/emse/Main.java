package fr.emse;

import fr.emse.warehouse.ExperimentRunner;
import fr.emse.warehouse.ExperimentSuite;
import fr.emse.warehouse.MinimumAmrSearchResult;
import fr.emse.warehouse.SimulationResult;
import fr.emse.warehouse.WarehouseConfig;
import fr.emse.warehouse.WarehouseUiApp;

public class Main { 
    public static void main(String[] args) throws Exception { 
        String configPath = "configuration.ini";
        boolean runSuite = false;
        boolean runMinAmrSearch = false;
        boolean runUi = false;
        Double targetAverageDelivery = null;
        Integer minFleet = null;
        Integer maxFleet = null;
        for (String arg : args) {
            if ("--suite".equalsIgnoreCase(arg)) {
                runSuite = true;
            } else if ("--min-amr-search".equalsIgnoreCase(arg)) {
                runMinAmrSearch = true;
            } else if ("--ui".equalsIgnoreCase(arg)) {
                runUi = true;
            } else if (arg.startsWith("--target-avg=")) {
                targetAverageDelivery = Double.parseDouble(arg.substring("--target-avg=".length()));
            } else if (arg.startsWith("--min-fleet=")) {
                minFleet = Integer.parseInt(arg.substring("--min-fleet=".length()));
            } else if (arg.startsWith("--max-fleet=")) {
                maxFleet = Integer.parseInt(arg.substring("--max-fleet=".length()));
            } else {
                configPath = arg;
            }
        }
        if (runUi) {
            WarehouseUiApp.launch(configPath);
            return;
        }
        WarehouseConfig config = WarehouseConfig.fromIni(configPath);
        if (runMinAmrSearch) {
            ExperimentSuite suite = new ExperimentSuite(config);
            double target = targetAverageDelivery == null ? 40.0d : targetAverageDelivery;
            int min = minFleet == null ? 1 : minFleet;
            int max = maxFleet == null ? Math.max(min, config.getAmrCount() + 4) : maxFleet;
            MinimumAmrSearchResult result = suite.findMinimumFleetForTarget(target, min, max);
            ExperimentRunner.printMinimumFleetResult(result);
        } else if (runSuite) {
            ExperimentSuite suite = new ExperimentSuite(config);
            for (SimulationResult result : suite.runDefaultComparison()) {
                ExperimentRunner.printResult(result);
            }
        } else {
            ExperimentRunner runner = new ExperimentRunner(config);
            SimulationResult result = runner.runSingle();
            ExperimentRunner.printResult(result);
        }
    } 
}