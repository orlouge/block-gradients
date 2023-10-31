package io.github.orlouge.blockgradients;

import io.github.orlouge.blockgradients.mixin.SpriteContentsAccessor;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.SpriteContents;
import net.minecraft.text.Text;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.*;

public class BlockColorEntry {
    public int id = -1;
    private final Vec3d dominant, average, stddev, median;
    public final boolean hasDominant;
    private final Map<Block, Set<Direction>> blocks =
            new TreeMap<>(Comparator.comparing(block -> block.getName().getString().length()));
    private final List<AbstractTexture> textures;
    private final List<SpriteContents> sprites;

    public BlockColorEntry(Block block, SpriteContents spriteContents, Direction direction) {
        this.blocks.put(block, direction != null ? new TreeSet<>(List.of(direction)) : null);
        this.sprites = new ArrayList<>(List.of(spriteContents));
        NativeImage image = ((SpriteContentsAccessor) spriteContents).getMipmapLevelsImages()[0];
        this.textures = new ArrayList<>(List.of(new NativeImageBackedTexture(image)));
        int width = image.getWidth(), height = image.getHeight();
        int[] histoR = new int[256], histoG = new int[256], histoB = new int[256];

        int avgR = 0, avgG = 0, avgB = 0, medianR = 127, medianG = 127, medianB = 127;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int r = image.getRed(x, y), g = image.getGreen(x, y), b = image.getBlue(x, y);
                r = r >= 0 ? r : 256 + r;
                g = g >= 0 ? g : 256 + g;
                b = b >= 0 ? b : 256 + b;
                avgR += r;
                avgG += g;
                avgB += b;
                histoR[r] += 1;
                histoG[g] += 1;
                histoB[b] += 1;
            }
        }

        int k = (width * height) / 2, total = 0;
        for (int i = 0; i < 256; i++) {
            total += histoR[i];
            if (total > k) {
                medianR = i;
                break;
            }
        }
        total = 0;
        for (int i = 0; i < 256; i++) {
            total += histoG[i];
            if (total > k) {
                medianG = i;
                break;
            }
        }
        total = 0;
        for (int i = 0; i < 256; i++) {
            total += histoB[i];
            if (total > k) {
                medianB = i;
                break;
            }
        }


        this.average = new Vec3d(
                (double) avgR / ((double) (width * height * 255)),
                (double) avgG / ((double) (width * height * 255)),
                (double) avgB / ((double) (width * height * 255))
        );
        this.median = new Vec3d(medianR / 255d, medianG / 255d, medianB / 255d);

        int stdR = 0, stdG = 0, stdB = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int r = image.getRed(x, y), g = image.getGreen(x, y), b = image.getBlue(x, y);
                r = r >= 0 ? r : 256 + r;
                g = g >= 0 ? g : 256 + g;
                b = b >= 0 ? b : 256 + b;
                int rd = (r - medianR) * (r - medianR), gd = (g - medianG) * (g - medianG), bd = (b - medianB) * (b - medianB);
                stdR += rd;
                stdG += gd;
                stdB += bd;
            }
        }

        this.stddev = new Vec3d(
                Math.sqrt((double) stdR / ((double) (width * height))) / 255d,
                Math.sqrt((double) stdG / ((double) (width * height))) / 255d,
                Math.sqrt((double) stdB / ((double) (width * height))) / 255d
        );

        if (this.median.length() == 0 || this.average.length() == 0) this.hasDominant = true;
        else this.hasDominant = this.stddev.length() < 0.25 && this.median.dotProduct(this.average) / (this.median.length() * this.average.length()) > 0.99;

        this.dominant = this.average.add(this.median).multiply(0.5);
    }

    public Vec3d averageColor() {
        return average;
    }

    public Vec3d medianColor() {
        return median;
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
        return new Vec3d(color.x, color.y, color.z);
    }

    public Map<Block, Set<Direction>> getBlocks() {
        return this.blocks;
    }

    public AbstractTexture getTexture(int offset) {
        return textures.get(offset % textures.size());
    }

    public SpriteContents getSprite(int offset) {
        return sprites.get(offset % sprites.size());
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
                this.sprites.addAll(other.sprites);
                this.textures.addAll(other.textures);
            }
            return true;
        }
    }

    public boolean isSimilar(BlockColorEntry other) {
        return other.averageColor().distanceTo(this.averageColor()) < 0.015 && other.medianColor().distanceTo(this.medianColor()) < 0.03 && other.stddev.distanceTo(this.stddev) < 0.04;
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
