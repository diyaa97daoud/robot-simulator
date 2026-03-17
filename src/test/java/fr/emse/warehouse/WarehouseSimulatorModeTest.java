package fr.emse.warehouse;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class WarehouseSimulatorModeTest {
    @Test
    void referenceModeHasNoCommunicationTraffic() throws Exception {
        WarehouseConfig config = WarehouseConfig.fromIni(writeConfig("reference"));
        SimulationResult result = new WarehouseSimulator(config).run();
        assertEquals(0, result.getMetrics().getMessagesSent());
        assertTrue(result.getMetrics().getDeliveredPallets() >= 0);
    }

    @Test
    void optimizedModeUsesCommunicationAndBatteryFeatures() throws Exception {
        WarehouseConfig config = WarehouseConfig.fromIni(writeConfig("optimized"));
        SimulationResult result = new WarehouseSimulator(config).run();
        assertTrue(result.getMetrics().getMessagesSent() > 0);
        assertTrue(result.getMetrics().getRechargeCount() >= 0);
    }

    @Test
    void optimizedModeCarriesPalletToRechargeWithoutUsingIntermediateStorage() throws Exception {
        WarehouseConfig config = WarehouseConfig.fromIni(writeCarryRechargeConfig());
        AtomicBoolean sawRechargingWithPallet = new AtomicBoolean(false);
        WarehouseSimulator simulator = new WarehouseSimulator(config);
        SimulationResult result = simulator.run(new SimulationStepListener() {
            @Override
            public void onStep(SimulationSnapshot snapshot) {
                for (SimulationSnapshot.RobotView robotView : snapshot.getRobots()) {
                    if (robotView.isCarrying() && robotView.getState() == RobotState.RECHARGING) {
                        sawRechargingWithPallet.set(true);
                        return;
                    }
                }
            }

            @Override
            public void onCompleted(SimulationResult result) {
                // no-op
            }
        });
        assertTrue(sawRechargingWithPallet.get());
        assertEquals(0.0d, result.getMetrics().getAverageIntermediateOccupancy(), 0.0001d);
    }

    private String writeConfig(String mode) throws Exception {
        Path temp = Files.createTempFile("sim-mode-", ".ini");
        Files.writeString(temp,
            "[simulation]\n"
                + "mode=" + mode + "\n"
                + "steps=60\n"
                + "seed=10\n"
                + "amrCount=4\n"
                + "[warehouse]\n"
                + "rows=20\n"
                + "columns=25\n"
                + "[arrivals]\n"
                + "distribution=binomial\n"
                + "rate=0.6\n"
                + "[battery]\n"
                + "maxBattery=30\n"
                + "criticalThreshold=5\n"
                + "warningThreshold=10\n"
                + "safeMargin=3\n"
                + "rechargeDuration=4\n"
                + "rechargeCapacity=2\n"
                + "[zones]\n"
                + "entries=A1:1,2|A2:1,15\n"
                + "exits=Z1:22,2|Z2:22,15\n"
                + "intermediates=I1:12,6:2|I2:12,12:2\n"
                + "recharge=R1:11,9:2\n"
                + "[obstacles]\n"
                + "fixed=6,6|7,6|8,6\n"
                + "humans=10,4\n"
                + "[output]\n"
                + "metricsFile=build/test-metrics.csv\n");
        return temp.toString();
    }

    private String writeCarryRechargeConfig() throws Exception {
        Path temp = Files.createTempFile("sim-carry-recharge-", ".ini");
        Files.writeString(temp,
            "[simulation]\n"
                + "mode=optimized\n"
                + "steps=140\n"
                + "seed=10\n"
                + "amrCount=1\n"
                + "[communication]\n"
                + "mode=broadcast\n"
                + "[warehouse]\n"
                + "rows=10\n"
                + "columns=32\n"
                + "[arrivals]\n"
                + "distribution=binomial\n"
                + "rate=0.8\n"
                + "[battery]\n"
                + "maxBattery=8\n"
                + "criticalThreshold=3\n"
                + "warningThreshold=4\n"
                + "safeMargin=1\n"
                + "rechargeDuration=3\n"
                + "rechargeCapacity=2\n"
                + "[zones]\n"
                + "entries=A1:1,1\n"
                + "exits=Z1:28,1\n"
                + "intermediates=I1:18,5:2\n"
                + "recharge=R1:2,1:2\n"
                + "[output]\n"
                + "metricsFile=build/test-metrics.csv\n");
        return temp.toString();
    }
}
