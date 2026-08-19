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
package com.chillpavz.oredetectorreborn.item;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import com.chillpavz.oredetectorreborn.Constants;
import com.chillpavz.oredetectorreborn.config.OreDetectorConfig;
import com.chillpavz.oredetectorreborn.registry.ModDataComponents;
import com.chillpavz.oredetectorreborn.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The Ore Detector: one item, attuned to whatever Attunement Liquid is in its tank.
 *
 * <p>Two independent pools. Durability is mechanical wear and costs a flat 1 per scan. Liquid is
 * the per-ore cost, 1 mB for each ore block actually found, spent from that ore's own charge. Load
 * fewer liquids and the beam reaches further; load more and it widens instead.
 */
public class AttunedDetectorItem extends Item {

    /** Ticks to pour one bottle. Deliberately close to eating, so it reads as a channelled action. */
    public static final int POUR_TICKS = 32;

    public AttunedDetectorItem(Properties properties) {
        super(properties);
    }

    public static OreTank tankOf(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ORE_TANK, OreTank.EMPTY);
    }

    private static void setTank(ItemStack stack, OreTank tank) {
        if (tank.isEmpty()) {
            stack.remove(ModDataComponents.ORE_TANK);
        } else {
            stack.set(ModDataComponents.ORE_TANK, tank);
        }
    }

    /** How far the beam reaches into a surface, given how many distinct liquids are loaded. */
    public static int downReach(int typeCount) {
        int step = OreDetectorConfig.reachStep;
        return step * (typeCount <= 1 ? 3 : typeCount == 2 ? 2 : 1);
    }

    /** Sideways and upward reach is three quarters of the downward reach. */
    public static int sideReach(int typeCount) {
        return downReach(typeCount) * 3 / 4;
    }

    /** Half-width of the scanned column: 3x3, 5x5, then 7x7 as more liquids are loaded. */
    public static int columnRadius(int typeCount) {
        return typeCount <= 1 ? 1 : typeCount == 2 ? 2 : 3;
    }

    // ------------------------------------------------------------------ interactions

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        // Pouring and draining are both holds handled in use(); let them through.
        if (pourableOffhand(player) != null || looksAtCauldron(context.getLevel(), player)) {
            return InteractionResult.PASS;
        }
        return scan(context.getLevel(), player, context.getClickedPos(), context.getClickedFace(),
                context.getItemInHand());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack detector = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (pourTarget(player, detector) != null || drainTicks(player, detector) > 0) {
            player.startUsingItem(hand);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return 0;
        }
        if (pourTarget(player, stack) != null) {
            return POUR_TICKS;
        }
        int drain = drainTicks(player, stack);
        return drain > 0 ? drain : 0;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return stack;
        }
        ItemStack bottle = pourTarget(player, stack);
        if (bottle != null) {
            pour(level, player, stack, bottle);
            return stack;
        }
        if (drainTicks(player, stack) > 0) {
            drain(level, player, stack);
        }
        return stack;
    }

    // ------------------------------------------------------------------ pour

    /** The off-hand stack if it is Attunement Liquid with an ore set, else null. */
    private static ItemStack pourableOffhand(Player player) {
        ItemStack offhand = player.getOffhandItem();
        if (!(offhand.getItem() instanceof AttunementLiquidItem)) {
            return null;
        }
        return offhand.get(ModDataComponents.ORE_TYPE) == null ? null : offhand;
    }

    /** The bottle to pour, or null when there is nothing to pour or no room for it. */
    private static ItemStack pourTarget(Player player, ItemStack detector) {
        ItemStack bottle = pourableOffhand(player);
        if (bottle == null) {
            return null;
        }
        String ore = bottle.get(ModDataComponents.ORE_TYPE);
        // A completely full tank, or a seventh ore type, refuses the pour outright rather than
        // swallowing the bottle for nothing.
        return tankOf(detector).acceptable(ore) > 0 ? bottle : null;
    }

    private static void pour(Level level, Player player, ItemStack detector, ItemStack bottle) {
        String ore = bottle.get(ModDataComponents.ORE_TYPE);
        OreTank tank = tankOf(detector);
        int accepted = tank.acceptable(ore);
        if (accepted <= 0) {
            return;
        }
        if (!level.isClientSide()) {
            // The whole bottle is consumed even when only part of it fits. That is the only way to
            // get more than three ore types into a 300 mB tank.
            setTank(detector, tank.pour(ore, accepted));
            bottle.shrink(1);
            if (!player.getInventory().add(new ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE))) {
                player.drop(new ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE), false);
            }
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                net.minecraft.sounds.SoundEvents.BOTTLE_EMPTY, SoundSource.PLAYERS, 0.8F, 1.0F);
    }

    // ------------------------------------------------------------------ drain

    private static boolean looksAtCauldron(Level level, Player player) {
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
        if (hit.getType() != BlockHitResult.Type.BLOCK) {
            return false;
        }
        return level.getBlockState(hit.getBlockPos()).getBlock() instanceof AbstractCauldronBlock;
    }

    /**
     * How long emptying the tank into a cauldron should take, scaled by what is actually in it, or
     * 0 when there is nothing to drain or nothing to drain into.
     */
    private static int drainTicks(Player player, ItemStack detector) {
        OreTank tank = tankOf(detector);
        if (tank.isEmpty() || !looksAtCauldron(player.level(), player)) {
            return 0;
        }
        // One bottle's worth takes as long as pouring one in.
        return Math.max(POUR_TICKS, tank.total() * POUR_TICKS / OreTank.BOTTLE);
    }

    private static void drain(Level level, Player player, ItemStack detector) {
        if (!level.isClientSide()) {
            // Deliberately discards. Recovering the liquid needs a cauldron that remembers an ore
            // and an amount, which is a later stage.
            setTank(detector, OreTank.EMPTY);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                net.minecraft.sounds.SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS, 0.8F, 0.8F);
    }

    // ------------------------------------------------------------------ scan

    private InteractionResult scan(Level level, Player player, BlockPos clicked, Direction face,
                                   ItemStack detector) {
        OreTank tank = tankOf(detector);
        if (tank.isEmpty()) {
            if (player instanceof ServerPlayer server) {
                actionBar(server, Component.translatable("hud." + Constants.MOD_ID + ".no_liquid")
                        .withStyle(ChatFormatting.GRAY));
            }
            return InteractionResult.SUCCESS;
        }
        if (player.getCooldowns().isOnCooldown(detector)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        int types = tank.typeCount();
        Direction into = face.getOpposite();
        int reach = into == Direction.DOWN ? downReach(types) : sideReach(types);
        int radius = columnRadius(types);

        Map<String, Integer> found = new LinkedHashMap<>();
        for (int depth = 1; depth <= reach; depth++) {
            for (int a = -radius; a <= radius; a++) {
                for (int b = -radius; b <= radius; b++) {
                    BlockPos pos = offset(clicked, into, depth, a, b);
                    BlockState state = level.getBlockState(pos);
                    String ore = OreLookup.oreTypeOf(state);
                    if (ore != null && tank.holds(ore)) {
                        found.merge(ore, 1, Integer::sum);
                    }
                }
            }
        }

        // Wear is flat: one per scan regardless of what turned up. The per-ore cost is the liquid.
        detector.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);

        OreTank remaining = tank;
        int reported = 0;
        for (Map.Entry<String, Integer> entry : found.entrySet()) {
            int available = remaining.amountOf(entry.getKey());
            int spend = Math.min(available, entry.getValue());
            remaining = remaining.drain(entry.getKey(), spend);
            reported += spend;
        }
        setTank(detector, remaining);

        report(player, found, reported);
        player.getCooldowns().addCooldown(detector, OreDetectorConfig.cooldownTicks);
        return InteractionResult.SUCCESS;
    }

    private static BlockPos offset(BlockPos origin, Direction into, int depth, int a, int b) {
        BlockPos base = origin.relative(into, depth);
        return switch (into.getAxis()) {
            case Y -> base.offset(a, 0, b);
            case X -> base.offset(0, a, b);
            case Z -> base.offset(a, b, 0);
        };
    }

    private static void report(Player player, Map<String, Integer> found, int reported) {
        if (found.isEmpty()) {
            if (player instanceof ServerPlayer server) {
                actionBar(server, Component.translatable("hud." + Constants.MOD_ID + ".none_multi")
                        .withStyle(ChatFormatting.GRAY));
            }
            playBeep(player, false);
            return;
        }
        Component message = null;
        for (Map.Entry<String, Integer> entry : found.entrySet()) {
            Component part = Component.literal(entry.getValue() + " ")
                    .append(OreLookup.displayName(entry.getKey()))
                    .withColor(OreLookup.colorOf(entry.getKey()));
            message = message == null ? part : message.copy().append(Component.literal(", ")).append(part);
        }
        if (player instanceof ServerPlayer server) {
            actionBar(server, Component.translatable("hud." + Constants.MOD_ID + ".found_multi", message));
        }
        playBeep(player, true);
    }

    /** Player.displayClientMessage is gone at 26.x; the action bar is a ServerPlayer system message. */
    private static void actionBar(ServerPlayer player, Component message) {
        player.sendSystemMessage(message, true);
    }

    private static void playBeep(Player player, boolean found) {
        if (player instanceof ServerPlayer server) {
            server.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    found ? ModSounds.FOUND : ModSounds.NOT_FOUND, SoundSource.PLAYERS,
                    (float) OreDetectorConfig.soundVolume, 1.0F);
        }
    }

    // ------------------------------------------------------------------ tooltip

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> adder, TooltipFlag flag) {
        OreTank tank = tankOf(stack);
        int types = tank.typeCount();
        if (tank.isEmpty()) {
            adder.accept(Component.translatable("tooltip." + Constants.MOD_ID + ".tank_empty")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }
        adder.accept(Component.translatable("tooltip." + Constants.MOD_ID + ".tank",
                tank.total(), OreTank.CAPACITY).withStyle(ChatFormatting.GRAY));
        for (Map.Entry<String, Integer> entry : tank.sorted()) {
            adder.accept(Component.literal(" ")
                    .append(OreLookup.displayName(entry.getKey()))
                    .append(Component.literal(": " + entry.getValue() + " mB"))
                    .withColor(OreLookup.colorOf(entry.getKey())));
        }
        adder.accept(Component.translatable("tooltip." + Constants.MOD_ID + ".range",
                downReach(types), sideReach(types), columnRadius(types) * 2 + 1)
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
