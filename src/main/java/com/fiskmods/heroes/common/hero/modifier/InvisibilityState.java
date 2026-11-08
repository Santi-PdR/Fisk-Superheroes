package com.fiskmods.heroes.common.hero.modifier;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.data.var.Vars;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

/** Applies suit invisibility without creating or replacing a vanilla potion effect. */
public final class InvisibilityState
{
    private InvisibilityState() {}

    public static void update(Player player, SHPlayerData data)
    {
        if (data == null || player.level().isClientSide) return;
        boolean active = data.getHeroType() != null && data.getHeroType().getPowerContainer().getEntries().stream()
                .anyMatch(entry -> entry.isEnabled() && entry.isModifierEnabled(player, data)
                        && (entry.getModifier().getId().equals(FiskHeroes.id("invisibility"))
                                && data.getData().get(Vars.INVISIBLE)
                            || entry.getModifier().getId().equals(FiskHeroes.id("shadowform"))
                                && data.getData().get(Vars.SHADOWFORM)));
        if (active)
        {
            if (!data.isSuitInvisibilityApplied())
                data.beginSuitInvisibility(player.isInvisible() && !player.hasEffect(MobEffects.INVISIBILITY));
            player.setInvisible(true);
        }
        else release(player, data);
    }

    public static void release(Player player, SHPlayerData data)
    {
        if (!player.level().isClientSide && data.isSuitInvisibilityApplied())
            player.setInvisible(data.endSuitInvisibility() || player.hasEffect(MobEffects.INVISIBILITY));
    }
}
