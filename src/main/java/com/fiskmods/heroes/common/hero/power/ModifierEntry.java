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
    private final Map<PowerProperty<?>, PropertyValue<?>> properties = new LinkedHashMap<>();
    private boolean enabled = true;

    public ModifierEntry(Modifier modifier, @Nullable String card)
    {
        this.modifier = modifier;
        this.card = card;

        for (Map.Entry<PowerProperty<?>, Object> e : modifier.getDefaultProperties().entrySet())
        {
            setProperty(e.getKey(), e.getValue());
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

    public Map<PowerProperty<?>, PropertyValue<?>> getProperties()
    {
        return properties;
    }

    /** Sets a property from a plain Java value (the modifier's defaults). */
    public void setProperty(PowerProperty<?> property, Object value)
    {
        com.google.gson.JsonElement json = null;

        if (value instanceof Number number)
        {
            json = new com.google.gson.JsonPrimitive(number);
        }
        else if (value instanceof Boolean bool)
        {
            json = new com.google.gson.JsonPrimitive(bool);
        }
        else if (value != null)
        {
            json = new com.google.gson.JsonPrimitive(String.valueOf(value));
        }

        setProperty(property, json);
    }

    /** Sets a property from the power file; strings may be script expressions. */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void setProperty(PowerProperty<?> property, com.google.gson.JsonElement json)
    {
        if (property != null)
        {
            properties.put(property, PropertyValue.of((PowerProperty) property, json));
        }
    }

    private PropertyValue<?> value(PowerProperty<?> property)
    {
        return properties.get(property);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(PowerProperty<T> property)
    {
        PropertyValue<?> value = value(property);
        return value != null ? (T) value.literal() : property.getDefault();
    }

    /** Resolves a property for an entity, evaluating script-valued properties. */
    @SuppressWarnings("unchecked")
    public <T> T get(net.minecraft.world.entity.Entity entity, PowerProperty<T> property)
    {
        PropertyValue<?> value = value(property);

        if (value == null)
        {
            return property.getDefault();
        }

        return entity != null ? (T) value.resolve(entity) : (T) value.literal();
    }

    public float getFloat(PowerProperty<Float> property)
    {
        Float value = get(property);
        return value != null ? value : 0.0F;
    }

    public float getFloat(net.minecraft.world.entity.Entity entity, PowerProperty<Float> property)
    {
        Float value = get(entity, property);
        return value != null ? value : 0.0F;
    }

    public int getInt(PowerProperty<Integer> property)
    {
        Integer value = get(property);
        return value != null ? value : 0;
    }

    public int getInt(net.minecraft.world.entity.Entity entity, PowerProperty<Integer> property)
    {
        Integer value = get(entity, property);
        return value != null ? value : 0;
    }

    public boolean getBoolean(PowerProperty<Boolean> property)
    {
        Boolean value = get(property);
        return value != null && value;
    }

    public boolean getBoolean(net.minecraft.world.entity.Entity entity, PowerProperty<Boolean> property)
    {
        Boolean value = get(entity, property);
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
