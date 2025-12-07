package code.delivery;

import code.model.Position;
import java.util.*;

public class SearchStats {
    public final boolean success;
    public final int cost;
    public final int expanded;
    public final long timeNanos;
    public final long memoryUsedBytes;
    public final List<Position> route;
    public final List<String> actions;

    public SearchStats(boolean success, int cost, int expanded, long timeMs, long memoryUsedBytes, List<Position> route) {
        this(success, cost, expanded, timeMs * 1_000_000L, memoryUsedBytes, route, Collections.emptyList());
    }

    public SearchStats(boolean success, int cost, int expanded, long timeNanos, long memoryUsedBytes,
                       List<Position> route, List<String> actions) {
        this.success = success;
        this.cost = cost;
        this.expanded = expanded;
        this.timeNanos = timeNanos;
        this.memoryUsedBytes = memoryUsedBytes;
        this.route = route != null ? new ArrayList<>(route) : Collections.emptyList();
        this.actions = actions != null ? new ArrayList<>(actions) : Collections.emptyList();
    }

    public static String formatTime(long nanos) {
        if (nanos < 1_000) return nanos + "ns";
        else if (nanos < 1_000_000) return (nanos / 1_000) + "µs";
        else return String.format("%.2fms", nanos / 1_000_000.0);
    }

    @Override
    public String toString() {
        return String.format("cost=%d nodes=%d time=%s mem=%dkB",
                cost, expanded, formatTime(timeNanos), memoryUsedBytes);
    }
}