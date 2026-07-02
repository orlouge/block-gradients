package io.github.orlouge.blockgradients;

import io.github.orlouge.blockgradients.mixin.SpriteContentsAccessor;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.InfestedBlock;
import net.minecraft.world.level.block.GameMasterBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.RenderShape;

import java.util.*;
import java.util.regex.Matcher;

public class BlockColorEntries {
    private static ArrayList<BlockColorEntry> entries = null;
    private static final Map<Block, BlockColorEntry> entryMap = new HashMap<>();

    public static List<BlockColorEntry> getEntries() {
        if (entries == null) {
            Set<Map.Entry<ResourceKey<Block>, Block>> blockRegistry = BuiltInRegistries.BLOCK.entrySet();
            List<BlockColorEntry> xSortedEntries =
                    blockRegistry.stream()
                    .flatMap(entry -> getBlockColor(entry.getValue()).stream())
                    .sorted(Comparator.comparing(entry -> entry.averageColor().x()))
                    .toList();
            List<BlockColorEntry> ySortedEntries = new ArrayList<>();
            for (BlockColorEntry entry : xSortedEntries) {
                if (ySortedEntries.size() == 0 || !ySortedEntries.get(ySortedEntries.size() - 1).tryMerge(entry)) {
                    ySortedEntries.add(entry);
                }
            }
            ySortedEntries.sort(Comparator.comparing(entry -> entry.averageColor().y()));
            List<BlockColorEntry> zSortedEntries = new ArrayList<>();
            for (BlockColorEntry entry : ySortedEntries) {
                if (zSortedEntries.size() == 0 || !zSortedEntries.get(zSortedEntries.size() - 1).tryMerge(entry)) {
                    zSortedEntries.add(entry);
                }
            }
            zSortedEntries.sort(Comparator.comparing(entry -> entry.averageColor().z()));
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
        BlockState state = block.defaultBlockState();
        Matcher blacklistMatcher = Config.BLACKLIST_PATTERN.matcher(
                BuiltInRegistries.BLOCK.getKey(block).toString()
        );
        if (
                state.getRenderShape() != RenderShape.MODEL ||
                state.useShapeForLightOcclusion() ||
                !state.isSolidRender() ||
                block instanceof FenceBlock ||
                block instanceof FenceGateBlock ||
                block instanceof InfestedBlock ||
                block instanceof WallBlock ||
                block instanceof AnvilBlock ||
                block instanceof DiodeBlock ||
                block instanceof GameMasterBlock ||
                blacklistMatcher.matches()
        ) {
            return List.of();
        }

        Minecraft client = Minecraft.getInstance();
        ModelManager modelManager = client.getModelManager();
        HashMap<TextureAtlasSprite, Direction> sprites = new HashMap<>();
        ArrayList<BlockColorEntry> blockColors = new ArrayList<>();

        for (Direction direction : Direction.values()) {
            if (state.skipRendering(state, direction)) {
                continue;
            }
            BlockStateModel model = modelManager.getBlockStateModelSet().get(state);
            List<BlockStateModelPart> parts = new ArrayList<>();
            model.collectParts(client.level.getRandom(), parts);
            List<BakedQuad> quads = parts.stream().flatMap(part -> part.getQuads(direction).stream()).toList();
            if (quads.size() == 1) {
                for (BakedQuad quad : quads) {
                    TextureAtlasSprite sprite = quad.materialInfo().sprite();
                    if (sprite.contents().width() < 16 || sprite.contents().height() < 16) continue;
                    if (sprites.containsKey(sprite)) {
                        sprites.put(sprite, direction);
                    } else {
                        sprites.put(sprite, null);
                    }
                }
            }
        }

        for (Map.Entry<TextureAtlasSprite, Direction> spriteDir : sprites.entrySet()) {
            SpriteContents contents = spriteDir.getKey().contents();
            NativeImage image = ((SpriteContentsAccessor) contents).getMipmapLevelsImages()[0];
            if (image.format() == NativeImage.Format.RGBA) {
                blockColors.add(new BlockColorEntry(block, spriteDir.getKey(), sprites.size() == 1 ? null : spriteDir.getValue()));
            }
        }

        return blockColors;
    }
}
