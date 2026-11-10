package com.fiskmods.heroes.common.item;

import java.util.List;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.hero.Hero;
import com.fiskmods.heroes.common.hero.HeroIteration;
import com.fiskmods.heroes.common.hero.HeroTracker;
import com.fiskmods.heroes.common.sound.SHSounds;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * Beretta 93R ported from 1.7.10 ItemBeretta93R:
 * supports Single Shot & 3-Shot Burst fire modes (toggle via Sneak + Right-Click),
 * dual-wielding, mode tooltip, and 3D ModelBeretta93R renderer.
 */
public final class ItemBeretta93R extends ItemGun
{
    private static final String TAG_BURST_MODE = "BurstMode";
    private static final String TAG_BURST_REMAINING = "BurstRemaining";
    private static final String TAG_BURST_NEXT_TICK = "BurstNextTick";

    public ItemBeretta93R(int magazineSize, int cooldownTicks, int reloadTicks, double range, float damage, Properties properties)
    {
        super(magazineSize, cooldownTicks, reloadTicks, range, damage, properties);
    }

    public static boolean isBurstMode(ItemStack stack)
    {
        return !stack.hasTag() || !stack.getTag().contains(TAG_BURST_MODE) || stack.getTag().getBoolean(TAG_BURST_MODE);
    }

    @Override
    public int getShotCooldownTicks(ItemStack stack)
    {
        int base = isBurstMode(stack) ? cooldownTicks : Math.max(4, cooldownTicks / 3);
        return isDual(stack) ? Math.round(base * 1.2F) : base;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown())
        {
            if (!level.isClientSide)
            {
                boolean next = !isBurstMode(stack);
                stack.getOrCreateTag().putBoolean(TAG_BURST_MODE, next);
                stack.getOrCreateTag().putInt(TAG_BURST_REMAINING, 0);
                SHSounds.play(player, FiskHeroes.id("item.gun.beretta.switch"), SoundSource.PLAYERS, 1.0F, 1.0F);
                Component modeName = Component.translatable(next ? "tooltip.beretta_93r.mode.burst" : "tooltip.beretta_93r.mode.single");
                player.displayClientMessage(Component.translatable("tooltip.mode", modeName), true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return super.use(level, player, hand);
    }

    @Override
    public boolean fireFromAttack(Player player, ItemStack stack)
    {
        boolean fired = super.fireFromAttack(player, stack);
        if (fired && isBurstMode(stack) && getAmmo(stack) > 0)
        {
            stack.getOrCreateTag().putInt(TAG_BURST_REMAINING, Math.min(2, getAmmo(stack)));
            stack.getOrCreateTag().putLong(TAG_BURST_NEXT_TICK, player.level().getGameTime() + 3L);
        }
        return fired;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected)
    {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (level.isClientSide || !(entity instanceof Player player) || !stack.hasTag()) return;

        int remaining = stack.getTag().getInt(TAG_BURST_REMAINING);
        if (remaining <= 0) return;

        if (!isSelected && player.getMainHandItem() != stack)
        {
            stack.getTag().putInt(TAG_BURST_REMAINING, 0);
            return;
        }

        long now = level.getGameTime();
        if (now < stack.getTag().getLong(TAG_BURST_NEXT_TICK)) return;

        HeroIteration iter = HeroTracker.getHero(player);
        Hero hero = iter != null ? iter.getHero() : null;
        var data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(player);
        if (hero == null || data == null || data.getData().get(com.fiskmods.heroes.common.data.var.Vars.RELOAD_TIMER) > 0.0F
                || !hasPermission(player, hero) || getAmmo(stack) <= 0)
        {
            stack.getTag().putInt(TAG_BURST_REMAINING, 0);
            return;
        }

        if (triggerSingleShot(player, stack))
        {
            stack.getTag().putInt(TAG_BURST_REMAINING, remaining - 1);
            stack.getTag().putLong(TAG_BURST_NEXT_TICK, now + 3L);
        }
        else
        {
            stack.getTag().putInt(TAG_BURST_REMAINING, 0);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag)
    {
        super.appendHoverText(stack, level, tooltip, flag);
        Component modeName = Component.translatable(isBurstMode(stack)
                ? "tooltip.beretta_93r.mode.burst"
                : "tooltip.beretta_93r.mode.single").withStyle(ChatFormatting.YELLOW);
        tooltip.add(Component.translatable("tooltip.mode", modeName).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer)
    {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions()
        {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer()
            {
                return com.fiskmods.heroes.client.render.Beretta93RItemRenderer.getInstance();
            }
        });
    }
}
