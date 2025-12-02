package code.model;

public class Agent {
    public final String id;
    public Position pos;
    public Agent(String id, Position pos){ this.id = id; this.pos = pos; }
    @Override public String toString(){ return id + "@" + pos; }
}
