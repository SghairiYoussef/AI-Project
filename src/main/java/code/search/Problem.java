package code.search;

import java.util.List;

public abstract class Problem {
    public abstract Object initialState();
    public abstract boolean goalTest(Object state);
    public abstract List<String> operators();
    public abstract Object apply(Object state, String operator);
    public abstract int stepCost(Object state, String operator);
}
