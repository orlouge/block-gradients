package io.github.orlouge.blockgradients;

import io.github.orlouge.blockgradients.mixin.SpriteContentsAccessor;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedModelManager;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.Sprite;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.Direction;

import java.util.*;
import java.util.regex.Matcher;

public class BlockColorEntries {
    private static ArrayList<BlockColorEntry> entries = null;
    private static final Map<Block, BlockColorEntry> entryMap = new HashMap<>();

    public static List<BlockColorEntry> getEntries() {
        if (entries == null) {
            Set<Map.Entry<RegistryKey<Block>, Block>> blockRegistry = Registries.BLOCK.getEntrySet();
            List<BlockColorEntry> xSortedEntries =
                    blockRegistry.stream()
                    .flatMap(entry -> getBlockColor(entry.getValue()).stream())
                    .sorted(Comparator.comparing(entry -> entry.averageColor().getX()))
                    .toList();
            List<BlockColorEntry> ySortedEntries = new ArrayList<>();
            for (BlockColorEntry entry : xSortedEntries) {
                if (ySortedEntries.size() == 0 || !ySortedEntries.get(ySortedEntries.size() - 1).tryMerge(entry)) {
                    ySortedEntries.add(entry);
                }
            }
            ySortedEntries.sort(Comparator.comparing(entry -> entry.averageColor().getY()));
            List<BlockColorEntry> zSortedEntries = new ArrayList<>();
            for (BlockColorEntry entry : ySortedEntries) {
                if (zSortedEntries.size() == 0 || !zSortedEntries.get(zSortedEntries.size() - 1).tryMerge(entry)) {
                    zSortedEntries.add(entry);
                }
            }
            zSortedEntries.sort(Comparator.comparing(entry -> entry.averageColor().getZ()));
            entries = new ArrayList<>();
            int id = 0;
            for (BlockColorEntry entry : zSortedEntries) {
                if (entries.size() == 0 || !entries.get(entries.size() - 1).tryMerge(entry)) {
                    entry.id = id;
                    entries.add(entry);
                    id++;
                }
            }
            for (BlockColorEntry entry : entries) {
                for (Block block : entry.getBlocks().keySet()) {
                    entryMap.put(block, entry);
                }
            }
        }

        return entries;
    }

    public static BlockColorEntry getEntry(int id) {
        if (entries == null) getEntries();
        return entries.get(id);
    }

    public static BlockColorEntry getEntry(Block block) {
        if (entries == null) getEntries();
        return entryMap.get(block);
    }

    private static List<BlockColorEntry> getBlockColor(Block block) {
        BlockState state = block.getDefaultState();
        Matcher blacklistMatcher = Config.BLACKLIST_PATTERN.matcher(
                Registries.BLOCK.getId(block).toString()
        );
        if (
                state.getRenderType() != BlockRenderType.MODEL ||
                state.hasSidedTransparency() ||
                !state.isOpaque() ||
                block instanceof FenceBlock ||
                block instanceof FenceGateBlock ||
                block instanceof InfestedBlock ||
                block instanceof WallBlock ||
                block instanceof AnvilBlock ||
                block instanceof AbstractRedstoneGateBlock ||
                block instanceof OperatorBlock ||
                blacklistMatcher.matches()
        ) {
            /*
            if (state.getRenderType() != BlockRenderType.MODEL) System.out.println(block.getName() + " not a model");
            else if (state.hasSidedTransparency() || !state.isOpaque()) System.out.println(block.getName() + " not opaque");
            else if (blacklistMatcher.matches()) System.out.println(block.getName() + " blacklisted identifier");
            else System.out.println(block.getName() + " blacklisted class");
            */
            return List.of();
        }

        MinecraftClient client = MinecraftClient.getInstance();
        BakedModelManager modelManager = client.getBakedModelManager();
        HashMap<Sprite, Direction> sprites = new HashMap<>();
        ArrayList<BlockColorEntry> blockColors = new ArrayList<>();

        for (Direction direction : Direction.values()) {
            if (state.isSideInvisible(state, direction)) {
                continue;
            }
            BakedModel model = modelManager.getBlockModels().getModel(state);
            List<BakedQuad> quads = model.getQuads(state, direction, client.world.getRandom());
            if (quads.size() == 1) {
                for (BakedQuad quad : quads) {
                    Sprite sprite = quad.getSprite();
                    if (sprite.getContents().getWidth() < 16 || sprite.getContents().getHeight() < 16) continue;
                    if (sprites.containsKey(sprite)) {
                        sprites.put(sprite, direction);
                    } else {
                        sprites.put(sprite, null);
                    }
                }
            }
        }

        for (Map.Entry<Sprite, Direction> spriteDir : sprites.entrySet()) {
            NativeImage image = ((SpriteContentsAccessor) spriteDir.getKey().getContents()).getMipmapLevelsImages()[0];
            //if (image.getFormat() != NativeImage.Format.RGBA) System.out.println(block.getName() + " format " + image.getFormat().name());
            if (image.getFormat() == NativeImage.Format.RGBA) {
                blockColors.add(new BlockColorEntry(block, image, sprites.size() == 1 ? null : spriteDir.getValue()));
            }
        }

        //if (blockColors.size() != 1) System.out.println("Block " + block + " color entries " + blockColors);
        return blockColors;
    }
}
