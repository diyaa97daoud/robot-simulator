package fr.emse.warehouse;

public class SimulationMetrics {
    private int totalDeliveryTime;
    private int deliveredPallets;
    private int totalDistance;
    private int totalRobotWaitSteps;
    private int rechargeCount;
    private int rechargeWaitSteps;
    private int blockedConflicts;
    private int messagesSent;
    private int intermediateOccupancySum;
    private int intermediateSamples;
    private int undeliveredBacklog;

    public void addDeliveryTime(int value) {
        totalDeliveryTime += Math.max(0, value);
    }

    public void incrementDeliveredPallets() {
        deliveredPallets++;
    }

    public void addDistance(int steps) {
        totalDistance += Math.max(0, steps);
    }

    public void addRobotWaitStep() {
        totalRobotWaitSteps++;
    }

    public void incrementRechargeCount() {
        rechargeCount++;
    }

    public void addRechargeWaitStep() {
        rechargeWaitSteps++;
    }

    public void incrementBlockedConflicts() {
        blockedConflicts++;
    }

    public void addMessagesSent(int c) {
        messagesSent += Math.max(0, c);
    }

    public void sampleIntermediateOccupancy(int occupancy) {
        intermediateOccupancySum += Math.max(0, occupancy);
        intermediateSamples++;
    }

    public int getTotalDeliveryTime() {
        return totalDeliveryTime;
    }

    public int getDeliveredPallets() {
        return deliveredPallets;
    }

    public int getTotalDistance() {
        return totalDistance;
    }

    public int getTotalRobotWaitSteps() {
        return totalRobotWaitSteps;
    }

    public int getRechargeCount() {
        return rechargeCount;
    }

    public int getRechargeWaitSteps() {
        return rechargeWaitSteps;
    }

    public int getBlockedConflicts() {
        return blockedConflicts;
    }

    public int getMessagesSent() {
        return messagesSent;
    }

    public double getAveragePalletDeliveryTime() {
        if (deliveredPallets == 0) {
            return 0.0d;
        }
        return ((double) totalDeliveryTime) / deliveredPallets;
    }

    public double getAverageIntermediateOccupancy() {
        if (intermediateSamples == 0) {
            return 0.0d;
        }
        return ((double) intermediateOccupancySum) / intermediateSamples;
    }

    public int getUndeliveredBacklog() {
        return undeliveredBacklog;
    }

    public void setUndeliveredBacklog(int undeliveredBacklog) {
        this.undeliveredBacklog = Math.max(0, undeliveredBacklog);
    }
}
