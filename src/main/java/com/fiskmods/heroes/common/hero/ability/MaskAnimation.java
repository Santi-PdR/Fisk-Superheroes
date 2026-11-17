package com.fiskmods.heroes.common.hero.ability;

import com.fiskmods.heroes.common.data.var.DataContainer;
import com.fiskmods.heroes.common.data.var.Vars;
import net.minecraft.util.Mth;

/** Server-authoritative mask progress; original suit iterations choose the transition duration. */
public final class MaskAnimation
{
    private MaskAnimation() { }

    public static void tick(DataContainer data, int duration)
    {
        int ticks = Mth.clamp(duration, 1, Byte.MAX_VALUE);
        boolean open = data.get(Vars.MASK_OPEN);
        int timer = Mth.clamp(data.get(Vars.MASK_OPEN_TIMER), 0, ticks);
        timer = Mth.clamp(timer + (open ? 1 : -1), 0, ticks);
        data.set(Vars.MASK_OPEN_TIMER, (byte) timer);
        float progress = Mth.clamp(data.get(Vars.MASK_OPEN_TIMER2) + (open ? 1.0F : -1.0F) / ticks, 0.0F, 1.0F);
        // Avoid an extra tick caused by floating-point accumulation at either endpoint.
        if (progress < 0.00001F) progress = 0.0F;
        if (progress > 0.99999F) progress = 1.0F;
        data.set(Vars.MASK_OPEN_TIMER2, progress);
    }
}
