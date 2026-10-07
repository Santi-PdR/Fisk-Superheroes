package com.fiskmods.heroes.common.hero.modifier;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.data.var.Vars;

public final class DamageGroups
{
    private static final ThreadLocal<java.util.Map<String, Double>> DAMAGE_PROFILE = new ThreadLocal<>();

    public static void useDamageProfile(java.util.Map<String, Double> profile)
    {
        if (profile == null || profile.isEmpty())
        {
            DAMAGE_PROFILE.remove();
        }
        else
        {
            DAMAGE_PROFILE.set(profile);
        }
    }

    public static void clearDamageProfile()
    {
        DAMAGE_PROFILE.remove();
    }

    /** Executes an attack with the type fractions declared by a power's damageProfile object. */
    public static void withDamageProfile(JsonElement definition, Runnable attack)
    {
        java.util.Map<String, Double> previous = DAMAGE_PROFILE.get();
        java.util.Map<String, Double> profile = readProfileTypes(definition);
        useDamageProfile(profile);
        try
        {
            attack.run();
        }
        finally
        {
            useDamageProfile(previous);
        }
    }

    /** Executes a native weapon hit with its original Fisk damage-type fractions. */
    public static void withDamageProfile(java.util.Map<String, Double> profile, Runnable attack)
    {
        java.util.Map<String, Double> previous = DAMAGE_PROFILE.get();
        useDamageProfile(profile);
        try
        {
            attack.run();
        }
        finally
        {
            useDamageProfile(previous);
        }
    }

    /** Applies a typed damage profile and its entity side effects, matching the original damage properties. */
    public static boolean applyProfileDamage(net.minecraft.world.entity.LivingEntity target,
            net.minecraft.world.entity.LivingEntity attacker, DamageSource source, float amount, JsonElement definition)
    {
        JsonObject properties = definition != null && definition.isJsonObject()
                && definition.getAsJsonObject().get("properties") != null
                && definition.getAsJsonObject().get("properties").isJsonObject()
                        ? definition.getAsJsonObject().getAsJsonObject("properties") : null;
        boolean cookEntity = properties != null && properties.has("COOK_ENTITY")
                && properties.get("COOK_ENTITY").getAsBoolean();
        boolean wasBurning = target.isOnFire();
        if (cookEntity) target.setSecondsOnFire(1);

        boolean[] accepted = { false };
        withDamageProfile(definition, () -> accepted[0] = target.hurt(source, amount));
        if (!accepted[0])
        {
            if (cookEntity && !wasBurning) target.clearFire();
            return false;
        }

        if (properties != null)
        {
            JsonElement ignite = properties.get("IGNITE");
            if (ignite != null && ignite.isJsonPrimitive() && ignite.getAsJsonPrimitive().isNumber())
            {
                target.setSecondsOnFire(Math.max(0, ignite.getAsInt()));
            }

            JsonElement heat = properties.get("HEAT_TRANSFER");
            if (heat != null && heat.isJsonPrimitive() && heat.getAsJsonPrimitive().isNumber())
            {
                SHPlayerData data = SHDataCapabilities.getPlayer(target);
                int transfer = Math.max(0, heat.getAsInt());
                if (data != null && data.getData().get(Vars.METAL_SKIN) && transfer > 0)
                {
                    MetalSkinHeat.add(target, transfer / 100.0F);
                }
            }
        }
        return true;
    }

    /** Reads the configured damage amount, falling back when the power has no profile amount. */
    public static float profileDamage(JsonElement definition, float fallback)
    {
        if (definition != null && definition.isJsonObject())
        {
            JsonElement damage = definition.getAsJsonObject().get("damage");
            if (damage != null && damage.isJsonPrimitive() && damage.getAsJsonPrimitive().isNumber())
            {
                return damage.getAsFloat();
            }
        }
        return fallback;
    }

    private static java.util.Map<String, Double> readProfileTypes(JsonElement definition)
    {
        java.util.Map<String, Double> profile = new java.util.LinkedHashMap<>();
        if (definition == null || !definition.isJsonObject()) return profile;
        JsonObject object = definition.getAsJsonObject();
        JsonElement types = object.get("types");
        if (types == null || !types.isJsonObject()) return profile;
        for (java.util.Map.Entry<String, JsonElement> entry : types.getAsJsonObject().entrySet())
        {
            JsonElement fraction = entry.getValue();
            if (fraction != null && fraction.isJsonPrimitive() && fraction.getAsJsonPrimitive().isNumber())
            {
                profile.put(entry.getKey().toUpperCase(java.util.Locale.ROOT), Math.max(0.0D, fraction.getAsDouble()));
            }
        }
        return profile;
    }

