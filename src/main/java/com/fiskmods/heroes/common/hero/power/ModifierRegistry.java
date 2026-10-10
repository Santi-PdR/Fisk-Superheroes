package com.fiskmods.heroes.common.hero.power;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.resources.ResourceLocation;

/**
 * Registry of every modifier implemented by the mod. Modifiers are always implemented in Java;
 * hero packs merely reference them by id and tune their properties.
 */
public class ModifierRegistry
{
    public static final ModifierRegistry INSTANCE = new ModifierRegistry();

    private final Map<ResourceLocation, Modifier> map = new LinkedHashMap<>();

    public Modifier register(Modifier modifier)
    {
        map.put(modifier.getId(), modifier);
        return modifier;
    }

    @Nullable
    public Modifier get(ResourceLocation id)
    {
        return map.get(id);
    }

    @Nullable
    public Modifier get(String id)
    {
        Modifier modifier = map.get(ResourceLocation.tryParse(id));

        if (modifier == null && id != null && id.indexOf(':') == -1)
        {
            modifier = map.get(new ResourceLocation("fiskheroes", id));
        }

        return modifier;
    }

    public Collection<Modifier> getModifiers()
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
