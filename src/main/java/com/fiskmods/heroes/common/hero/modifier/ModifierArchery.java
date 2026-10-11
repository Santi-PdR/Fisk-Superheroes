package com.fiskmods.heroes.common.hero.modifier;

import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.data.var.Vars;
import com.fiskmods.heroes.common.hero.power.Modifier;
import com.fiskmods.heroes.common.hero.power.ModifierEntry;
import net.minecraft.world.entity.LivingEntity;

/** Handles the held horizontal-aim ability used by the archer hero iterations. */
public class ModifierArchery extends Modifier
{
    public static final String KEY_HORIZONTAL = "HORIZONTAL_BOW";
    public static final String KEY_QUIVER = "QUIVER_CYCLE";

    public ModifierArchery(net.minecraft.resources.ResourceLocation id)
    {
        super(id);
    }

    @Override
    public void onActivate(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        data.getData().set(Vars.HORIZONTAL_BOW, true);
    }

    @Override
    public void onToggle(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        data.getData().set(Vars.HORIZONTAL_BOW, false);
    }
}
