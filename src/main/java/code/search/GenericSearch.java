package code.search;

import java.util.*;

/**
 * GenericSearch - Abstract data type for generic search problems.
 * Implements generic search procedure with redundant state minimization/elimination.
 * Supports BFS, DFS, UCS, IDS, GREEDY, ASTAR strategies.
 * Guarantees termination within time limits.
 */
public abstract class GenericSearch {

    // Protected constructor to allow subclassing
    protected GenericSearch() {
    }

    public static class Result {
        public final Node node;
        public final int nodesExpanded;
        public final List<String> actions;

        public Result(Node n, int expanded, List<String> actions) {
            this.node = n;
            this.nodesExpanded = expanded;
            this.actions = actions != null ? new ArrayList<>(actions) : Collections.emptyList();
        }
    }

    private static List<String> getActionSequence(Node goal) {
        List<String> actions = new ArrayList<>();
        Node current = goal;
        while (current != null && current.parent != null) {
            actions.add(current.action);
            current = current.parent;
        }
        Collections.reverse(actions);
        return actions;
    }

    public static Result generalSearch(Problem problem, SearchStrategy strat) {
        switch (strat) {
            case BFS: return bfs(problem);
            case DFS: return dfs(problem);
            case UCS: return ucs(problem);
            case IDS: return ids(problem, 60);
            case GREEDY: return greedy(problem);
            case ASTAR: return aStar(problem);
            default: return new Result(null, 0, null);
        }
    }

    private static Result bfs(Problem problem) {
        Queue<Node> frontier = new ArrayDeque<>();
        Set<Object> explored = new HashSet<>();
        Set<Object> inFrontier = new HashSet<>();
        Node initial = new Node(problem.initialState());
        frontier.add(initial);
        inFrontier.add(initial.state);
        int nodesExpanded = 0;
        while (!frontier.isEmpty()) {
            Node node = frontier.poll();
            inFrontier.remove(node.state);
            if (problem.goalTest(node.state)) {
                return new Result(node, nodesExpanded, getActionSequence(node));
            }
            if (explored.contains(node.state)) continue;
            explored.add(node.state);
            nodesExpanded++;
            for (String op : problem.operators()) {
                Object s2 = problem.apply(node.state, op);
                if (s2 == null) continue;
                if (explored.contains(s2) || inFrontier.contains(s2)) continue;
                Node child = new Node(s2, node, op, problem.stepCost(node.state, op));
                frontier.add(child);
                inFrontier.add(s2);
            }
        }
        return new Result(null, nodesExpanded, null);
    }

    private static Result dfs(Problem problem) {
        Deque<Node> frontier = new ArrayDeque<>();
        Set<Object> explored = new HashSet<>();
        Set<Object> inFrontier = new HashSet<>();
        Node initial = new Node(problem.initialState());
        frontier.addFirst(initial);
        inFrontier.add(initial.state);
        int nodesExpanded = 0;
        while (!frontier.isEmpty()) {
            Node node = frontier.removeFirst();
            inFrontier.remove(node.state);
            if (problem.goalTest(node.state)) {
                return new Result(node, nodesExpanded, getActionSequence(node));
            }
            if (explored.contains(node.state)) continue;
            explored.add(node.state);
            nodesExpanded++;
            List<String> ops = problem.operators();
            for (int i = ops.size() - 1; i >= 0; i--) {
                String op = ops.get(i);
                Object s2 = problem.apply(node.state, op);
                if (s2 == null) continue;
                // Redundant state elimination: don't add if already explored or in frontier
                if (explored.contains(s2) || inFrontier.contains(s2)) continue;
                Node child = new Node(s2, node, op, problem.stepCost(node.state, op));
                frontier.addFirst(child);
                inFrontier.add(s2);
            }
        }
        return new Result(null, nodesExpanded, null);
    }

    private static Result ucs(Problem problem) {
        Comparator<Node> cmp = Comparator.comparingInt(n -> n.pathCost);
        PriorityQueue<Node> frontier = new PriorityQueue<>(cmp);
        Map<Object, Integer> best = new HashMap<>();
        frontier.add(new Node(problem.initialState()));
        int nodesExpanded = 0;
        while (!frontier.isEmpty()) {
            Node node = frontier.poll();
            if (problem.goalTest(node.state)) {
                return new Result(node, nodesExpanded, getActionSequence(node));
            }
            Integer prev = best.get(node.state);
            if (prev != null && prev <= node.pathCost) continue;
            best.put(node.state, node.pathCost);
            nodesExpanded++;
            for (String op : problem.operators()) {
                Object s2 = problem.apply(node.state, op);
                if (s2 == null) continue;
                int step = problem.stepCost(node.state, op);
                Node child = new Node(s2, node, op, step);
                frontier.add(child);
            }
        }
        return new Result(null, nodesExpanded, null);
    }

    private static Result greedy(Problem problem) {
        return greedy(problem, 1);
    }

