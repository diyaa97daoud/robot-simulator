package fr.emse.warehouse;

public class SimulationResult {
    private final SimulationMode mode;
    private final int seed;
    private final int amrCount;
    private final SimulationMetrics metrics;

    public SimulationResult(SimulationMode mode, int seed, int amrCount, SimulationMetrics metrics) {
        this.mode = mode;
        this.seed = seed;
        this.amrCount = amrCount;
        this.metrics = metrics;
    }

    public String toCsvHeader() {
        return "mode,seed,amrCount,delivered,undeliveredBacklog,totalDeliveryTime,avgDeliveryTime,totalDistance,rechargeCount,rechargeWaitSteps,blockedConflicts,messagesSent,avgIntermediateOccupancy";
    }

    public String toCsvRow() {
        return mode + "," + seed + "," + amrCount + ","
            + metrics.getDeliveredPallets() + ","
            + metrics.getUndeliveredBacklog() + ","
            + metrics.getTotalDeliveryTime() + ","
            + String.format("%.3f", metrics.getAveragePalletDeliveryTime()) + ","
            + metrics.getTotalDistance() + ","
            + metrics.getRechargeCount() + ","
            + metrics.getRechargeWaitSteps() + ","
            + metrics.getBlockedConflicts() + ","
            + metrics.getMessagesSent() + ","
            + String.format("%.3f", metrics.getAverageIntermediateOccupancy());
    }

    public SimulationMetrics getMetrics() {
        return metrics;
    }
}
