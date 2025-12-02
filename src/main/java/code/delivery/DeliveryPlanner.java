package code.delivery;

import code.model.*;
import java.util.*;

/**
 * DeliveryPlanner (Option 3: nearest destination per store) with destination reservation.
 *
 * For each remaining store:
 *  - determine nearest available destination (skip reserved ones)
 *  - for each agent evaluate best strategy for:
 *       agent -> store  AND  store -> destination
 *  - choose best agent/store pair (combined metrics)
 *  - append both legs to agent route
 *  - update agent position to the destination
 *  - mark destination as reserved (cannot be used again)
 *
 * After all assignments: apply collision avoidance (wait insertions).
 */
public class DeliveryPlanner {

    private static final String[] STRATEGIES = {"BFS","DFS","UCS","IDS","GREEDY","ASTAR"};

    public static class Assignment {
        public final Agent agent;
        public final List<Position> route;
        public final String strategy; // summary
        public final SearchStats stats;
        public Assignment(Agent a, List<Position> r, String strat, SearchStats st){ this.agent=a; this.route=r; this.strategy=strat; this.stats=st; }
        public String strategySummary(){ return strategy + " | stats=" + stats; }
    }

    private static class Candidate {
        Position store;
        Position dest;
        Agent agentSnapshot;
        String stratToStore;
        String stratToDest;
        SearchStats statsToStore;
        SearchStats statsToDest;
        List<Position> routeToStore;
        List<Position> routeToDest;

        Candidate(Position store, Position dest, Agent agentSnapshot){
            this.store = store; this.dest = dest; this.agentSnapshot = agentSnapshot;
        }

        long combinedExpanded(){ return (long)statsToStore.expanded + (long)statsToDest.expanded; }
        long combinedTime(){ return statsToStore.timeMs + statsToDest.timeMs; }
        long combinedMem(){ return statsToStore.memoryUsedKB + statsToDest.memoryUsedKB; }
    }

    /**
     * Get nearest available destination for a store (skip reservedDest).
     */
    private static Position nearestAvailableDestination(Grid grid, Position store, Set<Position> reservedDest){
        Position best = null;
        int bestD = Integer.MAX_VALUE;
        for(Position d : grid.destinations){
            if(reservedDest.contains(d)) continue;
            int dist = Math.abs(d.x - store.x) + Math.abs(d.y - store.y);
            if(dist < bestD){ bestD = dist; best = d; }
        }
        return best;
    }

