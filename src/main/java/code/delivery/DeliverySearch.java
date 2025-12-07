package code.delivery;

import code.model.Grid;
import code.model.Position;
import code.model.WorldParser;
import code.search.*;
import java.util.*;

/**
 * DeliverySearch extends GenericSearch, implementing the Package Delivery search
 * problem for each truck-product pair separately.
 */
public class DeliverySearch extends GenericSearch {

    /**
     * Required static method: solve
     * Finds path for a single truck to destination using specified search strategy.
     * @param grid The grid environment
     * @param start Starting position
     * @param goal Goal position
     * @param strategy Search strategy (BFS, DFS, UCS, IDS, GREEDY, ASTAR)
     * @return String in format "plan;cost;nodesExpanded"
     */
    public static String solve(Grid grid, Position start, Position goal, String strategy) {
        SearchResult result = solveInternal(grid, start, goal, strategy);
        if (result == null || result.plan == null) {
            return "No solution;-1;0";
        }
        return result.plan + ";" + result.cost + ";" + result.expanded;
    }

    public static SearchResult solveInternal(Grid grid, Position start, Position goal, String strat){
        DeliveryProblem problem = new DeliveryProblem(grid, start, goal);
        SearchStrategy s;
        try { s = SearchStrategy.valueOf(strat.toUpperCase()); }
        catch(Exception ex){ return null; }

        GenericSearch.Result res = GenericSearch.generalSearch(problem, s);

        if(res.node == null) return new SearchResult(null, Integer.MAX_VALUE, res.nodesExpanded, Collections.emptyList());

        // reconstruct actions
        List<String> actions = new ArrayList<>();
        code.search.Node cur = res.node;
        while(cur.parent != null){
            actions.add(cur.action);
            cur = cur.parent;
        }
        Collections.reverse(actions);
        String plan = String.join(",", actions);

        // reconstruct route positions
        List<Position> route = new ArrayList<>();
        Position p = start;
        route.add(p);
        for(String a : actions){
            switch(a){
                case "up": p = new Position(p.x, p.y-1); break;
                case "down": p = new Position(p.x, p.y+1); break;
                case "left": p = new Position(p.x-1, p.y); break;
                case "right": p = new Position(p.x+1, p.y); break;
                case "tunnel":
                    Position partner = grid.tunnelPartner(p);
                    if(partner != null) p = partner;
                    break;
                case "wait":
                    break;
                default:
                    break;
            }
            route.add(p);
        }

        return new SearchResult(plan, res.node.pathCost, res.nodesExpanded, route);
    }

    public static SearchStats solveWithStats(Grid grid, Position start, Position goal, String strat){
        long memBefore = usedMemoryBytes();
        long t0 = System.nanoTime(); // ← HIGH-RESOLUTION

        SearchResult r = solveInternal(grid, start, goal, strat);

        long t1 = System.nanoTime(); // ← HIGH-RESOLUTION
        long memAfter = usedMemoryBytes();

        boolean success = r != null && r.plan != null;
        int cost = success ? r.cost : Integer.MAX_VALUE;
        int expanded = (r != null) ? r.expanded : 0;
        long timeNanos = t1 - t0;
        long memBytes = Math.max(0L, memAfter - memBefore);
        List<Position> route = success ? r.route : Collections.emptyList();

        List<String> actions = Collections.emptyList();
        if (success && r.plan != null && !r.plan.isEmpty()) {
            actions = Arrays.asList(r.plan.split(","));
        }

        return new SearchStats(success, cost, expanded, timeNanos, memBytes, route, actions);
    }

    private static long usedMemoryBytes() {
        Runtime rt = Runtime.getRuntime();
        return rt.totalMemory() - rt.freeMemory();
    }

