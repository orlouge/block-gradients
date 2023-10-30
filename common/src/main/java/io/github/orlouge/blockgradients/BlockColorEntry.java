package io.github.orlouge.blockgradients;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.Text;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.*;

public class BlockColorEntry {
    public int id = -1;
    private Vec3d dominant, average, stddev;
    public final boolean hasDominant;
    private final Map<Block, Set<Direction>> blocks =
            new TreeMap<>(Comparator.comparing(block -> block.getName().getString().length()));
    private final List<NativeImageBackedTexture> textures;
    private static final int DOMINANT_PERCENTAGE = 80, DOMINANT_MAXDIFF = 25000;

    public BlockColorEntry(Block block, NativeImage image, Direction direction) {
        this.blocks.put(block, direction != null ? new TreeSet<>(List.of(direction)) : null);
        this.textures = new ArrayList<>(List.of(new NativeImageBackedTexture(image)));
        int width = image.getWidth(), height = image.getHeight(), dominantCount = 0;

        int avgR = 0, avgG = 0, avgB = 0, domR = 0, domG = 0, domB = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int r = image.getRed(x, y), g = image.getGreen(x, y), b = image.getBlue(x, y);
                avgR += r >= 0 ? r : 256 + r;
                avgG += g >= 0 ? g : 256 + g;
                avgB += b >= 0 ? b : 256 + b;
            }
        }

        double averageR = (double) avgR / ((double) (width * height));
        double averageG = (double) avgG / ((double) (width * height));
        double averageB = (double) avgB / ((double) (width * height));
        avgR = (int) averageR;
        avgG = (int) averageG;
        avgB = (int) averageB;
        averageR /= 255d;
        averageG /= 255d;
        averageB /= 255d;
        this.average = new Vec3d(averageR, averageG, averageB);

        int stdR = 0, stdG = 0, stdB = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int r = image.getRed(x, y), g = image.getGreen(x, y), b = image.getBlue(x, y);
                r = r >= 0 ? r : 256 + r;
                g = g >= 0 ? g : 256 + g;
                b = b >= 0 ? b : 256 + b;
                int rd = (r - avgR) * (r - avgR), gd = (g - avgG) * (g - avgG), bd = (b - avgB) * (b - avgB);
                stdR += rd;
                stdG += gd;
                stdB += bd;

                if (rd + gd + bd > DOMINANT_MAXDIFF) {
                    continue;
                }

                domR += r;
                domG += g;
                domB += b;
                dominantCount++;
            }
        }

        this.stddev = new Vec3d(
                Math.sqrt((double) stdR / ((double) (width * height))) / 255d,
                Math.sqrt((double) stdG / ((double) (width * height))) / 255d,
                Math.sqrt((double) stdB / ((double) (width * height))) / 255d
        );

        hasDominant = dominantCount > width * height * DOMINANT_PERCENTAGE / 100;
        this.dominant = new Vec3d(
                ((double) domR / ((double) dominantCount)) / 255d,
                ((double) domG / ((double) dominantCount)) / 255d,
                ((double) domB / ((double) dominantCount)) / 255d
        );

        //System.out.println(block.getName() + " colors: " + averageColor() + (hasDominant ? ", " + dominantColor() : "") + " (" + dominantCount + ")");
    }

    public Vec3d averageColor() {
        return average;
    }

    public Vec3d dominantColor() {
        return dominant;
    }

    public Vec3d dominantFeatures() {
        return features(dominantColor());
    }

    public Vec3d averageFeatures() {
        return features(averageColor());
    }

    public static Double dominantColor(BlockColorEntry entry, int dimension) {
        return switch (dimension) {
            case 0 -> entry.dominant.getX();
            case 1 -> entry.dominant.getY();
            default -> entry.dominant.getZ();
        };
    }

    public static Double averageColor(BlockColorEntry entry, int dimension) {
        return switch (dimension) {
            case 0 -> entry.average.getX();
            case 1 -> entry.average.getY();
            default -> entry.average.getZ();
        };
    }

    public Double dominantSquaredDistance(BlockColorEntry blockColorEntry) {
        return this.dominantColor().squaredDistanceTo(blockColorEntry.dominantColor());
    }

    public Double averageSquaredDistance(BlockColorEntry blockColorEntry) {
        return this.averageColor().squaredDistanceTo(blockColorEntry.averageColor());
    }

    public static Double dominantFeature(BlockColorEntry entry, int dimension) {
        return switch (dimension) {
            case 0 -> entry.dominantFeatures().x;
            case 1 -> entry.dominantFeatures().y;
            default -> entry.dominantFeatures().z;
        };
    }

    public static Double averageFeature(BlockColorEntry entry, int dimension) {
        return switch (dimension) {
            case 0 -> entry.averageFeatures().x;
            case 1 -> entry.averageFeatures().y;
            default -> entry.averageFeatures().z;
        };
    }

    private static Vec3d features(Vec3d color) {
        /*
        double max, min, diff, inc;
        if (color.x > color.y && color.x > color.z) {
            max = color.x;
            min = Math.min(color.y, color.z);
            diff = color.y - color.z;
            inc = 0;
        } else if (color.y > color.x && color.y > color.z) {
            max = color.y;
            min = Math.min(color.x, color.z);
            diff = color.z - color.x;
            inc = 2;
        } else {
            max = color.z;
            min = Math.min(color.x, color.y);
            diff = color.x - color.y;
            inc = 4;
        }
        double l = (max + min) / 2;
        if (max == min) {
            return new Vec3d(0, 0, l);
        } else {
            double h = (inc + diff / (max - min)) / 6;
            double s = l <= 0.5 ? (max - min) / (max + min) : (max - min) / (2 - max - min);
            if (h < 0) h += 1;
            return new Vec3d(h, s, l);
        }
        */
        //double y = (color.x + color.y + color.z) / 3d;
        //double a = (color.z - y) / 2d;
        //double c = (color.x - y) / 2d;
        //double b = (y - 0.5d) / 9d;
        //return new Vec3d(a, b, c);
        return new Vec3d(color.x, color.y, color.z);
    }

    public Map<Block, Set<Direction>> getBlocks() {
        return this.blocks;
    }

    public AbstractTexture getTexture(int offset) {
        return textures.get(offset % textures.size());
    }

    public boolean tryMerge(BlockColorEntry other) {
        if (!this.isSimilar(other)) {
        //if (!this.isIdentical(other)) {
            return false;
        } else {
            for (Block block : other.blocks.keySet()) {
                Set<Direction> dirSet = this.blocks.computeIfAbsent(block, b -> new TreeSet<>());
                Set<Direction> otherDirSet = other.blocks.get(block);
                if (otherDirSet != null) {
                    dirSet.addAll(otherDirSet);
                } else {
                    this.blocks.put(block, null);
                }
                this.textures.addAll(other.textures);
            }
            // System.out.println("Merging " + this + " and " + other + ": " + this.blocks);
            return true;
        }
    }

    public boolean isSimilar(BlockColorEntry other) {
        // return other.averageColor().distanceTo(this.averageColor()) < 0.004 && (this.hasDominant == other.hasDominant) && (!this.hasDominant || other.dominantColor().distanceTo(this.dominantColor()) < 0.004);
        return other.averageColor().distanceTo(this.averageColor()) < 0.02 && other.stddev.distanceTo(this.stddev) < 0.02;
    }

    /*
    public boolean isIdentical(BlockColorEntry other) {
        try {
            return Arrays.equals(this.texture.getImage().getBytes(), other.texture.getImage().getBytes());
        } catch (IOException e) {
            return false;
        }
    }
     */

    @Override
    public String toString() {
        return this.blocks.keySet().stream().findFirst().map(Block::getName).orElse(Text.empty()).asTruncatedString(100);
    }
}
