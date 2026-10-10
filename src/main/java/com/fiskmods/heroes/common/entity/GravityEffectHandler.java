package com.fiskmods.heroes.common.entity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Applies the short-lived gravity factors created by gravity manipulation waves. */
public final class GravityEffectHandler
{
    private static final int DURATION_TICKS = 16;
    private static final double NORMAL_GRAVITY = 0.08D;
    private static final Map<ServerLevel, Map<UUID, Effect>> EFFECTS = new WeakHashMap<>();

    private GravityEffectHandler()
    {
    }

    public static void setGravity(ServerLevel level, Entity entity, float factor)
    {
        EFFECTS.computeIfAbsent(level, ignored -> new HashMap<>())
                .put(entity.getUUID(), new Effect(factor, DURATION_TICKS));
    }

    public static void tick(ServerLevel level)
    {
        Map<UUID, Effect> effects = EFFECTS.get(level);
        if (effects == null) return;

        Iterator<Map.Entry<UUID, Effect>> iterator = effects.entrySet().iterator();
        while (iterator.hasNext())
        {
            Map.Entry<UUID, Effect> entry = iterator.next();
            Entity entity = level.getEntity(entry.getKey());
            Effect effect = entry.getValue();
            if (entity == null || !entity.isAlive() || entity.isNoGravity())
            {
                iterator.remove();
                continue;
            }

            Vec3 motion = entity.getDeltaMovement();
            entity.setDeltaMovement(motion.x, motion.y - NORMAL_GRAVITY * (effect.factor - 1.0F), motion.z);
            entity.hurtMarked = true;

            if (--effect.ticks <= 0) iterator.remove();
        }

        if (effects.isEmpty()) EFFECTS.remove(level);
    }

    public static void clear(ServerLevel level)
    {
        EFFECTS.remove(level);
    }

    private static final class Effect
    {
        private final float factor;
        private int ticks;

        private Effect(float factor, int ticks)
        {
            this.factor = factor;
            this.ticks = ticks;
        }
    }
}
