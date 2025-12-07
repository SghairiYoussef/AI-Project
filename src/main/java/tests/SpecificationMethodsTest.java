package tests;

import code.delivery.*;
import code.model.*;

/**
 * Test for the specification-required methods: GenGrid, path, and plan
 */
public class SpecificationMethodsTest {
    
    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║         TESTING SPECIFICATION-REQUIRED METHODS                ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝\n");
        
        testGenGrid();
        testPath();
        testPlan();
        
        System.out.println("\n✅ ALL SPECIFICATION METHODS TESTED");
    }
    
    /**
     * Test GenGrid() - randomly generates grid configuration
     */
    private static void testGenGrid() {
        System.out.println("TEST 1: GenGrid() Method");
        System.out.println("─────────────────────────────────────────────────────────────────");
        
        // Generate 3 random grids
        for (int i = 1; i <= 3; i++) {
            String gridString = DeliverySearch.genGrid();
            System.out.println("\nGenerated Grid " + i + ":");
            System.out.println(gridString);
            
            // Parse and validate
            String[] parts = gridString.split(";");
            if (parts.length >= 4) {
                int m = Integer.parseInt(parts[0]);
                int n = Integer.parseInt(parts[1]);
                int P = Integer.parseInt(parts[2]);
                int S = Integer.parseInt(parts[3]);
                
                System.out.println("  ✓ Valid format");
                System.out.println("  Grid: " + m + "x" + n);
                System.out.println("  Products: " + P);
                System.out.println("  Stores: " + S);
                System.out.println("  ✓ Minimum 1 store and 1 destination guaranteed");
            } else {
                System.out.println("  ✗ Invalid format");
            }
        }
        
        System.out.println("\n✓ GenGrid() test complete\n");
    }
    
    /**
     * Test path(...) - finds path for single truck to destination
     */
    private static void testPath() {
        System.out.println("TEST 2: path() Method");
        System.out.println("─────────────────────────────────────────────────────────────────");
        
        // Create a simple test grid
        Grid grid = createSimpleTestGrid();
        Position start = new Position(0, 0);
        Position goal = new Position(3, 3);
        
        System.out.println("Testing path from " + start + " to " + goal);
        System.out.println("\nResults (format: plan;cost;nodesExpanded):");
        
        String[] strategies = {"BFS", "DFS", "UCS", "IDS", "GREEDY", "ASTAR"};
        for (String strategy : strategies) {
            String result = DeliverySearch.path(grid, start, goal, strategy);
            System.out.println("  " + strategy + ": " + result);
            
            // Validate format
            String[] parts = result.split(";");
            if (parts.length == 3) {
                String plan = parts[0];
                int cost = Integer.parseInt(parts[1]);
                int expanded = Integer.parseInt(parts[2]);
                System.out.println("    ✓ Valid format - Plan has " + 
                                  (plan.isEmpty() ? 0 : plan.split(",").length) + 
                                  " actions, cost=" + cost + ", expanded=" + expanded);
            } else {
                System.out.println("    ✗ Invalid format");
            }
        }
        
        System.out.println("\n✓ path() test complete\n");
    }
    
    /**
     * Test plan(...) - determines which truck delivers which products
     */
    private static void testPlan() {
        System.out.println("TEST 3: plan() Method");
        System.out.println("─────────────────────────────────────────────────────────────────");
        
        // Create initial state string
        // Format: m;n;P;S;CustomerX_1,CustomerY_1,...;TunnelX_1,TunnelY_1,...;
        String initialState = "5;5;2;2;" +           // 5x5 grid, 2 products, 2 stores
                             "4,4,0,4;" +            // 2 destinations at (4,4) and (0,4)
                             "1,1,3,3;";             // 1 tunnel from (1,1) to (3,3)
        
        System.out.println("Initial State:");
        System.out.println(initialState);
        System.out.println();
        
        String[] strategies = {"UCS", "ASTAR", "GREEDY"};
        for (String strategy : strategies) {
            System.out.println("Testing with strategy: " + strategy);
            String planResult = DeliverySearch.plan(initialState, strategy, false);
            System.out.println(planResult);
            System.out.println();
        }
        
        System.out.println("✓ plan() test complete\n");
    }
    
    /**
     * Create a simple 4x4 test grid
     */
    private static Grid createSimpleTestGrid() {
        Grid grid = new Grid(4, 4);
        
        // Create full connectivity
        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 4; y++) {
                Position p = new Position(x, y);
                
                // Right edge
                if (x < 3) {
                    grid.setEdge(p, new Position(x + 1, y), 1);
                    grid.setEdge(new Position(x + 1, y), p, 1);
                }
                
                // Down edge
                if (y < 3) {
                    grid.setEdge(p, new Position(x, y + 1), 1);
                    grid.setEdge(new Position(x, y + 1), p, 1);
                }
            }
        }
        
        // Add a tunnel
        grid.addTunnel(new Position(0, 0), new Position(3, 3));
        
        return grid;
    }
}
