package code.model;

import java.util.*;

/**
 * Grid: explicit directed edges, tunnel list, stores/dests/agents.
 * Edges with cost==0 are blocked.
 */
public class Grid {
    public final int width;
    public final int height;
    private final Map<String, Integer> edgeCost = new LinkedHashMap<>();
    private final List<Position[]> tunnels = new ArrayList<>();
    public final List<Position> stores = new ArrayList<>();
    public final List<Position> destinations = new ArrayList<>();
    public final List<Agent> agents = new ArrayList<>();

    public Grid(int width, int height){ this.width = width; this.height = height; }

    private String key(Position a, Position b){ return a.x + "," + a.y + ":" + b.x + "," + b.y; }

    public void setEdge(Position a, Position b, int cost){ edgeCost.put(key(a,b), cost); }

    public Integer getEdgeCost(Position a, Position b){ return edgeCost.get(key(a,b)); }

    public Map<String,Integer> getEdgeCostMap(){ return Collections.unmodifiableMap(edgeCost); }

    public boolean hasEdge(Position a, Position b){ return edgeCost.containsKey(key(a,b)); }

    public void addTunnel(Position a, Position b){ tunnels.add(new Position[]{a,b}); }

    public List<Position[]> getTunnels(){ return Collections.unmodifiableList(tunnels); }

    public Position tunnelPartner(Position p){
        for(Position[] t : tunnels){
            if(t[0].equals(p)) return t[1];
            if(t[1].equals(p)) return t[0];
        }
        return null;
    }

    public List<Position> neighbors(Position p){
        List<Position> res = new ArrayList<>();
        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
        for(int[] d : dirs){
            int nx = p.x + d[0], ny = p.y + d[1];
            if(nx<0 || ny<0 || nx>=width || ny>=height) continue;
            Position q = new Position(nx, ny);
            Integer c = getEdgeCost(p,q);
            if(c != null && c > 0) res.add(q);
        }
        Position partner = tunnelPartner(p);
        if(partner != null) res.add(partner);
        return res;
    }

    public int moveCost(Position a, Position b){
        Position partner = tunnelPartner(a);
        if(partner != null && partner.equals(b)){
            return Math.max(1, Math.abs(a.x - b.x) + Math.abs(a.y - b.y));
        }
        Integer c = getEdgeCost(a,b);
        if(c == null) return Integer.MAX_VALUE/4;
        return c;
    }

    public int minEdgeCost(){ return edgeCost.values().stream().filter(v->v>0).min(Integer::compareTo).orElse(1); }

    public boolean inBounds(Position p){ return p.x>=0 && p.y>=0 && p.x<width && p.y<height; }

    /** Option 3 mapping: return nearest destination to store (Manhattan) */
    public Position getNearestDestination(Position store){
        if(destinations.isEmpty()) return null;
        return destinations.stream()
                .min(Comparator.comparingInt(d -> Math.abs(d.x - store.x) + Math.abs(d.y - store.y)))
                .orElse(destinations.get(0));
    }
}
