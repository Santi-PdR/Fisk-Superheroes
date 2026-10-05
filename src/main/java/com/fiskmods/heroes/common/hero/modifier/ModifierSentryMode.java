package com.fiskmods.heroes.common.hero.modifier;

import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.data.var.Vars;
import com.fiskmods.heroes.common.hero.power.Modifier;
import com.fiskmods.heroes.common.hero.power.ModifierEntry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/** Drives the Iron Man suit-open state and its five-tick transition. */
public final class ModifierSentryMode extends Modifier
{
    public ModifierSentryMode(ResourceLocation id)
    {
        super(id);
    }

    @Override
    public void onActivate(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        boolean open = !data.getData().get(Vars.SUIT_OPEN);
        data.getData().set(Vars.SUIT_OPEN, open);
        entry.setToggled(entity, open);
        AbilityData.playSound(entity, entry, open ? "OPEN" : "CLOSE");
    }

    @Override
    public void tick(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        byte timer = data.getData().get(Vars.SUIT_OPEN_TIMER);
        boolean open = data.getData().get(Vars.SUIT_OPEN);
        byte maskTimer = data.getData().get(Vars.MASK_OPEN_TIMER);

        if (maskTimer == 0)
        {
            if (timer < 5 && open)
            {
                data.getData().set(Vars.SUIT_OPEN_TIMER, (byte) (timer + 1));
            }
            else if (timer > 0 && !open)
            {
                data.getData().set(Vars.SUIT_OPEN_TIMER, (byte) (timer - 1));
            }
        }
        else if (open)
        {
            data.getData().set(Vars.MASK_OPEN, false);
        }
    }

}
