package com.fiskmods.heroes.common.hero.power;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.gson.JsonElement;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * A power as declared by a hero pack JSON file. A power is a named bundle of modifier entries
 * (its "cards") which heroes may grant themselves with {@code hero.addPowers(...)}.
 */
public class Power
{
    public static final PowerRegistry REGISTRY = new PowerRegistry();

    private final ResourceLocation id;
    private final String nameKey;
    private final Map<String, ModifierEntry> entries = new LinkedHashMap<>();
    private final JsonElement hud;

    public Power(ResourceLocation id, String nameKey, JsonElement hud)
    {
        this.id = id;
        this.nameKey = nameKey;
        this.hud = hud;
    }

    public ResourceLocation getId()
    {
        return id;
    }

    public String getNameKey()
    {
        return nameKey != null ? nameKey : "power." + id.getNamespace() + "." + id.getPath() + ".name";
    }

    public String getLocalizedName()
    {
        return Component.translatable(getNameKey()).getString();
    }

    public JsonElement getHud()
    {
        return hud;
    }

    public void addEntry(String card, ModifierEntry entry)
    {
        entries.put(card, entry);
    }

    @Nullable
    public ModifierEntry getEntry(String card)
    {
        return entries.get(card);
    }

    public Collection<ModifierEntry> getEntries()
    {
        return entries.values();
    }

    /**
     * Creates fresh modifier entries for a hero. Each hero gets its own entries so that property
     * overrides declared by another power never leak between heroes.
     */
    public Map<String, ModifierEntry> createEntries()
    {
        Map<String, ModifierEntry> map = new LinkedHashMap<>();

        for (Map.Entry<String, ModifierEntry> e : entries.entrySet())
        {
            ModifierEntry source = e.getValue();
            ModifierEntry copy = new ModifierEntry(source.getModifier(), source.getCard());
            copy.getProperties().putAll(source.getProperties());
            map.put(e.getKey(), copy);
        }

        return map;
    }

    public static Power get(ResourceLocation id)
    {
        return REGISTRY.get(id);
    }

    /** Registry of the powers declared by hero packs. */
    public static class PowerRegistry
    {
        private final Map<ResourceLocation, Power> map = new LinkedHashMap<>();

        public Power register(Power power)
        {
            map.put(power.getId(), power);
            return power;
        }

        @Nullable
        public Power get(ResourceLocation id)
        {
            return map.get(id);
        }

        @Nullable
        public Power get(String id)
        {
            ResourceLocation key = ResourceLocation.tryParse(id);

            if (key == null)
            {
                key = new ResourceLocation("fiskheroes", id);
            }

            return map.get(key);
        }

        public Collection<Power> getPowers()
        {
            return map.values();
        }

        public int size()
        {
            return map.size();
        }

        public void clear()
        {
            map.clear();
        }
    }
}
