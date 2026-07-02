package io.github.orlouge.blockgradients;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.github.orlouge.blockgradients.mixin.SpriteContentsAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class BlockMapScreen extends Screen {
    private final GradientMap averageBlockMap, dominantBlockMap;
    private int offsetX = 0, offsetY = 0, previousOffsetX = 0, previousOffsetY = 0, size = 16, previousSize = -1, maxColumns = 1;
    private boolean resetSize = true, renderAverage = true;
    private float textureOffset = 0;

    public BlockMapScreen(GradientMap averageBlockMap, GradientMap dominantBlockMap) {
        super(Component.literal("BlockMap"));
        this.averageBlockMap = averageBlockMap;
        this.dominantBlockMap = dominantBlockMap;
        if (dominantBlockMap.getRow(0).size() > 0) renderAverage = false;
    }

    public static void openBlockMap(Minecraft mc) {
        ItemStack main = mc.player.getMainHandItem(), off = mc.player.getOffhandItem();
        if (!(main.getItem() instanceof BlockItem destBlock)) {
            mc.player.displayClientMessage(Component.literal(main.getItem().getName(main).getString() + " is not a block"), true);
        } else if (!(off.getItem() instanceof BlockItem sourceBlock)) {
            mc.player.displayClientMessage(Component.literal(off.getItem().getName(off).getString() + " is not a block"), true);
        } else {
            BlockColorEntry source = BlockColorEntries.getEntry(sourceBlock.getBlock());
            BlockColorEntry dest = BlockColorEntries.getEntry(destBlock.getBlock());
            if (source == null) {
                mc.player.displayClientMessage(Component.literal(sourceBlock.getBlock().getName().getString() + " is translucent or blacklisted"), true);
            } else if (dest == null) {
                mc.player.displayClientMessage(Component.literal(destBlock.getBlock().getName().getString() + " is translucent or blacklisted"), true);
            } else {
                GradientMap averageBlockMap = GradientMap.build(BlockColorEntries.getEntries(), source, dest, false);
                GradientMap dominantBlockMap = GradientMap.build(BlockColorEntries.getEntries(), source, dest, true);
                while (KeyBindings.OPEN_BLOCKMAP_KEY_BINDING.get().consumeClick()) ;
                if (mc.screen == null) {
                    mc.setScreen(new BlockMapScreen(averageBlockMap, dominantBlockMap));
                } else if (mc.screen instanceof BlockMapScreen) {
                    ((BlockMapScreen) mc.screen).switchBlockMap();
                }
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        textureOffset += 0.03;
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        if (this.resetSize) {
            if (this.renderAverage) {
                this.setSize(width / (5 + IntStream.range(0, 10).map(y -> averageBlockMap.getRow(y).size()).max().orElse(0)));
            } else {
                this.setSize(width / (5 + IntStream.range(0, 10).map(y -> dominantBlockMap.getRow(y).size()).max().orElse(0)));
            }
            offsetY += 15;
            offsetX += 5;
            this.resetSize = false;
        }
        this.renderBackground(context, mouseX, mouseY, delta);
        this.renderBlockMap(context, this.renderAverage ? averageBlockMap : dominantBlockMap, mouseX, mouseY);
    }

    public void switchBlockMap() {
        int prevX = previousOffsetX, prevY = previousOffsetY, prevSize = this.previousSize;
        this.previousOffsetX = offsetX;
        this.previousOffsetY = offsetY;
        this.offsetX = prevX;
        this.offsetY = prevY;
        this.previousSize = size;
        this.size = prevSize;
        this.resetSize = this.resetSize || (prevSize == -1);
        this.renderAverage = !this.renderAverage;
    }

    private void setSize(int size) {
        this.size = Math.max(1, Math.min(128, size));
    }

    private void renderBlockMap(GuiGraphics context, GradientMap blockMap, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);

        BlockColorEntry selectedEntry = null;
        for (int gy = (-offsetY + height) / size; gy >= -offsetY / size; gy--) {
            for (GradientMap.Cell cell : blockMap.getRow(gy)) {
                int gx = cell.cellX;
                maxColumns = Math.max(gx, maxColumns);
                BlockColorEntry entry = cell.entry;
                if (entry != null) {
                    int x = offsetX + gx * size, y = offsetY + gy * size;
                    if (x > -size && y > -size && x < width && y < height) {
                        int spriteIndex = gy + (int) textureOffset;
                        SpriteContentsAccessor sprite = (SpriteContentsAccessor) entry.getSprite(spriteIndex);
                        NativeImage image = sprite.getMipmapLevelsImages()[0];
                        setShaderTexture(0, entry.getTexture(spriteIndex));
                        float u = 0, v = 0, udiff = 1, vdiff = 1;
                        if (image != null) {
                            udiff = (float) sprite.getWidth() / image.getWidth();
                            vdiff = (float) sprite.getHeight() / image.getHeight();
                        }
                        RenderSystem.setShader(GameRenderer::getPositionTexShader);
                        Matrix4f posMatrix = context.pose().last().pose();
                        BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
                        bufferBuilder.addVertex(posMatrix, x, y, 0).setUv(u, v);
                        bufferBuilder.addVertex(posMatrix, x, y + size, 0).setUv(u, v + vdiff);
                        bufferBuilder.addVertex(posMatrix, x + size, y + size, 0).setUv(u + udiff, v + vdiff);
                        bufferBuilder.addVertex(posMatrix, x + size, y, 0).setUv(u + udiff, v);
                        BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
                        if (mouseX >= x && mouseY >= y && mouseX < x + size && mouseY < y + size) {
                            selectedEntry = entry;
                        }
                    }
                }
            }
        }


        if (renderAverage) {
            context.drawString(this.font, Component.literal("Average color"), 1, 1, ~0);
        } else {
            context.drawString(this.font, Component.literal("Dominant color"), 1, 1, ~0);
        }

        if (selectedEntry != null) {
            LinkedList<Component> text = selectedEntry.getBlocks().entrySet().stream()
                            .map(entry -> {
                                String dirString = "";
                                if (entry.getValue() != null) {
                                    dirString = " ( ";
                                    for (Direction direction : entry.getValue()) {
                                        dirString += direction + " ";
                                    }
                                    dirString += ")";
                                }
                                return Component.literal(entry.getKey().getName().getString() + dirString);
                            }).collect(Collectors.toCollection(LinkedList::new));
            Vec3 colorVec = this.renderAverage ? selectedEntry.averageColor() : selectedEntry.dominantColor();
            int r = (int) (colorVec.x() * 255), g = (int) (colorVec.y() * 255), b = (int) (colorVec.z() * 255);
            int color = (r << 16) | (g << 8) | b;
            String colorString = String.format("#%02x%02x%02x", r, g, b);
            text.addFirst(Component.literal(colorString).withStyle(Style.EMPTY.withColor(color)));
            context.renderComponentTooltip(this.font, text, mouseX, mouseY);
        }
    }

    private static void setShaderTexture(int i, AbstractTexture texture) {
        RenderSystem.setShaderTexture(i, texture.getId());
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        offsetX += (int) deltaX;
        offsetY += (int) deltaY;
        offsetX = Math.min(5, Math.max(- maxColumns * size + width, offsetX));
        offsetY = Math.min(15, offsetY);
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        // return super.mouseScrolled(mouseX, mouseY, amount);
        int oldSize = this.size;
        if (verticalAmount > 0) {
            this.setSize(oldSize + 1 + oldSize / 8);
        } else if (verticalAmount < 0) {
            this.setSize(oldSize - 1 - oldSize / 8);
        } else {
            return true;
        }
        offsetX = (offsetX - (int) mouseX) * this.size / oldSize + (int) mouseX;
        offsetY = (offsetY - (int) mouseY) * this.size / oldSize + (int) mouseY;
        offsetX = Math.min(5, Math.max(- maxColumns * size + width, offsetX));
        offsetY = Math.min(15, offsetY);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (KeyBindings.OPEN_BLOCKMAP_KEY_BINDING.get().matches(keyCode, scanCode)) {
            this.switchBlockMap();
            return true;
        } else {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
    }
}
