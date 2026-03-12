package fr.emse.warehouse;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PalletGenerator {
    private final Random random;
    private final String distribution;
    private final double rate;

    public PalletGenerator(int seed, String distribution, double rate) {
        this.random = new Random(seed);
        this.distribution = distribution == null ? "poisson" : distribution.trim().toLowerCase();
        this.rate = Math.max(0.0d, rate);
    }

    public int sampleCount() {
        if ("geometric".equals(distribution)) {
            int count = 0;
            double p = Math.max(0.0001d, Math.min(rate, 0.9999d));
            while (random.nextDouble() > p && count < 5) {
                count++;
            }
            return count > 0 ? 1 : 0;
        }
        if ("binomial".equals(distribution)) {
            int n = 3;
            int c = 0;
            double p = Math.max(0.0d, Math.min(rate, 1.0d));
            for (int i = 0; i < n; i++) {
                if (random.nextDouble() <= p) {
                    c++;
                }
            }
            return c;
        }

        double l = Math.exp(-Math.max(0.01d, rate));
        int k = 0;
        double p = 1.0d;
        do {
            k++;
            p *= random.nextDouble();
        } while (p > l && k < 10);
        return Math.max(0, k - 1);
    }

    public String randomExitZoneId(List<Zone> exits) {
        if (exits.isEmpty()) {
            throw new IllegalStateException("No exit zones configured");
        }
        return exits.get(random.nextInt(exits.size())).getId();
    }

    public <T> T chooseRandom(List<T> values) {
        if (values.isEmpty()) {
            throw new IllegalStateException("Cannot sample from empty list");
        }
        return values.get(random.nextInt(values.size()));
    }

    public List<Position> moveHumans(List<Position> humans, int rows, int cols, List<Position> blocked) {
        List<Position> moved = new ArrayList<Position>();
        for (Position human : humans) {
            List<Position> candidates = new ArrayList<Position>();
            candidates.add(human);
            candidates.add(new Position(human.x + 1, human.y));
            candidates.add(new Position(human.x - 1, human.y));
            candidates.add(new Position(human.x, human.y + 1));
            candidates.add(new Position(human.x, human.y - 1));

            Position selected = human;
            for (int tries = 0; tries < 4; tries++) {
                Position candidate = candidates.get(random.nextInt(candidates.size()));
                if (candidate.x >= 0 && candidate.y >= 0 && candidate.x < cols && candidate.y < rows && !blocked.contains(candidate)) {
                    selected = candidate;
                    break;
                }
            }
            moved.add(selected);
        }
        return moved;
    }
}
