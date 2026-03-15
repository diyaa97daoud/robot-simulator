package fr.emse.warehouse;

public class MinimumAmrSearchResult {
    private final int minFleet;
    private final int maxFleet;
    private final int selectedFleet;
    private final double targetAverageDeliveryTime;
    private final double selectedAverageDeliveryTime;
    private final boolean targetReached;

    public MinimumAmrSearchResult(
        int minFleet,
        int maxFleet,
        int selectedFleet,
        double targetAverageDeliveryTime,
        double selectedAverageDeliveryTime,
        boolean targetReached
    ) {
        this.minFleet = minFleet;
        this.maxFleet = maxFleet;
        this.selectedFleet = selectedFleet;
        this.targetAverageDeliveryTime = targetAverageDeliveryTime;
        this.selectedAverageDeliveryTime = selectedAverageDeliveryTime;
        this.targetReached = targetReached;
    }

    public int getMinFleet() {
        return minFleet;
    }

    public int getMaxFleet() {
        return maxFleet;
    }

    public int getSelectedFleet() {
        return selectedFleet;
    }

    public double getTargetAverageDeliveryTime() {
        return targetAverageDeliveryTime;
    }

    public double getSelectedAverageDeliveryTime() {
        return selectedAverageDeliveryTime;
    }

    public boolean isTargetReached() {
        return targetReached;
    }
}