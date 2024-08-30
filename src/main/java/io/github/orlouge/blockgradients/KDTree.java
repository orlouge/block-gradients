package io.github.orlouge.blockgradients;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.IntStream;

public class KDTree<T> {
    private final Node root;

    private KDTree(Node root) {
        this.root = root;
    }

    public KDTree(Collection<T> elements, int dimensions, BiFunction<T, Integer, Double> component) {
        this.root = buildNode(elements, 0, dimensions, component);
    }

    public KDTree<T> delete(T element, int dimensions, BiFunction<T, Integer, Double> component) {
        if (root == null) return null;
        return new KDTree<T>(root.delete(element, 0, dimensions, component));
    }

    public T nearest(T element, int dimensions, BiFunction<T, Integer, Double> component, BiFunction<T, T, Double> squaredDistance) {
        if (root == null) return null;
        return root.nearest(element, root.element, 0, dimensions, component, squaredDistance);
    }

    public Collection<T> kNearest(T element, int k, int dimensions, BiFunction<T, Integer, Double> component, BiFunction<T, T, Double> squaredDistance, Predicate<T> filter) {
        if (root == null) return Collections.emptyList();
        Comparator<T> comparator = Comparator.comparing(el -> squaredDistance.apply(el, element));
        PriorityQueue<T> queue = new PriorityQueue<>(k, comparator.reversed());
        root.kNearest(element, k, queue, 0, dimensions, component, squaredDistance, filter);
        LinkedList<T> sortedNeighbors = new LinkedList<>();
        queue.forEach(sortedNeighbors::push);
        return sortedNeighbors;
    }

    public Collection<T> rangeSearch(T element, double[] min, double[] max, int dimensions, BiFunction<T, Integer, Double> component) {
        List<T> result = new LinkedList<>();
        root.rangeSearch(result, min, max, 0, dimensions, component);
        return result;
    }

    private Node buildNode(Collection<T> elements, int dimension, int dimensions, BiFunction<T, Integer, Double> component) {
        List<T> sorted = elements.stream().sorted(Comparator.comparing(el -> component.apply(el, dimension))).toList();
        int middle = sorted.size() >> 1;
        int newDimension = (dimension + 1) % dimensions;
        return new Node(
                sorted.get(middle),
                middle > 0 ? buildNode(sorted.subList(0, middle), newDimension, dimensions, component) : null,
                middle < sorted.size() - 1 ? buildNode(sorted.subList(middle + 1, sorted.size()), newDimension, dimensions, component) : null
        );
    }

    private class Node {
        private final T element;
        private final Node left, right;

        public Node(T element, Node left, Node right) {
            this.element = element;
            this.left = left;
            this.right = right;
        }

        public Node delete(T deleted, int dimension, int dimensions, BiFunction<T, Integer, Double> component) {
            int subDimension = (dimension + 1) % dimensions;
            if (deleted == element) {
                if (right != null) {
                    T min = right.minimum(dimension, subDimension, dimensions, component);
                    return new Node(min, left, right.delete(min, subDimension, dimensions, component));
                } else if (left != null) {
                    T min = left.minimum(dimension, subDimension, dimensions, component);
                    return new Node(min, null, left.delete(min, subDimension, dimensions, component));
                } else {
                    return null;
                }
            } else {
                if (component.apply(deleted, dimension) < component.apply(element, dimension)) {
                    return new Node(element, left == null ? null : left.delete(element, subDimension, dimensions, component), right);
                } else {
                    return new Node(element, left, right == null ? null : right.delete(element, subDimension, dimensions, component));
                }
            }
        }

        public T nearest(T compared, T candidate, int dimension, int dimensions, BiFunction<T, Integer, Double> component, BiFunction<T, T, Double> squaredDistance) {
            if (squaredDistance.apply(element, compared) < squaredDistance.apply(candidate, compared)) candidate = element;
            double lineDist = component.apply(compared, dimension) - component.apply(element, dimension);
            Node sub1 = lineDist < 0 ? left : right, sub2 = lineDist < 0 ? right : left;
            candidate = sub1.nearest(compared, candidate, (dimension + 1) % dimensions, dimensions, component, squaredDistance);
            if (squaredDistance.apply(candidate, compared) >= lineDist * lineDist) {
                candidate = sub2.nearest(compared, candidate, (dimension + 1) % dimensions, dimensions, component, squaredDistance);
            }
            return candidate;
        }

        public void kNearest(T compared, int k, PriorityQueue<T> candidates, int dimension, int dimensions, BiFunction<T, Integer, Double> component, BiFunction<T, T, Double> squaredDistance, Predicate<T> filter) {
            if (filter.test(element)) {
                if (candidates.size() < k) {
                    candidates.add(element);
                } else if (squaredDistance.apply(element, compared) < squaredDistance.apply(candidates.peek(), compared)) {
                    candidates.poll();
                    candidates.add(element);
                }
            }
            double lineDist = component.apply(compared, dimension) - component.apply(element, dimension);
            Node sub1 = lineDist < 0 ? left : right, sub2 = lineDist < 0 ? right : left;
            if (sub1 != null) {
                sub1.kNearest(compared, k, candidates, (dimension + 1) % dimensions, dimensions, component, squaredDistance, filter);
            }
            if (sub2 != null && (sub1 == null || candidates.size() < k || squaredDistance.apply(candidates.peek(), compared) >= lineDist * lineDist)) {
                sub2.kNearest(compared, k, candidates, (dimension + 1) % dimensions, dimensions, component, squaredDistance, filter);
            }
        }

        public void rangeSearch(List<T> found, double[] min, double[] max, int dimension, int dimensions, BiFunction<T, Integer, Double> component) {
            if (IntStream.range(0, min.length).allMatch(dim -> component.apply(element, dim) >= min[dim] && component.apply(element, dim) <= max[dim])) {
                found.add(element);
            }
            double line = component.apply(element, dimension);
            if (left != null && min[dimension] <= line) {
                left.rangeSearch(found, min, max, (dimension + 1) % dimensions, dimensions, component);
            }
            if (right != null && max[dimension] >= line) {
                right.rangeSearch(found, min, max, (dimension + 1) % dimensions, dimensions, component);
            }
        }

        public T minimum(int comparedDimension, int dimension, int dimensions, BiFunction<T, Integer, Double> component) {
            if (comparedDimension == dimension) {
                return left == null ? element : left.minimum(comparedDimension, (dimension + 1) % dimensions, dimensions, component);
            } else {
                T leftMin = left.minimum(comparedDimension, (dimension + 1) % dimensions, dimensions, component);
                T rightMin = right.minimum(comparedDimension, (dimension + 1) % dimensions, dimensions, component);
                T min = element;
                double minComponent = component.apply(min, comparedDimension);
                double leftComponent = component.apply(leftMin, comparedDimension);
                if (leftComponent < minComponent) {
                    minComponent = leftComponent;
                    min = leftMin;
                }
                if (component.apply(rightMin, comparedDimension) < minComponent) {
                    min = rightMin;
                }
                return min;
            }
        }
    }
}
