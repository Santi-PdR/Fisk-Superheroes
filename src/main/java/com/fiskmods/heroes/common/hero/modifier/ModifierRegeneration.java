package com.fiskmods.heroes.common.hero.modifier;

import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.hero.power.Modifier;
import com.fiskmods.heroes.common.hero.power.PowerProperty;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * Adjusts natural regeneration's interval, not the amount healed. FoodData still handles hunger,
 * saturation, exhaustion, the naturalRegeneration gamerule and starvation normally.
 */
public class ModifierRegeneration extends Modifier
{
    public ModifierRegeneration(ResourceLocation id)
    {
        super(id);
    }

    /** Called by the FoodData transformer at the two natural-healing comparisons. */
    public static int healInterval(Player player, int vanillaInterval)
    {
        SHPlayerData data = SHDataCapabilities.getPlayer(player);
        if (data == null || data.getHeroType() == null) return vanillaInterval;

        double factor = data.getHeroType().getPowerContainer().getEntries().stream()
                .filter(entry -> entry.getModifier() instanceof ModifierRegeneration
                        && entry.isEnabled() && entry.isModifierEnabled(player, data))
                .mapToDouble(entry -> entry.getFloat(player, PowerProperty.FACTOR))
                .max().orElse(Double.NaN);
        if (Double.isNaN(factor)) return vanillaInterval;
        if (factor <= 0.0D) return Integer.MAX_VALUE;
        return Math.max(1, (int) Math.ceil(vanillaInterval / factor));
    }
}
