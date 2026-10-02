package com.fiskmods.heroes.common.hero.modifier;

import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.hero.power.Modifier;
import com.fiskmods.heroes.common.hero.power.ModifierEntry;
import com.fiskmods.heroes.common.hero.power.PowerProperty;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/**
 * Accelerated healing. The factor scales the vanilla regeneration rate, as in the original mod
 * (where the factor was applied to the healing interval).
 */
public class ModifierRegeneration extends Modifier
{
    public ModifierRegeneration(ResourceLocation id)
    {
        super(id);
    }

    @Override
    public void tick(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        float factor = entry.getFloat(entity, PowerProperty.FACTOR);

        if (factor <= 0)
        {
            factor = 1.0F;
        }

        int delay = entry.getInt(entity, PowerProperty.DELAY);
        int interval = delay > 0 ? delay : Math.max(1, (int) (80.0F / factor));

        boolean fed = !(entity instanceof net.minecraft.world.entity.player.Player player) || player.getFoodData().getFoodLevel() > 0;

        if (fed && entity.getHealth() < entity.getMaxHealth() && entity.tickCount % interval == 0)
        {
            entity.heal(1.0F * Math.max(1.0F, factor));
        }
    }
}
