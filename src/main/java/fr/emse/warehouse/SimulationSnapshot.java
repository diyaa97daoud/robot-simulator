package fr.emse.warehouse;

import java.util.List;

public class SimulationSnapshot {
    private final int step;
    private final int rows;
    private final int columns;
    private final List<ZoneView> entries;
    private final List<ZoneView> exits;
    private final List<ZoneView> intermediates;
    private final ZoneView recharge;
    private final List<Position> fixedObstacles;
    private final List<Position> humans;
    private final List<RobotView> robots;
    private final List<PalletView> pallets;
    private final int delivered;

    public SimulationSnapshot(
        int step,
        int rows,
        int columns,
        List<ZoneView> entries,
        List<ZoneView> exits,
        List<ZoneView> intermediates,
        ZoneView recharge,
        List<Position> fixedObstacles,
        List<Position> humans,
        List<RobotView> robots,
        List<PalletView> pallets,
        int delivered
    ) {
        this.step = step;
        this.rows = rows;
        this.columns = columns;
        this.entries = entries;
        this.exits = exits;
        this.intermediates = intermediates;
        this.recharge = recharge;
        this.fixedObstacles = fixedObstacles;
        this.humans = humans;
        this.robots = robots;
        this.pallets = pallets;
        this.delivered = delivered;
    }

    public int getStep() {
        return step;
    }

    public int getRows() {
        return rows;
    }

    public int getColumns() {
        return columns;
    }

    public List<ZoneView> getEntries() {
        return entries;
    }

    public List<ZoneView> getExits() {
        return exits;
    }

    public List<ZoneView> getIntermediates() {
        return intermediates;
    }

    public ZoneView getRecharge() {
        return recharge;
    }

    public List<Position> getFixedObstacles() {
        return fixedObstacles;
    }

    public List<Position> getHumans() {
        return humans;
    }

    public List<RobotView> getRobots() {
        return robots;
    }

    public List<PalletView> getPallets() {
        return pallets;
    }

    public int getDelivered() {
        return delivered;
    }

    public static class ZoneView {
        private final String id;
        private final ZoneType type;
        private final Position position;
        private final int capacity;
        private final int occupancy;

        public ZoneView(String id, ZoneType type, Position position, int capacity, int occupancy) {
            this.id = id;
            this.type = type;
            this.position = position;
            this.capacity = capacity;
            this.occupancy = occupancy;
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
    }

    public static class RobotView {
        private final int id;
        private final Position position;
        private final RobotState state;
        private final int battery;
        private final int maxBattery;
        private final boolean carrying;

        public RobotView(int id, Position position, RobotState state, int battery, int maxBattery, boolean carrying) {
            this.id = id;
            this.position = position;
            this.state = state;
            this.battery = battery;
            this.maxBattery = maxBattery;
            this.carrying = carrying;
        }

        public int getId() {
            return id;
        }

        public Position getPosition() {
            return position;
        }

        public RobotState getState() {
            return state;
        }

        public int getBattery() {
            return battery;
        }

        public int getMaxBattery() {
            return maxBattery;
        }

        public boolean isCarrying() {
            return carrying;
        }
    }

    public static class PalletView {
        private final int id;
        private final Position position;
        private final String destinationExit;
        private final PalletStatus status;
        private final Integer assignedRobotId;

        public PalletView(int id, Position position, String destinationExit, PalletStatus status, Integer assignedRobotId) {
            this.id = id;
            this.position = position;
            this.destinationExit = destinationExit;
            this.status = status;
            this.assignedRobotId = assignedRobotId;
        }

        public int getId() {
            return id;
        }

        public Position getPosition() {
            return position;
        }

        public String getDestinationExit() {
            return destinationExit;
        }

        public PalletStatus getStatus() {
            return status;
        }

        public Integer getAssignedRobotId() {
            return assignedRobotId;
        }
    }
}
