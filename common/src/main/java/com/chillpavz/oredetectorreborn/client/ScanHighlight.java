/*
 * This file is part of Ore Detector Reborn.
 * Copyright (c) 2026 chillpavz
 *
 * SPDX-License-Identifier: LicenseRef-PolyForm-Shield-1.0.0
 *
 * Licensed under the PolyForm Shield License 1.0.0. You may use, modify and
 * redistribute this file for any purpose EXCEPT providing a product that competes
 * with Ore Detector Reborn. See LICENSE, or
 * <https://polyformproject.org/licenses/shield/1.0.0>.
 *
 * Required Notice: Copyright chillpavz (https://github.com/ChillPavz/OreDetectorReborn)
 *
 * Portions descend from Ore Detector by restonic4, MIT licensed. See NOTICE.
 */
package com.chillpavz.oredetectorreborn.client;

import java.util.ArrayList;
import java.util.List;

import com.chillpavz.oredetectorreborn.Constants;
import com.chillpavz.oredetectorreborn.item.OreLookup;
import com.chillpavz.oredetectorreborn.network.ScanHighlightPayload;
import com.chillpavz.oredetectorreborn.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Draws the last scan's hits through the terrain, for as long as the goggles allow.
 *
 * <p>This is client state only. It is fed by {@link ScanHighlightPayload} and emitted once per
 * client tick, which is the whole implementation: vanilla's own gizmo pass clears the depth
 * buffer before drawing anything marked always-on-top, so seeing through rock costs no mixin, no
 * shader and no access widener.
 *
 * <p>Re-emitting every tick rather than using {@code persistForMillis} is deliberate. Gizmos
 * cannot be withdrawn once handed over, and this highlight has to be able to stop early: when the
 * player walks away from the scan, when the window lapses, and when the world changes. Holding
 * the state here keeps all three in one place.
 *
 * <p>The highlight is drawn at ONE fixed strength. An earlier version dimmed and flickered it as
 * strain rose, and in play that was simply irritating: a degraded picture nags continuously,
 * where the Nausea on crossing the ceiling is a sharp, interesting cost to play around. Strain is
 * still tracked and still refuses; it just no longer touches how this looks.
 *
 * <p>Note this class deliberately touches NO client-only class. Everything it needs (game time,
 * where the player is) is passed in by each loader's client tick hook, so it stays compilable and
 * harmless in common.
 */
public final class ScanHighlight {

    /** Fade the highlight away over its last second rather than snapping it off. */
    private static final int FADE_TICKS = 20;
    /** Alpha of the translucent body of a highlighted block, before fading and strain. */
    private static final int FILL_ALPHA = 90;
    /** The outline is drawn harder than the fill so a single block still reads at a distance. */
    private static final int STROKE_ALPHA = 220;
    private static final float STROKE_WIDTH = 1.5F;
    /** Grows the box a hair so it does not fight with the block's own faces. Vanilla uses 0.02. */
    private static final float PADDING = 0.02F;

    /** Set once if emitting ever throws, after which the highlight quietly stops being drawn. */
    private static boolean disabled;

    private static List<Entry> entries = List.of();
    private static ResourceKey<Level> dimension;
    private static BlockPos origin = BlockPos.ZERO;
    private static int cancelRadius;
    private static long expiresAtTick;
    private static int totalTicks;

    private ScanHighlight() {
    }

    /** Takes a scan result. A later scan replaces an earlier one outright rather than stacking. */
    public static void accept(ScanHighlightPayload payload, long gameTime, ResourceKey<Level> dimension) {
        ScanHighlight.dimension = dimension;
        List<Entry> next = new ArrayList<>();
        for (ScanHighlightPayload.Group group : payload.groups()) {
            int rgb = OreLookup.colorOf(group.oreType());
            for (BlockPos pos : group.positions()) {
                next.add(new Entry(pos.immutable(), rgb));
            }
        }
        entries = List.copyOf(next);
        origin = payload.origin().immutable();
        cancelRadius = payload.cancelRadius();
        totalTicks = Math.max(1, payload.durationTicks());
        expiresAtTick = gameTime + totalTicks;
    }

    /** Drops the highlight. Called when the player leaves a world so it cannot bleed into another. */
    public static void clear() {
        entries = List.of();
    }

    /**
     * Emits this tick's highlight. Must be called from a client tick, which vanilla already runs
     * inside a gizmo collector, so no collector has to be opened here.
     *
     * @param gameTime  the client level's game time
     * @param player    the local player, for the walk-away and still-wearing checks
     * @param dimension the world the player is in now, so a highlight cannot cross a portal
     */
    public static void tick(long gameTime, Player player, ResourceKey<Level> dimension) {
        if (entries.isEmpty() || disabled) {
            return;
        }
        // The highlight belongs to the goggles, so taking them off ends it immediately. Without
        // this it survived unequipping them and even throwing them away, which reads as the mod
        // having handed out permanent x-ray.
        if (!player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.GOGGLES)) {
            clear();
            return;
        }
        // A dimension change does not disconnect, so without this a highlight could survive a
        // portal and be drawn over whatever happens to sit at those coordinates in the next world.
        if (!dimension.equals(ScanHighlight.dimension)) {
            clear();
            return;
        }
        long remaining = expiresAtTick - gameTime;
        if (remaining <= 0) {
            clear();
            return;
        }
        // Walking away drops it. The scanned column runs INTO the surface, so the player is never
        // inside it; the honest reading of "leave the column" is leaving the area it was cast from.
        if (player.getEyePosition().distanceToSqr(Vec3.atCenterOf(origin))
                > (double) cancelRadius * cancelRadius) {
            clear();
            return;
        }
        // The only thing that changes the strength is the window running out.
        float fade = Math.min(1.0F, remaining / (float) FADE_TICKS);
        int fillAlpha = Math.round(FILL_ALPHA * fade);
        int strokeAlpha = Math.round(STROKE_ALPHA * fade);
        if (fillAlpha <= 0 && strokeAlpha <= 0) {
            return;
        }

        try {
            for (Entry entry : entries) {
                Gizmos.cuboid(entry.pos(), PADDING, GizmoStyle.strokeAndFill(
                                ARGB.color(strokeAlpha, entry.rgb()),
                                STROKE_WIDTH,
                                ARGB.color(fillAlpha, entry.rgb())))
                        .setAlwaysOnTop();
            }
        } catch (Throwable failure) {
            // Degrade, never crash. Losing the highlight is a disappointment; taking the client
            // down over a rendering detail is a bug report.
            disabled = true;
            clear();
            Constants.LOG.error("Could not draw the scan highlight, so the goggles will stop "
                    + "showing one for this session.", failure);
        }
    }

    private record Entry(BlockPos pos, int rgb) {
    }
}
