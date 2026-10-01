package com.fiskmods.heroes.common.hero.modifier;

import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.data.var.Vars;
import com.fiskmods.heroes.common.hero.power.Modifier;
import com.fiskmods.heroes.common.hero.power.ModifierEntry;
import com.fiskmods.heroes.common.hero.power.PowerProperty;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Super speed. The player is accelerated while sprinting, gains step-assist and is slowed down
 * again when the ability is switched off.
 */
public class ModifierSuperSpeed extends Modifier
{
    public ModifierSuperSpeed(ResourceLocation id)
    {
        super(id);
    }

    @Override
    public void onActivate(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        boolean state = !entry.isToggled(entity);
        entry.setToggled(entity, state);
        data.getData().set(Vars.SPEEDING, state);
    }

    @Override
    public void tick(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        if (!(entity instanceof Player player))
        {
            return;
        }

        boolean speeding = data.getData().get(Vars.SPEEDING);
        float timer = data.getData().get(Vars.SPEED_SPRINT_TIMER);

        if (speeding && player.isSprinting())
        {
            float speed = entry.getFloat(PowerProperty.SPEED);
            Vec3 look = player.getLookAngle().scale(0.1D * speed);
            Vec3 motion = player.getDeltaMovement();

            player.setDeltaMovement(motion.x + (look.x - motion.x) * 0.2D, motion.y, motion.z + (look.z - motion.z) * 0.2D);
            data.getData().set(Vars.SPEED_SPRINT_TIMER, Math.min(40.0F, timer + 1.0F));

            if (player.onGround())
            {
                player.fallDistance = 0.0F;
            }
        }
        else if (timer > 0)
        {
            data.getData().set(Vars.SPEED_SPRINT_TIMER, Math.max(0.0F, timer - 2.0F));
        }
    }

    /** Called when the ability is disabled (e.g. by the cooldown modifier) so the state resets. */
    public static void disable(LivingEntity entity)
    {
        SHPlayerData data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(entity);

        if (data != null)
        {
            data.getData().set(Vars.SPEEDING, false);
            data.getData().set(Vars.SPEED_SPRINT_TIMER, 0.0F);
        }
    }
}
