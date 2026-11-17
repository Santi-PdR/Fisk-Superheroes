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

    @SubscribeEvent
    public static void onAdded(MobEffectEvent.Added event)
    {
        if (event.getEntity().level().isClientSide && Boolean.getBoolean("fiskheroes.ciVelocityDiagnostics")
                && event.getEffectInstance().getEffect() == ModEffects.VELOCITY_NINE.get())
            com.fiskmods.heroes.FiskHeroes.LOGGER.info("Velocity 9 synchronized: entity={}, amplifier={}",
                    event.getEntity().getName().getString(), event.getEffectInstance().getAmplifier());
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
