package io.github.orlouge.blockgradients;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class BlockMapScreen extends Screen {
    private final GradientMap averageBlockMap, dominantBlockMap;
    private int offsetX = 0, offsetY = 0, previousOffsetX = 0, previousOffsetY = 0, size = 16, previousSize = -1;
    private boolean resetSize = true, renderAverage = true;
    private float textureOffset = 0;

    public BlockMapScreen(GradientMap averageBlockMap, GradientMap dominantBlockMap) {
        super(Text.of("BlockMap"));
        this.averageBlockMap = averageBlockMap;
        this.dominantBlockMap = dominantBlockMap;
        if (dominantBlockMap.getRow(0).size() > 0) renderAverage = false;
    }

    public static void openBlockMap(MinecraftClient mc) {
        ItemStack main = mc.player.getMainHandStack(), off = mc.player.getOffHandStack();
        if (!(main.getItem() instanceof BlockItem destBlock)) {
            mc.player.sendMessage(Text.of(main.getItem().getName() + " is not a block"), true);
        } else if (!(off.getItem() instanceof BlockItem sourceBlock)) {
            mc.player.sendMessage(Text.of(off.getItem().getName() + " is not a block"), true);
        } else {
            BlockColorEntry source = BlockColorEntries.getEntry(sourceBlock.getBlock());
            BlockColorEntry dest = BlockColorEntries.getEntry(destBlock.getBlock());
            if (source == null) {
                mc.player.sendMessage(Text.of(sourceBlock.getBlock().getName() + " is translucent or blacklisted"), true);
            } else if (dest == null) {
                mc.player.sendMessage(Text.of(destBlock.getBlock().getName() + " is translucent or blacklisted"), true);
            } else {
                GradientMap averageBlockMap = GradientMap.build(BlockColorEntries.getEntries(), source, dest, false);
                GradientMap dominantBlockMap = GradientMap.build(BlockColorEntries.getEntries(), source, dest, true);
                while (KeyBindings.OPEN_BLOCKMAP_KEY_BINDING.get().wasPressed()) ;
                if (mc.currentScreen == null) {
                    mc.setScreen(new BlockMapScreen(averageBlockMap, dominantBlockMap));
                } else if (mc.currentScreen instanceof BlockMapScreen) {
                    ((BlockMapScreen) mc.currentScreen).switchBlockMap();
                }
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        textureOffset += 0.05;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        if (this.resetSize) {
            if (this.renderAverage) {
                this.setSize(width / (5 + IntStream.range(0, 10).map(y -> averageBlockMap.getRow(y).size()).max().orElse(0)));
            } else {
                this.setSize(width / (5 + IntStream.range(0, 10).map(y -> dominantBlockMap.getRow(y).size()).max().orElse(0)));
            }
            offsetY += 20;
            offsetX += 5;
            this.resetSize = false;
        }
        this.renderBackground(context);
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

    private void renderBlockMap(DrawContext context, GradientMap blockMap, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);

        BlockColorEntry selectedEntry = null;
        for (int gy = (-offsetY + height) / size; gy >= -offsetY / size; gy--) {
            for (GradientMap.Cell cell : blockMap.getRow(gy)) {
                int gx = cell.cellX;
                BlockColorEntry entry = cell.entry;
                if (entry != null) {
                    int x = offsetX + gx * size, y = offsetY + gy * size;
                    if (x > -size && y > -size && x < width && y < height) {
                        setShaderTexture(0, entry.getTexture(gy + (int) textureOffset));
                        //context.drawTexture(x, y, 0, 0, size, size, size, size);
                        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
                        Matrix4f posMatrix = context.getMatrices().peek().getPositionMatrix();
                        BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
                        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
                        bufferBuilder.vertex(posMatrix, x, y, 0).texture(0, 0).next();
                        bufferBuilder.vertex(posMatrix, x, y + size, 0).texture(0, 1).next();
                        bufferBuilder.vertex(posMatrix, x + size, y + size, 0).texture(1, 1).next();
                        bufferBuilder.vertex(posMatrix, x + size, y, 0).texture(1, 0).next();
                        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
                        if (mouseX >= x && mouseY >= y && mouseX < x + size && mouseY < y + size) {
                            selectedEntry = entry;
                        }
                    }
                }
            }
        }


        if (renderAverage) {
            context.drawTextWithShadow(this.textRenderer, Text.of("Average color"), 1, 1, ~0);
        } else {
            context.drawTextWithShadow(this.textRenderer, Text.of("Dominant color"), 1, 1, ~0);
        }

        if (selectedEntry != null) {
            LinkedList<OrderedText> text = selectedEntry.getBlocks().entrySet().stream()
                            .map(entry -> {
                                String dirString = "";
                                if (entry.getValue() != null) {
                                    dirString = " ( ";
                                    for (Direction direction : entry.getValue()) {
                                        dirString += direction + " ";
                                    }
                                    dirString += ")";
                                }
                                return OrderedText.styledForwardsVisitedString(
                                        entry.getKey().getName().getString() + dirString,
                                        Style.EMPTY
                                );
                            }).collect(Collectors.toCollection(LinkedList::new));
            Vec3d colorVec = this.renderAverage ? selectedEntry.averageColor() : selectedEntry.dominantColor();
            int r = (int) (colorVec.getX() * 255), g = (int) (colorVec.getY() * 255), b = (int) (colorVec.getZ() * 255);
            int color = (r << 16) | (g << 8) | b;
            String colorString = String.format("#%02x%02x%02x", r, g, b);
            text.addFirst(OrderedText.styledForwardsVisitedString(colorString, Style.EMPTY.withColor(color)));
            context.drawOrderedTooltip(this.textRenderer, text, mouseX, mouseY);
        }
    }

    private static void setShaderTexture(int i, AbstractTexture texture) {
        RenderSystem.setShaderTexture(i, texture.getGlId());
        /*
        int[] shaderTextures = RenderSystem.shaderTextures;
        if (i >= 0 && i < shaderTextures.length) {
            if (!RenderSystem.isOnRenderThread()) {
                RenderSystem.recordRenderCall(() -> {
                    shaderTextures[i] = texture.getGlId();
                });
            } else {
                shaderTextures[i] = texture.getGlId();
            }
        }

         */
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        offsetX += (int) deltaX;
        offsetY += (int) deltaY;
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        // return super.mouseScrolled(mouseX, mouseY, amount);
        int oldSize = this.size;
        if (amount > 0) {
            this.setSize(oldSize + 1 + oldSize / 8);
        } else if (amount < 0) {
            this.setSize(oldSize - 1 - oldSize / 8);
        } else {
            return true;
        }
        offsetX = (offsetX - (int) mouseX) * this.size / oldSize + (int) mouseX;
        offsetY = (offsetY - (int) mouseY) * this.size / oldSize + (int) mouseY;
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (KeyBindings.OPEN_BLOCKMAP_KEY_BINDING.get().matchesKey(keyCode, scanCode)) {
            this.switchBlockMap();
            return true;
        } else {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
    }
}
