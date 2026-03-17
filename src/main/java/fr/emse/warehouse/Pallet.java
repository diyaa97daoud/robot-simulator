package fr.emse.warehouse;

public class Pallet {
    private final int id;
    private final String entryZoneId;
    private final String destinationExitZoneId;
    private final int arrivalStep;
    private PalletStatus status;
    private Position position;
    private Integer assignedRobotId;

    public Pallet(int id, String entryZoneId, String destinationExitZoneId, int arrivalStep, Position position) {
        this.id = id;
        this.entryZoneId = entryZoneId;
        this.destinationExitZoneId = destinationExitZoneId;
        this.arrivalStep = arrivalStep;
        this.position = position;
        this.status = PalletStatus.WAITING_AT_ENTRY;
    }

    public int getId() {
        return id;
    }

    public String getEntryZoneId() {
        return entryZoneId;
    }

    public String getDestinationExitZoneId() {
        return destinationExitZoneId;
    }

    public int getArrivalStep() {
        return arrivalStep;
    }

    public PalletStatus getStatus() {
        return status;
    }

    public void setStatus(PalletStatus status) {
        this.status = status;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public Integer getAssignedRobotId() {
        return assignedRobotId;
    }

    public void setAssignedRobotId(Integer assignedRobotId) {
        this.assignedRobotId = assignedRobotId;
    }
}
