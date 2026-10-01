package com.fiskmods.heroes.common.hero.power;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Nullable;

import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.data.var.Vars;

import net.minecraft.world.entity.LivingEntity;

/**
 * An instance of a {@link Modifier} as granted by a power, including the property overrides that
 * power applied and the transient toggle/cooldown state.
 */
public class ModifierEntry
{
    private final Modifier modifier;
    private final String card;
    private final Map<PowerProperty<?>, Object> properties = new LinkedHashMap<>();
    private boolean enabled = true;

    public ModifierEntry(Modifier modifier, @Nullable String card)
    {
        this.modifier = modifier;
        this.card = card;

        for (Map.Entry<PowerProperty<?>, Object> e : modifier.getDefaultProperties().entrySet())
        {
            properties.put(e.getKey(), e.getValue());
        }
    }

    public Modifier getModifier()
    {
        return modifier;
    }

    public String getCard()
    {
        return card;
    }

    public Map<PowerProperty<?>, Object> getProperties()
    {
        return properties;
    }

    public void setProperty(PowerProperty<?> property, Object value)
    {
        if (value != null)
        {
            properties.put(property, value);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T get(PowerProperty<T> property)
    {
        Object value = properties.get(property);
        return (T) (value != null ? value : property.getDefault());
    }

    public float getFloat(PowerProperty<Float> property)
    {
        Float value = get(property);
        return value != null ? value : 0.0F;
    }

    public int getInt(PowerProperty<Integer> property)
    {
        Integer value = get(property);
        return value != null ? value : 0;
    }

    public boolean getBoolean(PowerProperty<Boolean> property)
    {
        Boolean value = get(property);
        return value != null && value;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    /** Whether the owning hero's {@code setModifierEnabled} predicate allows this modifier. */
    public boolean isModifierEnabled(LivingEntity entity, SHPlayerData data)
    {
        if (data.getHeroType() != null)
        {
            return data.getHeroType().isModifierEnabled(entity, modifier.getId().getPath());
        }

        return true;
    }

    /* ------------------------------------------------------------------ */
    /* Ability state                                                       */
    /* ------------------------------------------------------------------ */

    public boolean isToggled(LivingEntity entity)
    {
        return Vars.getToggleState(entity, modifier.getId());
    }

    public void setToggled(LivingEntity entity, boolean state)
    {
        Vars.setToggleState(entity, modifier.getId(), state);
    }

    @Override
    public String toString()
    {
        return modifier.getName();
    }
}
