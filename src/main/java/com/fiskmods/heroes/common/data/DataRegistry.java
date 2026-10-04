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

    public DataVar<?> register(ResourceLocation name, DataType<?> type, boolean resetWithoutSuit)
    {
        DataVar<?> var = byName.get(name);

        if (var == null)
        {
            var = create(name, type, resetWithoutSuit);
            byName.put(name, var);
            byString.put(name.toString(), var);
            byString.put(name.getPath(), var);
        }

        return var;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static DataVar<?> create(ResourceLocation name, DataType<?> type, boolean resetWithoutSuit)
    {
        return new DataVar(name, type, type.getDefaultValue(), resetWithoutSuit);
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
