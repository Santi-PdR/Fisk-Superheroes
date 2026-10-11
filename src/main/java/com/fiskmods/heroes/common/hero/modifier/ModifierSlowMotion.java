package com.fiskmods.heroes.common.hero.modifier;

import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.data.var.Vars;
import com.fiskmods.heroes.common.hero.power.Modifier;
import com.fiskmods.heroes.common.hero.power.ModifierEntry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * The "slow motion" state of speedsters: everything around them appears slower. Implemented by
 * granting the wearer speed while the pack script drives the world-side effects.
 */
public class ModifierSlowMotion extends Modifier
{
    public ModifierSlowMotion(ResourceLocation id)
    {
        super(id);
    }

    @Override
    public void onActivate(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        boolean state = !entry.isToggled(entity);
        entry.setToggled(entity, state);
        data.getData().set(Vars.SLOW_MOTION, state);
    }

    @Override
    public void tick(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        if (data.getData().get(Vars.SLOW_MOTION))
        {
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 5, 1, true, false, false));
        }
        else
        {
            data.getData().set(Vars.SLOW_MOTION, false);
        }
    }
}
