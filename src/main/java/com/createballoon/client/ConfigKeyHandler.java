package com.createballoon.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
public final class ConfigKeyHandler {
    public static final KeyMapping OPEN_CONFIG = new KeyMapping(
            "key.create_balloon.config", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_E,
            "key.categories.create_balloon");

    private static boolean wasVDown;

    public static void init() {
        NeoForge.EVENT_BUS.addListener(ConfigKeyHandler::onClientTick);
        NeoForge.EVENT_BUS.addListener(ConfigKeyHandler::onScreenOpen);
    }

    public static void onRegisterKeys(RegisterKeyMappingsEvent e) {
        e.register(OPEN_CONFIG);
    }

    private static void onClientTick(ClientTickEvent.Post e) {
        var mc = Minecraft.getInstance();
        if (mc.player == null) return;
        long w = mc.getWindow().getWindow();
        boolean v = InputConstants.isKeyDown(w, GLFW.GLFW_KEY_V);
        boolean ek = InputConstants.isKeyDown(w, GLFW.GLFW_KEY_E);
        if (wasVDown && ek) {
            int state = GLFW.glfwGetKey(w, GLFW.GLFW_KEY_E);
            if (state == GLFW.GLFW_PRESS && mc.screen == null) {
                mc.setScreen(new GuiConfigScreen());
                wasVDown = false;
                return;
            }
        }
        wasVDown = v;
    }

    private static void onScreenOpen(ScreenEvent.Opening e) {
        if (e.getScreen() instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen) {
            var mc = Minecraft.getInstance();
            long w = mc.getWindow().getWindow();
            if (InputConstants.isKeyDown(w, GLFW.GLFW_KEY_V)) {
                e.setCanceled(true);
                mc.setScreen(new GuiConfigScreen());
            }
        }
    }
}
