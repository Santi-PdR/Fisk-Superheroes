package com.fiskmods.heroes.common.item;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Heatwave's Heat Gun ported from 1.7.10 ItemHeatGun:
 * melts ice/snow, ignites flammable blocks & entities, heats MetalSkin, and uses 3D ModelHeatGun.
 */
public final class ItemHeatGun extends ItemGun
{
    public ItemHeatGun(int magazineSize, int cooldownTicks, int reloadTicks, double range, float damage, Properties properties)
    {
        super(magazineSize, cooldownTicks, reloadTicks, range, damage, false, properties);
    }

    @Override
    protected void onRayBlockHit(ServerPlayer shooter, BlockHitResult block)
    {
        if (block.getType() == HitResult.Type.MISS) return;
        Level level = shooter.level();
        BlockPos pos = block.getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (state.is(Blocks.ICE) || state.is(Blocks.FROSTED_ICE))
        {
            level.setBlockAndUpdate(pos, level.dimensionType().ultraWarm() ? Blocks.AIR.defaultBlockState() : Blocks.WATER.defaultBlockState());
            return;
        }
        if (state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.POWDER_SNOW))
        {
            level.removeBlock(pos, false);
            return;
        }
        BlockPos adj = pos.relative(block.getDirection());
        if (state.isFlammable(level, pos, block.getDirection())
                && level.isEmptyBlock(adj) && BaseFireBlock.canBePlacedAt(level, adj, block.getDirection()))
        {
            level.setBlockAndUpdate(adj, BaseFireBlock.getState(level, adj));
        }
    }

    @Override
    protected void onRayEntityContact(ServerPlayer shooter, LivingEntity hit, boolean acceptedHit)
    {
        // The old continuous beam applied 1/170 per tick. This port fires a ray at its
        // configured cadence, so one contact represents that many beam ticks.
        int elapsedTicks = Math.max(1, getShotCooldownTicks(shooter.getMainHandItem()));
        com.fiskmods.heroes.common.hero.modifier.MetalSkinHeat.add(hit,
                (float) elapsedTicks / com.fiskmods.heroes.common.hero.modifier.MetalSkinHeat.HEAT_GUN_TICKS);
    }

    @Override
    protected void onRayEntityHit(ServerPlayer shooter, LivingEntity hit)
    {
        hit.setSecondsOnFire(5);
    }

    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer)
    {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions()
        {
            @Override
            public boolean applyForgeHandTransform(com.mojang.blaze3d.vertex.PoseStack pose,
                    net.minecraft.client.player.LocalPlayer player, net.minecraft.world.entity.HumanoidArm arm,
                    net.minecraft.world.item.ItemStack stack, float partialTick, float equip, float swing)
            {
                com.fiskmods.heroes.client.render.HeatGunAimPose.apply(pose, player, arm, partialTick);
                return false;
            }

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer()
            {
                return com.fiskmods.heroes.client.render.HeatGunItemRenderer.getInstance();
            }
        });
    }
}
