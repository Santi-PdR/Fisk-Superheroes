package com.fiskmods.heroes.common.hero.ability;

import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.data.var.Vars;
import com.fiskmods.heroes.common.hero.HeroTracker;
import com.fiskmods.heroes.common.hero.power.ModifierEntry;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Drives every active modifier of an entity: ticking, damage interception and lifecycle hooks.
 */
public final class ModifierHandler
{
    private ModifierHandler()
    {
    }

    public static void tick(LivingEntity entity)
    {
        HeroTracker.update(entity);
        tickTracked(entity);
    }

    /** Tick after the caller has already updated the equipped suit this tick. */
    public static void tickTracked(LivingEntity entity)
    {
        SHPlayerData data = SHDataCapabilities.getPlayer(entity);

        if (data == null)
        {
            return;
        }

        if (!entity.level().isClientSide)
            com.fiskmods.heroes.common.hero.modifier.ShieldRecovery.tick(entity, data);
        if (entity instanceof Player player)
            com.fiskmods.heroes.common.hero.modifier.InvisibilityState.update(player, data);

        if (data.getHeroType() == null)
        {
            data.getData().set(Vars.METAL_SKIN, false);
            return;
        }

        boolean hasActiveMetalSkin = data.getHeroType().getPowerContainer().getEntries().stream()
                .anyMatch(entry -> entry.getModifier().getId().getPath().equals("metal_skin")
                        && entry.isEnabled() && entry.isModifierEnabled(entity, data));
        data.getData().set(Vars.METAL_SKIN, hasActiveMetalSkin);

        float projectionTimer = data.getData().get(Vars.ENERGY_PROJECTION_TIMER);
        if (projectionTimer > 0.0F)
        {
            data.getData().set(Vars.ENERGY_PROJECTION_TIMER,
                    net.minecraft.util.Mth.approach(projectionTimer, 0.0F, 0.2F));
        }

        float teleportTimer = data.getData().get(Vars.TELEPORT_TIMER);
        if (teleportTimer > 0.0F)
        {
            data.getData().set(Vars.TELEPORT_TIMER,
                    net.minecraft.util.Mth.approach(teleportTimer, 0.0F, 0.2F));
        }

        boolean integrated = entity.level() instanceof net.minecraft.server.level.ServerLevel;

        if (integrated && data.getHeroType().getTickHandler() != null)
        {
            try
            {
                data.getHeroType().getTickHandler().call(
                        new com.fiskmods.heroes.pack.js.JSEntity(entity), new com.fiskmods.heroes.pack.js.JSManager());
            }
            catch (Exception e)
            {
                com.fiskmods.heroes.FiskHeroes.LOGGER.error("Error running hero tick handler for {}", data.getHeroType().getName(), e);
            }
        }

        for (ModifierEntry entry : data.getHeroType().getPowerContainer().getEntries())
        {
            if ("potion_retention".equals(entry.getModifier().getId().getPath())
                    || "potion_immunity".equals(entry.getModifier().getId().getPath())) continue;
            if (!entry.isEnabled() || !entry.isModifierEnabled(entity, data))
            {
                if (entry.getModifier().getId().equals(com.fiskmods.heroes.FiskHeroes.id("wall_crawling")))
                    data.getData().set(Vars.WALL_CRAWLING, (byte) 0);
                continue;
            }

            try
            {
                if (integrated)
                {
                    dispatchSounds(entity, entry, data);
                    entry.getModifier().tick(entity, entry, data);
                }
                else
                {
                    entry.getModifier().tickClient(entity, entry, data);
                }
            }
            catch (Exception e)
            {
                com.fiskmods.heroes.FiskHeroes.LOGGER.error("Error ticking modifier {}", entry.getModifier().getName(), e);
            }
        }
    }

    /**
     * Plays the modifier's enable/disable sound when its state variable flips, and keeps the
     * looping variant running while the state is on. Mirrors the original's data-driven sound
     * dispatch (the sound definitions declare which of their entries loop and how they fade).
     */
    private static void dispatchSounds(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        com.fiskmods.heroes.common.data.var.DataVar<Boolean> state = entry.getModifier().getSoundState();

        if (state == null)
        {
            return;
        }

        boolean value = data.getData().get(state);
        Boolean previous = data.getSoundState(entry.getModifier().getId());

        if (previous == null || previous != value)
        {
            data.setSoundState(entry.getModifier().getId(), value);
            // Equipping an inactive power is not a DISABLE transition.
            if (previous == null && !value) return;
            if (!value) com.fiskmods.heroes.common.hero.modifier.AbilityData.stopSoundLoops(entity, entry, "ENABLE");
            com.fiskmods.heroes.common.hero.modifier.AbilityData.playSound(entity, entry, value ? "ENABLE" : "DISABLE");
        }
    }

    /** Runs potion immunity and duration retention before the player ticks its active effects. */
    public static void tickPotionEffects(Player player)
    {
        if (player.level().isClientSide) return;
        SHPlayerData data = SHDataCapabilities.getPlayer(player);
        if (data == null || data.getHeroType() == null) return;

        for (ModifierEntry entry : data.getHeroType().getPowerContainer().getEntries())
        {
            String path = entry.getModifier().getId().getPath();
            if (entry.isEnabled() && ("potion_retention".equals(path) || "potion_immunity".equals(path))
                    && entry.isModifierEnabled(player, data))
            {
                entry.getModifier().tick(player, entry, data);
            }
        }
    }

