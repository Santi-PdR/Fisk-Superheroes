package com.fiskmods.heroes.common.hero.modifier;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;

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
