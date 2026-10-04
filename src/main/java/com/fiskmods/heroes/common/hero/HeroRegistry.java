package com.fiskmods.heroes.common.hero;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.resources.ResourceLocation;

/**
 * Registry of every hero provided by the loaded hero packs.
 */
public class HeroRegistry
{
    private final Map<ResourceLocation, Hero> byName = new LinkedHashMap<>();
    private final Map<String, Hero> byId = new LinkedHashMap<>();
    private final List<Hero> sorted = new ArrayList<>();

    public Hero register(ResourceLocation id, Hero hero, Map<String, HeroIteration.Candidate> candidates)
    {
        Hero existing = byName.get(id);

        if (existing != null)
        {
            return existing;
        }

        hero.register(candidates);
        byName.put(id, hero);
        byId.put(id.toString(), hero);
        sorted.clear();
        sorted.addAll(byName.values());
        sorted.sort(Hero.COMPARING_TIER);
        return hero;
    }

    @Nullable
    public Hero getHero(ResourceLocation id)
    {
        return byName.get(id);
    }

    @Nullable
    public Hero getHero(String id)
    {
        Hero hero = byId.get(id);

        if (hero == null && id != null && id.indexOf(':') == -1)
        {
            hero = byId.get("fiskheroes:" + id);
        }

        return hero;
    }

    @Nullable
    public Hero getHeroIgnoreCase(String id)
    {
        Hero hero = getHero(id);

        if (hero == null && id != null)
        {
            for (Hero value : byName.values())
            {
                if (value.getName().equalsIgnoreCase(id) || value.getRegistryName().getPath().equalsIgnoreCase(id))
                {
                    return value;
                }

                for (String alias : value.getAliases())
                {
                    if (alias.equalsIgnoreCase(id))
                    {
                        return value;
                    }
                }
            }
        }

        return hero;
    }

    public boolean contains(ResourceLocation id)
    {
        return byName.containsKey(id);
    }

    public Collection<Hero> getHeroes()
    {
        return byName.values();
    }

    /** Heroes sorted by tier, then by name: the order used by the suit selection GUI. */
    public List<Hero> getSortedHeroes()
    {
        return sorted;
    }

    public List<HeroIteration> getIterations(Hero hero)
    {
        return new ArrayList<>(hero.getIterations().values());
    }

    public int size()
    {
        return byName.size();
    }

    public void clear()
    {
        byName.clear();
        byId.clear();
        sorted.clear();
    }

    public void sort()
    {
        sorted.clear();
        sorted.addAll(byName.values());
        sorted.sort(Comparator.comparing(Hero::getTier).thenComparing(Hero::getLocalizedName).thenComparing(Hero::getName));
    }
}
