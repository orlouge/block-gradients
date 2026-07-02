package io.github.orlouge.blockgradients;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

import java.util.function.Supplier;

public class KeyBindings {
    public static final Supplier<KeyMapping> OPEN_BLOCKMAP_KEY_BINDING = memoize(() -> new KeyMapping(
            "key.blockgradients.openblockmap",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            "category.blockgradients.blockgradients"
    ));

    private static <T> Supplier<T> memoize(Supplier<T> supplier) {
        return new Supplier<>() {
            private T value;

            @Override
            public T get() {
                if (value == null) {
                    value = supplier.get();
                }
                return value;
            }
        };
    }
}
