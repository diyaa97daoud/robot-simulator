package fr.emse.warehouse;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.ini4j.Ini;
import org.ini4j.Profile.Section;

public class WarehouseConfig {
    private final SimulationMode mode;
    private final CommunicationMode communicationMode;
    private final int rows;
    private final int columns;
    private final int steps;
    private final int seed;
    private final int amrCount;
    private final int maxBattery;
    private final int criticalThreshold;
    private final int warningThreshold;
    private final int safeMargin;
    private final int rechargeDuration;
    private final int rechargeCapacity;
    private final String palletDistribution;
    private final double palletRate;
    private final int uiStepDelayMs;
    private final List<Zone> entryZones;
    private final List<Zone> exitZones;
    private final List<Zone> intermediateZones;
    private final Zone rechargeZone;
    private final List<Position> staticObstacles;
    private final List<Position> humans;
    private final String metricsOutputFile;

    public WarehouseConfig(
        SimulationMode mode,
        CommunicationMode communicationMode,
        int rows,
        int columns,
        int steps,
        int seed,
        int amrCount,
        int maxBattery,
        int criticalThreshold,
        int warningThreshold,
        int safeMargin,
        int rechargeDuration,
        int rechargeCapacity,
        String palletDistribution,
        double palletRate,
        int uiStepDelayMs,
        List<Zone> entryZones,
        List<Zone> exitZones,
        List<Zone> intermediateZones,
        Zone rechargeZone,
        List<Position> staticObstacles,
        List<Position> humans,
        String metricsOutputFile
    ) {
        this.mode = mode;
        this.communicationMode = communicationMode;
        this.rows = rows;
        this.columns = columns;
        this.steps = steps;
        this.seed = seed;
        this.amrCount = amrCount;
        this.maxBattery = maxBattery;
        this.criticalThreshold = criticalThreshold;
        this.warningThreshold = warningThreshold;
        this.safeMargin = safeMargin;
        this.rechargeDuration = rechargeDuration;
        this.rechargeCapacity = rechargeCapacity;
        this.palletDistribution = palletDistribution;
        this.palletRate = palletRate;
        this.uiStepDelayMs = uiStepDelayMs;
        this.entryZones = entryZones;
        this.exitZones = exitZones;
        this.intermediateZones = intermediateZones;
        this.rechargeZone = rechargeZone;
        this.staticObstacles = staticObstacles;
        this.humans = humans;
        this.metricsOutputFile = metricsOutputFile;
    }

    public static WarehouseConfig fromIni(String path) throws IOException {
        Ini ini = new Ini(new File(path));

        Section simulation = ini.get("simulation");
        Section warehouse = ini.get("warehouse");
        Section battery = ini.get("battery");
        Section zones = ini.get("zones");
        Section arrivals = ini.get("arrivals");
        Section obstacles = ini.get("obstacles");
        Section output = ini.get("output");
        Section ui = ini.get("ui");

        SimulationMode mode = SimulationMode.fromString(readString(simulation, "mode", "optimized"));
        Section communication = ini.get("communication");
        CommunicationMode communicationMode = CommunicationMode.fromString(readString(communication, "mode", "broadcast"));
        int rows = readInt(warehouse, "rows", 30);
        int columns = readInt(warehouse, "columns", 45);
        int steps = readInt(simulation, "steps", 1500);
        int seed = readInt(simulation, "seed", 42);
        int amrCount = readInt(simulation, "amrCount", 6);

        int maxBattery = readInt(battery, "maxBattery", 80);
        int criticalThreshold = readInt(battery, "criticalThreshold", 12);
        int warningThreshold = readInt(battery, "warningThreshold", 24);
        int safeMargin = readInt(battery, "safeMargin", 8);
        int rechargeDuration = readInt(battery, "rechargeDuration", 12);
        int rechargeCapacity = readInt(battery, "rechargeCapacity", 2);

        String dist = readString(arrivals, "distribution", "poisson");
        double rate = readDouble(arrivals, "rate", 0.4d);
        int uiStepDelay = readInt(ui, "stepDelayMs", 80);

        List<Zone> entryZones = parseZones(readString(zones, "entries", "A1:3,2|A2:3,14|A3:3,24"), ZoneType.ENTRY, 9999);
        List<Zone> exitZones = parseZones(readString(zones, "exits", "Z1:40,3|Z2:40,26"), ZoneType.EXIT, 9999);
        List<Zone> intermediate = parseZones(
            readString(zones, "intermediates", "I1:22,10:3|I2:22,20:3"),
            ZoneType.INTERMEDIATE,
            3
        );

        Zone recharge = parseRechargeZone(readString(zones, "recharge", "R1:21,16:2"), rechargeCapacity);

        List<Position> staticObstacles = parsePositions(readString(obstacles, "fixed", ""));
        List<Position> humans = parsePositions(readString(obstacles, "humans", ""));
        String metricsOutput = readString(output, "metricsFile", "simulation-metrics.csv");

        validate(rows, columns, entryZones, exitZones, intermediate, recharge);

        return new WarehouseConfig(
            mode, communicationMode, rows, columns, steps, seed, amrCount,
            maxBattery, criticalThreshold, warningThreshold, safeMargin, rechargeDuration, rechargeCapacity,
            dist, rate, uiStepDelay, entryZones, exitZones, intermediate, recharge, staticObstacles, humans, metricsOutput
        );
    }

