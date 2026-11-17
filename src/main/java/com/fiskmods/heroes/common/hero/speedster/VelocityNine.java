package com.fiskmods.heroes.common.hero.speedster;

import java.util.Map;
import java.util.WeakHashMap;

import com.fiskmods.heroes.common.hero.modifier.ModEffects;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Server-side Velocity 9 dosing and natural expiration, based on the original status effects. */
@Mod.EventBusSubscriber(modid = com.fiskmods.heroes.FiskHeroes.MODID)
public final class VelocityNine
{
    private static final Map<LivingEntity, Integer> EXPIRED = new WeakHashMap<>();
    private static final Map<net.minecraft.world.entity.player.Player, Boolean> AUTOMATIC_SPEED = new WeakHashMap<>();

    private VelocityNine() { }

    public static void dose(LivingEntity entity)
    {
        if (entity.level().isClientSide) return;
        var velocity = entity.getEffect(ModEffects.VELOCITY_NINE.get());
        var sickness = entity.getEffect(ModEffects.SPEED_SICKNESS.get());
        int amplifier = velocity != null ? velocity.getAmplifier() + 1 : sickness != null ? sickness.getAmplifier() + 1 : 0;
        amplifier = Math.min(amplifier, 255);
        int duration = 6000 / (amplifier + 1);
        // The original independently keeps the larger duration and amplifier.
        if (velocity != null)
        {
            duration = velocity.isInfiniteDuration() ? -1 : Math.max(duration, velocity.getDuration());
            entity.removeEffect(ModEffects.VELOCITY_NINE.get());
        }
        entity.removeEffect(ModEffects.SPEED_SICKNESS.get());
        EXPIRED.remove(entity);
        entity.addEffect(new MobEffectInstance(ModEffects.VELOCITY_NINE.get(), duration, amplifier, false, false, true));
        if (Boolean.getBoolean("fiskheroes.ciVelocityDiagnostics"))
            com.fiskmods.heroes.FiskHeroes.LOGGER.info("Velocity 9 dose: entity={}, amplifier={}, duration={}",
                    entity.getName().getString(), amplifier, duration);
    }

    /** The original automatically enables speed at full V9 levels when no suit supplies it. */
    public static void tickPlayer(net.minecraft.world.entity.player.Player player,
            com.fiskmods.heroes.common.data.SHPlayerData data)
    {
        if (player.level().isClientSide || data == null) return;
        var hero = data.getHeroType();
        boolean hasSpeedPower = hero != null && hero.getPowerContainer().getEntries().stream()
                .anyMatch(entry -> entry.getModifier() instanceof com.fiskmods.heroes.common.hero.modifier.ModifierSuperSpeed
                        && entry.isEnabled() && entry.isModifierEnabled(player, data));
        var velocity = player.getEffect(ModEffects.VELOCITY_NINE.get());
        if (hasSpeedPower)
        {
            AUTOMATIC_SPEED.remove(player);
        }
        else if (velocity != null && com.fiskmods.heroes.common.config.SHConfig.ALLOW_SUPER_SPEED.get())
        {
            boolean starting = AUTOMATIC_SPEED.put(player, Boolean.TRUE) == null;
            var vars = data.getData();
            int maximum = Math.min(127, 3 * (velocity.getAmplifier() + 1));
            vars.set(com.fiskmods.heroes.common.data.var.Vars.SPEED, (byte) maximum);
            vars.set(com.fiskmods.heroes.common.data.var.Vars.SPEEDING, true);
            if (starting && Boolean.getBoolean("fiskheroes.ciVelocityDiagnostics"))
                com.fiskmods.heroes.FiskHeroes.LOGGER.info("Velocity 9 automatic speed: entity={}, level={}",
                        player.getName().getString(), maximum);
            boolean moving = com.fiskmods.heroes.common.data.PlayerInputTracker.forward(player) != 0
                    || com.fiskmods.heroes.common.data.PlayerInputTracker.strafe(player) != 0;
            vars.set(com.fiskmods.heroes.common.data.var.Vars.MOVING, moving);
            vars.set(com.fiskmods.heroes.common.data.var.Vars.SPEED_SPRINTING, moving && player.isSprinting());
        }
        else if (AUTOMATIC_SPEED.remove(player) != null)
        {
            com.fiskmods.heroes.common.hero.modifier.ModifierSuperSpeed.disable(player);
        }
    }

    @SubscribeEvent
    public static void onExpired(MobEffectEvent.Expired event)
    {
        var entity = event.getEntity();
        var effect = event.getEffectInstance();
        if (!entity.level().isClientSide && effect != null && effect.getEffect() == ModEffects.VELOCITY_NINE.get())
            EXPIRED.put(entity, effect.getAmplifier());
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event)
    {
        var entity = event.getEntity();
        if (entity.level().isClientSide) return;
        Integer amplifier = EXPIRED.remove(entity);
        if (amplifier != null && entity.isAlive() && !entity.hasEffect(ModEffects.VELOCITY_NINE.get()))
            entity.addEffect(new MobEffectInstance(ModEffects.SPEED_SICKNESS.get(), -1, amplifier, false, false, true));
        if (entity.hasEffect(ModEffects.VELOCITY_NINE.get())) entity.removeEffect(ModEffects.SPEED_SICKNESS.get());
    }
}
