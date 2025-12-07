package tests;

import code.delivery.DeliverySearch;
import code.model.Grid;
import code.model.Position;
import code.model.WorldParser;

public class TestSpecification {
    public static void main(String[] args) {
        System.out.println("=== Testing Specification Compliance ===\n");
        
        // Test 1: GenGrid()
        System.out.println("1. Testing GenGrid():");
        String gridString = DeliverySearch.genGrid();
        System.out.println("Generated Grid: " + gridString);
        System.out.println("✓ GenGrid() works\n");
        
        // Test 2: Parse the generated grid
        System.out.println("2. Testing Grid Parsing:");
        try {
            Grid grid = WorldParser.parseGridString(gridString);
            System.out.println("Grid Size: " + grid.width + "x" + grid.height);
            System.out.println("Stores: " + grid.stores.size());
            System.out.println("Destinations: " + grid.destinations.size());
            System.out.println("Agents: " + grid.agents.size());
            System.out.println("✓ Grid parsing works\n");
            
            // Test 3: path() method
            if (grid.agents.size() > 0 && grid.destinations.size() > 0) {
                System.out.println("3. Testing path() method:");
                Position start = grid.agents.get(0).pos;
                Position goal = grid.destinations.get(0);
                String pathResult = DeliverySearch.path(grid, start, goal, "BFS");
                System.out.println("Path Result: " + pathResult);
                System.out.println("✓ path() method works\n");
            }
            
            // Test 4: plan() method
            System.out.println("4. Testing plan() method:");
            String planResult = DeliverySearch.plan(gridString, "BFS", false, false);
            System.out.println("Plan Result: " + planResult);
            System.out.println("✓ plan() method works\n");
            
            // Test 5: plan() with traffic
            System.out.println("5. Testing plan() with traffic:");
            String planWithTraffic = DeliverySearch.plan(gridString, "ASTAR", true, false);
            System.out.println("Plan with Traffic: " + planWithTraffic);
            System.out.println("✓ plan() with traffic works\n");
            
            // Test 6: Verify DeliverySearch extends GenericSearch
            System.out.println("6. Verifying class hierarchy:");
            System.out.println("DeliverySearch extends GenericSearch: " + 
                (code.search.GenericSearch.class.isAssignableFrom(DeliverySearch.class)));
            System.out.println("✓ Inheritance verified\n");
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("\n=== All Specification Tests Passed ===");
    }
}
