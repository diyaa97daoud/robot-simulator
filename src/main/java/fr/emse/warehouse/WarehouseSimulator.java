package fr.emse.warehouse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WarehouseSimulator {
    private final WarehouseConfig config;
    private final GridPathfinder pathfinder;
    private final PalletGenerator generator;
    private final List<RobotAgent> robots;
    private final Map<Integer, Pallet> pallets;
    private final Map<String, Zone> zonesById;
    private final Set<Position> fixedBlocked;
    private final SimulationMetrics metrics;
    private final List<RobotMessage> messageBus;
    private int nextPalletId;

    public WarehouseSimulator(WarehouseConfig config) {
        this.config = config;
        this.pathfinder = new GridPathfinder(config.getRows(), config.getColumns());
        this.generator = new PalletGenerator(config.getSeed(), config.getPalletDistribution(), config.getPalletRate());
        this.robots = new ArrayList<RobotAgent>();
        this.pallets = new LinkedHashMap<Integer, Pallet>();
        this.zonesById = new HashMap<String, Zone>();
        this.fixedBlocked = new HashSet<Position>(config.getStaticObstacles());
        this.metrics = new SimulationMetrics();
        this.messageBus = new ArrayList<RobotMessage>();
        this.nextPalletId = 1;

        registerZones();
        if (config.getMode() == SimulationMode.OPTIMIZED) {
            initializeOptimizedFleet();
        }
    }

    private void registerZones() {
        for (Zone z : config.getEntryZones()) {
            zonesById.put(z.getId(), z);
        }
        for (Zone z : config.getExitZones()) {
            zonesById.put(z.getId(), z);
        }
        for (Zone z : config.getIntermediateZones()) {
            zonesById.put(z.getId(), z);
        }
        zonesById.put(config.getRechargeZone().getId(), config.getRechargeZone());
    }

    private void initializeOptimizedFleet() {
        for (int i = 1; i <= Math.max(1, config.getAmrCount()); i++) {
            Zone spawn = config.getEntryZones().get((i - 1) % config.getEntryZones().size());
            robots.add(new RobotAgent(i, spawn.getPosition(), config.getMaxBattery()));
        }
    }

    public SimulationResult run() throws IOException {
        return run(null);
    }

    public SimulationResult run(SimulationStepListener listener) throws IOException {
        List<Position> humans = new ArrayList<Position>(config.getHumans());

        for (int step = 0; step < config.getSteps(); step++) {
            spawnPallets(step);

            humans = generator.moveHumans(humans, config.getRows(), config.getColumns(), new ArrayList<Position>(fixedBlocked));
            Set<Position> blocked = collectBlocked(humans);

            if (config.getMode() == SimulationMode.REFERENCE) {
                runReferenceStep(step, blocked);
            } else {
                runOptimizedStep(step, blocked);
            }

            int occupancy = 0;
            for (Zone intermediate : config.getIntermediateZones()) {
                occupancy += intermediate.getOccupancy();
            }
            metrics.sampleIntermediateOccupancy(occupancy);

            if (listener != null) {
                listener.onStep(buildSnapshot(step, humans));
                sleepForUiDelay();
            }
        }

        SimulationResult result = new SimulationResult(
            config.getMode(),
            config.getSeed(),
            config.getAmrCount(),
            metrics
        );
        writeMetrics(result, config.getMetricsOutputFile());
        if (listener != null) {
            listener.onCompleted(result);
        }
        return result;
    }

    private void sleepForUiDelay() {
        int delay = Math.max(0, config.getUiStepDelayMs());
        if (delay == 0) {
            return;
        }
        try {
            Thread.sleep(delay);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private SimulationSnapshot buildSnapshot(int step, List<Position> humans) {
        List<SimulationSnapshot.ZoneView> entries = new ArrayList<SimulationSnapshot.ZoneView>();
        for (Zone zone : config.getEntryZones()) {
            entries.add(new SimulationSnapshot.ZoneView(
                zone.getId(),
                zone.getType(),
                zone.getPosition(),
                zone.getCapacity(),
                zone.getOccupancy()
            ));
        }
        List<SimulationSnapshot.ZoneView> exits = new ArrayList<SimulationSnapshot.ZoneView>();
        for (Zone zone : config.getExitZones()) {
            exits.add(new SimulationSnapshot.ZoneView(
                zone.getId(),
                zone.getType(),
                zone.getPosition(),
                zone.getCapacity(),
                zone.getOccupancy()
            ));
        }
        List<SimulationSnapshot.ZoneView> intermediates = new ArrayList<SimulationSnapshot.ZoneView>();
        for (Zone zone : config.getIntermediateZones()) {
            intermediates.add(new SimulationSnapshot.ZoneView(
                zone.getId(),
                zone.getType(),
                zone.getPosition(),
                zone.getCapacity(),
                zone.getOccupancy()
            ));
        }
        Zone recharge = config.getRechargeZone();
        SimulationSnapshot.ZoneView rechargeView = new SimulationSnapshot.ZoneView(
            recharge.getId(),
            recharge.getType(),
            recharge.getPosition(),
            recharge.getCapacity(),
            countRechargingRobots()
        );

        List<SimulationSnapshot.RobotView> robotViews = new ArrayList<SimulationSnapshot.RobotView>();
        for (RobotAgent robot : robots) {
            robotViews.add(new SimulationSnapshot.RobotView(
                robot.getId(),
                robot.getPosition(),
                robot.getState(),
                robot.getBattery(),
                robot.getMaxBattery(),
                robot.getCarryingPalletId() != null
            ));
        }
        List<SimulationSnapshot.PalletView> palletViews = new ArrayList<SimulationSnapshot.PalletView>();
        for (Pallet pallet : pallets.values()) {
            if (pallet.getStatus() == PalletStatus.DELIVERED) {
                continue;
            }
            palletViews.add(new SimulationSnapshot.PalletView(
                pallet.getId(),
                pallet.getPosition(),
                pallet.getDestinationExitZoneId(),
                pallet.getStatus(),
                pallet.getAssignedRobotId()
            ));
        }

        return new SimulationSnapshot(
            step,
            config.getRows(),
            config.getColumns(),
            entries,
            exits,
            intermediates,
            rechargeView,
            new ArrayList<Position>(fixedBlocked),
            new ArrayList<Position>(humans),
            robotViews,
            palletViews,
            metrics.getDeliveredPallets()
        );
    }

    private int countRechargingRobots() {
        int charging = 0;
        for (RobotAgent robot : robots) {
            if (robot.getState() == RobotState.RECHARGING) {
                charging++;
            }
        }
        return charging;
    }

    private void runReferenceStep(int step, Set<Position> blocked) {
        List<Pallet> readyPallets = listReadyPallets();
        for (Pallet pallet : readyPallets) {
            Zone exitZone = zonesById.get(pallet.getDestinationExitZoneId());
            if (exitZone == null) {
                continue;
            }
            int dist = pathfinder.shortestDistance(pallet.getPosition(), exitZone.getPosition(), blocked);
            if (dist >= Integer.MAX_VALUE / 8) {
                continue;
            }
            metrics.addDistance(dist);
            metrics.addDeliveryTime(Math.max(0, step - pallet.getArrivalStep() + dist));
            metrics.incrementDeliveredPallets();
            pallet.setStatus(PalletStatus.DELIVERED);
            pallet.setAssignedRobotId(null);
        }
    }

    private void runOptimizedStep(int step, Set<Position> blocked) {
        processRecharging(step);
        createBidsAndClaims(blocked);

        Map<Integer, Position> proposedMoves = new HashMap<Integer, Position>();
        for (RobotAgent robot : robots) {
            Position next = decideNextPosition(robot, blocked, step);
            proposedMoves.put(robot.getId(), next);
        }

        Map<Integer, Position> approvedMoves = resolveConflicts(proposedMoves);
        for (RobotAgent robot : robots) {
            Position oldPos = robot.getPosition();
            Position newPos = approvedMoves.get(robot.getId());
            if (!oldPos.equals(newPos)) {
                robot.setPosition(newPos);
                robot.setBattery(robot.getBattery() - 1);
                robot.incrementMoves();
                metrics.addDistance(1);
            } else {
                robot.incrementWaitingSteps();
                metrics.addRobotWaitStep();
            }
            applyStateTransitions(robot, step);
        }
    }

    private void processRecharging(int step) {
        int activeCharging = 0;
        for (RobotAgent robot : robots) {
            if (robot.getState() == RobotState.RECHARGING) {
                activeCharging++;
            }
        }

        for (RobotAgent robot : robots) {
            if (robot.getState() == RobotState.RECHARGING) {
                Integer start = robot.getRechargeStartStep();
                if (start != null && step - start >= config.getRechargeDuration()) {
                    robot.setBattery(robot.getMaxBattery());
                    robot.setState(RobotState.IDLE);
                    robot.setRechargeStartStep(null);
                }
            } else if (robot.getState() == RobotState.MOVING_TO_RECHARGE
                && robot.getPosition().equals(config.getRechargeZone().getPosition())) {
                if (activeCharging < config.getRechargeCapacity()) {
                    robot.setState(RobotState.RECHARGING);
                    robot.setRechargeStartStep(step);
                    activeCharging++;
                    metrics.incrementRechargeCount();
                } else {
                    robot.setState(RobotState.WAITING);
                    metrics.addRechargeWaitStep();
                }
            }
        }
    }

    private void createBidsAndClaims(Set<Position> blocked) {
        messageBus.clear();

        List<Pallet> waitingPallets = listReadyPallets();
        if (waitingPallets.isEmpty()) {
            return;
        }

        for (RobotAgent robot : robots) {
            if (!robot.isIdleLike()) {
                continue;
            }
            if (robot.getBattery() <= config.getCriticalThreshold()) {
                robot.setState(RobotState.MOVING_TO_RECHARGE);
                continue;
            }

            Bid best = computeBestBid(robot, waitingPallets, blocked);
            if (best != null) {
                messageBus.add(new RobotMessage(
                    MessageType.BID, robot.getId(), null, best.pallet.getId(), best.score, best.targetZoneId
                ));
            }
        }
        metrics.addMessagesSent(messageBus.size());

        Map<Integer, List<RobotMessage>> bidsByPallet = new HashMap<Integer, List<RobotMessage>>();
        for (RobotMessage message : messageBus) {
            if (message.getPalletId() == null) {
                continue;
            }
            bidsByPallet.computeIfAbsent(message.getPalletId(), k -> new ArrayList<RobotMessage>()).add(message);
        }

        for (Map.Entry<Integer, List<RobotMessage>> entry : bidsByPallet.entrySet()) {
            Integer palletId = entry.getKey();
            Pallet pallet = pallets.get(palletId);
            if (pallet == null || pallet.getAssignedRobotId() != null || pallet.getStatus() != PalletStatus.WAITING_AT_ENTRY) {
                continue;
            }
            List<RobotMessage> bids = entry.getValue();
            bids.sort(Comparator
                .comparingDouble(RobotMessage::getScore)
                .thenComparingInt(msg -> robotTaskLoad(msg.getSenderRobotId()))
                .thenComparingInt(RobotMessage::getSenderRobotId));

            RobotMessage winner = bids.get(0);
            RobotAgent robot = robotById(winner.getSenderRobotId());
            if (robot == null) {
                continue;
            }
            pallet.setAssignedRobotId(robot.getId());
            robot.setTargetPalletId(palletId);
            robot.setTargetZoneId(winner.getPayload());
            robot.setState(RobotState.MOVING_TO_PICKUP);

            messageBus.add(new RobotMessage(
                MessageType.CLAIM, robot.getId(), null, palletId, winner.getScore(), winner.getPayload()
            ));
            metrics.addMessagesSent(1);
        }
    }

    private Position decideNextPosition(RobotAgent robot, Set<Position> blocked, int step) {
        if (robot.getState() == RobotState.RECHARGING) {
            return robot.getPosition();
        }
        if (robot.getBattery() <= config.getCriticalThreshold() && robot.getState() != RobotState.MOVING_TO_RECHARGE) {
            robot.setState(RobotState.MOVING_TO_RECHARGE);
        }

        Position target = null;
        switch (robot.getState()) {
            case MOVING_TO_PICKUP:
                target = targetPickupPosition(robot);
                break;
            case CARRYING_TO_EXIT:
            case CARRYING_TO_INTERMEDIATE:
            case MOVING_TO_INTERMEDIATE_PICKUP:
                target = targetZonePosition(robot.getTargetZoneId());
                break;
            case MOVING_TO_RECHARGE:
            case WAITING:
                target = config.getRechargeZone().getPosition();
                break;
            default:
                return robot.getPosition();
        }
        if (target == null) {
            return robot.getPosition();
        }
        return pathfinder.nextStep(robot.getPosition(), target, blocked);
    }

    private void applyStateTransitions(RobotAgent robot, int step) {
        if (robot.getState() == RobotState.MOVING_TO_INTERMEDIATE_PICKUP) {
            Pallet pallet = palletById(robot.getTargetPalletId());
            if (pallet == null || pallet.getStatus() != PalletStatus.STORED_INTERMEDIATE) {
                resetRobotTask(robot);
                return;
            }
            if (robot.getPosition().equals(pallet.getPosition())) {
                Zone inter = findIntermediateByPosition(pallet.getPosition());
                if (inter != null) {
                    inter.decrementOccupancy();
                }
                pallet.setStatus(PalletStatus.CARRIED_BY_ROBOT);
                robot.setCarryingPalletId(pallet.getId());
                robot.setTargetZoneId(pallet.getDestinationExitZoneId());
                robot.setState(RobotState.CARRYING_TO_EXIT);
            }
            return;
        }

        if (robot.getState() == RobotState.MOVING_TO_PICKUP) {
            Pallet pallet = palletById(robot.getTargetPalletId());
            if (pallet == null || pallet.getStatus() != PalletStatus.WAITING_AT_ENTRY) {
                robot.setState(RobotState.IDLE);
                robot.setTargetPalletId(null);
                robot.setTargetZoneId(null);
                return;
            }
            if (robot.getPosition().equals(pallet.getPosition())) {
                pallet.setStatus(PalletStatus.CARRIED_BY_ROBOT);
                robot.setCarryingPalletId(pallet.getId());
                String targetZoneId = robot.getTargetZoneId();
                if (targetZoneId != null && targetZoneId.startsWith("I")) {
                    robot.setState(RobotState.CARRYING_TO_INTERMEDIATE);
                } else {
                    robot.setTargetZoneId(pallet.getDestinationExitZoneId());
                    robot.setState(RobotState.CARRYING_TO_EXIT);
                }
            }
            return;
        }

        if (robot.getState() == RobotState.CARRYING_TO_INTERMEDIATE) {
            Pallet carrying = palletById(robot.getCarryingPalletId());
            Zone inter = zoneById(robot.getTargetZoneId(), ZoneType.INTERMEDIATE);
            if (carrying == null || inter == null) {
                resetRobotTask(robot);
                return;
            }
            carrying.setPosition(robot.getPosition());
            if (robot.getPosition().equals(inter.getPosition())) {
                if (inter.hasSpace()) {
                    inter.incrementOccupancy();
                    carrying.setStatus(PalletStatus.STORED_INTERMEDIATE);
                    carrying.setAssignedRobotId(null);
                    carrying.setPosition(inter.getPosition());
                    robot.setCarryingPalletId(null);
                    robot.setTargetPalletId(null);
                    robot.setTargetZoneId(null);
                    robot.setState(RobotState.IDLE);
                } else {
                    robot.setState(RobotState.CARRYING_TO_EXIT);
                    robot.setTargetZoneId(carrying.getDestinationExitZoneId());
                }
            }
            return;
        }

        if (robot.getState() == RobotState.CARRYING_TO_EXIT) {
            Pallet carrying = palletById(robot.getCarryingPalletId());
            if (carrying == null) {
                resetRobotTask(robot);
                return;
            }
            Zone exit = zoneById(carrying.getDestinationExitZoneId(), ZoneType.EXIT);
            carrying.setPosition(robot.getPosition());
            if (exit != null && robot.getPosition().equals(exit.getPosition())) {
                carrying.setStatus(PalletStatus.DELIVERED);
                carrying.setAssignedRobotId(null);
                metrics.incrementDeliveredPallets();
                metrics.addDeliveryTime(step - carrying.getArrivalStep());
                robot.incrementTasksCompleted();
                resetRobotTask(robot);
            }
            return;
        }

        if (robot.getState() == RobotState.IDLE || robot.getState() == RobotState.WAITING) {
            if (robot.getBattery() <= config.getWarningThreshold()) {
                robot.setState(RobotState.MOVING_TO_RECHARGE);
            } else {
                claimIntermediateIfNearby(robot);
            }
        }
    }

    private void claimIntermediateIfNearby(RobotAgent robot) {
        if (!robot.isIdleLike()) {
            return;
        }
        Pallet best = null;
        int bestDist = Integer.MAX_VALUE;
        for (Pallet pallet : pallets.values()) {
            if (pallet.getStatus() != PalletStatus.STORED_INTERMEDIATE || pallet.getAssignedRobotId() != null) {
                continue;
            }
            int dist = robot.getPosition().manhattanDistance(pallet.getPosition());
            if (dist < bestDist) {
                bestDist = dist;
                best = pallet;
            }
        }
        if (best == null) {
            robot.setState(RobotState.IDLE);
            return;
        }
        best.setAssignedRobotId(robot.getId());
        robot.setTargetPalletId(best.getId());
        robot.setTargetZoneId(best.getDestinationExitZoneId());
        robot.setState(RobotState.MOVING_TO_INTERMEDIATE_PICKUP);
    }

    private Map<Integer, Position> resolveConflicts(Map<Integer, Position> proposedMoves) {
        Map<Integer, Position> approved = new HashMap<Integer, Position>();
        Map<Position, List<Integer>> byPosition = new HashMap<Position, List<Integer>>();

        for (Map.Entry<Integer, Position> entry : proposedMoves.entrySet()) {
            byPosition.computeIfAbsent(entry.getValue(), k -> new ArrayList<Integer>()).add(entry.getKey());
        }

        for (Map.Entry<Integer, Position> entry : proposedMoves.entrySet()) {
            approved.put(entry.getKey(), entry.getValue());
        }

        for (Map.Entry<Position, List<Integer>> conflict : byPosition.entrySet()) {
            List<Integer> contenders = conflict.getValue();
            if (contenders.size() <= 1) {
                continue;
            }
            contenders.sort((a, b) -> comparePriority(robotById(a), robotById(b)));
            Integer winner = contenders.get(0);
            for (Integer robotId : contenders) {
                if (!robotId.equals(winner)) {
                    RobotAgent robot = robotById(robotId);
                    approved.put(robotId, robot.getPosition());
                    metrics.incrementBlockedConflicts();
                }
            }
        }

        return approved;
    }

    private int comparePriority(RobotAgent left, RobotAgent right) {
        boolean leftCarrying = left.getCarryingPalletId() != null;
        boolean rightCarrying = right.getCarryingPalletId() != null;
        if (leftCarrying != rightCarrying) {
            return leftCarrying ? -1 : 1;
        }

        boolean leftRecharge = left.getState() == RobotState.MOVING_TO_RECHARGE;
        boolean rightRecharge = right.getState() == RobotState.MOVING_TO_RECHARGE;
        if (leftRecharge && rightRecharge && left.getBattery() != right.getBattery()) {
            return Integer.compare(left.getBattery(), right.getBattery());
        }

        int leftLoad = robotTaskLoad(left.getId());
        int rightLoad = robotTaskLoad(right.getId());
        if (leftLoad != rightLoad) {
            return Integer.compare(leftLoad, rightLoad);
        }
        return Integer.compare(left.getId(), right.getId());
    }

    private Bid computeBestBid(RobotAgent robot, List<Pallet> waitingPallets, Set<Position> blocked) {
        Bid best = null;
        for (Pallet pallet : waitingPallets) {
            if (pallet.getAssignedRobotId() != null) {
                continue;
            }
            Zone exit = zoneById(pallet.getDestinationExitZoneId(), ZoneType.EXIT);
            if (exit == null) {
                continue;
            }
            int pickupCost = pathfinder.shortestDistance(robot.getPosition(), pallet.getPosition(), blocked);
            int directDeliveryCost = pathfinder.shortestDistance(pallet.getPosition(), exit.getPosition(), blocked);
            if (pickupCost >= Integer.MAX_VALUE / 8 || directDeliveryCost >= Integer.MAX_VALUE / 8) {
                continue;
            }
            int congestionPenalty = blocked.contains(pallet.getPosition()) ? 4 : 0;
            int needed = pickupCost + directDeliveryCost + config.getSafeMargin();
            int batteryPenalty = robot.getBattery() < needed ? (needed - robot.getBattery()) * 4 : 0;

            Zone bestIntermediate = bestIntermediateZone(pallet, exit, blocked);
            int queuePenalty = 0;
            String targetZoneId = exit.getId();
            int deliveryCost = directDeliveryCost;

            if (bestIntermediate != null) {
                int toInter = pathfinder.shortestDistance(pallet.getPosition(), bestIntermediate.getPosition(), blocked);
                int interToExit = pathfinder.shortestDistance(bestIntermediate.getPosition(), exit.getPosition(), blocked);
                int occupancyPenalty = bestIntermediate.getCapacity() == 0 ? 100 : (bestIntermediate.getOccupancy() * 5);
                int viaCost = toInter + interToExit + occupancyPenalty;
                if (viaCost + 2 < directDeliveryCost || robot.getBattery() < needed) {
                    deliveryCost = toInter;
                    targetZoneId = bestIntermediate.getId();
                    queuePenalty = occupancyPenalty;
                }
            }

            double score = pickupCost + deliveryCost + congestionPenalty + batteryPenalty + queuePenalty;
            if (best == null || score < best.score) {
                best = new Bid(pallet, score, targetZoneId);
            }
        }
        return best;
    }

    private Zone bestIntermediateZone(Pallet pallet, Zone exit, Set<Position> blocked) {
        Zone best = null;
        int bestScore = Integer.MAX_VALUE;
        for (Zone zone : config.getIntermediateZones()) {
            if (!zone.hasSpace()) {
                continue;
            }
            int toInter = pathfinder.shortestDistance(pallet.getPosition(), zone.getPosition(), blocked);
            int interToExit = pathfinder.shortestDistance(zone.getPosition(), exit.getPosition(), blocked);
            int score = toInter + interToExit + zone.getOccupancy() * 4;
            if (score < bestScore) {
                bestScore = score;
                best = zone;
            }
        }
        return best;
    }

    private void spawnPallets(int step) {
        int count = Math.max(0, generator.sampleCount());
        for (int i = 0; i < count; i++) {
            Zone entry = generator.chooseRandom(config.getEntryZones());
            String exitId = generator.randomExitZoneId(config.getExitZones());
            Pallet pallet = new Pallet(nextPalletId++, entry.getId(), exitId, step, entry.getPosition());
            pallets.put(pallet.getId(), pallet);
        }
    }

    private List<Pallet> listReadyPallets() {
        List<Pallet> ready = new ArrayList<Pallet>();
        for (Pallet pallet : pallets.values()) {
            if (pallet.getStatus() == PalletStatus.WAITING_AT_ENTRY && pallet.getAssignedRobotId() == null) {
                ready.add(pallet);
            }
        }
        return ready;
    }

    private Set<Position> collectBlocked(List<Position> humans) {
        Set<Position> blocked = new HashSet<Position>(fixedBlocked);
        blocked.addAll(humans);
        return blocked;
    }

    private Position targetPickupPosition(RobotAgent robot) {
        Pallet pallet = palletById(robot.getTargetPalletId());
        if (pallet == null) {
            return null;
        }
        if (robot.getState() == RobotState.MOVING_TO_INTERMEDIATE_PICKUP && pallet.getStatus() == PalletStatus.STORED_INTERMEDIATE) {
            return pallet.getPosition();
        }
        return pallet.getPosition();
    }

    private Position targetZonePosition(String zoneId) {
        Zone z = zonesById.get(zoneId);
        return z == null ? null : z.getPosition();
    }

    private RobotAgent robotById(int id) {
        for (RobotAgent robot : robots) {
            if (robot.getId() == id) {
                return robot;
            }
        }
        return null;
    }

    private int robotTaskLoad(int robotId) {
        RobotAgent robot = robotById(robotId);
        if (robot == null) {
            return Integer.MAX_VALUE / 4;
        }
        int load = 0;
        if (robot.getCarryingPalletId() != null) {
            load += 2;
        }
        if (robot.getTargetPalletId() != null) {
            load += 1;
        }
        if (robot.getState() == RobotState.MOVING_TO_RECHARGE || robot.getState() == RobotState.RECHARGING) {
            load += 1;
        }
        return load;
    }

    private Pallet palletById(Integer id) {
        if (id == null) {
            return null;
        }
        return pallets.get(id);
    }

    private Zone zoneById(String id, ZoneType type) {
        Zone z = zonesById.get(id);
        if (z == null) {
            return null;
        }
        return z.getType() == type ? z : null;
    }

    private Zone findIntermediateByPosition(Position position) {
        for (Zone zone : config.getIntermediateZones()) {
            if (zone.getPosition().equals(position)) {
                return zone;
            }
        }
        return null;
    }

    private void resetRobotTask(RobotAgent robot) {
        robot.setCarryingPalletId(null);
        robot.setTargetPalletId(null);
        robot.setTargetZoneId(null);
        robot.setState(RobotState.IDLE);
    }

    private void writeMetrics(SimulationResult result, String filePath) throws IOException {
        Path output = Path.of(filePath);
        boolean exists = Files.exists(output);
        if (!exists) {
            Files.writeString(output, result.toCsvHeader() + System.lineSeparator(), StandardOpenOption.CREATE);
        }
        Files.writeString(output, result.toCsvRow() + System.lineSeparator(), StandardOpenOption.APPEND);
    }

    private static class Bid {
        private final Pallet pallet;
        private final double score;
        private final String targetZoneId;

        private Bid(Pallet pallet, double score, String targetZoneId) {
            this.pallet = pallet;
            this.score = score;
            this.targetZoneId = targetZoneId;
        }
    }
}
