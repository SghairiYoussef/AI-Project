package code.delivery;

import code.model.Grid;
import code.model.Position;
import code.search.Problem;
import java.util.ArrayList;
import java.util.List;

public class DeliveryProblem extends Problem {
    public final Grid grid;
    public final Position start;
    public final Position goal;
    public DeliveryProblem(Grid g, Position s, Position goal){
        this.grid = g; this.start = s; this.goal = goal;
    }
    @Override public Object initialState(){ return start; }
    @Override public boolean goalTest(Object state){ return ((Position)state).equals(goal); }
    @Override public List<String> operators(){
        List<String> ops = new ArrayList<>();
        ops.add("up"); ops.add("down"); ops.add("left"); ops.add("right"); ops.add("tunnel");
        return ops;
    }
    @Override public Object apply(Object state, String operator){
        Position p = (Position) state;
        if("tunnel".equals(operator)){
            Position partner = grid.tunnelPartner(p);
            return partner == null ? null : partner;
        }
        int nx = p.x, ny = p.y;
        switch(operator){
            case "up": ny = p.y - 1; break;
            case "down": ny = p.y + 1; break;
            case "left": nx = p.x - 1; break;
            case "right": nx = p.x + 1; break;
            default: return null;
        }
        Position np = new Position(nx, ny);
        if(!grid.inBounds(np)) return null;
        int c = grid.moveCost(p, np);
        if(c == 0) return null;
        if(c >= Integer.MAX_VALUE/4) return null;
        return np;
    }
    @Override public int stepCost(Object state, String operator){
        Position p = (Position) state;
        if("tunnel".equals(operator)){
            Position partner = grid.tunnelPartner(p);
            return partner == null ? Integer.MAX_VALUE/4 : Math.max(1, Math.abs(p.x - partner.x) + Math.abs(p.y - partner.y));
        }
        Position q = (Position) apply(state, operator);
        if(q == null) return Integer.MAX_VALUE/4;
        return grid.moveCost(p,q);
    }
}
