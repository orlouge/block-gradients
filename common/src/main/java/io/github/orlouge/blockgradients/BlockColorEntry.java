package io.github.orlouge.blockgradients;

import io.github.orlouge.blockgradients.mixin.SpriteContentsAccessor;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class BlockColorEntry {
    public int id = -1;
    private final Vec3 dominant, average, stddev, median;
    public final boolean hasDominant;
    private final Map<Block, Set<Direction>> blocks =
            new TreeMap<>(Comparator.comparing(block -> block.getName().getString().length()));
    private final List<SpriteContents> sprites;
    private final List<TextureAtlasSprite> atlasSprites;

    public BlockColorEntry(Block block, TextureAtlasSprite atlasSprite, Direction direction) {
        this.blocks.put(block, direction != null ? new TreeSet<>(List.of(direction)) : null);
        SpriteContents spriteContents = atlasSprite.contents();
        this.sprites = new ArrayList<>(List.of(spriteContents));
        this.atlasSprites = new ArrayList<>(List.of(atlasSprite));
        NativeImage image = ((SpriteContentsAccessor) spriteContents).getMipmapLevelsImages()[0];
        int width = image.getWidth(), height = image.getHeight();
        int[] histoR = new int[256], histoG = new int[256], histoB = new int[256];

        int avgR = 0, avgG = 0, avgB = 0, medianR = 127, medianG = 127, medianB = 127;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = image.getPixel(x, y);
                int r = ARGB.red(pixel), g = ARGB.green(pixel), b = ARGB.blue(pixel);
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


        this.average = new Vec3(
                (double) avgR / ((double) (width * height * 255)),
                (double) avgG / ((double) (width * height * 255)),
                (double) avgB / ((double) (width * height * 255))
        );
        this.median = new Vec3(medianR / 255d, medianG / 255d, medianB / 255d);

        int stdR = 0, stdG = 0, stdB = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = image.getPixel(x, y);
                int r = ARGB.red(pixel), g = ARGB.green(pixel), b = ARGB.blue(pixel);
                int rd = (r - medianR) * (r - medianR), gd = (g - medianG) * (g - medianG), bd = (b - medianB) * (b - medianB);
                stdR += rd;
                stdG += gd;
                stdB += bd;
            }
        }

        this.stddev = new Vec3(
                Math.sqrt((double) stdR / ((double) (width * height))) / 255d,
                Math.sqrt((double) stdG / ((double) (width * height))) / 255d,
                Math.sqrt((double) stdB / ((double) (width * height))) / 255d
        );

        if (this.median.length() == 0 || this.average.length() == 0) this.hasDominant = true;
        else this.hasDominant = this.stddev.length() < 0.25 && this.median.dot(this.average) / (this.median.length() * this.average.length()) > 0.99;

        this.dominant = this.average.add(this.median).scale(0.5);
    }

    public Vec3 averageColor() {
        return average;
    }

    public Vec3 medianColor() {
        return median;
    }

    public Vec3 dominantColor() {
        return dominant;
    }

    public Vec3 dominantFeatures() {
        return features(dominantColor());
    }

    public Vec3 averageFeatures() {
        return features(averageColor());
    }

    public static Double dominantColor(BlockColorEntry entry, int dimension) {
        return switch (dimension) {
            case 0 -> entry.dominant.x();
            case 1 -> entry.dominant.y();
            default -> entry.dominant.z();
        };
    }

    public static Double averageColor(BlockColorEntry entry, int dimension) {
        return switch (dimension) {
            case 0 -> entry.average.x();
            case 1 -> entry.average.y();
            default -> entry.average.z();
        };
    }

    public Double dominantSquaredDistance(BlockColorEntry blockColorEntry) {
        return this.dominantColor().distanceToSqr(blockColorEntry.dominantColor());
    }

    public Double averageSquaredDistance(BlockColorEntry blockColorEntry) {
        return this.averageColor().distanceToSqr(blockColorEntry.averageColor());
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

    private static Vec3 features(Vec3 color) {
        return new Vec3(color.x, color.y, color.z);
    }

    public Map<Block, Set<Direction>> getBlocks() {
        return this.blocks;
    }

    public TextureAtlasSprite getAtlasSprite(int offset) {
        return atlasSprites.get(offset % atlasSprites.size());
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
                this.atlasSprites.addAll(other.atlasSprites);
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
        return this.blocks.keySet().stream().findFirst().map(Block::getName).orElse(Component.empty()).getString();
    }
}
