package fr.emse.warehouse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class GridPathfinderTest {
    @Test
    void computesShortestDistanceAroundObstacles() {
        GridPathfinder pathfinder = new GridPathfinder(10, 10);
        Set<Position> blocked = new HashSet<Position>();
        blocked.add(new Position(2, 1));
        blocked.add(new Position(2, 2));
        blocked.add(new Position(2, 3));

        int distance = pathfinder.shortestDistance(new Position(1, 2), new Position(4, 2), blocked);
        assertTrue(distance > 3);
    }

    @Test
    void returnsSamePositionWhenAlreadyAtGoal() {
        GridPathfinder pathfinder = new GridPathfinder(10, 10);
        Position start = new Position(5, 5);
        Position next = pathfinder.nextStep(start, start, new HashSet<Position>());
        assertEquals(start, next);
    }
}
