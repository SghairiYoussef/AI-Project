package code;

import code.model.*;
import code.delivery.*;
import code.viz.SwingVisualizer;

import java.io.IOException;
import java.util.*;

/**
 * Runner: automated assignment (Option 3 nearest destination with reservation).
 */
public class Main {
    public static void main(String[] args) throws IOException {
        String worldFile = "src/main/resources/sample.world";
        if (args.length >= 1) worldFile = args[0];

        Grid grid = WorldParser.parse(worldFile);

        System.out.println("Parsed world:");
        System.out.println("Grid " + grid.width + "x" + grid.height);
        System.out.println("Stores: " + grid.stores);
        System.out.println("Destinations: " + grid.destinations);
        System.out.println("Agents: " + grid.agents);

        // Plan multi-delivery using Reservation (each destination only once)
        List<DeliveryPlanner.Assignment> assignments = DeliveryPlanner.planMultiDelivery(grid);

        System.out.println("\nAssignments (agent -> route steps):");
        Map<Agent, List<code.model.Position>> routes = new LinkedHashMap<>();
        for (DeliveryPlanner.Assignment a : assignments) {
            System.out.println(a.agent.id + " => " + a.route.size() + " steps, " + a.strategySummary());
            routes.put(a.agent, a.route);
        }

        // Visualize sequentially (one agent at a time)
        SwingVisualizer.showFrame(grid, routes, 220, true);
    }
}
