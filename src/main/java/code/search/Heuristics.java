package code.search;

import code.model.*;
import code.delivery.DeliveryProblem;
import java.util.List;

/**
 * Admissible heuristics for the delivery problem.
 * 
 * ADMISSIBILITY PROOFS:
 * 
 * 1. deliveryAdmissible (Manhattan-based):
 *    - Calculates: Manhattan distance * minimum edge cost
 *    - PROOF: Manhattan distance is the minimum number of moves needed in a grid without obstacles.
 *             Since actual path must navigate obstacles and traffic, real cost ≥ Manhattan * min_cost.
 *             Therefore h(n) ≤ h*(n) (never overestimates), making it admissible.
 * 
 * 2. tunnelAware (Tunnel-considering):
 *    - Calculates: min(direct Manhattan, Manhattan via any tunnel) * minimum edge cost
 *    - PROOF: Considers shortcuts through tunnels but still uses Manhattan distance (straight-line grid).
 *             Tunnel cost = Manhattan distance between entrances (the minimum possible).
 *             Since we take min of all routes and multiply by minimum edge cost,
 *             we never overestimate the actual path cost. Hence h(n) ≤ h*(n), making it admissible.
 * 
 * Both heuristics are consistent (monotonic) because:
 * - Each step toward the goal reduces the heuristic by at most the step cost
 * - h(n) ≤ c(n, a, n') + h(n') for any action a
 */
public final class Heuristics {
    private Heuristics(){}

    public static int manhattan(Position a, Position b){
        return Math.abs(a.x - b.x) + Math.abs(a.y - b.y);
    }

    /**
     * Admissible heuristic 1: Manhattan distance scaled by minimum edge cost
     * Never overestimates because it assumes straight-line path with minimum possible cost
     */
    public static int deliveryAdmissible(DeliveryProblem p, Object state){
        Position pos = (Position) state;
        return manhattan(pos, p.goal) * Math.max(1, p.grid.minEdgeCost());
    }

    /**
     * Admissible heuristic 2: Tunnel-aware Manhattan distance
     * Considers shortcuts through tunnels but still uses optimistic Manhattan distances
     * Never overestimates because tunnel costs use Manhattan distance (minimum possible)
     */
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
