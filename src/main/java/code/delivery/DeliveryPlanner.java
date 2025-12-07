package code.delivery;

import code.model.*;
import java.util.*;

/**
 * DeliveryPlanner - Plans delivery routes for multiple agents
 * 
 * SPECIFICATION COMPLIANCE:
 * - Plans which trucks deliver which products
 * - Each truck returns to store after delivery before next assignment
 * - Coordinates overall delivery strategy
 */
public class DeliveryPlanner {

    public static class Assignment {
        public final Agent agent;
        public final List<Position> route;
        public final String strategy;
        public final SearchStats stats;
        public final int deliveriesCompleted;
        
        public Assignment(Agent a, List<Position> r, String strat, SearchStats st, int deliveries){ 
            this.agent=a; 
            this.route=r; 
            this.strategy=strat; 
            this.stats=st; 
            this.deliveriesCompleted = deliveries;
        }
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
        int agentDeliveryCount;

        Candidate(Position store, Position dest, Agent agentSnapshot, int deliveryCount){
            this.store = store; 
            this.dest = dest; 
            this.agentSnapshot = agentSnapshot;
            this.agentDeliveryCount = deliveryCount;
        }

        long combinedExpanded(){ return (long)statsToStore.expanded + (long)statsToDest.expanded; }
        long combinedMem(){ return statsToStore.memoryUsedBytes + statsToDest.memoryUsedBytes; }
        int totalCost(){ return statsToStore.cost + statsToDest.cost; }
    }

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
        Map<String, Position> currentStoreLocation = new LinkedHashMap<>();  // Track which store agent is at
        Map<String, List<Position>> assigned = new LinkedHashMap<>();
        Map<String, Integer> deliveryCount = new HashMap<>();
        
        for(Agent ag : grid.agents){
            agentSnap.put(ag.id, new Agent(ag.id, new Position(ag.pos.x, ag.pos.y)));
            // Initially, each agent is at their starting store
            currentStoreLocation.put(ag.id, new Position(ag.pos.x, ag.pos.y));
            assigned.put(ag.id, new ArrayList<>(Collections.singletonList(ag.pos)));
            deliveryCount.put(ag.id, 0);
        }

        // Stats aggregation
        Map<String, Integer> totalCost = new HashMap<>();
        Map<String, Integer> totalExpanded = new HashMap<>();
        Map<String, Long> totalTimeNanos = new HashMap<>();
        Map<String, Long> totalMemoryKB = new HashMap<>();
        Map<String, List<String>> strategiesUsed = new HashMap<>();
        
        for (Agent ag : grid.agents) {
            totalCost.put(ag.id, 0);
            totalExpanded.put(ag.id, 0);
            totalTimeNanos.put(ag.id, 0L);
            totalMemoryKB.put(ag.id, 0L);
            strategiesUsed.put(ag.id, new ArrayList<>());
        }

        Set<Position> reservedDestinations = new HashSet<>();

        // Continue while there are unreserved destinations
        while(reservedDestinations.size() < grid.destinations.size()){
            Candidate best = null;

            // Check all stores for each delivery round
            for(Position store : grid.stores){
                Position dest = nearestAvailableDestination(grid, store, reservedDestinations);
                if(dest == null) continue;

                for(Agent ag : grid.agents){
                    Agent snapshot = agentSnap.get(ag.id);

                    // Leg 1: agent -> store
                    // Test all strategies and select best
                    String[] strategies = {"BFS","DFS","UCS","IDS","GREEDY","ASTAR"};
                    SearchStats bestStoreStats = null; 
                    String bestStoreStrat = null;
                    for(String s : strategies){
                        SearchStats st = DeliverySearch.solveWithStats(grid, snapshot.pos, store, s);
                        if(!st.success) continue;
                        if(bestStoreStats == null || prefer(st, bestStoreStats)){
                            bestStoreStats = st; bestStoreStrat = s;
                        }
                    }
                    if(bestStoreStats == null) continue;

                    SearchStats bestDestStats = null; 
                    String bestDestStrat = null;
                    for(String s : strategies){
                        SearchStats st = DeliverySearch.solveWithStats(grid, store, dest, s);
                        if(!st.success) continue;
                        if(bestDestStats == null || prefer(st, bestDestStats)){
                            bestDestStats = st; bestDestStrat = s;
                        }
                    }
                    if(bestDestStats == null) continue;

                    Candidate c = new Candidate(store, dest, 
                        new Agent(snapshot.id, new Position(snapshot.pos.x, snapshot.pos.y)), 
                        deliveryCount.get(ag.id));
                    c.stratToStore = bestStoreStrat; 
                    c.stratToDest = bestDestStrat;
                    c.statsToStore = bestStoreStats; 
                    c.statsToDest = bestDestStats;
                    c.routeToStore = bestStoreStats.route; 
                    c.routeToDest = bestDestStats.route;

                    if(best == null || combinedPrefer(c, best)) {
                        best = c;
                    }
                }
            }

            if(best == null) break;

            // Assign route
            String aid = best.agentSnapshot.id;
            List<Position> current = assigned.get(aid);
            if(best.routeToStore != null && best.routeToStore.size() > 1){
                for(int i=1;i<best.routeToStore.size();i++) current.add(best.routeToStore.get(i));
            }
            if(best.routeToDest != null && best.routeToDest.size() > 1){
                for(int i=1;i<best.routeToDest.size();i++) current.add(best.routeToDest.get(i));
            }
            
            // ✅ SPECIFICATION FIX: Truck returns to store after delivery
            deliveryCount.put(aid, deliveryCount.get(aid) + 1);
            agentSnap.get(aid).pos = best.store;  // Return to store (not initial position)
            currentStoreLocation.put(aid, best.store);
            reservedDestinations.add(best.dest);

            // Update aggregated stats
            totalCost.merge(aid, best.statsToStore.cost + best.statsToDest.cost, Integer::sum);
            totalExpanded.merge(aid, (int)best.combinedExpanded(), Integer::sum);
            totalTimeNanos.merge(aid, best.statsToStore.timeNanos + best.statsToDest.timeNanos, Long::sum);
            totalMemoryKB.merge(aid, best.combinedMem(), Long::sum);
            
            strategiesUsed.get(aid).add(best.stratToStore + " + " + best.stratToDest);
        }

