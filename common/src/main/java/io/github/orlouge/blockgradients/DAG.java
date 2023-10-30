package io.github.orlouge.blockgradients;

import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.IntStream;

public class DAG {
    public static <T> List<Node<T>> toposort(Collection<Node<T>> nodes, int maxId) {
        LinkedList<Node<T>> result = new LinkedList<>();
        boolean[] visited = new boolean[maxId + 1];
        for (Node<T> node : nodes) {
            if (!visited[node.id]) {
                _toposort(node, result, visited);
            }
        }
        return result;
    }

    public static <T> boolean isDAG(Collection<Node<T>> nodes, int maxId) {
        boolean[] visited = new boolean[maxId + 1];
        boolean[] inPath = new boolean[maxId + 1];

        for (Node<T> node : nodes) {
            if (!visited[node.id] && _hasCycles(node, visited, inPath)) {
                System.out.println("Cycle starting from: " + node.element);
                return false;
            }
        }
        return true;
    }

    private static <T> void _toposort(Node<T> node, LinkedList<Node<T>> stack, boolean[] visited) {
        visited[node.id] = true;
        for (Edge<T> edge : node.edges.values()) {
            if (!visited[edge.dest.id]) {
                _toposort(edge.dest, stack, visited);
            }
        }
        stack.push(node);
    }

    private static <T> boolean _hasCycles(Node<T> node, boolean[] visited, boolean[] inPath) {
        if (!visited[node.id]) {
            visited[node.id] = true;
            inPath[node.id] = true;
            for (Edge<T> edge : node.edges.values()) {
                if ((!visited[edge.dest.id] && _hasCycles(edge.dest, visited, inPath)) || inPath[edge.dest.id]) {
                    if (inPath[node.id]) System.out.println(node.element + " -> " + edge.dest.element);
                    return true;
                }
            }
        }
        inPath[node.id] = false;
        return false;
    }

    public static <T> List<Node<T>> shortestPath(List<Node<T>> toposorted, int sourceId, int destId, int maxId) {
        if (toposorted.size() == 0) return Collections.emptyList();
        double[] cost = new double[maxId + 1];
        Arrays.fill(cost, Double.POSITIVE_INFINITY);
        cost[sourceId] = 0;
        Node<T>[] parent = (Node<T>[]) Array.newInstance(toposorted.get(0).getClass(), maxId + 1);
        for (Node<T> node : toposorted) {
            for (Edge<T> edge : node.edges.values()) {
                //if (edge.dest.id == destId) System.out.println(node.element + " -> " + edge.dest.element + " " + cost[edge.dest.id] + " <> " + cost[node.id] + " + " + edge.weight);
                if (cost[edge.dest.id] > cost[node.id] + edge.weight) {
                    cost[edge.dest.id] = cost[node.id] + edge.weight;
                    parent[edge.dest.id] = node;
                }
            }
        }
        if (parent[destId] == null) return Collections.emptyList();
        LinkedList<Node<T>> result = new LinkedList<>();
        Node<T> current = parent[destId];
        String costString = "";
        while (current.id != sourceId) {
            costString += cost[current.id] + " ";
            result.addFirst(current);
            current = parent[current.id];
            if (current == null) return Collections.emptyList();
        }
        //System.out.println("Cost: " + costString);
        return result;
    }

    public static class Node<T> {
        public final int id;
        public final T element;
        public final Map<T, Edge<T>> edges = new HashMap<>();

        public Node(int id, T element) {
            this.id = id;
            this.element = element;
        }

        @Override
        public String toString() {
            return element.toString();
        }
    }

    public static class Edge<T> {
        public final Node<T> dest;
        public double weight;

        public Edge(Node<T> dest, double weight) {
            this.dest = dest;
            this.weight = weight;
        }
    }
}
