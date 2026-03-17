package fr.emse.warehouse;

public class Zone {
    private final String id;
    private final ZoneType type;
    private final Position position;
    private final int capacity;
    private int occupancy;

    public Zone(String id, ZoneType type, Position position, int capacity) {
        this.id = id;
        this.type = type;
        this.position = position;
        this.capacity = Math.max(0, capacity);
        this.occupancy = 0;
    }

    public String getId() {
        return id;
    }

    public ZoneType getType() {
        return type;
    }

    public Position getPosition() {
        return position;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getOccupancy() {
        return occupancy;
    }

    public boolean hasSpace() {
        return occupancy < capacity;
    }

    public void incrementOccupancy() {
        occupancy++;
    }

    public void decrementOccupancy() {
        if (occupancy > 0) {
            occupancy--;
        }
    }
}
