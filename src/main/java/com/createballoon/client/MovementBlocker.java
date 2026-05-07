package com.createballoon.client;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.common.NeoForge;

@OnlyIn(Dist.CLIENT)
public final class MovementBlocker {
    public static void init() { NeoForge.EVENT_BUS.register(MovementBlocker.class); }

    @SubscribeEvent
    public static void onInput(MovementInputUpdateEvent e) {
        if (!ControlInputHandler.active()) return;
        if (e.getEntity() != Minecraft.getInstance().player) return;
        var in = e.getInput();
        in.forwardImpulse = 0;
        in.leftImpulse = 0;
        in.jumping = false;
        in.shiftKeyDown = false;
        in.up = false;
        in.down = false;
        in.left = false;
        in.right = false;
    }
}
