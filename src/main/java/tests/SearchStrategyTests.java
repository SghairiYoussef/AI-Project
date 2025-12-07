package tests;

import code.delivery.*;
import code.model.*;
import code.search.*;

import java.util.*;

/**
 * Test suite for verifying search strategy correctness and optimality
 */
public class SearchStrategyTests {

    public static void main(String[] args) {
        System.out.println("=== SEARCH STRATEGY TEST SUITE ===\n");
        
        testAllStrategiesComplete();
        testOptimalityUCS();
        testOptimalityAStar();
        testBFSCompleteness();
        testDFSTermination();
        testIDSCompleteness();
        testHeuristicsAdmissibility();
        testRedundantStateElimination();
        testTunnelHandling();
        
        System.out.println("\n=== ALL TESTS COMPLETED ===");
    }
    
    /**
     * Test 1: Verify all 6 search strategies are implemented
     */
    private static void testAllStrategiesComplete() {
        System.out.println("TEST 1: All Search Strategies Implemented");
        Grid grid = createSimpleGrid();
        Position start = new Position(0, 0);
        Position goal = new Position(2, 2);
        
        String[] strategies = {"BFS", "DFS", "UCS", "IDS", "GREEDY", "ASTAR"};
        int passed = 0;
        
        for (String strat : strategies) {
            String result = DeliverySearch.solve(grid, start, goal, strat);
            if (result != null && !result.contains("No solution")) {
                System.out.println("  ✓ " + strat + " implemented and returns result");
                passed++;
            } else {
                System.out.println("  ✗ " + strat + " failed or not implemented");
            }
        }
        
        System.out.println("  Result: " + passed + "/6 strategies working\n");
    }
    
    /**
     * Test 2: Verify UCS returns optimal solution
     */
    private static void testOptimalityUCS() {
        System.out.println("TEST 2: UCS Optimality");
        Grid grid = createGridWithDifferentCosts();
        Position start = new Position(0, 0);
        Position goal = new Position(3, 0);
        
        // UCS should find the path with lowest total cost
        SearchResult ucsResult = DeliverySearch.solveInternal(grid, start, goal, "UCS");
        SearchResult bfsResult = DeliverySearch.solveInternal(grid, start, goal, "BFS");
        
        if (ucsResult != null && bfsResult != null) {
            System.out.println("  UCS cost: " + ucsResult.cost);
            System.out.println("  BFS cost: " + bfsResult.cost);
            
            if (ucsResult.cost <= bfsResult.cost) {
                System.out.println("  ✓ UCS returns optimal or equal cost to BFS");
            } else {
                System.out.println("  ✗ UCS cost higher than BFS (should be optimal)");
            }
        } else {
            System.out.println("  ✗ UCS or BFS failed to find solution");
        }
        System.out.println();
    }
    
    /**
     * Test 3: Verify A* returns optimal solution
     */
    private static void testOptimalityAStar() {
        System.out.println("TEST 3: A* Optimality");
        Grid grid = createGridWithDifferentCosts();
        Position start = new Position(0, 0);
        Position goal = new Position(3, 0);
        
        SearchResult astarResult = DeliverySearch.solveInternal(grid, start, goal, "ASTAR");
        SearchResult ucsResult = DeliverySearch.solveInternal(grid, start, goal, "UCS");
        
        if (astarResult != null && ucsResult != null) {
            System.out.println("  A* cost: " + astarResult.cost);
            System.out.println("  UCS cost: " + ucsResult.cost);
            System.out.println("  A* nodes expanded: " + astarResult.expanded);
            System.out.println("  UCS nodes expanded: " + ucsResult.expanded);
            
            if (astarResult.cost == ucsResult.cost) {
                System.out.println("  ✓ A* returns optimal solution (same cost as UCS)");
            } else {
                System.out.println("  ✗ A* cost differs from UCS (should be optimal)");
            }
            
            if (astarResult.expanded <= ucsResult.expanded) {
                System.out.println("  ✓ A* expands fewer or equal nodes than UCS");
            } else {
                System.out.println("  ⚠ A* expanded more nodes than UCS (admissible heuristic should help)");
            }
        } else {
            System.out.println("  ✗ A* or UCS failed to find solution");
        }
        System.out.println();
    }
    