    /**
     * GenGrid() randomly generates a grid for package delivery.
     * Minimum 1 store and 1 destination. No upper limit as long as no 2 objects occupy same cell.
     * @return String representation: m;n;P;S;CustomerCoords;TunnelCoords;
     */
    public static String genGrid() {
        Random rand = new Random();

        int m = rand.nextInt(11) + 5;  // width: 5-15
        int n = rand.nextInt(11) + 5;  // height: 5-15

        int maxObjects = Math.min(10, (m * n) / 4);
        int numStores = rand.nextInt(maxObjects) + 1;  // At least 1
        int numDestinations = rand.nextInt(maxObjects) + 1;  // At least 1

        Set<String> usedCells = new HashSet<>();
        List<int[]> stores = new ArrayList<>();
        List<int[]> destinations = new ArrayList<>();
        List<int[]> tunnels = new ArrayList<>();

        for (int i = 0; i < numStores; i++) {
            int[] pos = getRandomFreePosition(m, n, usedCells, rand);
            stores.add(pos);
        }

        for (int i = 0; i < numDestinations; i++) {
            int[] pos = getRandomFreePosition(m, n, usedCells, rand);
            destinations.add(pos);
        }

        int numTunnels = rand.nextInt(4);
        for (int i = 0; i < numTunnels; i++) {
            int[] entrance1 = getRandomFreePosition(m, n, usedCells, rand);
            int[] entrance2 = getRandomFreePosition(m, n, usedCells, rand);
            tunnels.add(entrance1);
            tunnels.add(entrance2);
        }

        StringBuilder sb = new StringBuilder();

        // m;n;P;S;
        sb.append(m).append(";");
        sb.append(n).append(";");
        sb.append(numDestinations).append(";");  // P = number of products/destinations
        sb.append(numStores).append(";");  // S = number of stores

        StringBuilder storeStr = new StringBuilder();
        for (int i = 0; i < stores.size(); i++) {
            if (i > 0) storeStr.append(",");
            storeStr.append(stores.get(i)[0]).append(",").append(stores.get(i)[1]);
        }
        sb.append(storeStr).append(";");

        StringBuilder destStr = new StringBuilder();
        for (int i = 0; i < destinations.size(); i++) {
            if (i > 0) destStr.append(",");
            destStr.append(destinations.get(i)[0]).append(",").append(destinations.get(i)[1]);
        }
        sb.append(destStr).append(";");

        StringBuilder tunnelStr = new StringBuilder();
        for (int i = 0; i < tunnels.size(); i++) {
            if (i > 0) tunnelStr.append(",");
            tunnelStr.append(tunnels.get(i)[0]).append(",").append(tunnels.get(i)[1]);
        }
        sb.append(tunnelStr).append(";");

        StringBuilder obstacleStr = new StringBuilder();
        int numObstacles = rand.nextInt(m * n / 10);  // Random obstacles
        int obstacleCount = 0;
        for (int i = 0; i < numObstacles; i++) {
            int x1 = rand.nextInt(m);
            int y1 = rand.nextInt(n);
            int x2 = x1 + (rand.nextBoolean() ? 1 : 0);
            int y2 = y1 + (rand.nextBoolean() ? 1 : 0);
            if (x2 < m && y2 < n && (x1 != x2 || y1 != y2)) {  // Valid obstacle segment
                if (obstacleCount > 0) obstacleStr.append(",");
                obstacleStr.append(x1).append(",").append(y1).append(",").append(x2).append(",").append(y2);
                obstacleCount++;
            }
        }
        sb.append(obstacleStr).append(";");

        sb.append("random");

        return sb.toString();
    }


    private static int[] getRandomFreePosition(int m, int n, Set<String> usedCells, Random rand) {
        int x, y;
        String key;
        do {
            x = rand.nextInt(m);
            y = rand.nextInt(n);
            key = x + "," + y;
        } while (usedCells.contains(key));
        usedCells.add(key);
        return new int[]{x, y};
    }

    /**
     * Required static method: path
     * Finds path for a single truck to destination using specified search strategy.
     * @param grid The grid environment
     * @param start Starting position
     * @param goal Goal position  
     * @param strategy Search strategy name
     * @return String in format "plan;cost;nodesExpanded"
     */
    public static String path(Grid grid, Position start, Position goal, String strategy) {
        return solve(grid, start, goal, strategy);
    }

    /**
     * Required static method: plan
     * Determines which truck delivers which products and calls path for each route.
     * @param initialState String format: m;n;P;S;CustomerX_1,CustomerY_1,...;TunnelX_1,TunnelY_1,...;
     * @param strategy Search algorithm to use
     * @param traffic Whether to consider traffic (true = prefer heuristic)
     * @param visualize Boolean for visualization
     * @return Complete delivery plan with truck assignments in format: "plan;cost;nodesExpanded"
     */
    public static String plan(String initialState, String strategy, boolean traffic, boolean visualize) {
        try {
            // Parse world from string format  
            Grid grid = WorldParser.parseGridString(initialState);
            
            // ✅ FIXED: Use multi-agent planner WITH strategy parameter
            List<DeliveryPlanner.Assignment> assignments = DeliveryPlanner.planMultiDeliveryWithStrategy(grid, strategy);
            
            // Build result string from assignments
            StringBuilder planBuilder = new StringBuilder();
            int totalCost = 0;
            int totalExpanded = 0;
            
            for (DeliveryPlanner.Assignment assignment : assignments) {
                if (planBuilder.length() > 0) {
                    planBuilder.append(";");
                }
                for (Position pos : assignment.route) {
                    planBuilder.append(pos.x).append(",").append(pos.y).append(",");
                }
                totalCost += assignment.stats.cost;
                totalExpanded += assignment.stats.expanded;
            }
            
            // Remove trailing comma if present
            if (planBuilder.length() > 0 && planBuilder.charAt(planBuilder.length() - 1) == ',') {
                planBuilder.setLength(planBuilder.length() - 1);
            }
            
            String result = planBuilder.toString() + ";" + totalCost + ";" + totalExpanded;
            
            if (visualize && grid != null) {
                // Visualization if needed - simplified for now
                System.out.println("Plan: " + result);
            }
            
            return result;
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
    
    /**
     * Overloaded plan method without traffic parameter (defaults to false)
     * @param initialState String format: m;n;P;S;CustomerX_1,CustomerY_1,...;TunnelX_1,TunnelY_1,...;
     * @param strategy Search algorithm to use
     * @param visualize Boolean for visualization
     * @return Complete delivery plan with truck assignments
     */
    public static String plan(String initialState, String strategy, boolean visualize) {
        return plan(initialState, strategy, false, visualize);
    }
}
