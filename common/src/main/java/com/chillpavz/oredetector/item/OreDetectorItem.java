package com.chillpavz.oredetector.item;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import com.chillpavz.oredetector.Constants;
import com.chillpavz.oredetector.config.OreDetectorConfig;
import com.chillpavz.oredetector.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.UseCooldown;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * Base detector item. Right-click a surface: it scans an N x N beam INTO that surface (opposite the
 * clicked face) for the target ore, reaching further downward than sideways/upward. Reach, column
 * size and cooldown come from {@link OreDetectorConfig}. Reports the count in the action bar, plays
 * a cue, and wears the tool down by 1 per use plus 1 per ore found.
 */
public class OreDetectorItem extends Item {

    private static final float SOUND_PITCH = 1.0f;

    /**
     * Placeholder duration for the cooldown-group component below. It is never read — the real
     * duration is passed to {@code addCooldown} from the live config — but it MUST be strictly
     * positive: {@code UseCooldown}'s codec validates the field with {@code POSITIVE_FLOAT}, and a
     * zero makes the whole ItemStack fail to encode. That breaks inventory saving ("Value must be
     * positive: 0.0") and disconnects the client when a creative-slot packet re-validates the
     * stack. One tick is the smallest legal value.
     */
    private static final float COOLDOWN_GROUP_MARKER_SECONDS = 0.05f;

    public OreDetectorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (!level.isClientSide()) {
            ItemStack stack = context.getItemInHand();
            if (player != null && player.getCooldowns().isOnCooldown(stack)) {
                return InteractionResult.FAIL;
            }

            Direction scanDir = context.getClickedFace().getOpposite();
            int found = scan(level, context.getClickedPos(), scanDir);

            SoundEvent sound = found > 0 ? ModSounds.FOUND : ModSounds.NOT_FOUND;
            if (OreDetectorConfig.soundVolume > 0.0) {
                level.playSound(null, context.getClickedPos(), sound, SoundSource.PLAYERS,
                        (float) OreDetectorConfig.soundVolume, SOUND_PITCH);
                // The beep is a real noise: emit a vibration so nearby sculk sensors react to it.
                // Muting the detector (volume 0) therefore also makes it sculk-safe.
                level.gameEvent(player, GameEvent.INSTRUMENT_PLAY, context.getClickedPos());
            }

            if (player instanceof ServerPlayer serverPlayer) {
                Component message = found > 0
                        ? Component.translatable("hud.oredetector.found", found, getOreName()).withColor(getOreColor())
                        : Component.translatable("hud.oredetector.none", getOreName()).withStyle(ChatFormatting.GRAY);
                serverPlayer.sendSystemMessage(message, true);
            }

            if (player != null) {
                applyCooldown(stack, player);
                // 1 for the scan itself, plus 1 per ore found.
                stack.hurtAndBreak(1 + found, player, context.getHand());
            }
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * Puts THIS detector on cooldown rather than every detector of the same type. Vanilla keys
     * cooldowns by a "cooldown group" that defaults to the item's registry id, so a spare iron
     * detector anywhere in the inventory would otherwise share one timer with the one being used.
     * Stamping each stack with its own group the first time it is used makes the cooldown per-item
     * while keeping vanilla's cooldown sweep on the icon. Detectors are damageable and therefore
     * never stack, so a per-stack component costs nothing.
     */
    private static void applyCooldown(ItemStack stack, Player player) {
        UseCooldown cooldown = stack.get(DataComponents.USE_COOLDOWN);
        if (cooldown == null || cooldown.cooldownGroup().isEmpty()) {
            Identifier group = Identifier.fromNamespaceAndPath(Constants.MOD_ID,
                    "cooldown/" + UUID.randomUUID().toString().replace("-", ""));
            stack.set(DataComponents.USE_COOLDOWN,
                    new UseCooldown(COOLDOWN_GROUP_MARKER_SECONDS, Optional.of(group)));
        }
        player.getCooldowns().addCooldown(stack, OreDetectorConfig.cooldownTicks);
    }

    /** Counts matching ore in an N x N beam that starts at {@code origin} and extends along {@code dir}. */
    private int scan(Level level, BlockPos origin, Direction dir) {
        int depth = dir == Direction.DOWN ? OreDetectorConfig.downReach : OreDetectorConfig.sideReach;
        int radius = OreDetectorConfig.columnRadius;   // 0 -> 1x1, 1 -> 3x3, ... 3 -> 7x7

        // Two axes perpendicular to the scan direction, used for the (2r+1) x (2r+1) cross-section.
        int[] axisU;
        int[] axisV;
        switch (dir.getAxis()) {
            case Y -> { axisU = new int[]{1, 0, 0}; axisV = new int[]{0, 0, 1}; }
            case X -> { axisU = new int[]{0, 1, 0}; axisV = new int[]{0, 0, 1}; }
            default -> { axisU = new int[]{1, 0, 0}; axisV = new int[]{0, 1, 0}; }
        }
        int stepX = dir.getStepX();
        int stepY = dir.getStepY();
        int stepZ = dir.getStepZ();

        int count = 0;
        for (int u = -radius; u <= radius; u++) {
            for (int v = -radius; v <= radius; v++) {
                for (int d = 0; d < depth; d++) {
                    BlockPos pos = origin.offset(
                            u * axisU[0] + v * axisV[0] + d * stepX,
                            u * axisU[1] + v * axisV[1] + d * stepY,
                            u * axisU[2] + v * axisV[2] + d * stepZ);
                    if (isValidBlock(level.getBlockState(pos))) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> adder, TooltipFlag flag) {
        adder.accept(Component.translatable("tooltip.oredetector.usage", getOreName()).withStyle(ChatFormatting.GRAY));
        adder.accept(Component.translatable("tooltip.oredetector.range", OreDetectorConfig.downReach, OreDetectorConfig.sideReach)
                .withStyle(ChatFormatting.DARK_GRAY));
        super.appendHoverText(stack, context, display, adder, flag);
    }

    /** The block(s) this detector reacts to. */
    public boolean isValidBlock(BlockState state) {
        return false;
    }

    /** Display name of the ore this detector looks for, used in messages and tooltips. */
    protected Component getOreName() {
        return Component.translatable("ore.oredetector.unknown");
    }

    /** RGB color used to tint the "found" message, chosen to resemble the ore. */
    protected int getOreColor() {
        return 0xFFFFFF;
    }
}