    public static List<Assignment> planMultiDelivery(Grid grid){
        // create agent snapshots and assigned routes
        Map<String, Agent> agentSnap = new LinkedHashMap<>();
        Map<String, List<Position>> assigned = new LinkedHashMap<>();
        for(Agent ag : grid.agents){
            agentSnap.put(ag.id, new Agent(ag.id, new Position(ag.pos.x, ag.pos.y)));
            assigned.put(ag.id, new ArrayList<>(Collections.singletonList(ag.pos)));
        }

        List<Position> remainingStores = new ArrayList<>(grid.stores);
        Set<Position> reservedDestinations = new HashSet<>(); // <-- reservation set

        while(!remainingStores.isEmpty()){
            Candidate best = null;

            for(Position store : new ArrayList<>(remainingStores)){
                // find nearest available destination
                Position dest = nearestAvailableDestination(grid, store, reservedDestinations);
                if(dest == null) continue; // no available dest

                for(Agent ag : grid.agents){
                    Agent snapshot = agentSnap.get(ag.id);

                    // Leg 1: agent -> store
                    SearchStats bestStoreStats = null; String bestStoreStrat = null; List<Position> bestStoreRoute = null;
                    for(String s : STRATEGIES){
                        SearchStats st = DeliverySearch.solveWithStats(grid, snapshot.pos, store, s);
                        if(!st.success) continue;
                        if(bestStoreStats == null || prefer(st, bestStoreStats)){ bestStoreStats = st; bestStoreStrat = s; bestStoreRoute = st.route; }
                    }
                    if(bestStoreStats == null) continue;

                    // Leg 2: store -> dest
                    SearchStats bestDestStats = null; String bestDestStrat = null; List<Position> bestDestRoute = null;
                    for(String s : STRATEGIES){
                        SearchStats st = DeliverySearch.solveWithStats(grid, store, dest, s);
                        if(!st.success) continue;
                        if(bestDestStats == null || prefer(st, bestDestStats)){ bestDestStats = st; bestDestStrat = s; bestDestRoute = st.route; }
                    }
                    if(bestDestStats == null) continue;

                    Candidate c = new Candidate(store, dest, new Agent(snapshot.id, new Position(snapshot.pos.x, snapshot.pos.y)));
                    c.stratToStore = bestStoreStrat; c.stratToDest = bestDestStrat;
                    c.statsToStore = bestStoreStats; c.statsToDest = bestDestStats;
                    c.routeToStore = bestStoreRoute; c.routeToDest = bestDestRoute;

                    if(best == null) best = c;
                    else {
                        if(combinedPrefer(c, best)) best = c;
                    }
                }
            }

            if(best == null) break;

            // assign best candidate
            String aid = best.agentSnapshot.id;
            List<Position> current = assigned.get(aid);

            // append routeToStore (skip first duplicate)
            if(best.routeToStore != null && best.routeToStore.size() > 1){
                for(int i=1;i<best.routeToStore.size();i++) current.add(best.routeToStore.get(i));
            }
            // append routeToDest (skip first duplicate)
            if(best.routeToDest != null && best.routeToDest.size() > 1){
                for(int i=1;i<best.routeToDest.size();i++) current.add(best.routeToDest.get(i));
            }

            // update snapshot pos to destination
            agentSnap.get(aid).pos = best.dest;

            // reserve destination so nobody else can use it
            reservedDestinations.add(best.dest);

            // remove store from remaining
            remainingStores.remove(best.store);

            // log
            System.out.println("Assigned store " + best.store + " -> dest " + best.dest + " to agent " + aid +
                    " using " + best.stratToStore + " + " + best.stratToDest +
                    " (combined expanded=" + best.combinedExpanded() + ", time=" + best.combinedTime() + "ms)");
        }

        // collision avoidance
        Map<String, List<Position>> safe = applyCollisionAvoidance(assigned, grid);

        // produce assignments
        List<Assignment> out = new ArrayList<>();
        for(Agent ag : grid.agents){
            List<Position> r = safe.getOrDefault(ag.id, assigned.get(ag.id));
            SearchStats dummy = new SearchStats(true, 0, 0, 0, 0, r);
            out.add(new Assignment(new Agent(ag.id, ag.pos), r, "AUTO", dummy));
        }

        return out;
    }

    private static boolean prefer(SearchStats a, SearchStats b){
        if(a.expanded < b.expanded) return true;
        if(a.expanded > b.expanded) return false;
        if(a.timeMs < b.timeMs) return true;
        if(a.timeMs > b.timeMs) return false;
        return a.memoryUsedKB < b.memoryUsedKB;
    }

    private static boolean combinedPrefer(Candidate A, Candidate B){
        if(A.combinedExpanded() < B.combinedExpanded()) return true;
        if(A.combinedExpanded() > B.combinedExpanded()) return false;
        if(A.combinedTime() < B.combinedTime()) return true;
        if(A.combinedTime() > B.combinedTime()) return false;
        return A.combinedMem() < B.combinedMem();
    }

    private static Map<String, List<Position>> applyCollisionAvoidance(Map<String, List<Position>> routes, Grid grid){
        Map<String, List<Position>> sched = new LinkedHashMap<>();
        for(Map.Entry<String, List<Position>> e : routes.entrySet()){
            List<Position> copy = new ArrayList<>(e.getValue());
            if(copy.isEmpty()){
                Position start = null;
                for(Agent a : grid.agents) if(a.id.equals(e.getKey())) start = a.pos;
                if(start != null) copy.add(start);
            }
            sched.put(e.getKey(), copy);
        }

        Map<Integer, Map<Position, String>> reserved = new HashMap<>();
        boolean changed = true;
        int passes = 0;
        while(changed && passes < 2000){
            changed = false;
            reserved.clear();
            int maxT = 0;
            for(List<Position> r : sched.values()) maxT = Math.max(maxT, r.size());
            for(int t=0;t<maxT;t++){
                for(String aid : new ArrayList<>(sched.keySet())){
                    List<Position> r = sched.get(aid);
                    Position p = (t < r.size()) ? r.get(t) : r.get(r.size()-1);
                    Map<Position, String> atT = reserved.computeIfAbsent(t, k->new HashMap<>());
                    if(atT.containsKey(p)){
                        int insertPos = Math.min(t, r.size()-1);
                        Position prev = r.get(Math.max(0, insertPos-1));
                        r.add(insertPos, prev);
                        changed = true;
                        break;
                    } else {
                        atT.put(p, aid);
                    }
                }
                if(changed) break;
            }
            passes++;
        }
        return sched;
    }
}
