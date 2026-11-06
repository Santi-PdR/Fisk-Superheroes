package com.fiskmods.heroes.common.hero.modifier;

import com.fiskmods.heroes.common.config.SHConfig;
import com.fiskmods.heroes.common.data.PlayerInputTracker;
import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.data.var.Vars;
import com.fiskmods.heroes.common.hero.attribute.SHAttributes;
import com.fiskmods.heroes.common.hero.power.Modifier;
import com.fiskmods.heroes.common.hero.power.ModifierEntry;
import com.fiskmods.heroes.common.hero.power.PowerProperty;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** Speedster state; vanilla movement uses the speed attribute applied by SHAttributes. */
public class ModifierSuperSpeed extends Modifier
{
    public ModifierSuperSpeed(ResourceLocation id)
    {
        super(id);
    }

    @Override
    public void onActivate(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        if (!SHConfig.ALLOW_SUPER_SPEED.get())
        {
            disable(entity);
            return;
        }
        boolean state = !data.getData().get(Vars.SPEEDING);
        entry.setToggled(entity, state);
        data.getData().set(Vars.SPEEDING, state);
    }

    @Override
    public void tick(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        if (!(entity instanceof Player player)) return;
        if (!SHConfig.ALLOW_SUPER_SPEED.get())
        {
            disable(entity);
            return;
        }
        boolean moving = PlayerInputTracker.forward(player) != 0 || PlayerInputTracker.strafe(player) != 0;
        boolean speeding = data.getData().get(Vars.SPEEDING);
        data.getData().set(Vars.MOVING, moving);
        data.getData().set(Vars.SPEED_SPRINTING, speeding && moving && player.isSprinting());


    }

    /** Base suit levels plus earned treadmill levels. Tachyon and Velocity 9 bonuses are separate. */
    public static int maxSpeed(Player player, ModifierEntry entry, SHPlayerData data)
    {
        int levels = (int) Math.floor(player.getAttributeValue(SHAttributes.BASE_SPEED_LEVELS.get()));
        if (entry.getBoolean(player, PowerProperty.CAN_USE_TREADMILL))
            levels += Math.max(0, (int) data.getData().get(Vars.SPEED_EXPERIENCE_LEVEL));
        return Mth.clamp(levels, 1, Byte.MAX_VALUE);
    }

    public static void changeSpeed(Player player, ModifierEntry entry, SHPlayerData data, int direction)
    {
        int speed = data.getData().get(Vars.SPEED);
        data.getData().set(Vars.SPEED, (byte) Mth.clamp(speed + Integer.signum(direction), 1, maxSpeed(player, entry, data)));
    }

    /** Called when super speed is disabled or its suit is removed. */
    public static void disable(LivingEntity entity)
    {
        SHPlayerData data = SHDataCapabilities.getPlayer(entity);
        if (data != null)
        {
            data.getData().set(Vars.SPEEDING, false);
            data.getData().set(Vars.SPEED_SPRINTING, false);
            data.getData().set(Vars.SPEED_SPRINT_TIMER, 0.0F);
            data.setToggleEnabled(com.fiskmods.heroes.FiskHeroes.id("super_speed"), false);
        }
    }
}
