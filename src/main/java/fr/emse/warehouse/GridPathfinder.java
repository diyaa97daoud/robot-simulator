package fr.emse.warehouse;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class GridPathfinder {
    private final int rows;
    private final int cols;

    public GridPathfinder(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
    }

    public int shortestDistance(Position start, Position goal, Set<Position> blocked) {
        if (start.equals(goal)) {
            return 0;
        }
        Queue<Position> queue = new ArrayDeque<Position>();
        Map<Position, Integer> dist = new HashMap<Position, Integer>();
        queue.add(start);
        dist.put(start, 0);

        while (!queue.isEmpty()) {
            Position curr = queue.poll();
            int cd = dist.get(curr);
            for (Position nxt : neighbors(curr)) {
                if (!inBounds(nxt) || blocked.contains(nxt) || dist.containsKey(nxt)) {
                    continue;
                }
                if (nxt.equals(goal)) {
                    return cd + 1;
                }
                dist.put(nxt, cd + 1);
                queue.add(nxt);
            }
        }
        return Integer.MAX_VALUE / 4;
    }

    public Position nextStep(Position start, Position goal, Set<Position> blocked) {
        if (start.equals(goal)) {
            return start;
        }
        Queue<Position> queue = new ArrayDeque<Position>();
        Map<Position, Position> parent = new HashMap<Position, Position>();
        Set<Position> seen = new HashSet<Position>();
        queue.add(start);
        seen.add(start);

        Position reached = null;
        while (!queue.isEmpty()) {
            Position curr = queue.poll();
            if (curr.equals(goal)) {
                reached = curr;
                break;
            }
            for (Position nxt : neighbors(curr)) {
                if (!inBounds(nxt) || blocked.contains(nxt) || seen.contains(nxt)) {
                    continue;
                }
                seen.add(nxt);
                parent.put(nxt, curr);
                queue.add(nxt);
            }
        }

        if (reached == null) {
            return start;
        }
        Position step = reached;
        while (parent.containsKey(step) && !parent.get(step).equals(start)) {
            step = parent.get(step);
        }
        return step;
    }

    private boolean inBounds(Position p) {
        return p.x >= 0 && p.y >= 0 && p.x < cols && p.y < rows;
    }

    private List<Position> neighbors(Position p) {
        List<Position> n = new ArrayList<Position>(4);
        n.add(new Position(p.x + 1, p.y));
        n.add(new Position(p.x - 1, p.y));
        n.add(new Position(p.x, p.y + 1));
        n.add(new Position(p.x, p.y - 1));
        return n;
    }
}
