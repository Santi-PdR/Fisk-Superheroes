package com.fiskmods.heroes.common.hero.modifier;

import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.hero.power.Modifier;
import com.fiskmods.heroes.common.hero.power.ModifierEntry;
import com.fiskmods.heroes.common.hero.power.PowerProperty;
import com.google.gson.JsonElement;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Señor Cactus's aimed, non-destructive spike shot. */
public final class ModifierSpikeBurst extends Modifier
{
    public ModifierSpikeBurst(ResourceLocation id)
    {
        super(id);
    }

    @Override
    public void onActivate(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        if (!(entity instanceof Player player) || !(player.level() instanceof ServerLevel level)) return;

        float range = entry.getFloat(player, PowerProperty.RANGE);
        if (range <= 0.0F) range = 24.0F;

        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        HitResult hit = ProjectileUtil.getHitResultOnViewVector(player, target -> target != player && target.isAlive(), range);
        Vec3 end = hit.getType() == HitResult.Type.MISS ? start.add(look.scale(range)) : hit.getLocation();

        double distance = start.distanceTo(end);
        int particles = Math.max(1, (int) Math.ceil(distance * 2.0D));
        level.sendParticles(ParticleTypes.CRIT, start.x, start.y, start.z, particles,
                look.x * distance * 0.5D, look.y * distance * 0.5D, look.z * distance * 0.5D, 0.0D);
        level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(
                        ParticleTypes.BLOCK, net.minecraft.world.level.block.Blocks.CACTUS.defaultBlockState()),
                end.x, end.y, end.z, 6, 0.12D, 0.12D, 0.12D, 0.03D);

        if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity target)
        {
            JsonElement profile = entry.get(player, PowerProperty.DAMAGE_PROFILE);
            float damage = DamageGroups.profileDamage(profile, 1.5F);
            DamageGroups.withDamageProfile(profile, () -> target.hurt(player.damageSources().playerAttack(player), damage));
        }

        AbilityData.playSound(player, entry, "SHOOT");
    }
}
