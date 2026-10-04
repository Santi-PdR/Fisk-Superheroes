package com.fiskmods.heroes.common.spell;

import com.fiskmods.heroes.common.hero.Hero;
import com.fiskmods.heroes.common.hero.power.Modifier;
import com.fiskmods.heroes.common.hero.power.ModifierEntry;
import com.fiskmods.heroes.common.hero.power.PowerProperty;
import net.minecraft.resources.ResourceLocation;

/** Modifier that exposes the spell list declared by a power to the spell wheel and cast handler. */
public final class ModifierSpellcasting extends Modifier
{
    public ModifierSpellcasting(ResourceLocation id)
    {
        super(id);
        addProperty(PowerProperty.SPELLS, SpellSet.EMPTY);
    }

    public static SpellSet getSpells(Hero hero)
    {
        if (hero == null)
        {
            return SpellSet.EMPTY;
        }

        for (ModifierEntry entry : hero.getPowerContainer().getEntries())
        {
            if (entry.getModifier() instanceof ModifierSpellcasting)
            {
                return entry.get(PowerProperty.SPELLS);
            }
        }

        return SpellSet.EMPTY;
    }
}
