package fr.emse;

import fr.emse.warehouse.ExperimentRunner;
import fr.emse.warehouse.ExperimentSuite;
import fr.emse.warehouse.SimulationResult;
import fr.emse.warehouse.WarehouseConfig;
import fr.emse.warehouse.WarehouseUiApp;

public class Main { 
    public static void main(String[] args) throws Exception { 
        String configPath = "configuration.ini";
        boolean runSuite = false;
        boolean runUi = false;
        for (String arg : args) {
            if ("--suite".equalsIgnoreCase(arg)) {
                runSuite = true;
            } else if ("--ui".equalsIgnoreCase(arg)) {
                runUi = true;
            } else {
                configPath = arg;
            }
        }
        if (runUi) {
            WarehouseUiApp.launch(configPath);
            return;
        }
        WarehouseConfig config = WarehouseConfig.fromIni(configPath);
        if (runSuite) {
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