package code.delivery;

import code.model.Grid;
import code.model.Position;
import code.search.*;
import java.util.*;

/**
 * DeliverySearch: inner solve that returns SearchResult and instrumented solveWithStats.
 */
public class DeliverySearch {

    public static SearchResult solveInternal(Grid grid, Position start, Position goal, String strat){
        DeliveryProblem problem = new DeliveryProblem(grid, start, goal);
        SearchStrategy s;
        try { s = SearchStrategy.valueOf(strat.toUpperCase()); }
        catch(Exception ex){ return null; }

        GeneralSearch.Result res = GeneralSearch.generalSearch(problem, s);

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
        long memBefore = usedMemoryKB();
        long t0 = System.currentTimeMillis();

        SearchResult r = solveInternal(grid, start, goal, strat);

        long t1 = System.currentTimeMillis();
        long memAfter = usedMemoryKB();

        boolean success = r != null && r.plan != null;
        int cost = success ? r.cost : Integer.MAX_VALUE;
        int expanded = (r != null) ? r.expanded : Integer.MAX_VALUE;
        long timeMs = t1 - t0;
        long memKb = Math.max(0L, memAfter - memBefore);
        List<Position> route = success ? r.route : Collections.emptyList();

        return new SearchStats(success, cost, expanded, timeMs, memKb, route);
    }

    private static long usedMemoryKB(){ Runtime rt = Runtime.getRuntime(); return (rt.totalMemory() - rt.freeMemory()) / 1024; }
}
