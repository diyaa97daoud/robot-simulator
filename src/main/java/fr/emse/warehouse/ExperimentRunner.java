package fr.emse.warehouse;

import java.io.IOException;

public class ExperimentRunner {
    private final WarehouseConfig config;

    public ExperimentRunner(WarehouseConfig config) {
        this.config = config;
    }

    public SimulationResult runSingle() throws IOException {
        WarehouseSimulator simulator = new WarehouseSimulator(config);
        return simulator.run();
    }

    public static void printResult(SimulationResult result) {
        SimulationMetrics m = result.getMetrics();
        System.out.println("Delivered pallets: " + m.getDeliveredPallets());
        System.out.println("Total delivery time: " + m.getTotalDeliveryTime());
        System.out.println("Average delivery time: " + String.format("%.3f", m.getAveragePalletDeliveryTime()));
        System.out.println("Total distance: " + m.getTotalDistance());
        System.out.println("Recharges: " + m.getRechargeCount());
        System.out.println("Recharge wait steps: " + m.getRechargeWaitSteps());
        System.out.println("Blocked conflicts: " + m.getBlockedConflicts());
        System.out.println("Messages sent: " + m.getMessagesSent());
        System.out.println("Avg intermediate occupancy: " + String.format("%.3f", m.getAverageIntermediateOccupancy()));
    }
}
