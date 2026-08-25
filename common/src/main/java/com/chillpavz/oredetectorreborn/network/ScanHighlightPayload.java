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

import java.util.List;

import com.chillpavz.oredetectorreborn.Constants;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Where a scan actually found ore, sent to the wearer of the goggles so the client can draw it.
 *
 * <p>The positions are sent rather than the scan's shape because the client must not simply redo
 * the scan. When a charge runs dry the server reports fewer blocks than it found, and the
 * highlight has to show exactly what the liquid paid for, not everything that is down there.
 * Re-deriving it client-side would quietly disagree with the count in the action bar.
 *
 * <p>There is no secret being kept here either way: a client already holds every block state in
 * its loaded chunks. Sending positions buys agreement with the server, not concealment.
 *
 * @param origin        the block that was clicked, used to drop the highlight once the player leaves
 * @param cancelRadius  how far from {@code origin} the player may stray before it is dropped
 * @param durationTicks how long the highlight lasts
 * @param groups        the blocks to draw, grouped by ore so the ore name is sent once per group
 *                      rather than once per block
 */
public record ScanHighlightPayload(BlockPos origin, int cancelRadius, int durationTicks,
                                   List<Group> groups) implements CustomPacketPayload {

    /**
     * Hard ceiling on positions per ore. In practice the liquid charge already caps this far
     * lower, since only the blocks the scan actually paid for are drawn, but the codec must not
     * trust the sender.
     */
    public static final int MAX_POSITIONS = 512;
    /** Comfortably above the tank's own type cap, so a well-formed packet can never reach it. */
    public static final int MAX_GROUPS = 8;

    public static final Type<ScanHighlightPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "scan_highlight"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ScanHighlightPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, ScanHighlightPayload::origin,
                    ByteBufCodecs.VAR_INT, ScanHighlightPayload::cancelRadius,
                    ByteBufCodecs.VAR_INT, ScanHighlightPayload::durationTicks,
                    Group.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_GROUPS)), ScanHighlightPayload::groups,
                    ScanHighlightPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** The blocks found for one ore. The ore is a bare material name, as everywhere else. */
    public record Group(String oreType, List<BlockPos> positions) {

        public static final StreamCodec<RegistryFriendlyByteBuf, Group> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.STRING_UTF8, Group::oreType,
                        BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_POSITIONS)), Group::positions,
                        Group::new);
    }
}
