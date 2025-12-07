package code;

import code.model.*;
import code.delivery.*;
import code.viz.SwingVisualizer;

import java.util.*;

public class Main {
    public static void main(String[] args) {
        System.out.println("══════════════════════════════════════════════════════════════=");
        System.out.println("  PACKAGE DELIVERY SEARCH AGENT - SPECIFICATION DEMO");
        System.out.println("══════════════════════════════════════════════════════════════=\n");

        System.out.println("1. Generating random grid using GenGrid()...");
        String gridString = DeliverySearch.genGrid();
        System.out.println("   Grid generated successfully\n");

        Grid grid = WorldParser.parseGridString(gridString);

        System.out.println("Grid Configuration:");
        System.out.println("  Dimensions: " + grid.width + "×" + grid.height);
        System.out.println("  Stores: " + grid.stores.size() + " → " + grid.stores);
        System.out.println("  Destinations: " + grid.destinations.size() + " → " + grid.destinations);
        System.out.println("  Agents: " + grid.agents.size());
        System.out.println("  Tunnels: " + grid.getTunnels().size());
        System.out.println();

        System.out.println("2. Testing path() method (single truck pathfinding)...");
        if (!grid.agents.isEmpty() && !grid.destinations.isEmpty()) {
            Agent firstAgent = grid.agents.get(0);
            Position firstDest = grid.destinations.get(0);

            System.out.println("   Finding path from " + firstAgent.pos + " to " + firstDest);

            String[] strategiesToTest = {"BFS", "UCS", "ASTAR"};
            for (String strat : strategiesToTest) {
                String result = DeliverySearch.path(grid, firstAgent.pos, firstDest, strat);
                String[] parts = result.split(";");
                if (parts.length >= 3) {
                    System.out.println("   " + strat + ": cost=" + parts[1] +
                                     ", nodes=" + parts[2] +
                                     ", actions=" + (parts[0].isEmpty() ? 0 : parts[0].split(",").length));
                }
            }
            System.out.println("   path() method working correctly\n");
        }

        System.out.println("3. Planning multi-agent delivery using plan()...");

        String[] planStrategies = {"AUTO", "UCS", "ASTAR", "GREEDY"};
        String bestStrategy = "AUTO";
        int bestCost = Integer.MAX_VALUE;

        System.out.println("   Testing strategies:");
        for (String strategy : planStrategies) {
            String planResult = DeliverySearch.plan(gridString, strategy, false);

            int totalCost = parseTotalCost(planResult);
            System.out.println("   " + strategy + ": total cost = " + totalCost);

            if (totalCost > 0 && totalCost < bestCost) {
                bestCost = totalCost;
                bestStrategy = strategy;
            }
        }

        System.out.println("   Best strategy: " + bestStrategy + " (cost=" + bestCost + ")\n");

        System.out.println("4. Executing final delivery plan with " + bestStrategy + "...\\n");

        List<DeliveryPlanner.Assignment> assignments =
            DeliveryPlanner.planMultiDeliveryWithStrategy(grid, bestStrategy);

        System.out.println("Optimal Assignments:");
        System.out.println("─────────────────────────────────────────────────────────────\n");

        Map<Agent, List<Position>> routes = new LinkedHashMap<>();
        int totalCost = 0;
        int totalSteps = 0;

        for (DeliveryPlanner.Assignment a : assignments) {
            System.out.println("Agent " + a.agent.id + ":");
            System.out.println("  Start: " + a.agent.pos);
            System.out.println("  Deliveries completed: " + a.deliveriesCompleted);
            System.out.println("  Total cost: " + a.stats.cost);
            System.out.println("  Nodes expanded: " + a.stats.expanded);
            System.out.println("  Strategies: " + a.strategy);
            System.out.println("  Actions: " + formatActions(a.stats.actions));
            System.out.println();

            routes.put(a.agent, a.route);
            totalCost += a.stats.cost;
            totalSteps += a.stats.actions.size();
        }

        System.out.println("══════════════════════════════════════════════════════════════=");
        System.out.println("Summary:");
        System.out.println("  Total agents: " + assignments.size());
        System.out.println("  Total deliveries: " + assignments.stream().mapToInt(a -> a.deliveriesCompleted).sum());
        System.out.println("  Total cost: " + totalCost);
        System.out.println("  Total actions: " + totalSteps);
        System.out.println("  Average cost per agent: " + (assignments.size() > 0 ? totalCost / assignments.size() : 0));
        System.out.println("══════════════════════════════════════════════════════════════=\n");

        System.out.println("5. Opening visualization window...\n");

        SwingVisualizer.showFrame(grid, routes, 220, true);
    }

    private static String formatActions(List<String> actions) {
        if (actions.isEmpty()) {
            return "[]";
        }
        if (actions.size() <= 10) {
            return actions.toString();
        }
        return "[" + String.join(", ", actions.subList(0, 5)) +
               " ... (" + (actions.size() - 5) + " more) ]";
    }

    private static int parseTotalCost(String planResult) {
        try {
            String[] parts = planResult.split(";");
            if (parts.length >= 2) {
                return Integer.parseInt(parts[parts.length - 2].trim());
            }
        } catch (Exception e) {
            // Ignore parsing errors and return max value
        }
        return Integer.MAX_VALUE;
    }
}