package com.fiskmods.heroes.common.hero.modifier;

import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.data.var.Vars;
import com.fiskmods.heroes.common.hero.power.Modifier;
import com.fiskmods.heroes.common.hero.power.ModifierEntry;
import com.fiskmods.heroes.common.hero.power.PowerProperty;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Powered flight: the player is lifted while the ability is toggled on and steered with the
 * movement keys, with an optional boost. Mirrors the original's flight model, including the
 * "stop flying when standing still" behaviour when the ability is not a toggle.
 */
public class ModifierControlledFlight extends Modifier
{
    public ModifierControlledFlight(ResourceLocation id)
    {
        super(id);
    }

    @Override
    public void onActivate(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        if (entry.getBoolean(entity, PowerProperty.IS_TOGGLE))
        {
            boolean state = !entry.isToggled(entity);
            entry.setToggled(entity, state);
            data.getData().set(Vars.FLYING, state);
        }
        else
        {
            data.getData().set(Vars.FLYING, true);
        }
    }

    @Override
    public void onToggle(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        if (!entry.getBoolean(entity, PowerProperty.IS_TOGGLE))
        {
            data.getData().set(Vars.FLYING, false);
        }
    }

    @Override
    public void tick(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        if (!(entity instanceof Player player))
        {
            return;
        }

        boolean flying = data.getData().get(Vars.FLYING);
        float timer = data.getData().get(Vars.BOOSTER_TIMER);

        if (flying)
        {
            // Motion is applied inside LivingEntity.travel on the local player. Server-side
            // velocity packets every tick would override that prediction and create drift.
            player.fallDistance = 0.0F;
            boolean boosting = entry.getBoolean(entity, PowerProperty.CAN_BOOST) && player.isSprinting();
            data.getData().set(Vars.BOOSTER_TIMER, Math.min(20.0F, timer + 1.0F));

            if (boosting)
            {
                data.getData().set(Vars.FLIGHT_SUPER_BOOST_TIMER, data.getData().get(Vars.FLIGHT_SUPER_BOOST_TIMER) + 1.0F);
            }
        }
        else if (timer > 0)
        {
            data.getData().set(Vars.BOOSTER_TIMER, 0.0F);
        }
    }

    /** Calculates one tick of flight; hover responds to movement input and boost has drag. */
    public static Vec3 controlledMotion(Vec3 motion, Vec3 look, float yaw, int forward, int strafe,
            boolean jump, boolean sneak, boolean boost, float speed, float boostSpeed)
    {
        if (boost)
        {
            // The original propelled flight applies 0.96 drag before thrust.
            return motion.scale(0.96D).add(look.scale(Math.max(0.0F, boostSpeed)));
        }
        double angle = Math.toRadians(yaw);
        double fx = -Math.sin(angle);
        double fz = Math.cos(angle);
        double x = fx * Integer.signum(forward) + fz * Integer.signum(strafe);
        double z = fz * Integer.signum(forward) - fx * Integer.signum(strafe);
        double length = Math.sqrt(x * x + z * z);
        if (length > 1.0D) { x /= length; z /= length; }
        double targetY = look.y * Integer.signum(forward) * speed
                + (jump ? speed : 0.0D) - (sneak ? speed : 0.0D);
        // This runs in the travel hook, so vanilla gravity does not run a second time.
        // With no input, velocity decays to zero rather than accumulating upward lift.
        return new Vec3(motion.x * 0.8D + x * speed * 0.2D,
                motion.y * 0.8D + targetY * 0.2D,
                motion.z * 0.8D + z * speed * 0.2D);
    }

    /**
     * Applies the flying knockback/impact behaviour when a flying player collides with a block or
     * an entity. Called from the collision handler.
     */
    public static void onCollision(Player player, SHPlayerData data, ModifierEntry entry)
    {
        float knockback = entry.getFloat(player, PowerProperty.KNOCKBACK);
        Vec3 motion = player.getDeltaMovement();
        double horizontal = Math.sqrt(motion.x * motion.x + motion.z * motion.z);

        if (horizontal > 0.6D && knockback > 0)
        {
            player.setDeltaMovement(motion.x * -knockback, motion.y * -knockback, motion.z * -knockback);
        }
    }

    public static boolean isFlying(LivingEntity entity)
    {
        com.fiskmods.heroes.common.data.SHPlayerData data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(entity);
        return data != null && data.getData().get(Vars.FLYING);
    }

    /** Flight drag/lift applied when the player stops steering. */
    public static void applyDrag(Player player)
    {
        Vec3 motion = player.getDeltaMovement();
        player.setDeltaMovement(motion.x * 0.98D, motion.y * 0.98D, motion.z * 0.98D);
    }

    public static ItemStack getBoostStack(Player player)
    {
        return player.getMainHandItem();
    }

    public static float getBoostPitch(float pitch)
    {
        return Mth.clamp(pitch, -90.0F, 90.0F);
    }
}
