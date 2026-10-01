package com.fiskmods.heroes.common.hero;

import java.util.LinkedHashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * A variant of a hero (the "alts" of a hero pack). Iterations may override the display name, the
 * individual armour pieces and the sound profile.
 */
public class HeroIteration implements Comparable<HeroIteration>
{
    private final Hero hero;
    private final int id;
    private final String key;

    private final ResourceLocation registryName;
    private final String fullName;
    private final String nameKey;
    private final String soundProfile;
    private final String vanity;
    private final int maskToggleTicks;
    private final boolean disableMask;
    private final Map<Integer, String> armorOverrides;

    HeroIteration(Hero hero, int id, String key, Candidate candidate)
    {
        this.hero = hero;
        this.id = id;
        this.key = key;

        String domain = candidate != null && candidate.domain != null ? candidate.domain : hero.getDomain();
        this.registryName = isDefault() ? hero.getRegistryName()
                : new ResourceLocation(domain, hero.getRegistryName().getPath() + "_" + key);
        this.fullName = isDefault() ? hero.getName() : hero.getName() + "/" + key;

        if (candidate != null)
        {
            nameKey = candidate.name;
            soundProfile = candidate.soundProfile;
            vanity = candidate.vanity;
            maskToggleTicks = candidate.maskToggleTicks > 0 ? candidate.maskToggleTicks : 5;
            disableMask = candidate.disableMask;

            Map<Integer, String> map = new LinkedHashMap<>();

            if (candidate.armor != null)
            {
                for (Map.Entry<String, String> e : candidate.armor.entrySet())
                {
                    int slot = slotFor(e.getKey());

                    if (slot != -1)
                    {
                        map.put(slot, e.getValue());
                    }
                }
            }

            armorOverrides = ImmutableMap.copyOf(map);
        }
        else
        {
            nameKey = null;
            soundProfile = null;
            vanity = null;
            maskToggleTicks = 5;
            disableMask = false;
            armorOverrides = ImmutableMap.of();
        }
    }

    private static int slotFor(String name)
    {
        return switch (name.toUpperCase(java.util.Locale.ROOT))
        {
            case "HELMET", "HEAD" -> 0;
            case "CHESTPLATE", "CHEST" -> 1;
            case "LEGGINGS", "LEGS" -> 2;
            case "BOOTS", "FEET" -> 3;
            default -> -1;
        };
    }

    public Hero getHero()
    {
        return hero;
    }

    public int getId()
    {
        return id;
    }

    public String getKey()
    {
        return key;
    }

    public boolean isDefault()
    {
        return key == null;
    }

    public ResourceLocation getRegistryName()
    {
        return registryName;
    }

    public String getFullName()
    {
        return fullName;
    }

    public String getSoundProfile()
    {
        return soundProfile;
    }

    public String getVanity()
    {
        return vanity;
    }

    public int getMaskToggleTicks()
    {
        return maskToggleTicks;
    }

    public boolean isMaskDisabled()
    {
        return disableMask;
    }

    /** The armour piece of this iteration for the given slot, falling back to the base hero. */
    public String getArmorType(int slot)
    {
        return armorOverrides.getOrDefault(slot, hero.getArmorType(slot));
    }

    public Map<Integer, String> getArmorOverrides()
    {
        return armorOverrides;
    }

    public boolean hasPieceOfSet(int slot)
    {
        return getArmorType(slot) != null;
    }

    public boolean isSuit()
    {
        return hero.getPiecesToSet() > 0;
    }

    public String getLocalizedName()
    {
        return getNameKey() != null ? Component.translatable(getNameKey()).getString() : hero.getLocalizedName();
    }

    public String getNameKey()
    {
        return nameKey != null ? nameKey : hero.getNameKey();
    }

    public int getTier()
    {
        return hero.getTier();
    }

    public String getVersionKey()
    {
        return hero.getVersionKey();
    }

    public Component getFormattedName()
    {
        return Component.translatable(getNameKey());
    }

    @Override
    public int compareTo(HeroIteration o)
    {
        int i = hero.compareTo(o.hero);

        if (i == 0)
        {
            i = Boolean.compare(isDefault(), o.isDefault());
        }

        return i != 0 ? i : getLocalizedName().compareTo(o.getLocalizedName());
    }

    @Override
    public int hashCode()
    {
        return registryName.hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        return obj instanceof HeroIteration && ((HeroIteration) obj).registryName.equals(registryName);
    }

    @Override
    public String toString()
    {
        return fullName;
    }

    /** Raw data of one iteration as declared in the hero pack {@code alts} section. */
    public static class Candidate
    {
        public String name;
        public String soundProfile;
        public String vanity;
        public String domain;
        public int maskToggleTicks;
        public boolean disableMask;
        public Map<String, String> armor;
    }
}
