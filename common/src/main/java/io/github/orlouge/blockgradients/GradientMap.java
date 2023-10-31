package io.github.orlouge.blockgradients;

import net.minecraft.util.math.Vec3d;

import java.util.*;

public class GradientMap {
    public static final double MAX_DIFF = 0.15;
    private static KDTree<BlockColorEntry> dominantNeighborTree = null, averageNeighborTree = null;
    private static Map<BlockColorEntry, Collection<BlockColorEntry>> dominantNeighborMap = null, averageNeighborMap = null;

    private final List<DAG.Node<BlockColorEntry>> nodes, sortedNodes;
    private final DAG.Node<BlockColorEntry> source, dest;
    private final int maxId;
    private final List<List<DAG.Node<BlockColorEntry>>> paths = new ArrayList<>();
    private boolean finished = false;
    private Vec3d lastMiddleColor = new Vec3d(-1, -1, -1);


    public GradientMap(List<DAG.Node<BlockColorEntry>> nodes, List<DAG.Node<BlockColorEntry>> sortedNodes, DAG.Node<BlockColorEntry> source, DAG.Node<BlockColorEntry> dest, int maxId, Vec3d initialMiddleColor) {
        this.nodes = nodes;
        this.sortedNodes = sortedNodes;
        this.source = source;
        this.dest = dest;
        this.maxId = maxId;
        this.lastMiddleColor = initialMiddleColor;
    }

    public static GradientMap build(List<BlockColorEntry> entries, BlockColorEntry source, BlockColorEntry dest, boolean dominant) {
        List<DAG.Node<BlockColorEntry>> nodes = entries.stream().map(e -> new DAG.Node<>(e.id, e)).toList();
        if (!DAG.isDAG(nodes, entries.size() - 1)) {
            return new GradientMap(Collections.emptyList(), Collections.emptyList(), null, null, 0, new Vec3d(-1, -1, -1));
        }
        DAG.Node<BlockColorEntry> sourceNode = nodes.get(source.id);
        DAG.Node<BlockColorEntry> destNode = nodes.get(dest.id);
        Vec3d destColor = dominant ? dest.dominantFeatures() : dest.averageFeatures();
        Vec3d sourceColor = dominant ? source.dominantFeatures() : source.averageFeatures();
        for (BlockColorEntry entry : entries) {
            if (dominant && !entry.hasDominant) continue;
            Vec3d entryColor = dominant ? entry.dominantFeatures() : entry.averageFeatures();

            Collection<BlockColorEntry> neighbors = getNeighborMap(entries, dominant).get(entry);

            for (BlockColorEntry neighbor : neighbors) {
                if (neighbor == entry) continue;
                Vec3d neighborColor = dominant ? neighbor.dominantFeatures() : neighbor.averageFeatures();

                Vec3d sourceToDest = destColor.subtract(sourceColor);
                Vec3d entryToNeighbor = neighborColor.subtract(entryColor);
                double cosine = sourceToDest.dotProduct(entryToNeighbor) / (sourceToDest.length() * entryToNeighbor.length() + 1e-10);
                double distance = entryColor.distanceTo(neighborColor);
                if (cosine > 0.0001 && distance > 0.04) {
                    double weight = 0.1 * (Math.exp(distance * 30) - 1);
                    weight += 0.1 * (Math.exp(15 * (1 - cosine)) - 1);
                    nodes.get(entry.id).edges.put(neighbor, new DAG.Edge<>(nodes.get(neighbor.id), weight));
                }
            }
        }
        sourceNode.edges.remove(dest);
        List<DAG.Node<BlockColorEntry>> sortedNodes = DAG.toposort(nodes, entries.size() - 1);
        Vec3d initialColor = sourceColor.add(destColor).multiply(-0.5).add(new Vec3d(0.5, 0.5, 0.5));
        return new GradientMap(
                nodes, sortedNodes, sourceNode, destNode, entries.size() - 1,
                new Vec3d(initialColor.getX() % 1, initialColor.getY() % 1, initialColor.getZ() % 1)
                );
    }

    public Collection<Cell> getRow(int y) {
        if (y < 0) return Collections.emptyList();
        while (!finished && y >= paths.size()) {
            generatePath();
        }
        if (y >= paths.size()) {
            return Collections.emptyList();
        }
        List<DAG.Node<BlockColorEntry>> path = this.paths.get(y);
        LinkedList<Cell> cells = new LinkedList<>();
        cells.add(new Cell(this.source.element, 0, y));
        for (int x = 1; x < path.size() + 1; x++) {
            cells.add(new Cell(path.get(x - 1).element, x, y));
        }
        cells.add(new Cell(this.dest.element, path.size() + 1, y));
        return cells;
    }