    private static void validate(
        int rows,
        int columns,
        List<Zone> entries,
        List<Zone> exits,
        List<Zone> inters,
        Zone recharge
    ) {
        if (rows <= 0 || columns <= 0) {
            throw new IllegalArgumentException("Warehouse rows/columns must be > 0");
        }
        if (entries.isEmpty() || exits.isEmpty()) {
            throw new IllegalArgumentException("At least one entry and one exit zone are required");
        }
        List<Zone> all = new ArrayList<Zone>();
        all.addAll(entries);
        all.addAll(exits);
        all.addAll(inters);
        if (recharge != null) {
            all.add(recharge);
        }
        for (Zone z : all) {
            if (z.getPosition().x < 0 || z.getPosition().x >= columns || z.getPosition().y < 0 || z.getPosition().y >= rows) {
                throw new IllegalArgumentException("Zone out of bounds: " + z.getId());
            }
            if (z.getCapacity() < 0) {
                throw new IllegalArgumentException("Zone capacity cannot be negative for: " + z.getId());
            }
        }
    }

    private static String readString(Section section, String key, String defaultValue) {
        if (section == null) {
            return defaultValue;
        }
        String value = section.get(key);
        return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
    }

    private static int readInt(Section section, String key, int defaultValue) {
        String value = readString(section, key, String.valueOf(defaultValue));
        return Integer.parseInt(value);
    }

    private static double readDouble(Section section, String key, double defaultValue) {
        String value = readString(section, key, String.valueOf(defaultValue));
        return Double.parseDouble(value);
    }

    private static List<Zone> parseZones(String raw, ZoneType zoneType, int defaultCapacity) {
        if (raw == null || raw.trim().isEmpty()) {
            return Collections.emptyList();
        }
        List<Zone> zones = new ArrayList<Zone>();
        String[] tokens = raw.split("\\|");
        for (String token : tokens) {
            if (token == null || token.trim().isEmpty()) {
                continue;
            }
            String[] parts = token.trim().split(":");
            if (parts.length < 2) {
                continue;
            }
            String id = parts[0].trim();
            Position pos = parsePosition(parts[1].trim());
            int capacity = defaultCapacity;
            if (parts.length >= 3) {
                capacity = Integer.parseInt(parts[2].trim());
            }
            zones.add(new Zone(id, zoneType, pos, capacity));
        }
        return zones;
    }

    private static Zone parseRechargeZone(String raw, int fallbackCapacity) {
        List<Zone> zones = parseZones(raw, ZoneType.RECHARGE, fallbackCapacity);
        if (zones.isEmpty()) {
            return new Zone("R1", ZoneType.RECHARGE, new Position(0, 0), fallbackCapacity);
        }
        Zone parsed = zones.get(0);
        return new Zone(parsed.getId(), ZoneType.RECHARGE, parsed.getPosition(), parsed.getCapacity());
    }