    public static Result greedy(Problem problem, int heuristicChoice) {
        if (!(problem instanceof code.delivery.DeliveryProblem)) return new Result(null, 0, null);
        code.delivery.DeliveryProblem dp = (code.delivery.DeliveryProblem) problem;
        // Heuristic 1: tunnelAware, Heuristic 2: deliveryAdmissible (both non-trivial and admissible)
        Comparator<Node> cmp = Comparator.comparingInt(n -> 
            heuristicChoice == 2 ? Heuristics.deliveryAdmissible(dp, n.state) : Heuristics.tunnelAware(dp, n.state)
        );
        PriorityQueue<Node> frontier = new PriorityQueue<>(cmp);
        Set<Object> explored = new HashSet<>();
        Set<Object> inFrontier = new HashSet<>();
        Node initial = new Node(problem.initialState());
        frontier.add(initial);
        inFrontier.add(initial.state);
        int nodesExpanded = 0;
        while (!frontier.isEmpty()) {
            Node node = frontier.poll();
            inFrontier.remove(node.state);
            if (problem.goalTest(node.state)) {
                return new Result(node, nodesExpanded, getActionSequence(node));
            }
            if (explored.contains(node.state)) continue;
            explored.add(node.state);
            nodesExpanded++;
            for (String op : problem.operators()) {
                Object s2 = problem.apply(node.state, op);
                if (s2 == null) continue;
                // Redundant state elimination
                if (explored.contains(s2) || inFrontier.contains(s2)) continue;
                Node child = new Node(s2, node, op, problem.stepCost(node.state, op));
                frontier.add(child);
                inFrontier.add(s2);
            }
        }
        return new Result(null, nodesExpanded, null);
    }

    private static Result aStar(Problem problem) {
        return aStar(problem, 1);
    }

    public static Result aStar(Problem problem, int heuristicChoice) {
        if (!(problem instanceof code.delivery.DeliveryProblem)) return new Result(null, 0, null);
        code.delivery.DeliveryProblem dp = (code.delivery.DeliveryProblem) problem;
        // A* with admissible heuristics: f(n) = g(n) + h(n)
        // Heuristic 1: tunnelAware, Heuristic 2: deliveryAdmissible (both admissible)
        Comparator<Node> cmp = Comparator.comparingInt(n -> n.pathCost + 
            (heuristicChoice == 2 ? Heuristics.deliveryAdmissible(dp, n.state) : Heuristics.tunnelAware(dp, n.state))
        );
        PriorityQueue<Node> frontier = new PriorityQueue<>(cmp);
        Map<Object, Integer> best = new HashMap<>();  // Track best path cost to each state
        frontier.add(new Node(problem.initialState()));
        int nodesExpanded = 0;
        while (!frontier.isEmpty()) {
            Node node = frontier.poll();
            if (problem.goalTest(node.state)) {
                return new Result(node, nodesExpanded, getActionSequence(node));
            }
            Integer prev = best.get(node.state);
            // Redundant state elimination: skip if we've found a better path to this state
            if (prev != null && prev <= node.pathCost) continue;
            best.put(node.state, node.pathCost);
            nodesExpanded++;
            for (String op : problem.operators()) {
                Object s2 = problem.apply(node.state, op);
                if (s2 == null) continue;
                Node child = new Node(s2, node, op, problem.stepCost(node.state, op));
                // Only add if this is a better path than previously found
                Integer prevChild = best.get(s2);
                if (prevChild == null || child.pathCost < prevChild) {
                    frontier.add(child);
                }
            }
        }
        return new Result(null, nodesExpanded, null);
    }

    private static Result ids(Problem problem, int maxDepth) {
        int totalExpanded = 0;
        for (int depth = 0; depth <= maxDepth; depth++) {
            Result r = depthLimitedSearch(problem, depth);
            totalExpanded += r.nodesExpanded;
            if (r.node != null) {
                return new Result(r.node, totalExpanded, r.actions);
            }
        }
        return new Result(null, totalExpanded, null);
    }

    private static Result depthLimitedSearch(Problem problem, int limit) {
        Deque<Node> frontier = new ArrayDeque<>();
        Set<Object> explored = new HashSet<>();
        Set<Object> inFrontier = new HashSet<>();
        Node initial = new Node(problem.initialState());
        frontier.addFirst(initial);
        inFrontier.add(initial.state);
        int nodesExpanded = 0;
        while (!frontier.isEmpty()) {
            Node node = frontier.removeFirst();
            inFrontier.remove(node.state);
            if (problem.goalTest(node.state)) {
                return new Result(node, nodesExpanded, getActionSequence(node));
            }
            if (node.depth >= limit) continue;
            if (explored.contains(node.state)) continue;
            explored.add(node.state);
            nodesExpanded++;
            List<String> ops = problem.operators();
            for (int i = ops.size() - 1; i >= 0; i--) {
                String op = ops.get(i);
                Object s2 = problem.apply(node.state, op);
                if (s2 == null) continue;
                // Redundant state elimination
                if (explored.contains(s2) || inFrontier.contains(s2)) continue;
                Node child = new Node(s2, node, op, problem.stepCost(node.state, op));
                frontier.addFirst(child);
                inFrontier.add(s2);
            }
        }
        return new Result(null, nodesExpanded, null);
    }
}
