package com.fiskmods.heroes.common.hero.modifier;

import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.data.var.Vars;
import com.fiskmods.heroes.common.hero.power.Modifier;
import com.fiskmods.heroes.common.hero.power.ModifierEntry;
import com.fiskmods.heroes.common.hero.power.PowerProperty;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/** Healing factor waits after the most recent damage, then heals each server tick. */
public final class ModifierHealingFactor extends Modifier
{
    public ModifierHealingFactor(ResourceLocation id)
    {
        super(id);
    }

    @Override
    public void tick(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        int delay = Math.max(0, entry.getInt(entity, PowerProperty.DELAY));
        int elapsed = data.getData().get(Vars.TIME_SINCE_DAMAGED);
        if (entity.getHealth() < entity.getMaxHealth() && elapsed >= delay)
        {
            entity.heal(1.0F);
            if (elapsed == delay) AbilityData.playSound(entity, entry, "HEAL");
        }
    }
}
