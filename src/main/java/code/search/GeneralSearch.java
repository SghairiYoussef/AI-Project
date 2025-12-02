package code.search;

import java.util.*;

/**
 * GeneralGraphSearch supporting BFS, DFS, UCS, IDS, GREEDY, ASTAR.
 */
public class GeneralSearch {

    public static class Result {
        public final Node node;
        public final int nodesExpanded;
        public Result(Node n, int expanded){ this.node = n; this.nodesExpanded = expanded; }
    }

    public static Result generalSearch(Problem problem, SearchStrategy strat){
        switch(strat){
            case BFS: return bfs(problem);
            case DFS: return dfs(problem);
            case UCS: return ucs(problem);
            case IDS: return ids(problem, 60);
            case GREEDY: return greedy(problem);
            case ASTAR: return aStar(problem);
            default: return new Result(null,0);
        }
    }

    private static Result bfs(Problem problem){
        Queue<Node> frontier = new ArrayDeque<>();
        Set<Object> explored = new HashSet<>();
        frontier.add(new Node(problem.initialState()));
        int nodesExpanded = 0;
        while(!frontier.isEmpty()){
            Node node = frontier.poll();
            if(problem.goalTest(node.state)) return new Result(node, nodesExpanded);
            if(explored.contains(node.state)) continue;
            explored.add(node.state);
            nodesExpanded++;
            for(String op : problem.operators()){
                Object s2 = problem.apply(node.state, op);
                if(s2==null) continue;
                Node child = new Node(s2, node, op, problem.stepCost(node.state, op));
                frontier.add(child);
            }
        }
        return new Result(null, nodesExpanded);
    }

    private static Result dfs(Problem problem){
        Deque<Node> frontier = new ArrayDeque<>();
        Set<Object> explored = new HashSet<>();
        frontier.addFirst(new Node(problem.initialState()));
        int nodesExpanded = 0;
        while(!frontier.isEmpty()){
            Node node = frontier.removeFirst();
            if(problem.goalTest(node.state)) return new Result(node, nodesExpanded);
            if(explored.contains(node.state)) continue;
            explored.add(node.state);
            nodesExpanded++;
            List<String> ops = problem.operators();
            for(int i=ops.size()-1;i>=0;i--){
                String op = ops.get(i);
                Object s2 = problem.apply(node.state, op);
                if(s2==null) continue;
                Node child = new Node(s2, node, op, problem.stepCost(node.state, op));
                frontier.addFirst(child);
            }
        }
        return new Result(null, nodesExpanded);
    }

    private static Result ucs(Problem problem){
        Comparator<Node> cmp = Comparator.comparingInt(n -> n.pathCost);
        PriorityQueue<Node> frontier = new PriorityQueue<>(cmp);
        Map<Object, Integer> best = new HashMap<>();
        frontier.add(new Node(problem.initialState()));
        int nodesExpanded = 0;
        while(!frontier.isEmpty()){
            Node node = frontier.poll();
            if(problem.goalTest(node.state)) return new Result(node, nodesExpanded);
            Integer prev = best.get(node.state);
            if(prev != null && prev <= node.pathCost) continue;
            best.put(node.state, node.pathCost);
            nodesExpanded++;
            for(String op : problem.operators()){
                Object s2 = problem.apply(node.state, op);
                if(s2==null) continue;
                int step = problem.stepCost(node.state, op);
                Node child = new Node(s2, node, op, step);
                frontier.add(child);
            }
        }
        return new Result(null, nodesExpanded);
    }

    private static Result greedy(Problem problem){
        if(!(problem instanceof code.delivery.DeliveryProblem)) return new Result(null,0);
        code.delivery.DeliveryProblem dp = (code.delivery.DeliveryProblem) problem;
        Comparator<Node> cmp = Comparator.comparingInt(n -> Heuristics.tunnelAware(dp, n.state));
        PriorityQueue<Node> frontier = new PriorityQueue<>(cmp);
        Set<Object> explored = new HashSet<>();
        frontier.add(new Node(problem.initialState()));
        int nodesExpanded = 0;
        while(!frontier.isEmpty()){
            Node node = frontier.poll();
            if(problem.goalTest(node.state)) return new Result(node, nodesExpanded);
            if(explored.contains(node.state)) continue;
            explored.add(node.state);
            nodesExpanded++;
            for(String op : problem.operators()){
                Object s2 = problem.apply(node.state, op);
                if(s2==null) continue;
                Node child = new Node(s2, node, op, problem.stepCost(node.state, op));
                frontier.add(child);
            }
        }
        return new Result(null, nodesExpanded);
    }

    private static Result aStar(Problem problem){
        if(!(problem instanceof code.delivery.DeliveryProblem)) return new Result(null,0);
        code.delivery.DeliveryProblem dp = (code.delivery.DeliveryProblem) problem;
        Comparator<Node> cmp = Comparator.comparingInt(n -> n.pathCost + Heuristics.tunnelAware(dp, n.state));
        PriorityQueue<Node> frontier = new PriorityQueue<>(cmp);
        Map<Object,Integer> best = new HashMap<>();
        frontier.add(new Node(problem.initialState()));
        int nodesExpanded = 0;
        while(!frontier.isEmpty()){
            Node node = frontier.poll();
            if(problem.goalTest(node.state)) return new Result(node, nodesExpanded);
            Integer prev = best.get(node.state);
            if(prev != null && prev <= node.pathCost) continue;
            best.put(node.state, node.pathCost);
            nodesExpanded++;
            for(String op : problem.operators()){
                Object s2 = problem.apply(node.state, op);
                if(s2==null) continue;
                Node child = new Node(s2, node, op, problem.stepCost(node.state, op));
                frontier.add(child);
            }
        }
        return new Result(null, nodesExpanded);
    }

    private static Result ids(Problem problem, int maxDepth){
        for(int depth=0; depth<=maxDepth; depth++){
            Result r = depthLimitedSearch(problem, depth);
            if(r.node != null) return r;
        }
        return new Result(null,0);
    }

    private static Result depthLimitedSearch(Problem problem, int limit){
        Deque<Node> frontier = new ArrayDeque<>();
        Set<Object> explored = new HashSet<>();
        frontier.addFirst(new Node(problem.initialState()));
        int nodesExpanded = 0;
        while(!frontier.isEmpty()){
            Node node = frontier.removeFirst();
            if(problem.goalTest(node.state)) return new Result(node, nodesExpanded);
            if(node.depth >= limit) continue;
            if(explored.contains(node.state)) continue;
            explored.add(node.state);
            nodesExpanded++;
            List<String> ops = problem.operators();
            for(int i=ops.size()-1;i>=0;i--){
                String op = ops.get(i);
                Object s2 = problem.apply(node.state, op);
                if(s2==null) continue;
                Node child = new Node(s2, node, op, problem.stepCost(node.state, op));
                frontier.addFirst(child);
            }
        }
        return new Result(null, nodesExpanded);
    }
}
