package code.search;

public class Node {
    public final Object state;
    public final Node parent;
    public final String action;
    public final int pathCost;
    public final int depth;
    public Node(Object state){ this(state, null, null, 0, 0); }
    public Node(Object state, Node parent, String action, int stepCost, int depth){
        this.state = state; this.parent = parent; this.action = action;
        this.pathCost = (parent==null) ? stepCost : parent.pathCost + stepCost;
        this.depth = depth;
    }
    public Node(Object state, Node parent, String action, int stepCost){
        this(state, parent, action, stepCost, (parent==null?0:parent.depth+1));
    }
}
