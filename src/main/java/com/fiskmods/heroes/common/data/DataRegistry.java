package com.fiskmods.heroes.common.data;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.data.var.DataVar;

import net.minecraft.resources.ResourceLocation;

/**
 * Registry of every data variable declared by the loaded hero packs. Lookups (especially the hot
 * path from scripts) go through {@link #get(String)}.
 */
public class DataRegistry implements Iterable<DataVar<?>>
{
    public static final DataRegistry INSTANCE = new DataRegistry();

    private final Map<ResourceLocation, DataVar<?>> byName = new LinkedHashMap<>();
    private final Map<String, DataVar<?>> byString = new HashMap<>();

    public <T> DataVar<T> register(ResourceLocation name, DataType<T> type, boolean resetWithoutSuit)
    {
        return register(name, type, type.getDefaultValue(), resetWithoutSuit);
    }

    /** Registers a built-in variable with its explicit default rather than the type's zero. */
    @SuppressWarnings("unchecked")
    public <T> DataVar<T> register(ResourceLocation name, DataType<T> type, T defaultValue, boolean resetWithoutSuit)
    {
        DataVar<?> var = byName.get(name);
        if (var == null)
        {
            var = new DataVar<>(name, type, defaultValue, resetWithoutSuit);
            byName.put(name, var);
            byString.put(name.toString(), var);
            byString.put(name.getPath(), var);
        }
        return (DataVar<T>) var;
    }

    /** Resolves a data variable key, which may be fully qualified or relative to {@code fiskheroes}. */
    public DataVar<?> get(String key)
    {
        DataVar<?> var = byString.get(key);

        if (var == null && key.indexOf(':') == -1)
        {
            var = byName.get(new ResourceLocation(FiskHeroes.MODID, key));
        }

        return var;
    }

    public DataVar<?> get(ResourceLocation name)
    {
        return byName.get(name);
    }

    public Collection<DataVar<?>> getValues()
    {
        return byName.values();
    }

    public int size()
    {
        return byName.size();
    }

    public void clear()
    {
        byName.clear();
        byString.clear();
    }

    @Override
    public Iterator<DataVar<?>> iterator()
    {
        return byName.values().iterator();
    }
}
