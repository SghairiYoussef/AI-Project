package code.delivery;

import code.model.Position;
import java.util.List;

public class SearchStats {
    public final boolean success;
    public final int cost;
    public final int expanded;
    public final long timeMs;
    public final long memoryUsedKB;
    public final List<Position> route;

    public SearchStats(boolean success, int cost, int expanded, long timeMs, long memoryUsedKB, List<Position> route){
        this.success = success; this.cost = cost; this.expanded = expanded; this.timeMs = timeMs; this.memoryUsedKB = memoryUsedKB; this.route = route;
    }

    @Override
    public String toString(){
        if(!success) return "FAIL";
        return String.format("cost=%d nodes=%d time=%dms mem=%dKB", cost, expanded, timeMs, memoryUsedKB);
    }
}
