package com.createballoon.block;

import dev.ryanhcode.sable.api.physics.force.ForceGroup;
import dev.ryanhcode.sable.api.physics.force.ForceGroups;
import dev.ryanhcode.sable.api.physics.force.QueuedForceGroup;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.neoforge.event.ForgeSablePostPhysicsTickEvent;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Reads Sable's aerodynamic drag (the {@code sable:drag} force group shown in Aeronautics' force
 * analysis) once per physics step and caches, per sub-level, the summed drag force and its point of
 * application. The balloon controller then applies an equal-and-opposite force at that point to cancel
 * the drag (see {@link HotAirBalloonBlock#physicsTick}).
 *
 * Note: Sable only records a force group's individual point forces while force-tracking is enabled, so
 * the balloon primary enables tracking on its sub-level every tick. Rapier's separate global linear
 * damping ({@code universalDrag}) is intentionally NOT cancelled — it is what settles the hover.
 */
public final class BalloonDragCanceller {

    /** {@code new ForceGroup(...)} would need Veil's RegistryObject; look up by id from the registry instead. */
    private static final ResourceLocation DRAG_ID = ResourceLocation.fromNamespaceAndPath("sable", "drag");

    public record Drag(Vector3d force, Vector3d point) {}

    private static final Map<ServerSubLevel, Drag> CACHE =
            Collections.synchronizedMap(new WeakHashMap<>());

    public static Drag get(ServerSubLevel subLevel) {
        return CACHE.get(subLevel);
    }

    public static void onPostPhysics(ForgeSablePostPhysicsTickEvent event) {
        try {
            ServerLevel level = event.getPhysicsSystem().getLevel();
            ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
            if (container == null) return;
            ForceGroup dragGroup = ForceGroups.REGISTRY.get(DRAG_ID);
            if (dragGroup == null) return;

            for (ServerSubLevel subLevel : container.getAllSubLevels()) {
                QueuedForceGroup qfg = subLevel.getQueuedForceGroups().get(dragGroup);
                if (qfg == null) { CACHE.remove(subLevel); continue; }

                Vector3d force = new Vector3d();
                Vector3d weightedPoint = new Vector3d();
                double totalWeight = 0;
                for (QueuedForceGroup.PointForce pf : qfg.getRecordedPointForces()) {
                    Vector3dc f = pf.force();
                    Vector3dc p = pf.point();
                    force.add(f);
                    double w = f.length();
                    weightedPoint.add(p.x() * w, p.y() * w, p.z() * w);
                    totalWeight += w;
                }

                if (totalWeight > 1e-9 && force.lengthSquared() > 1e-12) {
                    weightedPoint.mul(1.0 / totalWeight);
                    CACHE.put(subLevel, new Drag(force, weightedPoint));
                } else {
                    CACHE.remove(subLevel);
                }
            }
        } catch (Throwable ignored) {}
    }
}
