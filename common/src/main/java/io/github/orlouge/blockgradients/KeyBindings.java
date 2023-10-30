package io.github.orlouge.blockgradients;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Lazy;
import org.lwjgl.glfw.GLFW;

import java.util.function.Supplier;

public class KeyBindings {
    public static final Lazy<KeyBinding> OPEN_BLOCKMAP_KEY_BINDING = new Lazy<>(() -> new KeyBinding(
            "key.blockgradients.openblockmap",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            "category.blockgradients.blockgradients"
    ));
}
