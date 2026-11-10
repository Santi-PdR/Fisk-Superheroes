package com.fiskmods.heroes.common.item;

import java.util.List;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.entity.projectile.EquipmentProjectileEntity;
import com.fiskmods.heroes.common.sound.SHSounds;
import com.google.gson.JsonObject;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Captain Cold's Cold Gun ported from 1.7.10 ItemColdGun:
 * toggles between Cold Beam and Cryo Grenade modes (Sneak + Right-Click), freezes blocks/targets,
 * and uses 3D ModelColdGun + glowing lights layer.
 */
public final class ItemColdGun extends ItemGun
{
    private static final String TAG_GRENADE_MODE = "GrenadeMode";

    public ItemColdGun(int magazineSize, int cooldownTicks, int reloadTicks, double range, float damage, Properties properties)
    {
        super(magazineSize, cooldownTicks, reloadTicks, range, damage, false, properties);
    }

    public static boolean isGrenadeMode(ItemStack stack)
    {
        return stack.hasTag() && stack.getTag().getBoolean(TAG_GRENADE_MODE);
    }

    @Override
    public int getShotCooldownTicks(ItemStack stack)
    {
        return isGrenadeMode(stack) ? 24 : cooldownTicks;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown())
        {
            if (!level.isClientSide)
            {
                boolean next = !isGrenadeMode(stack);
                stack.getOrCreateTag().putBoolean(TAG_GRENADE_MODE, next);
                SHSounds.play(player, FiskHeroes.id("item.coldgun.aim"), SoundSource.PLAYERS, 1.0F, 1.0F);
                Component modeName = Component.translatable(next ? "tooltip.cold_gun.mode.grenade" : "tooltip.cold_gun.mode.beam");
                player.displayClientMessage(Component.translatable("tooltip.mode", modeName), true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return super.use(level, player, hand);
    }

    @Override
    protected void playShotSound(Player player, Level level, ItemStack stack)
    {
        if (isGrenadeMode(stack))
        {
            SHSounds.play(player, FiskHeroes.id("item.coldgun.grenade"), SoundSource.PLAYERS, 2.0F, 1.0F);
        }
        else
        {
            SHSounds.play(player, FiskHeroes.id("item.coldgun.beam.start"), SoundSource.PLAYERS, 2.0F, 1.0F);
        }
    }

    @Override
    protected void fire(ServerPlayer shooter, ItemStack stack)
    {
        if (isGrenadeMode(stack))
        {
            EquipmentProjectileEntity grenade = new EquipmentProjectileEntity(
                    shooter, "fiskheroes:freeze_grenade", new JsonObject(),
                    new ItemStack(ModItems.FREEZE_GRENADE.get()), 0.0F);
            shooter.level().addFreshEntity(grenade);
            return;
        }
        super.fire(shooter, stack);
    }

    @Override
    protected void onRayBlockHit(ServerPlayer shooter, BlockHitResult block)
    {
        if (block.getType() == HitResult.Type.MISS) return;
        Level level = shooter.level();
        BlockPos pos = block.getBlockPos();
        BlockState hitState = level.getBlockState(pos);
        if (hitState.is(Blocks.WATER))
        {
            level.setBlockAndUpdate(pos, Blocks.ICE.defaultBlockState());
            return;
        }
        if (hitState.is(Blocks.LAVA))
        {
            level.setBlockAndUpdate(pos, Blocks.OBSIDIAN.defaultBlockState());
            return;
        }
        BlockPos adj = pos.relative(block.getDirection());
        if (level.getBlockState(adj).is(Blocks.FIRE))
        {
            level.removeBlock(adj, false);
        }
        else if (level.isEmptyBlock(adj) && Blocks.SNOW.defaultBlockState().canSurvive(level, adj))
        {
            level.setBlockAndUpdate(adj, Blocks.SNOW.defaultBlockState());
        }
    }

    @Override
    protected void onRayEntityHit(ServerPlayer shooter, LivingEntity hit)
    {
        hit.clearFire();
        hit.setTicksFrozen(Math.min(hit.getTicksFrozen() + 140, hit.getTicksRequiredToFreeze() + 40));
        hit.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2));
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag)
    {
        super.appendHoverText(stack, level, tooltip, flag);
        Component modeName = Component.translatable(isGrenadeMode(stack)
                ? "tooltip.cold_gun.mode.grenade"
                : "tooltip.cold_gun.mode.beam").withStyle(ChatFormatting.AQUA);
        tooltip.add(Component.translatable("tooltip.mode", modeName).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer)
    {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions()
        {
            @Override
            public boolean applyForgeHandTransform(com.mojang.blaze3d.vertex.PoseStack pose,
                    net.minecraft.client.player.LocalPlayer player, net.minecraft.world.entity.HumanoidArm arm,
                    ItemStack stack, float partialTick, float equip, float swing)
            {
                com.fiskmods.heroes.client.render.ColdGunAimPose.apply(pose, player, arm, partialTick);
                return false;
            }

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer()
            {
                return com.fiskmods.heroes.client.render.ColdGunItemRenderer.getInstance();
            }
        });
    }
}
