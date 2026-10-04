package com.fiskmods.heroes.common.hero.modifier;

import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.data.var.Vars;
import com.fiskmods.heroes.common.hero.power.Modifier;
import com.fiskmods.heroes.common.hero.power.ModifierEntry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/**
 * Shape shifting: while the suit's shape shifting is engaged, the timer runs from 0 to 1 and the
 * disguise named by {@code fiskheroes:shape_shifting_to} is applied halfway through, exactly as the
 * original mod did it. The disguise drives which hero the entity renders and behaves as.
 */
public class ModifierShapeShifting extends Modifier
{
    ModifierShapeShifting(ResourceLocation id)
    {
        super(id);
    }

    @Override
    public void tick(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        float timer = data.getData().get(Vars.SHAPE_SHIFT_TIMER);

        // Halfway through, the disguise changes to the one being shifted into
        if (Math.round(timer * 100.0F) / 100.0F == 0.5F)
        {
            String next = data.getData().get(Vars.SHAPE_SHIFTING_TO);

            if (next != null)
            {
                data.getData().set(Vars.DISGUISE, next);
            }
        }

        boolean shifting = Boolean.TRUE.equals(data.getData().get(Vars.SHAPE_SHIFTING));
        float step = (shifting ? 1.0F : -1.0F) / 50.0F;
        data.getData().set(Vars.SHAPE_SHIFT_TIMER, Mth.clamp(timer + step, 0.0F, 1.0F));

        String disguise = data.getData().get(Vars.DISGUISE);

        if (disguise != null && disguise.isEmpty())
        {
            data.getData().set(Vars.DISGUISE, "");
        }
    }
}
