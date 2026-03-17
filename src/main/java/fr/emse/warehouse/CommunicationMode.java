package fr.emse.warehouse;

public enum CommunicationMode {
    BROADCAST,
    DYADIC;

    public static CommunicationMode fromString(String raw) {
        if (raw == null) {
            return BROADCAST;
        }
        String normalized = raw.trim().toLowerCase();
        if ("dyadic".equals(normalized)) {
            return DYADIC;
        }
        return BROADCAST;
    }
}