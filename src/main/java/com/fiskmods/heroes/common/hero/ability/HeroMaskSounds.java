package com.fiskmods.heroes.common.hero.ability;

import com.fiskmods.heroes.common.hero.HeroIteration;
import com.fiskmods.heroes.common.sound.SHSounds;
import net.minecraft.world.entity.LivingEntity;

/** Server-side suit-level mask cues, including profile overrides and layered loops. */
public final class HeroMaskSounds
{
    private HeroMaskSounds() { }

    public static void play(LivingEntity entity, HeroIteration iteration, boolean open)
    {
        if (iteration == null || entity.level().isClientSide) return;
        if (!open) stopOpenLoops(entity, iteration);
        for (var id : iteration.getHero().getSuitSoundEvents(open ? "MASK_OPEN" : "MASK_CLOSE", iteration.getSoundProfile()))
            SHSounds.play(entity, id);
    }

    public static void stopOpenLoops(LivingEntity entity, HeroIteration iteration)
    {
        if (iteration == null || entity.level().isClientSide) return;
        for (var id : iteration.getHero().getSuitSoundEvents("MASK_OPEN", iteration.getSoundProfile()))
        {
            var definition = SHSounds.getDefinition(id);
            if (definition != null && definition.isLoop()) SHSounds.stopLoop(entity, id);
        }
    }
}
