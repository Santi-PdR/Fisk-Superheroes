package com.fiskmods.heroes.common.hero;

import java.util.Locale;

import net.minecraft.world.entity.ai.attributes.Attribute;

/**
 * The armour attributes a hero can declare through {@code hero.addAttribute(name, amount, operation)}.
 * <p>
 * Attributes which map onto a vanilla/Forge attribute are applied as real attribute modifiers
 * (see {@link com.fiskmods.heroes.common.hero.attribute.SHAttributes}), the rest are exposed as
 * custom attributes registered by the mod.
 */
public enum HeroAttribute
{
    DAMAGE_REDUCTION(true),
    BASE_SPEED_LEVELS(true),
    PUNCH_DAMAGE(true),
    WEAPON_DAMAGE(true),
    IMPACT_DAMAGE(true),
    ARROW_DAMAGE(true),
    BOW_DRAWBACK(false),
    REACH_DISTANCE(true),
    BASE_SPEED(true),
    JUMP_HEIGHT(true),
    FALL_RESISTANCE(false),
    KNOCKBACK(true),
    MAX_HEALTH(true),
    SPRINT_SPEED(true),
    STEP_HEIGHT(true);

    /** Whether the attribute is combined additively before profile modifiers are applied. */
    private final boolean additive;

    HeroAttribute(boolean additive)
    {
        this.additive = additive;
    }

    public boolean isAdditive()
    {
        return additive;
    }

    public String getName()
    {
        return name().toLowerCase(Locale.ROOT);
    }

    public String getUnlocalizedName()
    {
        return "attribute.fiskheroes." + getName();
    }

    /** The vanilla attribute this hero attribute feeds into, or null if it is mod-side only. */
    public Attribute getVanillaAttribute()
    {
        return SHHeroAttributeMap.get(this);
    }

    public static HeroAttribute byName(String name)
    {
        for (HeroAttribute attribute : values())
        {
            if (attribute.name().equalsIgnoreCase(name) || attribute.getName().equalsIgnoreCase(name))
            {
                return attribute;
            }
        }

        return null;
    }
}
