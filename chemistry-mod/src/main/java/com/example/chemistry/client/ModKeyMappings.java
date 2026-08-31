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

    public static final KeyMapping FIX_TEMP = new KeyMapping(
            "key.mchemistry.fix_temp",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_I,
            KeyMapping.Category.MISC);

    public static final KeyMapping HEAT_UP = new KeyMapping(
            "key.mchemistry.heat_up",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UP,
            KeyMapping.Category.MISC);

    public static final KeyMapping HEAT_DOWN = new KeyMapping(
            "key.mchemistry.heat_down",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_DOWN,
            KeyMapping.Category.MISC);

    public static final KeyMapping SHOW_PORTS = new KeyMapping(
            "key.mchemistry.show_ports",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F9,
            KeyMapping.Category.MISC);

    private ModKeyMappings() {
    }
}