    static Modifiers.DamageGroup groupOf(DamageSource source, String declared)
    {
        if (declared != null && !declared.isEmpty())
        {
            String name = declared.toLowerCase(java.util.Locale.ROOT);

            if (name.contains("fire") || name.contains("heat"))
            {
                return Modifiers.DamageGroup.FIRE;
            }
            if (name.contains("cold") || name.contains("ice") || name.contains("cryo"))
            {
                return Modifiers.DamageGroup.COLD;
            }
            if (name.contains("bullet") || name.contains("projectile") || name.contains("arrow"))
            {
                return Modifiers.DamageGroup.PROJECTILE;
            }
            if (name.contains("electric") || name.contains("lightning"))
            {
                return Modifiers.DamageGroup.ELECTRICITY;
            }
            if (name.contains("cosmic"))
            {
                return Modifiers.DamageGroup.COSMIC;
            }
            if (name.contains("energy"))
            {
                return Modifiers.DamageGroup.ENERGY;
            }
            if (name.contains("explos"))
            {
                return Modifiers.DamageGroup.EXPLOSION;
            }
            if (name.contains("magic"))
            {
                return Modifiers.DamageGroup.MAGIC;
            }
        }

        if (source.is(DamageTypeTags.IS_FIRE))
        {
            return Modifiers.DamageGroup.FIRE;
        }
        if (source.is(DamageTypeTags.IS_FREEZING))
        {
            return Modifiers.DamageGroup.COLD;
        }
        if (source.is(DamageTypes.LIGHTNING_BOLT))
        {
            return Modifiers.DamageGroup.ELECTRICITY;
        }
        if (source.is(DamageTypeTags.IS_PROJECTILE))
        {
            return Modifiers.DamageGroup.PROJECTILE;
        }
        if (source.is(DamageTypeTags.IS_EXPLOSION))
        {
            return Modifiers.DamageGroup.EXPLOSION;
        }
        if (source.is(DamageTypeTags.IS_FALL))
        {
            return Modifiers.DamageGroup.FALL;
        }
        if (source.is(DamageTypeTags.WITCH_RESISTANT_TO))
        {
            return Modifiers.DamageGroup.MAGIC;
        }

        return null;
    }

    /** Returns the damage-type fraction carried by the current attack, if it names this group. */
    public static float profileFraction(Modifiers.DamageGroup group)
    {
        java.util.Map<String, Double> profile = DAMAGE_PROFILE.get();

        if (profile == null)
        {
            return -1.0F;
        }

        double fraction = 0.0D;
        for (java.util.Map.Entry<String, Double> type : profile.entrySet())
        {
            if (groupOfName(type.getKey()) == group)
            {
                fraction += Math.max(0.0D, type.getValue());
            }
        }
        return (float) Math.min(1.0D, fraction);
    }

    /** Returns whether this attack carries the named original Fisk damage type. */
    public static boolean profileHasType(String type)
    {
        java.util.Map<String, Double> profile = DAMAGE_PROFILE.get();
        return profile != null && type != null && profile.getOrDefault(type.toUpperCase(java.util.Locale.ROOT), 0.0D) > 0.0D;
    }

    /** Returns an exact profile fraction, or -1 when no attack profile is active. */
    public static float profileFraction(String type)
    {
        java.util.Map<String, Double> profile = DAMAGE_PROFILE.get();
        if (profile == null) return -1.0F;
        if (type == null) return 0.0F;
        return (float) Math.max(0.0D, Math.min(1.0D, profile.getOrDefault(type.toUpperCase(java.util.Locale.ROOT), 0.0D)));
    }

    private static Modifiers.DamageGroup groupOfName(String type)
    {
        String name = type.toLowerCase(java.util.Locale.ROOT);
        if (name.contains("fire") || name.contains("heat")) return Modifiers.DamageGroup.FIRE;
        if (name.contains("cold") || name.contains("ice") || name.contains("cryo")) return Modifiers.DamageGroup.COLD;
        if (name.contains("bullet") || name.contains("projectile") || name.contains("arrow") || name.contains("shuriken")) return Modifiers.DamageGroup.PROJECTILE;
        if (name.contains("electric") || name.contains("lightning")) return Modifiers.DamageGroup.ELECTRICITY;
        if (name.contains("cosmic")) return Modifiers.DamageGroup.COSMIC;
        if (name.contains("energy")) return Modifiers.DamageGroup.ENERGY;
        if (name.contains("explos")) return Modifiers.DamageGroup.EXPLOSION;
        if (name.contains("magic")) return Modifiers.DamageGroup.MAGIC;
        return null;
    }
}
