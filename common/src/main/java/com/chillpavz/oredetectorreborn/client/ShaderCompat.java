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

import java.lang.reflect.Method;

import com.chillpavz.oredetectorreborn.Constants;

/**
 * What the scan highlight can and cannot do while a shaderpack is running.
 *
 * <p>The highlight is drawn with vanilla's gizmos, and a shaderpack breaks that in two separate
 * ways. Iris has no override for the {@code debug_filled_box} pipeline, so the translucent body of
 * each box does not draw; and Iris deliberately skips the depth clear that gives
 * {@code setAlwaysOnTop()} its meaning, so what does draw is hidden behind terrain. The second one
 * is the one that matters, and it is not ours to fix: clearing the main depth texture mid frame
 * would wreck the shaderpack's own passes.
 *
 * <p>So this class does the two things that ARE ours. It asks Iris to map the missing pipeline, via
 * the public API Iris provides for exactly that, which restores the fill on ore the player can
 * already see. And it reports whether a shaderpack is running, so the goggles can say in their
 * tooltip why they are not drawing through rock rather than looking broken.
 *
 * <p>Entirely reflective, so there is no compile dependency on Iris and no client-only class in
 * this file. That second part is load bearing: {@link ScanHighlight} calls into here, and the
 * NeoForge payload handler that references it is registered on a dedicated server too.
 */
public final class ShaderCompat {

    private static final String IRIS_API = "net.irisshaders.iris.api.v0.IrisApi";
    private static final String IRIS_PROGRAM = "net.irisshaders.iris.api.v0.IrisProgram";
    /** Untextured coloured geometry, which is what a filled debug box is. */
    private static final String PROGRAM_FOR_FILL = "BASIC";

    private static volatile boolean shaderPackInUse;
    private static Object api;
    private static Method isShaderPackInUse;
    private static boolean resolved;

    private ShaderCompat() {
    }

    /** Cached so a tooltip can ask freely; refreshed once per client tick. */
    public static boolean shaderPackInUse() {
        return shaderPackInUse;
    }

    /** Re-reads Iris's state. Called from the client tick, where the cost is irrelevant. */
    public static void refresh() {
        Method method = resolve();
        if (method == null) {
            return;
        }
        try {
            shaderPackInUse = (Boolean) method.invoke(api);
        } catch (Throwable failure) {
            shaderPackInUse = false;
        }
    }

    private static Method resolve() {
        if (resolved) {
            return isShaderPackInUse;
        }
        resolved = true;
        try {
            Class<?> apiClass = Class.forName(IRIS_API);
            api = apiClass.getMethod("getInstance").invoke(null);
            isShaderPackInUse = apiClass.getMethod("isShaderPackInUse");
        } catch (Throwable absent) {
            // No Iris. Nothing to report and nothing to register.
            api = null;
            isShaderPackInUse = null;
        }
        return isShaderPackInUse;
    }

    /**
     * Asks Iris to shade the filled-box pipeline, which it otherwise leaves unmapped.
     *
     * <p>Call once from client init. Iris uses this same API on itself for the pipelines its loader
     * modules add, so this is the supported route rather than a workaround.
     *
     * <p>It throws if something has already claimed that pipeline, which is exactly what happens
     * the day Iris maps it themselves or another mod gets there first. That is a good outcome, not
     * an error, so it is caught and noted rather than allowed to take the client down.
     */
    public static void registerFilledBoxPipeline() {
        if (resolve() == null) {
            return;
        }
        try {
            Class<?> programClass = Class.forName(IRIS_PROGRAM);
            Object program = Enum.valueOf(programClass.asSubclass(Enum.class), PROGRAM_FOR_FILL);
            Class<?> pipelineClass = Class.forName("com.mojang.blaze3d.pipeline.RenderPipeline");
            Object filledBox = Class.forName("net.minecraft.client.renderer.RenderPipelines")
                    .getField("DEBUG_FILLED_BOX").get(null);
            api.getClass()
                    .getMethod("assignPipeline", pipelineClass, programClass)
                    .invoke(api, filledBox, program);
            Constants.LOG.info("Registered the filled box pipeline with Iris, so the scan "
                    + "highlight keeps its fill under a shaderpack.");
        } catch (Throwable failure) {
            // Older Iris without the API, or the pipeline already claimed. Either way the
            // highlight still draws its outline, so this is worth one line and no more.
            Constants.LOG.info("Could not register the filled box pipeline with Iris ({}). "
                    + "The scan highlight will draw as an outline under a shaderpack.",
                    failure.getClass().getSimpleName());
        }
    }
}
