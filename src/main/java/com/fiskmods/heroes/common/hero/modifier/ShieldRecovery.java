package com.fiskmods.heroes.common.hero.modifier;

import javax.annotation.Nullable;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.data.var.Vars;
import com.fiskmods.heroes.common.hero.power.ModifierEntry;
import com.fiskmods.heroes.common.hero.power.PowerProperty;
import com.google.gson.JsonObject;

import net.minecraft.world.entity.LivingEntity;

/** Shield damage and cooldown persist and recover even while no suit shield is equipped. */
public final class ShieldRecovery
{
    private ShieldRecovery() {}

    public static void tick(LivingEntity entity, SHPlayerData data)
    {
        JsonObject shield = null;
        if (data.getData().get(Vars.SHIELD_BLOCKING) && data.getHeroType() != null)
        {
            ModifierEntry entry = data.getHeroType().getPowerContainer().getEntry(FiskHeroes.id("shield"));
            if (entry != null && entry.isEnabled() && entry.isModifierEnabled(entity, data))
            {
                shield = AbilityData.object(entry.get(entity, PowerProperty.SHIELD));
                // An entry without JSON uses the original default shield configuration.
                if (shield == null) shield = new JsonObject();
            }
        }
        tickState(data, shield);
    }

    /** Null denotes no active shield: the original's default recovery then starts immediately. */
    public static void tickState(SHPlayerData data, @Nullable JsonObject activeShield)
    {
        var vars = data.getData();
        short elapsed = vars.get(Vars.TICKS_SINCE_SHIELD_DAMAGED);
        float damage = vars.get(Vars.SHIELD_DAMAGE);
        short cooldown = vars.get(Vars.SHIELD_COOLDOWN);
        if (damage <= 0.0F && cooldown <= 0) return;
        if (elapsed < Short.MAX_VALUE) vars.set(Vars.TICKS_SINCE_SHIELD_DAMAGED, (short) (elapsed + 1));
        if (cooldown > 0)
        {
            vars.set(Vars.SHIELD_COOLDOWN, (short) (cooldown - 1));
            return;
        }
        int delay = (int) number(activeShield, "recoveryDelay", 20.0F);
        float regeneration = number(activeShield, "regeneration", 10.0F);
        if (damage > 0.0F && (activeShield == null || elapsed > delay))
            vars.set(Vars.SHIELD_DAMAGE, Math.max(0.0F, damage - Math.max(0.0F, regeneration) / 20.0F));
    }

    private static float number(@Nullable JsonObject object, String name, float fallback)
    {
        return object != null && object.has(name) && object.get(name).isJsonPrimitive()
                && object.get(name).getAsJsonPrimitive().isNumber() ? object.get(name).getAsFloat() : fallback;
    }
}
