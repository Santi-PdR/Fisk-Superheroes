package com.fiskmods.heroes.common.hero.modifier;

import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.data.var.Vars;
import com.fiskmods.heroes.common.hero.power.Modifier;
import com.fiskmods.heroes.common.hero.power.ModifierEntry;
import com.fiskmods.heroes.common.hero.power.PowerProperty;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Charged area darkness ability used by Obsidian's umbrakinesis power. */
public class ModifierShadowDome extends Modifier
{
    public ModifierShadowDome(ResourceLocation id)
    {
        super(id);
    }

    @Override
    public void onActivate(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        if (data.getData().get(Vars.LIGHTSOUT_ID) < 0)
        {
            data.getData().set(Vars.LIGHTSOUT_TIMER, 0.0F);
            data.getData().set(Vars.LIGHTSOUT, true);
            AbilityData.playSound(entity, entry, "CAST");
        }
    }

    @Override
    public void onToggle(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        data.getData().set(Vars.LIGHTSOUT, false);
        if (data.getData().get(Vars.LIGHTSOUT_TIMER) < 1.0F)
        {
            data.getData().set(Vars.LIGHTSOUT_TIMER, 0.0F);
        }
    }

    @Override
    public void tick(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        if (!(entity instanceof Player player) || !(player.level() instanceof ServerLevel level)) return;

        int domeId = data.getData().get(Vars.LIGHTSOUT_ID);
        if (domeId >= 0)
        {
            net.minecraft.world.entity.Entity active = level.getEntity(domeId);
            if (!(active instanceof AreaEffectCloud cloud) || cloud.isRemoved())
            {
                data.getData().set(Vars.LIGHTSOUT_ID, -1);
                AbilityData.playSound(player, entry, "DISSOLVE");
            }
            else
            {
                float radius = Math.max(1.0F, entry.getFloat(player, PowerProperty.RADIUS));
                cloud.setRadius(Math.max(0.05F, (radius + 1.0F) * Math.min(1.0F, cloud.tickCount / (radius * 2.0F))));
                if (cloud.tickCount % 20 == 0) renderShell(level, cloud, cloud.getRadius());
                data.getData().set(Vars.LIGHTSOUT, false);
                data.getData().set(Vars.LIGHTSOUT_TIMER, 0.0F);
                return;
            }
        }

        if (!data.getData().get(Vars.LIGHTSOUT))
        {
            data.getData().set(Vars.LIGHTSOUT_TIMER, 0.0F);
            return;
        }

        int chargeTime = Math.max(1, entry.getInt(player, PowerProperty.CHARGE_TIME));
        float charge = Math.min(1.0F, data.getData().get(Vars.LIGHTSOUT_TIMER) + 1.0F / chargeTime);
        data.getData().set(Vars.LIGHTSOUT_TIMER, charge);
        if (charge < 1.0F) return;

        AreaEffectCloud cloud = new AreaEffectCloud(level, player.getX(), player.getY(), player.getZ());
        cloud.setOwner(player);
        cloud.setRadius(0.05F);
        cloud.setRadiusPerTick(0.0F);
        cloud.setRadiusOnUse(0.0F);
        cloud.setWaitTime(0);
        cloud.setDuration(Math.max(1, entry.getInt(player, PowerProperty.DURATION)));
        cloud.setParticle(ParticleTypes.SMOKE);
        cloud.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, true, true));

        if (level.addFreshEntity(cloud))
        {
            data.getData().set(Vars.LIGHTSOUT_ID, cloud.getId());
            AbilityData.playSound(player, entry, "FORM");
        }
        else
        {
            AbilityData.playSound(player, entry, "CAST_FAIL");
        }

        data.getData().set(Vars.LIGHTSOUT, false);
        data.getData().set(Vars.LIGHTSOUT_TIMER, 0.0F);
    }

    private static void renderShell(ServerLevel level, AreaEffectCloud cloud, float configuredRadius)
    {
        double radius = Math.max(1.0F, configuredRadius);
        Vec3 center = cloud.position();
        for (int y = -5; y <= 5; ++y)
        {
            double offsetY = y * radius * 0.2D;
            double ring = Math.sqrt(Math.max(0.0D, radius * radius - offsetY * offsetY));
            int points = Math.max(8, (int) (ring * 2.4D));
            for (int i = 0; i < points; ++i)
            {
                double angle = Math.PI * 2.0D * i / points;
                level.sendParticles(ParticleTypes.SMOKE,
                        center.x + Math.cos(angle) * ring,
                        center.y + offsetY,
                        center.z + Math.sin(angle) * ring,
                        1, 0.03D, 0.03D, 0.03D, 0.0D);
            }
        }
    }
}
