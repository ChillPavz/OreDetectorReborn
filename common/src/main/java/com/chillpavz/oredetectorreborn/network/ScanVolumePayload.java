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
package com.chillpavz.oredetectorreborn.network;

import com.chillpavz.oredetectorreborn.Constants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * The SHAPE a scan just swept, sent so the client can outline it.
 *
 * <p>Deliberately separate from {@link ScanHighlightPayload}, which carries where ore was FOUND.
 * The two answer different questions, go to different people and last for different lengths of
 * time: the volume is sent on every scan to whoever scanned, wearing goggles or not, because its
 * whole job is teaching how far and how wide the beam reaches. The highlight is the goggles' own
 * reward and is gated behind durability, liquid and strain.
 *
 * <p>Nothing here says anything about ore. It is the box the detector looked in, which the player
 * chose by clicking, so it gives away nothing they did not already do.
 *
 * @param origin        the block that was clicked; depth 0 of the beam, and part of the volume
 * @param into          the direction the beam travelled, always the opposite of the clicked face
 * @param reach         how many blocks deep the beam went, including the clicked block
 * @param radius        column radius, so the cross-section is {@code (2 * radius + 1)} square
 * @param durationTicks how long to outline it for
 */
public record ScanVolumePayload(BlockPos origin, Direction into, int reach, int radius,
                                int durationTicks) implements CustomPacketPayload {

    /** Matches the config's own reach ceiling; the codec must not trust the sender. */
    public static final int MAX_REACH = 96;
    public static final int MAX_RADIUS = 8;

    public static final Type<ScanVolumePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "scan_volume"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ScanVolumePayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, ScanVolumePayload::origin,
                    Direction.STREAM_CODEC, ScanVolumePayload::into,
                    ByteBufCodecs.VAR_INT, ScanVolumePayload::reach,
                    ByteBufCodecs.VAR_INT, ScanVolumePayload::radius,
                    ByteBufCodecs.VAR_INT, ScanVolumePayload::durationTicks,
                    ScanVolumePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