    private void generatePath() {
        if (source == null || dest == null) {
            finished = true;
            return;
        }
        if (finished) return;
        DAG.Node<BlockColorEntry> sourceNode = nodes.get(this.source.id);
        List<DAG.Node<BlockColorEntry>> path;
        path = DAG.shortestPath(
            this.sortedNodes, this.source.id, this.dest.id, this.maxId,
            (node, edge) -> edge.weight * Math.max(1, Math.max(
                Math.exp(10 * (0.15 - node.element.averageColor().distanceTo(lastMiddleColor))),
                Math.exp(10 * (0.15 - edge.dest.element.averageColor().distanceTo(lastMiddleColor)))
            ))
        );
        if (path.size() == 0) {
            finished = true;
            return;
        }
        Vec3d middleColor = path.get(path.size() / 2).element.averageColor();
        if (path.size() % 2 == 0 && path.size() / 2 > 1) {
            middleColor.add(path.get(path.size() / 2 + 1).element.averageColor());
            middleColor.multiply(0.5);
        }
        lastMiddleColor = lastMiddleColor.multiply(0.95).add(middleColor.multiply(0.05));
        DAG.Node<BlockColorEntry> shortestEdgeSourceNode = sourceNode;
        DAG.Edge<BlockColorEntry> shortestEdge = sourceNode.edges.get(path.get(0).element);
        shortestEdge.weight *= 50;
        for (int d = 1; d < 4; d++) {
            for (int i = 0; i < path.size() - d; i++) {
                DAG.Node<BlockColorEntry> node = nodes.get(path.get(i).id);
                DAG.Edge<BlockColorEntry> edge = node.edges.get(path.get(i + d).element);
                if (edge == null) continue;
                if (d == 1 && edge.weight < shortestEdge.weight) {
                    shortestEdge = edge;
                    shortestEdgeSourceNode = node;
                }
                edge.weight *= 1 + 50d / d;
            }
        }
        DAG.Edge<BlockColorEntry> lastEdge = nodes.get(path.get(path.size() - 1).id).edges.get(dest.element);
        if (lastEdge != null) {
            lastEdge.weight *= 50;
            if (lastEdge.weight < shortestEdge.weight) {
                shortestEdge = lastEdge;
                shortestEdgeSourceNode = nodes.get(path.size() - 1);
            }
        }
        shortestEdgeSourceNode.edges.remove(shortestEdge.dest.element);
        this.paths.add(path);
    }

    private static KDTree<BlockColorEntry> getNeighborTree(List<BlockColorEntry> entries, boolean dominant) {
        if (dominant) {
            if (dominantNeighborTree == null) {
                dominantNeighborTree = new KDTree<>(entries.stream().filter(e -> e.hasDominant).toList(), 3, BlockColorEntry::dominantColor);
            }
            return dominantNeighborTree;
        } else {
            if (averageNeighborTree == null) {
                averageNeighborTree = new KDTree<>(entries, 3, BlockColorEntry::averageColor);
            }
            return averageNeighborTree;
        }
    }

    private static Map<BlockColorEntry, Collection<BlockColorEntry>> getNeighborMap(List<BlockColorEntry> entries, boolean dominant) {
        KDTree<BlockColorEntry> neighborTree = getNeighborTree(entries, dominant);
        if (dominant) {
            if (dominantNeighborMap == null) {
                dominantNeighborMap = new HashMap<>();
                for (BlockColorEntry entry : entries) {
                    if (!entry.hasDominant) continue;
                    Vec3d entryColor = entry.dominantFeatures();

                    double maxDiff = MAX_DIFF * 1.2;
                    Vec3d min = entryColor.subtract(maxDiff, maxDiff, maxDiff), max = entryColor.add(maxDiff, maxDiff, maxDiff);
                    Collection<BlockColorEntry> neighbors = neighborTree.rangeSearch(
                            entry,
                            new double[]{min.getX(), min.getY(), min.getZ()},
                            new double[]{max.getX(), max.getY(), max.getZ()},
                            3,
                            BlockColorEntry::dominantColor
                    );
                    dominantNeighborMap.put(entry, neighbors);
                }
            }
            return dominantNeighborMap;
        } else {
            if (averageNeighborMap == null) {
                averageNeighborMap = new HashMap<>();
                for (BlockColorEntry entry : entries) {
                    Vec3d entryColor = entry.dominantFeatures();
                    Vec3d min = entryColor.subtract(MAX_DIFF, MAX_DIFF, MAX_DIFF), max = entryColor.add(MAX_DIFF, MAX_DIFF, MAX_DIFF);
                    Collection<BlockColorEntry> neighbors = neighborTree.rangeSearch(
                            entry,
                            new double[]{min.getX(), min.getY(), min.getZ()},
                            new double[]{max.getX(), max.getY(), max.getZ()},
                            3,
                            BlockColorEntry::averageColor
                    );
                    averageNeighborMap.put(entry, neighbors);
                }
            }
            return averageNeighborMap;
        }
    }

    public static class Cell {
        public final BlockColorEntry entry;
        public final int cellX, cellY;

        public Cell(BlockColorEntry entry, int cellX, int cellY) {
            this.entry = entry;
            this.cellX = cellX;
            this.cellY = cellY;
        }
    }
}
