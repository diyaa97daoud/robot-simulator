package fr.emse.warehouse;

public class RobotAgent {
    private final int id;
    private Position position;
    private final int maxBattery;
    private int battery;
    private RobotState state;
    private Integer carryingPalletId;
    private Integer targetPalletId;
    private String targetZoneId;
    private Integer rechargeStartStep;
    private int totalMoves;
    private int waitingSteps;
    private int tasksCompleted;

    public RobotAgent(int id, Position position, int maxBattery) {
        this.id = id;
        this.position = position;
        this.maxBattery = Math.max(1, maxBattery);
        this.battery = this.maxBattery;
        this.state = RobotState.IDLE;
    }

    public int getId() {
        return id;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public int getMaxBattery() {
        return maxBattery;
    }

    public int getBattery() {
        return battery;
    }

    public void setBattery(int battery) {
        this.battery = Math.max(0, Math.min(maxBattery, battery));
    }

    public RobotState getState() {
        return state;
    }

    public void setState(RobotState state) {
        this.state = state;
    }

    public Integer getCarryingPalletId() {
        return carryingPalletId;
    }

    public void setCarryingPalletId(Integer carryingPalletId) {
        this.carryingPalletId = carryingPalletId;
    }

    public Integer getTargetPalletId() {
        return targetPalletId;
    }

    public void setTargetPalletId(Integer targetPalletId) {
        this.targetPalletId = targetPalletId;
    }

    public String getTargetZoneId() {
        return targetZoneId;
    }

    public void setTargetZoneId(String targetZoneId) {
        this.targetZoneId = targetZoneId;
    }

    public Integer getRechargeStartStep() {
        return rechargeStartStep;
    }

    public void setRechargeStartStep(Integer rechargeStartStep) {
        this.rechargeStartStep = rechargeStartStep;
    }

    public int getTotalMoves() {
        return totalMoves;
    }

    public void incrementMoves() {
        totalMoves++;
    }

    public int getWaitingSteps() {
        return waitingSteps;
    }

    public void incrementWaitingSteps() {
        waitingSteps++;
    }

    public int getTasksCompleted() {
        return tasksCompleted;
    }

    public void incrementTasksCompleted() {
        tasksCompleted++;
    }

    public boolean isIdleLike() {
        return state == RobotState.IDLE || state == RobotState.WAITING;
    }
}