    private static List<Position> parsePositions(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return Collections.emptyList();
        }
        List<Position> positions = new ArrayList<Position>();
        String[] tokens = raw.split("\\|");
        for (String token : tokens) {
            String t = token.trim();
            if (t.isEmpty()) {
                continue;
            }
            positions.add(parsePosition(t));
        }
        return positions;
    }

    private static Position parsePosition(String xy) {
        String[] coords = xy.split(",");
        if (coords.length != 2) {
            throw new IllegalArgumentException("Invalid position format: " + xy);
        }
        return new Position(Integer.parseInt(coords[0].trim()), Integer.parseInt(coords[1].trim()));
    }

    public SimulationMode getMode() {
        return mode;
    }

    public CommunicationMode getCommunicationMode() {
        return communicationMode;
    }

    public int getRows() {
        return rows;
    }

    public int getColumns() {
        return columns;
    }

    public int getSteps() {
        return steps;
    }

    public int getSeed() {
        return seed;
    }

    public int getAmrCount() {
        return amrCount;
    }

    public int getMaxBattery() {
        return maxBattery;
    }

    public int getCriticalThreshold() {
        return criticalThreshold;
    }

    public int getWarningThreshold() {
        return warningThreshold;
    }

    public int getSafeMargin() {
        return safeMargin;
    }

    public int getRechargeDuration() {
        return rechargeDuration;
    }

    public int getRechargeCapacity() {
        return rechargeCapacity;
    }

    public String getPalletDistribution() {
        return palletDistribution;
    }

    public double getPalletRate() {
        return palletRate;
    }

    public int getUiStepDelayMs() {
        return uiStepDelayMs;
    }

    public List<Zone> getEntryZones() {
        return entryZones;
    }

    public List<Zone> getExitZones() {
        return exitZones;
    }

    public List<Zone> getIntermediateZones() {
        return intermediateZones;
    }

    public Zone getRechargeZone() {
        return rechargeZone;
    }

    public List<Position> getStaticObstacles() {
        return staticObstacles;
    }

    public List<Position> getHumans() {
        return humans;
    }

    public String getMetricsOutputFile() {
        return metricsOutputFile;
    }

    public WarehouseConfig copyWith(SimulationMode newMode, Integer newSeed, Integer newAmrCount, Double newPalletRate, String newMetricsFile) {
        return copyWith(newMode, newSeed, newAmrCount, newPalletRate, null, newMetricsFile);
    }

    public WarehouseConfig copyWith(
        SimulationMode newMode,
        Integer newSeed,
        Integer newAmrCount,
        Double newPalletRate,
        Integer newSteps,
        String newMetricsFile
    ) {
        return new WarehouseConfig(
            newMode == null ? mode : newMode,
            communicationMode,
            rows,
            columns,
            newSteps == null ? steps : newSteps,
            newSeed == null ? seed : newSeed,
            newAmrCount == null ? amrCount : newAmrCount,
            maxBattery,
            criticalThreshold,
            warningThreshold,
            safeMargin,
            rechargeDuration,
            rechargeCapacity,
            palletDistribution,
            newPalletRate == null ? palletRate : newPalletRate,
            uiStepDelayMs,
            cloneZones(entryZones),
            cloneZones(exitZones),
            cloneZones(intermediateZones),
            new Zone(rechargeZone.getId(), rechargeZone.getType(), rechargeZone.getPosition(), rechargeZone.getCapacity()),
            new ArrayList<Position>(staticObstacles),
            new ArrayList<Position>(humans),
            newMetricsFile == null ? metricsOutputFile : newMetricsFile
        );
    }

    private static List<Zone> cloneZones(List<Zone> zones) {
        List<Zone> cloned = new ArrayList<Zone>();
        for (Zone zone : zones) {
            cloned.add(new Zone(zone.getId(), zone.getType(), zone.getPosition(), zone.getCapacity()));
        }
        return cloned;
    }
}
