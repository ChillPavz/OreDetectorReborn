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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.chillpavz.oredetectorreborn.Constants;
import com.chillpavz.oredetectorreborn.config.OreDetectorConfig;
import com.chillpavz.oredetectorreborn.block.NullifiedCauldronBlock;
import com.chillpavz.oredetectorreborn.registry.ModDataComponents;
import com.chillpavz.oredetectorreborn.registry.ModItems;
import com.chillpavz.oredetectorreborn.network.ModNetworking;
import com.chillpavz.oredetectorreborn.network.ScanHighlightPayload;
import com.chillpavz.oredetectorreborn.network.ScanVolumePayload;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
import net.minecraft.world.phys.Vec3;

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

    /**
     * The shortest a pour or a drain can be, before the config scalar.
     *
     * <p>Without a floor the arithmetic is honest and the feel is wrong: a netherite bottle is
     * twelve millibuckets, which came out at two ticks and read as a click rather than a
     * channelled action. Scaling everything up instead would have fixed that at the cost of the
     * common case, since the multiplier needed to make netherite land makes a coal bottle a six
     * second hold. A floor plus the proportional part leaves coal exactly where it was and gives
     * the small bottles their weight back.
     */
    public static final int MIN_FLOW_TICKS = 6;

    /**
     * How long moving this much liquid takes, in or out.
     *
     * <p>The same curve both ways, because a bottle going in and the same amount coming out should
     * feel like the same action.
     */
    public static int flowTicks(int millibuckets) {
        int base = MIN_FLOW_TICKS
                + millibuckets * (POUR_TICKS - MIN_FLOW_TICKS) / LIQUID_PER_POUR;
        return OreDetectorConfig.scaleFlowTicks(base);
    }

    /**
     * The millibuckets a pour or a drain moves in {@link #POUR_TICKS}.
     *
     * <p>The biggest bottle in the game, so moving that much takes one pour's time and everything
     * smaller is proportionally quicker. A full tank of one ore is about five seconds either way.
     *
     * <p><b>Pouring is paced by this too, not per bottle.</b> Bottles stopped being one size when
     * rarity moved into what a bottle is worth, so a fixed time per bottle would have made filling
     * a tank with netherite a minute of holding right click: forty-odd bottles at a second and a
     * half each. Time now follows the liquid moved, which is the number the player can see.
     */
    public static final int LIQUID_PER_POUR = 165;

    /** How long the scan volume is outlined for. Short: it is feedback, not an overlay. */
    public static final int VOLUME_TICKS = 40;

    /** How long a rested pair of goggles holds the highlight up: twelve seconds. */
    public static final int HIGHLIGHT_TICKS = 240;

    /**
     * How far past the beam's own reach the player may wander before the highlight drops. The
     * scanned column runs INTO the surface, so the player is never standing inside it; this is
     * the distance from the block they clicked, which is what "leaving the scan" actually means.
     */
    public static final int CANCEL_MARGIN = 12;

    public AttunedDetectorItem(Properties properties) {
        super(properties);
    }

    /**
     * Keeps the anvil's prior-work penalty off this item; see {@link RepairCosts}. Without it the
     * Breeze Shard repair loop this item is designed around dies at the seventh anvil visit.
     */
    @Override
    public void inventoryTick(ItemStack stack, net.minecraft.server.level.ServerLevel level,
                              net.minecraft.world.entity.Entity entity,
                              net.minecraft.world.entity.EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);
        RepairCosts.clear(stack);
    }

    public static OreTank tankOf(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ORE_TANK, OreTank.EMPTY);
    }

    static void setTank(ItemStack stack, OreTank tank) {
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
        // MAIN hand only. Without this the detector scans from the OFF hand too, so right-clicking
        // while holding anything else fires a scan the player never asked for, and the goggles it
        // then wears down look like they are losing durability at random.
        if (context.getHand() != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        // Draining is a hold handled in use(); everything else is a scan. Pouring is NOT checked
        // here any more: it lives on the liquid item, so a bottle in either hand cannot block a scan.
        if (cauldronAimedAt(context.getLevel(), player) != null) {
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
        if (drainTicks(player, detector) > 0) {
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
        return Math.max(0, drainTicks(player, stack));
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
        if (drainTicks(player, stack) > 0) {
            drain(level, player, stack);
        }
        return stack;
    }

    // ------------------------------------------------------------------ drain

    /** The cauldron being aimed at, or null. Ours extends the vanilla class, so both qualify. */
    private static BlockPos cauldronAimedAt(Level level, Player player) {
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
        if (hit.getType() != BlockHitResult.Type.BLOCK) {
            return null;
        }
        BlockPos pos = hit.getBlockPos();
        return level.getBlockState(pos).getBlock() instanceof AbstractCauldronBlock ? pos : null;
    }

    /**
     * The ore a drain would remove: the SMALLEST charge in the tank.
     *
     * <p>Smallest first is deliberate. A tank with 10 mB of something in the way can be cleared in
     * one short pull without throwing away the charges you actually wanted, which is what makes
     * re-attuning cheap instead of all-or-nothing.
     */
    private static String drainTarget(ItemStack detector) {
        OreTank tank = tankOf(detector);
        String smallest = null;
        int least = Integer.MAX_VALUE;
        for (Map.Entry<String, Integer> entry : tank.charges().entrySet()) {
            if (entry.getValue() < least
                    || (entry.getValue() == least && smallest != null && entry.getKey().compareTo(smallest) < 0)) {
                least = entry.getValue();
                smallest = entry.getKey();
            }
        }
        return smallest;
    }

    /**
     * How long emptying one ore out takes, scaled by how much of it there is, or 0 when there is
     * nothing to drain or nothing to drain into.
     */
    private static int drainTicks(Player player, ItemStack detector) {
        String ore = drainTarget(detector);
        if (ore == null) {
            return 0;
        }
        BlockPos pos = cauldronAimedAt(player.level(), player);
        // Refused rather than clamped when it would tip the cauldron past a full bucket: the
        // player empties it first. Clamping would silently swallow the difference.
        if (pos == null || !NullifiedCauldronBlock.canAccept(
                player.level(), pos, tankOf(detector).amountOf(ore))) {
            return 0;
        }
        // PACED BY MILLIBUCKETS, NOT BY BOTTLES, and that distinction is the whole point.
        //
        // It used to be per bottle, so emptying one bottle's worth took one pour's time whatever
        // the ore. That is defensible arithmetic and it reads as a bug: 12 mB of netherite is a
        // full bottle and drained as slowly as 165 mB of coal, so the smaller number took longer.
        // Now that a millibucket is one ore reported, the millibucket count is the number the
        // player can actually see, and it is the one the hold has to follow.
        int amount = tankOf(detector).amountOf(ore);
        return flowTicks(amount);
    }

    private static void drain(Level level, Player player, ItemStack detector) {
        String ore = drainTarget(detector);
        if (ore == null) {
            return;
        }
        if (!level.isClientSide()) {
            OreTank tank = tankOf(detector);
            int amount = tank.amountOf(ore);
            // The liquid loses its ore on the way out, which is why the cauldron needs only one
            // fluid rather than one per combination a player might have mixed.
            BlockPos pos = cauldronAimedAt(level, player);
            if (pos == null || !NullifiedCauldronBlock.canAccept(level, pos, amount)) {
                return;
            }
            NullifiedCauldronBlock.addTo(level, pos, amount);
            setTank(detector, tank.drain(ore, amount));
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

        // Positions, not just counts: the goggles draw exactly the blocks this scan reported, so
        // the highlight and the action bar can never disagree about what is down there.
        Map<String, List<BlockPos>> hits = new LinkedHashMap<>();
        // depth 0 is the block that was clicked. It must be included: clicking directly on
        // an ore has to count it.
        for (int depth = 0; depth < reach; depth++) {
            for (int a = -radius; a <= radius; a++) {
                for (int b = -radius; b <= radius; b++) {
                    BlockPos pos = offset(clicked, into, depth, a, b);
                    BlockState state = level.getBlockState(pos);
                    String ore = OreLookup.oreTypeOf(state);
                    if (ore != null && tank.holds(ore)) {
                        hits.computeIfAbsent(ore, key -> new ArrayList<>()).add(pos.immutable());
                    }
                }
            }
        }

        Map<String, Integer> found = new LinkedHashMap<>();
        hits.forEach((ore, positions) -> found.put(ore, positions.size()));

        // Wear is flat: one per scan regardless of what turned up. The per-ore cost is the liquid.
        detector.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);

        OreTank remaining = tank;
        int reported = 0;
        // What each ore's charge actually covered, IN BLOCKS. The action bar reports everything
        // FOUND, but only this much was paid for, and only this much is drawn.
        //
        // Blocks and millibuckets were the same number while every ore cost a flat 1 mB, and they
        // are not any more: the cost per block is tiered, so a coal charge stretches 750 blocks
        // and a netherite charge 37. Keep this map counting BLOCKS - it drives the highlight and
        // the goggles' wear, neither of which cares what the liquid cost.
        Map<String, Integer> paidFor = new LinkedHashMap<>();
        // Ores whose charge was exhausted by this scan, so the player can be told which ones just
        // stopped working rather than discovering it on the next click.
        java.util.List<String> ranDry = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : found.entrySet()) {
            String ore = entry.getKey();
            int blocksFound = entry.getValue();
            int available = remaining.amountOf(ore);
            // ONE MILLIBUCKET IS ONE ORE, for every material alike. Rarity is in what a bottle is
            // worth, not in what a block costs to report; see OreGrinding.bottleSizeOf.
            //
            // That flat rate is also why the dregs bug cannot come back. When the price of a block
            // was tiered, a charge below that price bought nothing, so it never drained and the
            // scan still reported every block it saw: unlimited free detection for one
            // millibucket. At a flat 1 any charge at all buys at least one block.
            int affordable = Math.min(blocksFound, available);
            if (affordable < blocksFound) {
                // The charge could not cover everything found, so it is spent out and the ore
                // leaves the tank. That makes running dry a single visible event.
                ranDry.add(ore);
            }
            remaining = remaining.drain(ore, affordable);
            paidFor.put(ore, affordable);
            reported += affordable;
        }
        setTank(detector, remaining);
        showVolume(player, clicked, into, reach, radius);
        wearGoggles(player, reported);
        visualise(level, player, clicked, reach, hits, paidFor);

        rewardScan(player, reported > 0, types);
        report(player, paidFor, ranDry);
        player.getCooldowns().addCooldown(detector, OreDetectorConfig.cooldownTicks);
        return InteractionResult.SUCCESS;
    }

    /**
     * Shows the wearer where the scan's hits are, if they have the goggles on and the goggles are
     * up to it.
     *
     * <p>Only the blocks the liquid PAID for are drawn, nearest first. A scan can find ten iron on
     * three millibuckets of iron charge: the action bar still says ten, because that is what the
     * detector genuinely detected, but the goggles show the three closest. The text is the survey
     * and the highlight is what the charge could resolve.
     *
     * <p>This is the only thing Strain gates. It rises per visualisation and bleeds off on its
     * own, so it brakes a player spamming scans to sweep a cave without ever taking the detector
     * away from someone using it at a normal pace. Crossing the ceiling costs Nausea; below it,
     * nothing about the picture changes.
     *
     * <p>Nothing here touches the detector: an unworn or exhausted pair of goggles costs the scan
     * nothing, because the numbers in the action bar are the mod's actual output and the
     * highlight is a convenience on top of them.
     */
    private static void visualise(Level level, Player player, BlockPos clicked, int reach,
                                  Map<String, List<BlockPos>> hits, Map<String, Integer> paidFor) {
        if (hits.isEmpty() || !(player instanceof ServerPlayer server)) {
            return;
        }
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!head.is(ModItems.GOGGLES)) {
            return;
        }

        long now = level.getGameTime();
        Strain strain = Strain.of(head);
        if (strain.isBlockedAt(now)) {
            // Refused, but NOT punished again. The Nausea is applied once on the way over the
            // ceiling; re-applying it here would nauseate a player who only wanted the count in
            // the action bar, which the goggles do not gate and never should.
            actionBar(server, Component.translatable("hud." + Constants.MOD_ID + ".strained")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        Vec3 eye = player.getEyePosition();
        List<ScanHighlightPayload.Group> groups = new ArrayList<>();
        for (Map.Entry<String, List<BlockPos>> entry : hits.entrySet()) {
            int shown = Math.min(paidFor.getOrDefault(entry.getKey(), 0),
                    ScanHighlightPayload.MAX_POSITIONS);
            if (shown <= 0) {
                continue;
            }
            List<BlockPos> positions = new ArrayList<>(entry.getValue());
            if (positions.size() > shown) {
                // Nearest to the player, so a partial charge resolves the ore at your feet rather
                // than an arbitrary slice of the beam.
                positions.sort(Comparator.comparingDouble(pos -> Vec3.atCenterOf(pos).distanceToSqr(eye)));
                positions = positions.subList(0, shown);
            }
            groups.add(new ScanHighlightPayload.Group(entry.getKey(), List.copyOf(positions)));
        }
        if (groups.isEmpty()) {
            return;
        }

        ModNetworking.send(server, new ScanHighlightPayload(
                clicked, reach + CANCEL_MARGIN, HIGHLIGHT_TICKS, List.copyOf(groups)));

        Strain after = strain.plusScan(now);
        Strain.set(head, after);
        // Crossing the ceiling is the moment that costs something. This scan is still shown; the
        // next one is what gets refused.
        int nausea = Strain.nauseaTicks();
        if (nausea > 0 && after.isBlockedAt(now)) {
            server.addEffect(new MobEffectInstance(MobEffects.NAUSEA, nausea, 0));
        }
    }

    /**
     * Goggles wear by one per ore block reported, and only while actually worn. They are the pool
     * that scales with how much the scan found, now that the detector's own wear is flat.
     */
    private static void wearGoggles(Player player, int oreFound) {
        if (oreFound <= 0) {
            return;
        }
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        if (head.is(ModItems.GOGGLES)) {
            head.hurtAndBreak(oreFound, player, EquipmentSlot.HEAD);
        }
    }

    /**
     * Outlines what the scan just swept, for the player who swept it.
     *
     * <p>Replaces an earlier particle version that did not work, and the reason is worth keeping:
     * <b>particles are depth tested, and the beam runs INTO the surface</b>, so every particle past
     * the entry face was inside solid rock and invisible. Drawing the cross-section on the face
     * instead was visible but showed only width, and in practice the small gusts were so faint that
     * all anyone saw was the single burst on a hit, which read as "it only appears when it finds
     * something".
     *
     * <p>A gizmo has none of those problems: {@code setAlwaysOnTop} makes vanilla clear the depth
     * buffer for the pass, so the whole box draws through terrain and shows depth AND width at
     * once. Stroke only, since a filled box tens of blocks long would white out the screen.
     *
     * <p>Sent on EVERY scan, hit or miss, and to everyone, goggles or not. It reveals nothing
     * about ore, only the box the player themselves chose to point at.
     */
    private static void showVolume(Player player, BlockPos clicked, Direction into, int reach,
                                   int radius) {
        if (player instanceof ServerPlayer server) {
            ModNetworking.send(server, new ScanVolumePayload(
                    clicked.immutable(), into, reach, radius, VOLUME_TICKS));
        }
    }

    /**
     * The mining bonus a successful scan grants, if the player is wearing the goggles.
     *
     * <p>Deliberately NOT a standalone item or an upgrade. It rides on the goggles because they
     * already cost durability, liquid and strain to use, so the reward is self limiting: it only
     * arrives after a scan that was paid for, and only for as long as the highlight it belongs to.
     *
     * <p><b>Stronger on a FOCUSED tank.</b> Haste II on a single loaded liquid, Haste I on two or
     * more. The focused build already only wins on depth, and its total scanned volume is actually
     * the smallest of the three, so this gives it a second reason to exist that is not reach.
     *
     * <p>Gated on the same strain the highlight is, so strain has exactly one meaning: the goggles
     * have had enough. A version that kept granting Haste while refusing to draw would leave the
     * goggles half working in a state nobody could reason about.
     *
     * <p>Only on a scan that FOUND something, or it would reward clicking at bare walls, which is
     * the one case where the bonus has nothing to be spent on.
     */
    private static void rewardScan(Player player, boolean foundAnything, int types) {
        if (!foundAnything || OreDetectorConfig.hasteSeconds <= 0) {
            return;
        }
        if (!player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.GOGGLES)) {
            return;
        }
        ItemStack goggles = player.getItemBySlot(EquipmentSlot.HEAD);
        if (Strain.of(goggles).isBlockedAt(player.level().getGameTime())) {
            return;
        }
        int amplifier = types <= 1 ? 1 : 0;
        player.addEffect(new MobEffectInstance(MobEffects.HASTE,
                OreDetectorConfig.hasteSeconds * 20, amplifier, true, true, true));
    }

    private static BlockPos offset(BlockPos origin, Direction into, int depth, int a, int b) {
        BlockPos base = origin.relative(into, depth);
        return switch (into.getAxis()) {
            case Y -> base.offset(a, 0, b);
            case X -> base.offset(0, a, b);
            case Z -> base.offset(a, b, 0);
        };
    }

    /**
     * Says what the scan actually resolved.
     *
     * <p><b>This reports what the liquid PAID FOR, not everything the beam touched, and that
     * reverses an earlier decision deliberately.</b> Reporting the full count while charging for
     * part of it was defensible when the two differed only at the moment a tank ran out. It stopped
     * being defensible once the cost per block became tiered: a charge below one block's cost paid
     * for nothing, so the detector reported every block it saw for free, forever. Paying for what
     * you are told is the only version of this that cannot be farmed.
     *
     * @param found   blocks per ore that the charge actually covered
     * @param ranDry  ores whose charge this scan exhausted
     */
    private static void report(Player player, Map<String, Integer> paid,
                               java.util.List<String> ranDry) {
        // Copied rather than filtered in place: this map is the caller's, and visualise() reads it.
        // It happens to run first today, which is exactly the kind of thing that stops being true.
        Map<String, Integer> found = new LinkedHashMap<>();
        paid.forEach((ore, count) -> {
            if (count > 0) {
                found.put(ore, count);
            }
        });
        if (found.isEmpty()) {
            if (player instanceof ServerPlayer server && !ranDry.isEmpty()) {
                actionBar(server, dryMessage(ranDry));
                playBeep(player, false);
                return;
            }
            if (player instanceof ServerPlayer server) {
                actionBar(server, Component.translatable("hud." + Constants.MOD_ID + ".none_multi")
                        .withStyle(ChatFormatting.GRAY));
            }
            playBeep(player, false);
            return;
        }
        // Built on an empty, uncoloured root so the separators stay ordinary text. Appending to a
        // coloured component would tint the commas with the first ore's colour.
        net.minecraft.network.chat.MutableComponent message = Component.empty();
        boolean first = true;
        for (Map.Entry<String, Integer> entry : found.entrySet()) {
            if (!first) {
                message.append(Component.literal(", "));
            }
            message.append(Component.literal(entry.getValue() + " ")
                    .append(OreLookup.displayName(entry.getKey()))
                    .withColor(OreLookup.colorOf(entry.getKey())));
            first = false;
        }
        if (player instanceof ServerPlayer server) {
            net.minecraft.network.chat.MutableComponent line = Component.translatable(
                    "hud." + Constants.MOD_ID + ".found_multi", message);
            if (!ranDry.isEmpty()) {
                line.append(Component.literal(" ")).append(dryMessage(ranDry));
            }
            actionBar(server, line);
        }
        playBeep(player, true);
    }

    /** "Iron ran dry", naming the ores this scan used the last of. */
    private static Component dryMessage(java.util.List<String> ranDry) {
        net.minecraft.network.chat.MutableComponent names = Component.empty();
        for (int i = 0; i < ranDry.size(); i++) {
            if (i > 0) {
                names.append(Component.literal(", "));
            }
            names.append(OreLookup.displayName(ranDry.get(i))
                    .copy().withColor(OreLookup.colorOf(ranDry.get(i))));
        }
        return Component.translatable("hud." + Constants.MOD_ID + ".ran_dry", names)
                .withStyle(ChatFormatting.GRAY);
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
            // No rate shown: a millibucket is one ore for every material, so the number IS the
            // count of finds left for that ore.
            adder.accept(Component.literal(" ")
                    .append(OreLookup.displayName(entry.getKey()))
                    .append(Component.literal(": " + entry.getValue()))
                    .withColor(OreLookup.colorOf(entry.getKey())));
        }
        int width = columnRadius(types) * 2 + 1;
        adder.accept(Component.translatable("tooltip." + Constants.MOD_ID + ".range",
                downReach(types), sideReach(types), width, width)
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
