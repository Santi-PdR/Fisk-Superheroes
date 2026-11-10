package com.fiskmods.heroes.client.render;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import com.fiskmods.heroes.FiskHeroes;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** A bounded local flame stream for a shot accepted and clipped by the server. */
public final class HeatGunFlameEffects
{
    private static final Set<Player> LOGGED = Collections.newSetFromMap(new WeakHashMap<>());

    private HeatGunFlameEffects() {}

    public static void spawn(int entityId, Vec3 start, Vec3 end)
    {
        var level = Minecraft.getInstance().level;
        if (level == null || !finite(start) || !finite(end)) return;
        double length = start.distanceTo(end);
        if (!Double.isFinite(length) || length > 32.0D) return;
        int samples = Math.min(96, Math.max(1, (int) Math.ceil(length / 0.28D)));
        for (int i = 0; i < samples; ++i)
        {
            Vec3 point = start.lerp(end, (i + 0.5D) / samples);
            double jitter = 0.06D;
            double dx = (level.random.nextDouble() - 0.5D) * jitter;
            double dy = (level.random.nextDouble() - 0.5D) * jitter;
            double dz = (level.random.nextDouble() - 0.5D) * jitter;
            level.addParticle(ParticleTypes.SMALL_FLAME, point.x + dx, point.y + dy, point.z + dz, 0.0D, 0.005D, 0.0D);
        }
        if (level.getEntity(entityId) instanceof Player player && LOGGED.add(player))
            FiskHeroes.LOGGER.info("Heat gun flame emitted: wearer={}, samples={}", player.getName().getString(), samples);
    }

    private static boolean finite(Vec3 point)
    {
        return Double.isFinite(point.x) && Double.isFinite(point.y) && Double.isFinite(point.z);
    }
}
