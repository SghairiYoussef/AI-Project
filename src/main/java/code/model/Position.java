package code.model;

import java.util.Objects;

public class Position {
    public final int x;
    public final int y;
    public Position(int x, int y) { this.x = x; this.y = y; }
    @Override public boolean equals(Object o){
        if(this==o) return true;
        if(!(o instanceof Position)) return false;
        Position p=(Position)o;
        return x==p.x && y==p.y;
    }
    @Override public int hashCode(){ return Objects.hash(x,y); }
    @Override public String toString(){ return "(" + x + "," + y + ")"; }
}
