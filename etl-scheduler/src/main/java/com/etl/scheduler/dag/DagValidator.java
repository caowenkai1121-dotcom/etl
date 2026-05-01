package com.etl.scheduler.dag;

import java.util.*;

public class DagValidator {
    public static boolean hasCycle(List<DagNode> nodes, List<DagEdge> edges) {
        if (nodes == null || nodes.isEmpty()) return false;
        Map<String, List<String>> adj = new HashMap<>();
        for (DagNode n : nodes) {
            if (n == null || n.getCode() == null) continue;
            adj.put(n.getCode(), new ArrayList<>());
        }
        if (edges == null) return false;
        for (DagEdge e : edges) {
            if (adj.containsKey(e.getFrom())) {
                adj.get(e.getFrom()).add(e.getTo());
            }
        }
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();
        for (DagNode node : nodes) {
            if (node == null || node.getCode() == null) continue;
            if (dfs(node.getCode(), adj, visited, recursionStack)) return true;
        }
        return false;
    }

    private static boolean dfs(String node, Map<String, List<String>> adj, Set<String> visited, Set<String> stack) {
        if (stack.contains(node)) return true;
        if (visited.contains(node)) return false;
        visited.add(node);
        stack.add(node);
        for (String neighbor : adj.getOrDefault(node, List.of())) {
            if (dfs(neighbor, adj, visited, stack)) return true;
        }
        stack.remove(node);
        return false;
    }
}
