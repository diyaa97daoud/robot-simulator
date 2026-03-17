package fr.emse.warehouse;

public enum SimulationMode {
    REFERENCE,
    OPTIMIZED;

    public static SimulationMode fromString(String raw) {
        if (raw == null) {
            return OPTIMIZED;
        }
        String normalized = raw.trim().toLowerCase();
        if ("reference".equals(normalized)) {
            return REFERENCE;
        }
        return OPTIMIZED;
    }
}
