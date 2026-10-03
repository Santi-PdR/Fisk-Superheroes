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
            float speed = entry.getFloat(entity, PowerProperty.SPEED);
            float boostSpeed = entry.getFloat(entity, PowerProperty.BOOST_SPEED);
            boolean canBoost = entry.getBoolean(entity, PowerProperty.CAN_BOOST);
            boolean boosting = canBoost && player.isSprinting();
            double velocity = boosting ? speed + boostSpeed : speed;

            Vec3 look = player.getLookAngle();
            Vec3 motion = player.getDeltaMovement();

            // Forward thrust along the look vector, and vertical control from the look pitch
            double x = motion.x + look.x * velocity;
            double y = motion.y + look.y * velocity;
            double z = motion.z + look.z * velocity;

            boolean jumping = com.fiskmods.heroes.common.data.PlayerInputTracker.isJumping(player);
            boolean sneaking = com.fiskmods.heroes.common.data.PlayerInputTracker.isSneaking(player);

            if (jumping)
            {
                y += velocity;
            }
            else if (sneaking)
            {
                y -= velocity;
            }

            player.setDeltaMovement(x, y, z);
            player.fallDistance = 0.0F;
            player.hurtMarked = true;
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
