package code.delivery;

import code.model.Position;
import java.util.List;

public class SearchResult {
    public final String plan;
    public final int cost;
    public final int expanded;
    public final List<Position> route;
    public SearchResult(String plan, int cost, int expanded, List<Position> route){
        this.plan = plan; this.cost = cost; this.expanded = expanded; this.route = route;
    }
}
