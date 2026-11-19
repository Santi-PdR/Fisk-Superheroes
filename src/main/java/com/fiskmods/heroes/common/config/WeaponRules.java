package com.fiskmods.heroes.common.config;

import com.fiskmods.heroes.common.hero.Hero;
import com.fiskmods.heroes.common.hero.HeroTracker;
import com.fiskmods.heroes.pack.js.JSRule;

import net.minecraft.world.entity.LivingEntity;

/** The legacy hero rule callback for the currently ported Cold/Heat Gun rules. */
public enum WeaponRules
{
    RANGE_COLDGUN("fiskheroes:range_coldgun", 32.0F, 0.01, 256),
    DMG_COLDGUN("fiskheroes:dmg_coldgun", 7.0F, 0, 1000),
    RANGE_HEATGUN("fiskheroes:range_heatgun", 32.0F, 0.01, 256),
    DMG_HEATGUN("fiskheroes:dmg_heatgun", 5.0F, 0, 1000),
    COOLDOWN_COLDGUN("fiskheroes:cooldown_coldgun", 20, 0, 12000);

    private final String name;
    private final Number defaultValue;
    private final double min;
    private final double max;
    private final boolean integer;

    WeaponRules(String name, Number defaultValue, double min, double max)
    {
        this.name = name;
        this.defaultValue = defaultValue;
        this.min = min;
        this.max = max;
        this.integer = defaultValue instanceof Integer;
    }

    public double resolve(LivingEntity entity, Number configured)
    {
        return resolve(entity, HeroTracker.getHeroType(entity), configured);
    }

    /** Resolve using the current server config as value(), not the rule's default. */
    public double resolve(LivingEntity entity, Hero hero, Number configured)
    {
        Number current;
        if (integer) current = Integer.valueOf(configured.intValue());
        else current = Float.valueOf(configured.floatValue());
        Object result = hero == null ? current : hero.modifyRuleValue(entity, new JSRule(name, current, defaultValue, integer));
        double value = result instanceof Number number ? number.doubleValue() : current.doubleValue();
        if (!Double.isFinite(value)) value = current.doubleValue();
        value = Math.max(min, Math.min(max, value));
        return integer ? (int) value : value;
    }
}