    /**
     * Test 4: Verify BFS completeness
     */
    private static void testBFSCompleteness() {
        System.out.println("TEST 4: BFS Completeness");
        Grid grid = createSimpleGrid();
        Position start = new Position(0, 0);
        Position goal = new Position(2, 2);
        
        SearchResult result = DeliverySearch.solveInternal(grid, start, goal, "BFS");
        
        if (result != null && result.plan != null) {
            System.out.println("  ✓ BFS found solution: " + result.plan);
            System.out.println("  Cost: " + result.cost + ", Nodes expanded: " + result.expanded);
        } else {
            System.out.println("  ✗ BFS failed to find solution");
        }
        System.out.println();
    }
    
    /**
     * Test 5: Verify DFS terminates
     */
    private static void testDFSTermination() {
        System.out.println("TEST 5: DFS Termination");
        Grid grid = createSimpleGrid();
        Position start = new Position(0, 0);
        Position goal = new Position(2, 2);
        
        long startTime = System.currentTimeMillis();
        SearchResult result = DeliverySearch.solveInternal(grid, start, goal, "DFS");
        long elapsed = System.currentTimeMillis() - startTime;
        
        if (elapsed < 5000) {
            System.out.println("  ✓ DFS terminated in " + elapsed + "ms");
            if (result != null && result.plan != null) {
                System.out.println("  ✓ DFS found solution");
            }
        } else {
            System.out.println("  ✗ DFS took too long (" + elapsed + "ms)");
        }
        System.out.println();
    }
    
    /**
     * Test 6: Verify IDS completeness
     */
    private static void testIDSCompleteness() {
        System.out.println("TEST 6: IDS Completeness");
        Grid grid = createSimpleGrid();
        Position start = new Position(0, 0);
        Position goal = new Position(2, 2);
        
        SearchResult result = DeliverySearch.solveInternal(grid, start, goal, "IDS");
        
        if (result != null && result.plan != null) {
            System.out.println("  ✓ IDS found solution: " + result.plan);
            System.out.println("  Cost: " + result.cost + ", Nodes expanded: " + result.expanded);
        } else {
            System.out.println("  ✗ IDS failed to find solution");
        }
        System.out.println();
    }
    
    /**
     * Test 7: Verify heuristics are admissible
     */
    private static void testHeuristicsAdmissibility() {
        System.out.println("TEST 7: Heuristic Admissibility");
        Grid grid = createSimpleGrid();
        Position start = new Position(0, 0);
        Position goal = new Position(2, 2);
        
        DeliveryProblem problem = new DeliveryProblem(grid, start, goal);
        
        // Test deliveryAdmissible
        int h1 = Heuristics.deliveryAdmissible(problem, start);
        SearchResult ucsResult = DeliverySearch.solveInternal(grid, start, goal, "UCS");
        
        System.out.println("  deliveryAdmissible h(start): " + h1);
        System.out.println("  Actual optimal cost: " + (ucsResult != null ? ucsResult.cost : "N/A"));
        
        if (ucsResult != null && h1 <= ucsResult.cost) {
            System.out.println("  ✓ deliveryAdmissible is admissible (h ≤ h*)");
        } else if (ucsResult != null) {
            System.out.println("  ✗ deliveryAdmissible overestimates (h > h*)");
        }
        
        // Test tunnelAware
        int h2 = Heuristics.tunnelAware(problem, start);
        System.out.println("  tunnelAware h(start): " + h2);
        
        if (ucsResult != null && h2 <= ucsResult.cost) {
            System.out.println("  ✓ tunnelAware is admissible (h ≤ h*)");
        } else if (ucsResult != null) {
            System.out.println("  ✗ tunnelAware overestimates (h > h*)");
        }
        System.out.println();
    }
    
