package com.fiskmods.heroes.common.hero.power;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.resources.ResourceLocation;

/**
 * The set of modifier entries granted by the powers of a hero.
 */
public class PowerContainer
{
    private final List<ResourceLocation> powers = new ArrayList<>();
    private final List<ModifierEntry> entries = new ArrayList<>();
    private final Map<String, ModifierEntry> byKey = new LinkedHashMap<>();
    private boolean baked;

    public void addPower(ResourceLocation id)
    {
        if (!powers.contains(id))
        {
            powers.add(id);
            baked = false;
        }
    }

    public List<ResourceLocation> getPowers()
    {
        return powers;
    }

    public List<ModifierEntry> getEntries()
    {
        return entries;
    }

    @Nullable
    public ModifierEntry getEntry(ResourceLocation modifier)
    {
        return byKey.get(modifier.toString());
    }

    public boolean has(ResourceLocation modifier)
    {
        return byKey.containsKey(modifier.toString());
    }

    public boolean has(String modifier)
    {
        return byKey.containsKey(modifier);
    }

    public void addEntry(String key, ModifierEntry entry)
    {
        entries.add(entry);
        byKey.put(key, entry);
    }

    /** Builds the entry list from the registered powers. Called once the hero is fully loaded. */
    public void bake()
    {
        if (baked)
        {
            return;
        }

        entries.clear();
        byKey.clear();

        for (ResourceLocation id : powers)
        {
            Power power = Power.REGISTRY.get(id);

            if (power != null)
            {
                for (Map.Entry<String, ModifierEntry> e : power.createEntries().entrySet())
                {
                    ModifierEntry existing = byKey.get(e.getKey());

                    if (existing == null)
                    {
                        addEntry(e.getKey(), e.getValue());
                    }
                    else
                    {
                        // Overlay the later power's properties onto the existing entry
                        existing.getProperties().putAll(e.getValue().getProperties());
                    }
                }
            }
        }

        baked = true;
    }

    public boolean isEmpty()
    {
        return entries.isEmpty();
    }
}
