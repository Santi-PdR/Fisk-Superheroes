package com.fiskmods.heroes.common.hero.modifier;

import com.fiskmods.heroes.common.data.PlayerInputTracker;
import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.data.var.Vars;
import com.fiskmods.heroes.common.hero.power.Modifier;
import com.fiskmods.heroes.common.hero.power.ModifierEntry;
import com.fiskmods.heroes.common.hero.power.PowerProperty;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Passive flight shared by heroes whose packs grant {@code fiskheroes:flight}. */
public class ModifierFlight extends Modifier
{
    public ModifierFlight(ResourceLocation id)
    {
        super(id);
    }

    @Override
    public void tick(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        if (entity instanceof Player player) update(player, entry, data, true);
    }

    @Override
    public void tickClient(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        if (entity instanceof Player player) update(player, entry, data, false);
    }

    private static void update(Player player, ModifierEntry entry, SHPlayerData data, boolean server)
    {
        boolean previousGround = data.getData().get(Vars.PREV_ON_GROUND);
        boolean creativeFlying = player.getAbilities().mayfly && player.getAbilities().flying;
        boolean airborne = !player.onGround() && !creativeFlying;

        if (server && airborne && previousGround)
        {
            AbilityData.playSound(player, entry, "TAKEOFF");
        }
        else if (server && player.onGround() && !previousGround)
        {
            AbilityData.playSound(player, entry, "LAND");
        }

        if (airborne)
        {
            boolean hovering = data.getData().get(Vars.HOVERING);
            float speed = entry.getFloat(player, PowerProperty.SPEED) / 0.1F;
            if (hovering) speed *= 0.2F;
            if (data.getData().get(Vars.SPEEDING)) speed *= data.getData().get(Vars.SPEED) * 1.1F;
            if (player.isSprinting()) speed *= 1.2F;

            player.moveRelative(0.075F * speed, new Vec3(player.xxa, 0.0D, player.zza));

            Vec3 motion = player.getDeltaMovement();
            double gravity = 0.08D;
            double y = motion.y;
            float verticalScale = 1.0F + (speed - 1.0F) / 3.0F;

            if (hovering && !player.isInWater())
            {
                float scale = Math.max(data.getData().get(Vars.SCALE), 1.0F);
                if (y < -0.125D * scale) y += 0.125D * scale - gravity;
                else if (y > 0.05D * scale) y -= 0.05D * scale + gravity;
                else y = Math.sin(player.tickCount / 15.0D) * 0.025D * scale - gravity;
            }
            else if (!player.isInWater() && y - gravity < 0.0D)
            {
                y += 0.07D - gravity;
            }

            if (PlayerInputTracker.isJumping(player))
            {
                y += (hovering ? 0.2D : 0.125D) * verticalScale - gravity;
            }
            if (PlayerInputTracker.isSneaking(player))
            {
                y -= (hovering ? 0.125D : 0.075D) * verticalScale;
            }

            player.setDeltaMovement(motion.x, y, motion.z);
            player.fallDistance = 0.0F;
            if (server) player.hurtMarked = true;
        }

        data.getData().set(Vars.PREV_ON_GROUND, player.onGround());
    }
}
