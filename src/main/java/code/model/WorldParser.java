package code.model;

import java.nio.file.*;
import java.io.IOException;
import java.util.*;

/**
 * .world parser supporting:
 * GRID W H
 * STORE x y
 * DEST x y
 * AGENT id x y
 * EDGE x1 y1 x2 y2 cost
 * UNDIRECTED_EDGE x1 y1 x2 y2 cost
 * BLOCK x1 y1 x2 y2
 * TUNNEL x1 y1 x2 y2
 *
 * Comments start with #
 */
public class WorldParser {
    public static Grid parse(String path) throws IOException {
        List<String> lines = Files.readAllLines(Path.of(path));
        Grid grid = null;
        for(String raw : lines){
            String line = raw.trim();
            if(line.isEmpty() || line.startsWith("#")) continue;
            String[] tok = line.split("\\s+");
            String cmd = tok[0].toUpperCase();
            switch(cmd){
                case "GRID":
                    grid = new Grid(Integer.parseInt(tok[1]), Integer.parseInt(tok[2]));
                    break;
                case "STORE":
                    grid.stores.add(new Position(Integer.parseInt(tok[1]), Integer.parseInt(tok[2])));
                    break;
                case "DEST":
                    grid.destinations.add(new Position(Integer.parseInt(tok[1]), Integer.parseInt(tok[2])));
                    break;
                case "AGENT":
                    grid.agents.add(new Agent(tok[1], new Position(Integer.parseInt(tok[2]), Integer.parseInt(tok[3]))));
                    break;
                case "EDGE":
                    Position a = new Position(Integer.parseInt(tok[1]), Integer.parseInt(tok[2]));
                    Position b = new Position(Integer.parseInt(tok[3]), Integer.parseInt(tok[4]));
                    int cost = Integer.parseInt(tok[5]);
                    grid.setEdge(a,b,cost);
                    break;
                case "UNDIRECTED_EDGE":
                    Position a1 = new Position(Integer.parseInt(tok[1]), Integer.parseInt(tok[2]));
                    Position b1 = new Position(Integer.parseInt(tok[3]), Integer.parseInt(tok[4]));
                    int cost1 = Integer.parseInt(tok[5]);
                    grid.setEdge(a1,b1,cost1);
                    grid.setEdge(b1,a1,cost1);
                    break;
                case "BLOCK":
                    Position p1 = new Position(Integer.parseInt(tok[1]), Integer.parseInt(tok[2]));
                    Position p2 = new Position(Integer.parseInt(tok[3]), Integer.parseInt(tok[4]));
                    grid.setEdge(p1,p2,0);
                    grid.setEdge(p2,p1,0);
                    break;
                case "TUNNEL":
                    Position ta = new Position(Integer.parseInt(tok[1]), Integer.parseInt(tok[2]));
                    Position tb = new Position(Integer.parseInt(tok[3]), Integer.parseInt(tok[4]));
                    grid.addTunnel(ta,tb);
                    break;
                default:
                    System.err.println("Unknown directive: " + cmd);
            }
        }
        if(grid == null) throw new IllegalArgumentException("World file must start with GRID W H");
        return grid;
    }
}
