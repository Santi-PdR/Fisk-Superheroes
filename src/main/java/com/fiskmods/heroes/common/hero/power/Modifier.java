package com.fiskmods.heroes.common.hero.power;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Nullable;

import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.hero.Hero;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/**
 * A power modifier. Modifiers are the actual gameplay implementation referenced by the modifier
 * ids in a power JSON ({@code "modifiers": { "fiskheroes:flight": { ... } }}).
 * <p>
 * Every modifier declares the properties it understands; power JSON files may override them.
 */
public class Modifier
{
    private final ResourceLocation id;
    private final Map<PowerProperty<?>, Object> defaultProperties = new LinkedHashMap<>();
    private boolean hiddenFromHud;
    private com.fiskmods.heroes.common.data.var.DataVar<Boolean> soundState;

    public Modifier(ResourceLocation id)
    {
        this.id = id;
    }

    public ResourceLocation getId()
    {
        return id;
    }

    public String getName()
    {
        return id.toString();
    }

    public String getNameKey()
    {
        return "power." + id.getNamespace() + "." + id.getPath() + ".name";
    }

    public String getLocalizedName()
    {
        return Component.translatable(getNameKey()).getString();
    }

    public <T> Modifier addProperty(PowerProperty<T> property, T value)
    {
        defaultProperties.put(property, value);
        return this;
    }

    public <T> Modifier addProperty(PowerProperty<T> property)
    {
        defaultProperties.put(property, property.getDefault());
        return this;
    }

    public Map<PowerProperty<?>, Object> getDefaultProperties()
    {
        return defaultProperties;
    }

    /**
     * The data variable whose value decides whether this modifier is "on". Whenever it changes, the
     * modifier's {@code soundEvents} dispatch the matching enable/disable (or looping) sound, which
     * is how the original drove all of its suit audio from the data system.
     */
    public Modifier setSoundState(com.fiskmods.heroes.common.data.var.DataVar<Boolean> state)
    {
        this.soundState = state;
        return this;
    }

    public com.fiskmods.heroes.common.data.var.DataVar<Boolean> getSoundState()
    {
        return soundState;
    }

    public boolean isHiddenFromHud()
    {
        return hiddenFromHud;
    }

    public Modifier hideFromHud()
    {
        hiddenFromHud = true;
        return this;
    }

    /* ------------------------------------------------------------------ */
    /* Behaviour hooks                                                     */
    /* ------------------------------------------------------------------ */

    /** Called every tick on the server for entities which have this modifier active. */
    public void tick(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
    }

    /** Called every tick on the client for entities which have this modifier active. */
    public void tickClient(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
    }

    /** Called when the entity activates this modifier's ability. */
    public void onActivate(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
    }

    /** Called when a toggled ability is switched. */
    public void onToggle(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
    }

    /** Allows a modifier to suppress an incoming damage source. */
    public boolean isImmuneTo(LivingEntity entity, ModifierEntry entry, net.minecraft.world.damagesource.DamageSource source, float amount)
    {
        return false;
    }

    /** Allows a modifier to scale an incoming damage source. */
    public float modifyDamage(LivingEntity entity, ModifierEntry entry, net.minecraft.world.damagesource.DamageSource source, float amount)
    {
        return amount;
    }

    /** Called when a player wearing a suit with this modifier respawns or reconnects. */
    public void onRespawn(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
    }

    /** A script-visible permission this modifier grants. */
    public boolean grantsPermission(Hero hero, String permission)
    {
        return false;
    }

    @Nullable
    public Object getProperty(ModifierEntry entry, PowerProperty<?> property)
    {
        Object value = entry.getProperties().get(property);
        return value != null ? value : defaultProperties.get(property);
    }

    @Override
    public String toString()
    {
        return getName();
    }
}