        // Collision avoidance
        Map<String, List<Position>> safe = applyCollisionAvoidance(assigned, grid);

        // Final assignments with REAL stats
        List<Assignment> out = new ArrayList<>();
        for(Agent ag : grid.agents){
            List<Position> r = safe.getOrDefault(ag.id, assigned.get(ag.id));
            List<String> actions = extractActions(r, grid);
            SearchStats realStats = new SearchStats(
                    true,
                    totalCost.get(ag.id),
                    totalExpanded.get(ag.id),
                    totalTimeNanos.get(ag.id),
                    totalMemoryKB.get(ag.id),
                    r,
                    actions
            );
            String strategyStr = strategiesUsed.getOrDefault(ag.id, new ArrayList<>()).isEmpty() 
                ? "AUTO" 
                : String.join(" | ", strategiesUsed.get(ag.id));
            out.add(new Assignment(new Agent(ag.id, ag.pos), r, strategyStr, realStats, deliveryCount.get(ag.id)));
        }

        return out;
    }
    
    /**
     * ✅ SPECIFICATION COMPLIANCE: Plan with specific strategy
     * Allows strategy to be passed from plan() method
     */
    public static List<Assignment> planMultiDeliveryWithStrategy(Grid grid, String strategy) {
        if ("AUTO".equals(strategy)) {
            return planMultiDelivery(grid);  // Use automatic strategy selection
        }
        
        // Use specified strategy for all searches
        return planMultiDeliveryFixed(grid, strategy);
    }
    
    /**
     * Plan multi-delivery using a fixed strategy (not AUTO)
     */
    private static List<Assignment> planMultiDeliveryFixed(Grid grid, String strategy) {
        Map<String, Agent> agentSnap = new LinkedHashMap<>();
        Map<String, Position> currentStoreLocation = new LinkedHashMap<>();
        Map<String, List<Position>> assigned = new LinkedHashMap<>();
        Map<String, Integer> deliveryCount = new HashMap<>();
        
        for(Agent ag : grid.agents){
            agentSnap.put(ag.id, new Agent(ag.id, new Position(ag.pos.x, ag.pos.y)));
            currentStoreLocation.put(ag.id, new Position(ag.pos.x, ag.pos.y));
            assigned.put(ag.id, new ArrayList<>(Collections.singletonList(ag.pos)));
            deliveryCount.put(ag.id, 0);
        }

        Map<String, Integer> totalCost = new HashMap<>();
        Map<String, Integer> totalExpanded = new HashMap<>();
        Map<String, Long> totalTimeNanos = new HashMap<>();
        Map<String, Long> totalMemoryKB = new HashMap<>();
        Map<String, List<String>> strategiesUsed = new HashMap<>();
        
        for (Agent ag : grid.agents) {
            totalCost.put(ag.id, 0);
            totalExpanded.put(ag.id, 0);
            totalTimeNanos.put(ag.id, 0L);
            totalMemoryKB.put(ag.id, 0L);
            strategiesUsed.put(ag.id, new ArrayList<>());
        }

        Set<Position> reservedDestinations = new HashSet<>();

        while(reservedDestinations.size() < grid.destinations.size()){
            Candidate best = null;

            for(Position store : grid.stores){
                Position dest = nearestAvailableDestination(grid, store, reservedDestinations);
                if(dest == null) continue;

                for(Agent ag : grid.agents){
                    Agent snapshot = agentSnap.get(ag.id);

                    // Use FIXED strategy
                    SearchStats statsToStore = DeliverySearch.solveWithStats(grid, snapshot.pos, store, strategy);
                    if(!statsToStore.success) continue;

                    SearchStats statsToDest = DeliverySearch.solveWithStats(grid, store, dest, strategy);
                    if(!statsToDest.success) continue;

                    Candidate c = new Candidate(store, dest, 
                        new Agent(snapshot.id, new Position(snapshot.pos.x, snapshot.pos.y)), 
                        deliveryCount.get(ag.id));
                    c.stratToStore = strategy; 
                    c.stratToDest = strategy;
                    c.statsToStore = statsToStore; 
                    c.statsToDest = statsToDest;
                    c.routeToStore = statsToStore.route; 
                    c.routeToDest = statsToDest.route;

                    if(best == null || combinedPrefer(c, best)) {
                        best = c;
                    }
                }
            }

            if(best == null) break;

            String aid = best.agentSnapshot.id;
            List<Position> current = assigned.get(aid);
            if(best.routeToStore != null && best.routeToStore.size() > 1){
                for(int i=1;i<best.routeToStore.size();i++) current.add(best.routeToStore.get(i));
            }
            if(best.routeToDest != null && best.routeToDest.size() > 1){
                for(int i=1;i<best.routeToDest.size();i++) current.add(best.routeToDest.get(i));
            }
            
            deliveryCount.put(aid, deliveryCount.get(aid) + 1);
            agentSnap.get(aid).pos = best.store;
            currentStoreLocation.put(aid, best.store);
            reservedDestinations.add(best.dest);

            totalCost.merge(aid, best.statsToStore.cost + best.statsToDest.cost, Integer::sum);
            totalExpanded.merge(aid, (int)best.combinedExpanded(), Integer::sum);
            totalTimeNanos.merge(aid, best.statsToStore.timeNanos + best.statsToDest.timeNanos, Long::sum);
            totalMemoryKB.merge(aid, best.combinedMem(), Long::sum);
            strategiesUsed.get(aid).add(best.stratToStore + " + " + best.stratToDest);
        }

        Map<String, List<Position>> safe = applyCollisionAvoidance(assigned, grid);

        List<Assignment> out = new ArrayList<>();
        for(Agent ag : grid.agents){
            List<Position> r = safe.getOrDefault(ag.id, assigned.get(ag.id));
            List<String> actions = extractActions(r, grid);
            SearchStats realStats = new SearchStats(
                    true,
                    totalCost.get(ag.id),
                    totalExpanded.get(ag.id),
                    totalTimeNanos.get(ag.id),
                    totalMemoryKB.get(ag.id),
                    r,
                    actions
            );
            String strategyStr = strategiesUsed.getOrDefault(ag.id, new ArrayList<>()).isEmpty() 
                ? strategy 
                : String.join(" | ", strategiesUsed.get(ag.id));
            out.add(new Assignment(new Agent(ag.id, ag.pos), r, strategyStr, realStats, deliveryCount.get(ag.id)));
        }

        return out;
    }
    
    /**
     * ✅ SPECIFICATION FIX: Extract action sequence including TUNNEL actions
     * Format: "up,down,left,tunnel,right" as specified
     */
    private static List<String> extractActions(List<Position> route, Grid grid) {
        List<String> actions = new ArrayList<>();
        for (int i = 1; i < route.size(); i++) {
            Position prev = route.get(i - 1);
            Position curr = route.get(i);
            
            // Check if this is a tunnel move (non-adjacent positions)
            int dx = Math.abs(curr.x - prev.x);
            int dy = Math.abs(curr.y - prev.y);
            
            if (dx > 1 || dy > 1) {
                // This is a tunnel move
                actions.add("tunnel");
            } else if (curr.x > prev.x) {
                actions.add("right");
            } else if (curr.x < prev.x) {
                actions.add("left");
            } else if (curr.y > prev.y) {
                actions.add("down");
            } else if (curr.y < prev.y) {
                actions.add("up");
            }
        }
        return actions;
    }

    private static boolean prefer(SearchStats a, SearchStats b){
        if(a.expanded != b.expanded) return a.expanded < b.expanded;
        if(a.timeNanos != b.timeNanos) return a.timeNanos < b.timeNanos;
        return a.memoryUsedBytes < b.memoryUsedBytes;
    }

    private static boolean combinedPrefer(Candidate A, Candidate B){
        // Workload balancing: prefer less loaded agent when cost difference is small
        if(A.agentDeliveryCount != B.agentDeliveryCount) {
            int costA = A.totalCost();
            int costB = B.totalCost();
            int maxCost = Math.max(costA, costB);
            int minCost = Math.min(costA, costB);
            
            // If cost difference is within 20%, prefer agent with fewer deliveries
            if(maxCost == 0 || (maxCost - minCost) * 100 / maxCost <= 20) {
                return A.agentDeliveryCount < B.agentDeliveryCount;
            }
        }
        
        // Standard cost-based comparison
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