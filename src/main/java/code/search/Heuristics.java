package code.search;

import code.model.*;
import code.delivery.DeliveryProblem;
import java.util.List;

public final class Heuristics {
    private Heuristics(){}

    public static int manhattan(Position a, Position b){
        return Math.abs(a.x - b.x) + Math.abs(a.y - b.y);
    }

    public static int deliveryAdmissible(DeliveryProblem p, Object state){
        Position pos = (Position) state;
        return manhattan(pos, p.goal) * Math.max(1, p.grid.minEdgeCost());
    }

    public static int tunnelAware(DeliveryProblem p, Object state){
        Position pos = (Position) state;
        Position goal = p.goal;
        int base = manhattan(pos, goal);
        Grid grid = p.grid;
        int best = base;
        List<Position[]> tunnels = grid.getTunnels();
        for(Position[] t : tunnels){
            Position e1 = t[0], e2 = t[1];
            int len = Math.abs(e1.x - e2.x) + Math.abs(e1.y - e2.y);
            int via1 = manhattan(pos, e1) + len + manhattan(e2, goal);
            int via2 = manhattan(pos, e2) + len + manhattan(e1, goal);
            best = Math.min(best, Math.min(via1, via2));
        }
        return best * Math.max(1, grid.minEdgeCost());
    }
}
