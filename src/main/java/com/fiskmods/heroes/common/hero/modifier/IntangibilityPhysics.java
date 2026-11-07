package com.fiskmods.heroes.common.hero.modifier;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.data.var.Vars;

import net.minecraft.world.entity.player.Player;

/** Coordinates phase collision with Minecraft's per-tick spectator collision reset. */
public final class IntangibilityPhysics
{
    private IntangibilityPhysics() {}

    public static boolean isActive(Player player, SHPlayerData data)
    {
        return data != null && data.getHeroType() != null && data.getData().get(Vars.INTANGIBLE)
                && !player.hasEffect(ModEffects.PHASE_SUPPRESSANT.get())
                && data.getHeroType().getPowerContainer().getEntries().stream()
                        .anyMatch(entry -> entry.getModifier().getId().equals(FiskHeroes.id("intangibility"))
                                && entry.isEnabled() && entry.isModifierEnabled(player, data));
    }

    /** Called after Player.tick resets noPhysics and again when the phase state changes. */
    public static void update(Player player, SHPlayerData data)
    {
        if (data == null) return;
        if (isActive(player, data))
        {
            if (!data.isPhaseCollisionApplied())
                data.beginPhaseCollision(player.noPhysics);
            player.noPhysics = true;
        }
        else
        {
            release(player, data);
        }
    }

    public static void release(Player player, SHPlayerData data)
    {
        if (data.isPhaseCollisionApplied())
        {
            player.noPhysics = data.endPhaseCollision() || player.isSpectator();
        }
    }
}
