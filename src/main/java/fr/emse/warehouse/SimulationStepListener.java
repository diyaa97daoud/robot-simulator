package fr.emse.warehouse;

public interface SimulationStepListener {
    void onStep(SimulationSnapshot snapshot);

    void onCompleted(SimulationResult result);
}
