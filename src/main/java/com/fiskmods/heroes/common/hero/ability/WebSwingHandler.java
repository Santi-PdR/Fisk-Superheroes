package com.fiskmods.heroes.common.hero.ability;

import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.data.var.Vars;
import com.fiskmods.heroes.common.hero.HeroIteration;
import com.fiskmods.heroes.common.hero.HeroTracker;
import com.fiskmods.heroes.common.hero.modifier.AbilityData;
import com.fiskmods.heroes.common.hero.power.ModifierEntry;
import com.fiskmods.heroes.common.hero.power.PowerProperty;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Server-side block anchor and rope constraint for the pack's web-swing interaction. */
public final class WebSwingHandler
{
    private static final String ANCHOR_TAG = "FiskHeroesWebSwingAnchor";
    private static final String DIMENSION = "Dimension";
    private static final String X = "X";
    private static final String Y = "Y";
    private static final String Z = "Z";
    private static final String BLOCK = "Block";
    private static final String LENGTH = "Length";

    private WebSwingHandler()
    {
    }

    /** Starts a block grapple, or releases the current rope when clicked again. */
    public static boolean interact(ServerPlayer player)
    {
        if (!player.getMainHandItem().isEmpty()) return false;
        SHPlayerData data = SHDataCapabilities.getPlayer(player);
        HeroIteration iteration = HeroTracker.getHero(player);
        if (data == null || iteration == null || !data.getData().get(Vars.WEB_SWINGING)) return false;

        var persistent = player.getPersistentData();
        if (persistent.contains(ANCHOR_TAG))
        {
            release(player, data);
            return true;
        }

        ModifierEntry entry = findSwingModifier(iteration, player, data);
        if (entry == null) return false;

        float range = Math.max(1.0F, entry.getFloat(player, PowerProperty.RANGE));
        HitResult hit = player.pick(range, 0.0F, false);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) return false;

        BlockPos blockPos = blockHit.getBlockPos();
        BlockState state = player.level().getBlockState(blockPos);
        if (state.isAir() || state.getCollisionShape(player.level(), blockPos).isEmpty()) return false;

        Vec3 anchor = hit.getLocation();
        Vec3 center = player.position().add(0.0D, player.getBbHeight() * 0.5D, 0.0D);
        double ropeLength = Math.max(2.0D, center.distanceTo(anchor));
        var tag = new net.minecraft.nbt.CompoundTag();
        tag.putString(DIMENSION, player.level().dimension().location().toString());
        tag.putDouble(X, anchor.x);
        tag.putDouble(Y, anchor.y);
        tag.putDouble(Z, anchor.z);
        tag.putLong(BLOCK, blockPos.asLong());
        tag.putDouble(LENGTH, ropeLength);
        persistent.put(ANCHOR_TAG, tag);

        data.getData().set(Vars.WEB_RAPPEL, true);
        data.getData().set(Vars.WEB_ANCHOR_X, anchor.x);
        data.getData().set(Vars.WEB_ANCHOR_Y, anchor.y);
        data.getData().set(Vars.WEB_ANCHOR_Z, anchor.z);
        Vec3 towardAnchor = anchor.subtract(center).normalize();
        player.setDeltaMovement(player.getDeltaMovement().add(towardAnchor.scale(0.28D)).add(0.0D, 0.12D, 0.0D));
        player.hasImpulse = true;
        AbilityData.playSound(player, entry, "SHOOT");
        return true;
    }

    /** Applies a pendulum-like length constraint while the player remains attached. */
    public static void tick(ServerPlayer player)
    {
        var persistent = player.getPersistentData();
        if (!persistent.contains(ANCHOR_TAG)) return;

        SHPlayerData data = SHDataCapabilities.getPlayer(player);
        if (data == null || !data.getData().get(Vars.WEB_SWINGING) || player.isInWater()
                || !player.level().dimension().location().toString().equals(persistent.getCompound(ANCHOR_TAG).getString(DIMENSION)))
        {
            release(player, data);
            return;
        }

        var tag = persistent.getCompound(ANCHOR_TAG);
        BlockPos blockPos = BlockPos.of(tag.getLong(BLOCK));
        BlockState state = player.level().getBlockState(blockPos);
        if (state.isAir() || state.getCollisionShape(player.level(), blockPos).isEmpty())
        {
            release(player, data);
            return;
        }

        Vec3 anchor = new Vec3(tag.getDouble(X), tag.getDouble(Y), tag.getDouble(Z));
        Vec3 center = player.position().add(0.0D, player.getBbHeight() * 0.5D, 0.0D);
        Vec3 fromAnchor = center.subtract(anchor);
        double distance = fromAnchor.length();
        double ropeLength = Math.max(2.0D, tag.getDouble(LENGTH));
        if (distance <= ropeLength || distance < 1.0E-6D) return;

        // Remove only the outward velocity and correct excess rope length. Gravity and tangential
        // momentum remain, so the player swings around the fixed anchor instead of being pulled
        // straight into it on every tick.
        Vec3 radial = fromAnchor.scale(1.0D / distance);
        Vec3 velocity = player.getDeltaMovement();
        double outwardSpeed = velocity.dot(radial);
        if (outwardSpeed > 0.0D) velocity = velocity.subtract(radial.scale(outwardSpeed));
        double correction = Math.min(0.65D, 0.06D + (distance - ropeLength) * 0.22D);
        player.setDeltaMovement(velocity.subtract(radial.scale(correction)));
        player.hasImpulse = true;
        player.fallDistance = 0.0F;
    }

    public static void release(ServerPlayer player)
    {
        release(player, SHDataCapabilities.getPlayer(player));
    }

    private static void release(ServerPlayer player, SHPlayerData data)
    {
        player.getPersistentData().remove(ANCHOR_TAG);
        if (data != null)
        {
            data.getData().set(Vars.WEB_RAPPEL, false);
            data.getData().set(Vars.WEB_ANCHOR_X, 0.0D);
            data.getData().set(Vars.WEB_ANCHOR_Y, 0.0D);
            data.getData().set(Vars.WEB_ANCHOR_Z, 0.0D);
            data.getData().set(Vars.WEB_ROPE_ID, -1);
        }
    }

    private static ModifierEntry findSwingModifier(HeroIteration iteration, ServerPlayer player, SHPlayerData data)
    {
        return iteration.getHero().getPowerContainer().getEntries().stream()
                .filter(entry -> entry.getModifier().getId().getPath().equals("web_swinging"))
                .filter(ModifierEntry::isEnabled)
                .filter(entry -> entry.isModifierEnabled(player, data))
                .findFirst().orElse(null);
    }
}
