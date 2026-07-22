package com.createballoon.client;

import com.createballoon.network.ControlInputPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
public final class ControlInputHandler {
    private static BlockPos consolePos;
    private static boolean prevSpace, prevV, prevW, prevA, prevS, prevD;
    private static boolean exiting;

    public static void init() {
        NeoForge.EVENT_BUS.addListener(ControlInputHandler::tick);
        NeoForge.EVENT_BUS.addListener(ControlInputHandler::onLogout);
    }
    public static void start(BlockPos pos) { consolePos = pos; prevSpace = false; prevV = false;
        prevW = false; prevA = false; prevS = false; prevD = false; exiting = false; }
    public static void stop() { consolePos = null; exiting = false; }
    public static boolean active() { return consolePos != null || exiting; }

    private static void onLogout(ClientPlayerNetworkEvent.LoggingOut e) {
        stop();
    }

    private static void tick(ClientTickEvent.Post e) {
        if (consolePos == null) return;
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        long w = mc.getWindow().getWindow();
        boolean space = GLFW.glfwGetKey(w, GLFW.GLFW_KEY_SPACE) == GLFW.GLFW_PRESS;
        boolean v = GLFW.glfwGetKey(w, GLFW.GLFW_KEY_V) == GLFW.GLFW_PRESS;
        boolean shift = GLFW.glfwGetKey(w, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(w, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        boolean wKey = GLFW.glfwGetKey(w, GLFW.GLFW_KEY_W) == GLFW.GLFW_PRESS;
        boolean a = GLFW.glfwGetKey(w, GLFW.GLFW_KEY_A) == GLFW.GLFW_PRESS;
        boolean s = GLFW.glfwGetKey(w, GLFW.GLFW_KEY_S) == GLFW.GLFW_PRESS;
        boolean d = GLFW.glfwGetKey(w, GLFW.GLFW_KEY_D) == GLFW.GLFW_PRESS;

        if (shift && !exiting) {
            exiting = true;
            PacketDistributor.sendToServer(new ControlInputPacket(consolePos, false, false, true, false, false, false, false));
            return;
        }

        boolean changed = space != prevSpace || v != prevV || wKey != prevW || a != prevA || s != prevS || d != prevD;
        if (changed) {
            PacketDistributor.sendToServer(new ControlInputPacket(consolePos, space, v, false, wKey, s, a, d));
            prevSpace = space; prevV = v;
            prevW = wKey; prevA = a; prevS = s; prevD = d;
        }
    }
}
