package io.github.orlouge.blockgradients.mixin;

import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.SpriteContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SpriteContents.class)
public interface SpriteContentsAccessor {
	@Accessor
	NativeImage[] getMipmapLevelsImages();

	@Accessor
	SpriteContents.Animation getAnimation();

	@Accessor
	int getWidth();


	@Accessor
	int getHeight();
}
