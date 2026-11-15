package com.fiskmods.heroes.common.item;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.data.var.Vars;
import com.fiskmods.heroes.common.hero.HeroTracker;
import com.fiskmods.heroes.common.hero.ability.AbilityHandler;
import com.fiskmods.heroes.common.util.ProtectedBlockChanges;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
    // Heartbeats reuse the existing gun-fire packet. A lost release must not leave a beam
    // running indefinitely; no client packet directly executes a ray or controls its timing.
    private static final int REQUEST_TIMEOUT = 20;
    private static final Map<UUID, BeamRequest> REQUESTS = new HashMap<>();

    private static final class BeamRequest
    {
        final ItemStack stack;
        long expires;
        long lastTick = Long.MIN_VALUE;
        long nextSound;

        BeamRequest(ItemStack stack, long expires)
        {
            this.stack = stack;
            this.expires = expires;
        }
    }

    public ItemHeatGun(int magazineSize, int cooldownTicks, int reloadTicks, double range, float damage, Properties properties)
    {
        super(magazineSize, cooldownTicks, reloadTicks, range, damage, false, properties);
    }

    /** Arm/refresh a held attack; the server tick is the only place that casts the beam. */
    @Override
    public boolean fireFromAttack(Player player, ItemStack stack)
    {
        if (!(player instanceof ServerPlayer serverPlayer) || player.level().isClientSide
                || !player.isAlive() || player.isSpectator() || stack.isEmpty()
                || stack.getItem() != this || player.getMainHandItem() != stack) return false;
        HeroTracker.update(player);
        var hero = HeroTracker.getHeroType(player);
        if (hero == null || SHDataCapabilities.getPlayer(player) == null)
        {
            showUseNotice(player, "message.fiskheroes.gun.requires_suit");
            return false;
        }
        if (!hasPermission(player, hero))
        {
            showUseNotice(player, "message.fiskheroes.gun.not_permitted");
            return false;
        }
        if (!AbilityHandler.isKeyPressed(serverPlayer, "AIM")) return false;
        long expires = player.level().getGameTime() + REQUEST_TIMEOUT;
        BeamRequest request = REQUESTS.get(player.getUUID());
        if (request == null || request.stack != stack)
            REQUESTS.put(player.getUUID(), new BeamRequest(stack, expires));
        else request.expires = expires;
        return true;
    }

    /** Called once per server player tick after aim and suit state have been updated. */
    public static void tickBeam(ServerPlayer player)
    {
        BeamRequest request = REQUESTS.get(player.getUUID());
        if (request == null) return;
        long now = player.level().getGameTime();
        var data = SHDataCapabilities.getPlayer(player);
        var hero = HeroTracker.getHeroType(player);
        if (!player.isAlive() || player.isSpectator() || now >= request.expires
                || player.getMainHandItem() != request.stack
                || !(request.stack.getItem() instanceof ItemHeatGun gun)
                || hero == null || data == null || !gun.hasPermission(player, hero)
                || !AbilityHandler.isKeyPressed(player, "AIM") || !data.getData().get(Vars.AIMING))
        {
            clearBeam(player);
            return;
        }
        if (data.getData().get(Vars.AIMING_TIMER) < 1.0F || request.lastTick == now) return;
        request.lastTick = now;
        gun.fire(player, request.stack);
        if (now >= request.nextSound)
        {
            gun.playShotSound(player, player.level(), request.stack);
            request.nextSound = now + 20;
        }
    }

    public static void clearBeam(Player player)
    {
        REQUESTS.remove(player.getUUID());
    }

    @Override
    protected void onRayBlockHit(ServerPlayer shooter, BlockHitResult block)
    {
        applyBlockHit(shooter, block, com.fiskmods.heroes.common.config.SHConfig.ALLOW_GRIEFING.get());
    }

    private static void applyBlockHit(ServerPlayer shooter, BlockHitResult block, boolean allowGriefing)
    {
        if (block.getType() == HitResult.Type.MISS) return;
        Level level = shooter.level();
        BlockPos pos = block.getBlockPos();
        if (!allowGriefing || !level.getWorldBorder().isWithinBounds(pos) || !level.mayInteract(shooter, pos)
                || !shooter.mayUseItemAt(pos, block.getDirection(), shooter.getMainHandItem())) return;
        BlockState state = level.getBlockState(pos);
        if (state.is(Blocks.ICE) || state.is(Blocks.FROSTED_ICE))
        {
            if (ProtectedBlockChanges.canBreak(shooter, pos, state))
                level.setBlockAndUpdate(pos, level.dimensionType().ultraWarm() ? Blocks.AIR.defaultBlockState() : Blocks.WATER.defaultBlockState());
            return;
        }
        if (state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.POWDER_SNOW))
        {
            if (ProtectedBlockChanges.canBreak(shooter, pos, state)) level.removeBlock(pos, false);
            return;
        }
        BlockPos adj = pos.relative(block.getDirection());
        if (state.isFlammable(level, pos, block.getDirection())
                && level.getWorldBorder().isWithinBounds(adj) && level.mayInteract(shooter, adj)
                && shooter.mayUseItemAt(adj, block.getDirection(), shooter.getMainHandItem())
                && level.isEmptyBlock(adj) && BaseFireBlock.canBePlacedAt(level, adj, block.getDirection()))
        {
            ProtectedBlockChanges.placeBlock(shooter, adj, block.getDirection(), BaseFireBlock.getState(level, adj));
        }
    }

    @Override
    protected void onRayEntityContact(ServerPlayer shooter, LivingEntity hit, boolean acceptedHit)
    {
        // Contact transfers heat even when the target rejects the damage.
        com.fiskmods.heroes.common.hero.modifier.MetalSkinHeat.add(hit,
                1.0F / com.fiskmods.heroes.common.hero.modifier.MetalSkinHeat.HEAT_GUN_TICKS);
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