    /**
     * Test 8: Verify redundant state elimination
     */
    private static void testRedundantStateElimination() {
        System.out.println("TEST 8: Redundant State Elimination");
        Grid grid = createGridWithCycles();
        Position start = new Position(0, 0);
        Position goal = new Position(2, 2);
        
        // Grid with cycles should have redundant states eliminated
        SearchResult bfsResult = DeliverySearch.solveInternal(grid, start, goal, "BFS");
        
        if (bfsResult != null) {
            // Number of expanded nodes should be reasonable (not exponential)
            int maxExpected = grid.width * grid.height * 2; // Heuristic upper bound
            
            System.out.println("  Nodes expanded: " + bfsResult.expanded);
            System.out.println("  Max expected: " + maxExpected);
            
            if (bfsResult.expanded <= maxExpected) {
                System.out.println("  ✓ Redundant states appear to be eliminated");
            } else {
                System.out.println("  ✗ Too many nodes expanded (redundant states not eliminated?)");
            }
        }
        System.out.println();
    }
    
    /**
     * Test 9: Verify tunnel handling
     */
    private static void testTunnelHandling() {
        System.out.println("TEST 9: Tunnel Handling");
        Grid grid = createGridWithTunnel();
        Position start = new Position(0, 0);
        Position goal = new Position(4, 4);
        
        SearchResult result = DeliverySearch.solveInternal(grid, start, goal, "ASTAR");
        
        if (result != null && result.plan != null) {
            System.out.println("  ✓ Found solution with tunnel: " + result.plan);
            System.out.println("  Cost: " + result.cost);
            
            if (result.plan.contains("tunnel")) {
                System.out.println("  ✓ Solution uses tunnel");
            } else {
                System.out.println("  ⚠ Solution doesn't use tunnel (may still be optimal)");
            }
        } else {
            System.out.println("  ✗ Failed to find solution");
        }
        System.out.println();
    }
    
    // Helper methods to create test grids
    
    private static Grid createSimpleGrid() {
        Grid grid = new Grid(3, 3);
        // Create a simple 3x3 grid with uniform costs
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                Position p = new Position(x, y);
                // Add edges in all 4 directions with cost 1
                if (x < 2) grid.setEdge(p, new Position(x+1, y), 1);
                if (x > 0) grid.setEdge(p, new Position(x-1, y), 1);
                if (y < 2) grid.setEdge(p, new Position(x, y+1), 1);
                if (y > 0) grid.setEdge(p, new Position(x, y-1), 1);
            }
        }
        return grid;
    }
    
    private static Grid createGridWithDifferentCosts() {
        Grid grid = new Grid(4, 2);
        // Create a grid where one path has lower cost than another
        // Path 1 (top): 0,0 -> 1,0 -> 2,0 -> 3,0 (cost 1+1+1 = 3)
        // Path 2 (bottom then across): 0,0 -> 0,1 -> 1,1 -> 2,1 -> 3,1 -> 3,0 (higher cost)
        
        for (int x = 0; x < 3; x++) {
            grid.setEdge(new Position(x, 0), new Position(x+1, 0), 1); // Top path - low cost
            grid.setEdge(new Position(x+1, 0), new Position(x, 0), 1);
        }
        
        for (int x = 0; x < 3; x++) {
            grid.setEdge(new Position(x, 1), new Position(x+1, 1), 2); // Bottom path - higher cost
            grid.setEdge(new Position(x+1, 1), new Position(x, 1), 2);
        }
        
        // Vertical connections
        for (int x = 0; x < 4; x++) {
            grid.setEdge(new Position(x, 0), new Position(x, 1), 2);
            grid.setEdge(new Position(x, 1), new Position(x, 0), 2);
        }
        
        return grid;
    }
    
    private static Grid createGridWithCycles() {
        // Same as simple grid - has cycles
        return createSimpleGrid();
    }
    
    private static Grid createGridWithTunnel() {
        Grid grid = new Grid(5, 5);
        
        // Create basic grid connectivity
        for (int x = 0; x < 5; x++) {
            for (int y = 0; y < 5; y++) {
                Position p = new Position(x, y);
                if (x < 4) grid.setEdge(p, new Position(x+1, y), 2);
                if (x > 0) grid.setEdge(p, new Position(x-1, y), 2);
                if (y < 4) grid.setEdge(p, new Position(x, y+1), 2);
                if (y > 0) grid.setEdge(p, new Position(x, y-1), 2);
            }
        }
        
        // Add tunnel from (0,0) to (4,4) - should provide shortcut
        grid.addTunnel(new Position(0, 0), new Position(4, 4));
        
        return grid;
    }
}
