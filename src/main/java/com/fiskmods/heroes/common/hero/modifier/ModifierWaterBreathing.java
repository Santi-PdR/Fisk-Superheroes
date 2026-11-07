package com.fiskmods.heroes.common.hero.modifier;

import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.hero.power.Modifier;
import com.fiskmods.heroes.common.hero.power.ModifierEntry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/** Replenishes air without creating or extending an unrelated water-breathing potion. */
public class ModifierWaterBreathing extends Modifier
{
    public ModifierWaterBreathing(ResourceLocation id)
    {
        super(id);
    }

    @Override
    public void tick(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        entity.setAirSupply(entity.getMaxAirSupply());
    }
}
