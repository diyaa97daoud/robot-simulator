package fr.emse.warehouse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class WarehouseConfigTest {
    @Test
    void parsesCoreSections() throws Exception {
        Path temp = Files.createTempFile("warehouse-config-", ".ini");
        Files.writeString(temp,
            "[simulation]\n"
                + "mode=reference\n"
                + "steps=100\n"
                + "seed=12\n"
                + "amrCount=4\n"
                + "[warehouse]\n"
                + "rows=20\n"
                + "columns=30\n"
                + "[arrivals]\n"
                + "distribution=poisson\n"
                + "rate=0.3\n"
                + "[battery]\n"
                + "maxBattery=30\n"
                + "criticalThreshold=5\n"
                + "warningThreshold=10\n"
                + "safeMargin=3\n"
                + "rechargeDuration=4\n"
                + "rechargeCapacity=2\n"
                + "[zones]\n"
                + "entries=A1:1,1|A2:1,10\n"
                + "exits=Z1:20,1|Z2:20,10\n"
                + "intermediates=I1:10,5:2\n"
                + "recharge=R1:12,7:2\n");

        WarehouseConfig config = WarehouseConfig.fromIni(temp.toString());
        assertEquals(SimulationMode.REFERENCE, config.getMode());
        assertEquals(100, config.getSteps());
        assertEquals(20, config.getRows());
        assertEquals(30, config.getColumns());
        assertEquals(2, config.getEntryZones().size());
        assertEquals(2, config.getExitZones().size());
        assertTrue(config.getRechargeZone().getCapacity() >= 1);
    }
}
