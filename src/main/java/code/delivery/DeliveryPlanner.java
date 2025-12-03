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
        long combinedTime(){ return statsToStore.timeNanos + statsToDest.timeNanos; }
        long combinedMem(){ return statsToStore.memoryUsedBytes + statsToDest.memoryUsedBytes; }
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
        // Agent state tracking
        Map<String, Agent> agentSnap = new LinkedHashMap<>();
        Map<String, List<Position>> assigned = new LinkedHashMap<>();
        for(Agent ag : grid.agents){
            agentSnap.put(ag.id, new Agent(ag.id, new Position(ag.pos.x, ag.pos.y)));
            assigned.put(ag.id, new ArrayList<>(Collections.singletonList(ag.pos)));
        }

        // Stats aggregation for final summary
        Map<String, Integer> totalCost = new HashMap<>();
        Map<String, Integer> totalExpanded = new HashMap<>();
        Map<String, Long> totalTimeNanos = new HashMap<>();
        Map<String, Long> totalMemoryKB = new HashMap<>();
        for (Agent ag : grid.agents) {
            totalCost.put(ag.id, 0);
            totalExpanded.put(ag.id, 0);
            totalTimeNanos.put(ag.id, 0L);
            totalMemoryKB.put(ag.id, 0L);
        }

        List<Position> remainingStores = new ArrayList<>(grid.stores);
        Set<Position> reservedDestinations = new HashSet<>();

        while(!remainingStores.isEmpty()){
            Candidate best = null;
            String loggingAgentId = null;
            Position loggingStore = null, loggingDest = null;
            String chosenStoreStrat = null, chosenDestStrat = null;

            for(Position store : new ArrayList<>(remainingStores)){
                Position dest = nearestAvailableDestination(grid, store, reservedDestinations);
                if(dest == null) continue;

                for(Agent ag : grid.agents){
                    Agent snapshot = agentSnap.get(ag.id);

                    // Leg 1: agent -> store
                    SearchStats bestStoreStats = null; String bestStoreStrat = null;
                    for(String s : STRATEGIES){
                        SearchStats st = DeliverySearch.solveWithStats(grid, snapshot.pos, store, s);
                        if(!st.success) continue;
                        if(bestStoreStats == null || prefer(st, bestStoreStats)){
                            bestStoreStats = st; bestStoreStrat = s;
                        }
                    }
                    if(bestStoreStats == null) continue;

                    // Leg 2: store -> dest
                    SearchStats bestDestStats = null; String bestDestStrat = null;
                    for(String s : STRATEGIES){
                        SearchStats st = DeliverySearch.solveWithStats(grid, store, dest, s);
                        if(!st.success) continue;
                        if(bestDestStats == null || prefer(st, bestDestStats)){
                            bestDestStats = st; bestDestStrat = s;
                        }
                    }
                    if(bestDestStats == null) continue;

                    Candidate c = new Candidate(store, dest, new Agent(snapshot.id, new Position(snapshot.pos.x, snapshot.pos.y)));
                    c.stratToStore = bestStoreStrat; c.stratToDest = bestDestStrat;
                    c.statsToStore = bestStoreStats; c.statsToDest = bestDestStats;
                    c.routeToStore = bestStoreStats.route; c.routeToDest = bestDestStats.route;

                    if(best == null || combinedPrefer(c, best)) {
                        best = c;
                        loggingAgentId = ag.id;
                        loggingStore = store;
                        loggingDest = dest;
                        chosenStoreStrat = bestStoreStrat;
                        chosenDestStrat = bestDestStrat;
                    }
                }
            }

            if(best == null) break;

            // === DETAILED LOGGING ===
            System.out.println("\n🔍 Assignment Details for Agent " + loggingAgentId);
            System.out.println("Task: " + best.agentSnapshot.pos + " → " + loggingStore + " → " + loggingDest);

            System.out.println("\n➡️  Leg 1: Agent → Store");
            System.out.printf("%-8s | %8s | %10s | %6s | %6s%n", "Algo", "Expanded", "Time", "Cost", "Steps");
            for (String s : STRATEGIES) {
                SearchStats st = DeliverySearch.solveWithStats(grid, best.agentSnapshot.pos, loggingStore, s);
                if (st.success) {
                    System.out.printf("%-8s | %8d | %10s | %6d | %6d%n",
                            s, st.expanded, SearchStats.formatTime(st.timeNanos), st.cost, st.route.size() - 1);
                } else {
                    System.out.printf("%-8s | %8s | %10s | %6s | %6s%n", s, "—", "—", "—", "—");
                }
            }
            System.out.println("✅ Chosen: " + chosenStoreStrat + " | Actions: " + best.statsToStore.actions);

            System.out.println("\n➡️  Leg 2: Store → Destination");
            System.out.printf("%-8s | %8s | %10s | %6s | %6s%n", "Algo", "Expanded", "Time", "Cost", "Steps");
            for (String s : STRATEGIES) {
                SearchStats st = DeliverySearch.solveWithStats(grid, loggingStore, loggingDest, s);
                if (st.success) {
                    System.out.printf("%-8s | %8d | %10s | %6d | %6d%n",
                            s, st.expanded, SearchStats.formatTime(st.timeNanos), st.cost, st.route.size() - 1);
                } else {
                    System.out.printf("%-8s | %8s | %10s | %6s | %6s%n", s, "—", "—", "—", "—");
                }
            }
            System.out.println("✅ Chosen: " + chosenDestStrat + " | Actions: " + best.statsToDest.actions);

            // Assign route
            String aid = best.agentSnapshot.id;
            List<Position> current = assigned.get(aid);
            if(best.routeToStore != null && best.routeToStore.size() > 1){
                for(int i=1;i<best.routeToStore.size();i++) current.add(best.routeToStore.get(i));
            }
            if(best.routeToDest != null && best.routeToDest.size() > 1){
                for(int i=1;i<best.routeToDest.size();i++) current.add(best.routeToDest.get(i));
            }
            agentSnap.get(aid).pos = best.dest;
            reservedDestinations.add(best.dest);
            remainingStores.remove(best.store);

            // Update aggregated stats
            totalCost.merge(aid, best.statsToStore.cost + best.statsToDest.cost, Integer::sum);
            totalExpanded.merge(aid, (int)best.combinedExpanded(), Integer::sum);
            totalTimeNanos.merge(aid, best.statsToStore.timeNanos + best.statsToDest.timeNanos, Long::sum);
            totalMemoryKB.merge(aid, best.combinedMem(), Long::sum);

            System.out.println("📌 Final: " + best.store + " → " + best.dest + " assigned to " + aid +
                    " using " + best.stratToStore + " + " + best.stratToDest +
                    " (total expanded=" + best.combinedExpanded() +
                    ", time=" + SearchStats.formatTime(best.statsToStore.timeNanos + best.statsToDest.timeNanos) + ")\n");
        }

        // Collision avoidance
        Map<String, List<Position>> safe = applyCollisionAvoidance(assigned, grid);

        // Final assignments with REAL stats
        List<Assignment> out = new ArrayList<>();
        for(Agent ag : grid.agents){
            List<Position> r = safe.getOrDefault(ag.id, assigned.get(ag.id));
            SearchStats realStats = new SearchStats(
                    true,
                    totalCost.get(ag.id),
                    totalExpanded.get(ag.id),
                    totalTimeNanos.get(ag.id),
                    totalMemoryKB.get(ag.id),
                    r
            );
            out.add(new Assignment(new Agent(ag.id, ag.pos), r, "AUTO", realStats));
        }

        return out;
    }

    // UPDATED COMPARISON LOGIC — uses timeNanos and memory
    private static boolean prefer(SearchStats a, SearchStats b){
        if(a.expanded != b.expanded) return a.expanded < b.expanded;
        if(a.timeNanos != b.timeNanos) return a.timeNanos < b.timeNanos;
        return a.memoryUsedBytes < b.memoryUsedBytes;
    }

    private static boolean combinedPrefer(Candidate A, Candidate B){
        long expA = A.combinedExpanded(), expB = B.combinedExpanded();
        if(expA != expB) return expA < expB;
        long timeA = A.statsToStore.timeNanos + A.statsToDest.timeNanos;
        long timeB = B.statsToStore.timeNanos + B.statsToDest.timeNanos;
        if(timeA != timeB) return timeA < timeB;
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
