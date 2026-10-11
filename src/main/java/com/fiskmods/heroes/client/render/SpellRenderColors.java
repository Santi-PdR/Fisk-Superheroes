package com.fiskmods.heroes.client.render;

import com.fiskmods.heroes.common.hero.HeroIteration;
import com.fiskmods.heroes.common.hero.HeroTracker;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.world.entity.Entity;

/** Resolves the original per-hero spellcasting colors declared in hero model JSON. */
final class SpellRenderColors
{
    private SpellRenderColors()
    {
    }

    static int get(Entity caster, String property, int fallback)
    {
        HeroIteration iteration = caster != null ? HeroTracker.getHero(caster) : null;
        HeroModelData model = iteration != null ? HeroModelRegistry.get(iteration) : null;
        JsonObject effect = model != null ? model.getCustom().get("fiskheroes:spellcasting") : null;
        JsonElement color = effect != null ? effect.get(property) : null;
        if (color == null || !color.isJsonPrimitive() || !color.getAsJsonPrimitive().isString()) return fallback;

        String value = color.getAsString().trim();
        try
        {
            if (value.startsWith("#")) return (int) Long.parseLong(value.substring(1), 16) & 0xFFFFFF;
            if (value.startsWith("0x") || value.startsWith("0X")) return (int) Long.parseLong(value.substring(2), 16) & 0xFFFFFF;
            return Integer.decode(value) & 0xFFFFFF;
        }
        catch (NumberFormatException ignored)
        {
            return fallback;
        }
    }
}