    /** Applies every immunity and resistance of the wearer; returns the resulting damage amount. */
    public static float modifyDamage(LivingEntity entity, DamageSource source, float amount)
    {
        SHPlayerData data = SHDataCapabilities.getPlayer(entity);

        if (data == null || data.getHeroType() == null)
        {
            return amount;
        }

        if (data.isInvulnerable())
        {
            return 0.0F;
        }

        // Explicit power/projectile profiles must survive the nested LivingHurtEvent.
        // Restore the prior scope afterwards: retaliation can itself trigger another hurt event.
        java.util.Map<String, Double> previousProfile = com.fiskmods.heroes.common.hero.modifier.DamageGroups.currentDamageProfile();
        java.util.Map<String, Double> profile = previousProfile;
        net.minecraft.world.entity.Entity attacker = source.getEntity();
        if (profile == null && attacker instanceof Player player && source.getDirectEntity() == attacker
                && (source.is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK)
                        || source.is(net.minecraft.world.damagesource.DamageTypes.MOB_ATTACK)))
        {
            SHPlayerData attackerData = SHDataCapabilities.getPlayer(player);
            if (attackerData != null && attackerData.getHeroType() != null)
            {
                profile = attackerData.getHeroType().getMeleeDamageProfile(attacker);
            }
        }

        com.fiskmods.heroes.common.hero.modifier.DamageGroups.useDamageProfile(profile);
        try
        {
            float result = amount;

            for (ModifierEntry entry : data.getHeroType().getPowerContainer().getEntries())
            {
                if (!entry.isEnabled() || !entry.isModifierEnabled(entity, data))
                {
                    continue;
                }

                if (entry.getModifier().isImmuneTo(entity, entry, source, amount))
                {
                    return 0.0F;
                }

                result = entry.getModifier().modifyDamage(entity, entry, source, result, amount);
            }

            return Math.max(0.0F, result);
        }
        finally
        {
            com.fiskmods.heroes.common.hero.modifier.DamageGroups.useDamageProfile(previousProfile);
        }
    }

    /** Applies attacker-side modifiers before the defender processes the same hit. */
    public static float modifyOutgoingDamage(LivingEntity attacker, net.minecraft.world.entity.Entity target,
            DamageSource source, float amount)
    {
        if (attacker == null || attacker.level().isClientSide || source.getEntity() != attacker
                || source.getDirectEntity() != attacker
                || !(source.is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK)
                        || source.is(net.minecraft.world.damagesource.DamageTypes.MOB_ATTACK)))
        {
            return amount;
        }

        SHPlayerData data = SHDataCapabilities.getPlayer(attacker);
        if (data == null || data.getHeroType() == null) return amount;

        float result = amount;
        for (ModifierEntry entry : data.getHeroType().getPowerContainer().getEntries())
        {
            if (!entry.isEnabled() || !entry.isModifierEnabled(attacker, data)) continue;
            result = entry.getModifier().modifyOutgoingDamage(attacker, entry, target, source, result);
        }
        return Math.max(0.0F, result);
    }

    public static void onJump(LivingEntity entity)
    {
        SHPlayerData data = SHDataCapabilities.getPlayer(entity);

        if (data == null || data.getHeroType() == null)
        {
            return;
        }

        float size = data.getData().get(Vars.SCALE);
        float scale = size > 1.0F ? (float) Math.cbrt(size) : 1.0F;
        if (entity.isSprinting())
        {
            for (ModifierEntry entry : data.getHeroType().getPowerContainer().getEntries())
            {
                if ("leaping".equals(entry.getModifier().getId().getPath()) && entry.isEnabled()
                        && entry.isModifierEnabled(entity, data))
                {
                    com.fiskmods.heroes.common.hero.modifier.ModifierLeaping.onJump(entity, entry, scale);
                    break; // Cards such as springboard are alternatives, not cumulative boosts.
                }
            }
        }
        var attribute = entity.getAttribute(com.fiskmods.heroes.common.hero.attribute.SHAttributes.JUMP_HEIGHT.get());
        double height = attribute != null ? attribute.getValue() : 1.0D;
        entity.setDeltaMovement(entity.getDeltaMovement().add(0, 0.15D * (height - 1.0D) + 0.185D * (scale - 1.0D), 0));
        com.fiskmods.heroes.FiskHeroes.LOGGER.debug("Suit jump hero={} side={} vertical={}",
                data.getHeroType().getRegistryName(), entity.level().isClientSide ? "client" : "server",
                entity.getDeltaMovement().y);
        if (data.getHeroType().getPowerContainer().has(com.fiskmods.heroes.FiskHeroes.id("web_swinging"))
                && data.getData().get(Vars.TICKS_SINCE_SWINGING) < 5)
            data.getData().set(Vars.TICKS_SINCE_SWINGING, (short) 5);
    }

    public static boolean hasModifier(Player player, String id)
    {
        SHPlayerData data = SHDataCapabilities.getPlayer(player);

        if (data == null || data.getHeroType() == null)
        {
            return false;
        }

        return data.getHeroType().getPowerContainer().has(id);
    }
}
