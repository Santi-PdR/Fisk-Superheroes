package com.fiskmods.heroes.common.hero.modifier;

import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.hero.power.Modifier;
import com.fiskmods.heroes.common.hero.power.ModifierEntry;
import com.fiskmods.heroes.common.hero.power.PowerProperty;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Superhuman jumping. The pack script (or the hero's JUMP_HEIGHT attribute) declares the leap
 * amount; this modifier converts it into the jump impulse and keeps track of the airborne state so
 * that leaping does not cause fall damage.
 */
public class ModifierLeaping extends Modifier
{
    public ModifierLeaping(ResourceLocation id)
    {
        super(id);
    }

    @Override
    public void tick(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        boolean gliding = data.getData().get(com.fiskmods.heroes.common.data.var.Vars.GLIDING);
        float timer = data.getData().get(com.fiskmods.heroes.common.data.var.Vars.GLIDING_TIMER);

        if (gliding && !entity.onGround())
        {
            // Counts how long the wearer has been gliding; the HUD and the renderer animate off it
            data.getData().set(com.fiskmods.heroes.common.data.var.Vars.GLIDING_TIMER, Math.min(200.0F, timer + 1.0F));
        }
        else if (timer != 0.0F)
        {
            data.getData().set(com.fiskmods.heroes.common.data.var.Vars.GLIDING_TIMER, 0.0F);
        }
    }

    /** Jump impulse hook, invoked from the jump event. */
    public static void onJump(LivingEntity entity, ModifierEntry entry)
    {
        float leap = entry.getFloat(entity, PowerProperty.AMOUNT);

        if (leap != 0)
        {
            Vec3 motion = entity.getDeltaMovement();
            entity.setDeltaMovement(motion.x, motion.y + 0.2D * leap, motion.z);
            entity.hasImpulse = true;
        }
    }
}
