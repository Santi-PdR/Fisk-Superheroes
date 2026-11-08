package com.fiskmods.heroes.common.hero.ability;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.data.PlayerInputTracker;
import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.data.var.Vars;
import com.fiskmods.heroes.common.hero.modifier.ModEffects;
import com.fiskmods.heroes.common.hero.modifier.ModifierControlledFlight;
import com.fiskmods.heroes.common.hero.power.PowerProperty;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Movement hook for enabled suit flight and gliding, before vanilla travel applies gravity. */
public final class SuitTravel
{
    private SuitTravel() { }

    public static boolean travel(LivingEntity entity)
    {
        return controlledFlight(entity) || GlidingTravel.travel(entity);
    }

    public static boolean controlledFlight(LivingEntity entity)
    {
        if (!(entity instanceof Player player) || entity.level().isClientSide && !player.isLocalPlayer()
                || player.isPassenger() || player.getAbilities().flying || player.isFallFlying()
                || player.hasEffect(ModEffects.TUTRIDIUM.get())) return false;
        var data = SHDataCapabilities.getPlayer(player);
        var hero = data != null ? data.getHeroType() : null;
        if (hero == null || !data.getData().get(Vars.FLYING)) return false;
        var entry = hero.getPowerContainer().getEntries().stream()
                .filter(candidate -> candidate.getModifier() instanceof ModifierControlledFlight
                        && candidate.isEnabled() && candidate.isModifierEnabled(player, data))
                .findFirst().orElse(null);
        if (entry == null) return false;

        boolean boost = entry.getBoolean(player, PowerProperty.CAN_BOOST) && player.isSprinting();
        float speed = Math.max(0.0F, entry.getFloat(player, PowerProperty.SPEED));
        float boostSpeed = Math.max(0.0F, entry.getFloat(player, PowerProperty.BOOST_SPEED));
        Vec3 motion = ModifierControlledFlight.controlledMotion(player.getDeltaMovement(), player.getLookAngle(),
                player.getYRot(), PlayerInputTracker.forward(player), PlayerInputTracker.strafe(player),
                PlayerInputTracker.isJumping(player), PlayerInputTracker.isSneaking(player), boost, speed, boostSpeed);
        player.setDeltaMovement(motion);
        player.move(MoverType.SELF, motion);
        player.fallDistance = 0.0F;
        return true;
    }
}
