package com.fiskmods.heroes.common.hero.modifier;

import com.fiskmods.heroes.common.data.PlayerInputTracker;
import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.hero.power.Modifier;
import com.fiskmods.heroes.common.hero.power.ModifierEntry;
import com.fiskmods.heroes.common.hero.power.PowerProperty;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Sprint leaps use the original power's horizontal/vertical impulse pair. */
public class ModifierLeaping extends Modifier
{
    public ModifierLeaping(ResourceLocation id)
    {
        super(id);
    }

    public static void onJump(LivingEntity entity, ModifierEntry entry, float scale)
    {
        if (!entity.isSprinting()) return;
        JsonElement configured = entry.get(entity, PowerProperty.LEAP_AMOUNT);
        JsonArray leap = configured != null && configured.isJsonArray() ? configured.getAsJsonArray() : null;
        float forward = component(leap, 0, 1.5F);
        float up = component(leap, 1, 0.2F);
        float sideways = entity.xxa;
        float ahead = entity.zza;
        if (entity instanceof Player player && !entity.level().isClientSide)
        {
            sideways = PlayerInputTracker.strafe(player);
            ahead = PlayerInputTracker.forward(player);
        }
        entity.moveRelative(forward * scale, new Vec3(sideways, 0, ahead));
        entity.setDeltaMovement(entity.getDeltaMovement().add(0, up * scale, 0));
        entity.hasImpulse = true;
        if (!entity.level().isClientSide) AbilityData.playSound(entity, entry, "LEAP");
    }

    private static float component(JsonArray values, int index, float fallback)
    {
        return values != null && values.size() > index && values.get(index).isJsonPrimitive()
                && values.get(index).getAsJsonPrimitive().isNumber() ? values.get(index).getAsFloat() : fallback;
    }
}
