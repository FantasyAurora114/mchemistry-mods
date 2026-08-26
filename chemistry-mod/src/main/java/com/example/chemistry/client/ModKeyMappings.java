package com.example.chemistry.client;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;

public final class ModKeyMappings {

    public static final KeyMapping SNIFF = new KeyMapping(
            "key.mchemistry.sniff",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            KeyMapping.Category.MISC);

    public static final KeyMapping SQUEEZE_DROPPER = new KeyMapping(
            "key.mchemistry.squeeze_dropper",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            KeyMapping.Category.MISC);

    private ModKeyMappings() {
    }
}
