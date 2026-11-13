package com.fiskmods.heroes.common.data;

import com.fiskmods.heroes.common.data.var.DataContainer;
import com.fiskmods.heroes.common.hero.Hero;
import com.fiskmods.heroes.common.hero.HeroIteration;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/**
 * Per-player hero state: the currently equipped hero, transient suit information and the player's
 * data variables.
 */
public class SHPlayerData implements IDataHolder
{
    private final DataContainer data = new DataContainer();
    // These ids are deterministic, but deriving them hashes strings. Keep them for the lifetime
    // of this capability instead of computing all of them on every server tick.
    private java.util.UUID attributeIdOwner;
    private final java.util.UUID[] attributeIds = new java.util.UUID[com.fiskmods.heroes.common.hero.HeroAttribute.values().length];

    public java.util.UUID heroAttributeId(java.util.UUID owner, com.fiskmods.heroes.common.hero.HeroAttribute attribute)
    {
        if (!owner.equals(attributeIdOwner))
        {
            java.util.Arrays.fill(attributeIds, null);
            attributeIdOwner = owner;
        }
        int index = attribute.ordinal();
        if (attributeIds[index] == null)
            attributeIds[index] = java.util.UUID.nameUUIDFromBytes(("fiskheroes:" + owner + ":" + attribute.getName() + ":hero")
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return attributeIds[index];
    }

    // Client-only session marker; the server sends the active suit, not merely its appearance.
    private boolean authoritativeHero;
    private HeroIteration hero;
    private HeroIteration prevHero;
    private int heroChangeTime;
    private final java.util.Set<ResourceLocation> toggles = new java.util.LinkedHashSet<>();
    private final java.util.Map<ResourceLocation, Boolean> soundStates = new java.util.HashMap<>();
    private final java.util.Map<ResourceLocation, Long> spellCooldowns = new java.util.HashMap<>();

    /** Ticks the suit has been unusable (all pieces removed while powers were active). */
    private int suitDisabledTime;

    private boolean invulnerable;
    // Transient ownership of the noPhysics flag; never serialized or copied on respawn.
    private boolean phaseCollisionApplied;
    private boolean previousNoPhysics;

    public boolean isPhaseCollisionApplied() { return phaseCollisionApplied; }
    public void beginPhaseCollision(boolean previous)
    {
        phaseCollisionApplied = true;
        previousNoPhysics = previous;
    }
    public boolean endPhaseCollision()
    {
        phaseCollisionApplied = false;
        return previousNoPhysics;
    }

    private boolean suitInvisibilityApplied;
    private boolean previousInvisible;

    public boolean isSuitInvisibilityApplied() { return suitInvisibilityApplied; }
    public void beginSuitInvisibility(boolean previous)
    {
        suitInvisibilityApplied = true;
        previousInvisible = previous;
    }
    public boolean endSuitInvisibility()
    {
        suitInvisibilityApplied = false;
        return previousInvisible;
    }

    public DataContainer getData()
    {
        return data;
    }

    /** Last seen sound state of a modifier, so transitions can be detected across ticks. */
    public Boolean getSoundState(ResourceLocation modifier)
    {
        return soundStates.get(modifier);
    }

    public void setSoundState(ResourceLocation modifier, boolean state)
    {
        soundStates.put(modifier, state);
    }

    public java.util.Map<ResourceLocation, Boolean> getSoundStates()
    {
        return soundStates;
    }

    public long getSpellCooldownUntil(ResourceLocation spell)
    {
        return spellCooldowns.getOrDefault(spell, 0L);
    }

    public void setSpellCooldownUntil(ResourceLocation spell, long gameTime)
    {
        if (gameTime <= 0L)
        {
            spellCooldowns.remove(spell);
        }
        else
        {
            spellCooldowns.put(spell, gameTime);
        }
    }

    public boolean hasAuthoritativeHero()
    {
        return authoritativeHero;
    }

    public void markAuthoritativeHero()
    {
        authoritativeHero = true;
    }

    public HeroIteration getHero()
    {
        return hero;
    }

    public Hero getHeroType()
    {
        return hero != null ? hero.getHero() : null;
    }

    public void setHero(HeroIteration hero)
    {
        if (this.hero != hero)
        {
            this.prevHero = this.hero;
            this.hero = hero;
            this.heroChangeTime = 0;
        }
    }

    public HeroIteration getPrevHero()
    {
        return prevHero;
    }

    public int getHeroChangeTime()
    {
        return heroChangeTime;
    }

    public int getSuitDisabledTime()
    {
        return suitDisabledTime;
    }

    public void setSuitDisabledTime(int ticks)
    {
        suitDisabledTime = ticks;
    }

    public boolean isInvulnerable()
    {
        return invulnerable;
    }

    public void setInvulnerable(boolean invulnerable)
    {
        this.invulnerable = invulnerable;
    }

    public boolean isToggleEnabled(ResourceLocation id)
    {
        return toggles.contains(id);
    }

    public java.util.Set<ResourceLocation> getEnabledToggles()
    {
        return java.util.Set.copyOf(toggles);
    }

    public void replaceEnabledToggles(java.util.Set<ResourceLocation> enabled)
    {
        toggles.clear();
        toggles.addAll(enabled);
    }

    public void setToggleEnabled(ResourceLocation id, boolean state)
    {
        if (state)
        {
            toggles.add(id);
        }
        else
        {
            toggles.remove(id);
        }
    }

    public void copyFrom(SHPlayerData other)
    {
        other.data.copyTo(data);
        hero = other.hero;
        prevHero = other.prevHero;
        suitDisabledTime = other.suitDisabledTime;
        toggles.clear();
        toggles.addAll(other.toggles);
        spellCooldowns.clear();
        spellCooldowns.putAll(other.spellCooldowns);
    }

    public void tick()
    {
        heroChangeTime++;

        if (suitDisabledTime > 0)
        {
            suitDisabledTime--;
        }
    }

    public void writeTo(CompoundTag tag)
    {
        if (hero != null)
        {
            tag.putString("Hero", hero.getHero().getRegistryName().toString());

            if (hero.getKey() != null)
            {
                tag.putString("HeroIteration", hero.getKey());
            }
        }

        CompoundTag dataTag = new CompoundTag();
        data.writeTo(dataTag);

        if (!dataTag.isEmpty())
        {
            tag.put("Data", dataTag);
        }

        if (!toggles.isEmpty())
        {
            net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();

            for (ResourceLocation id : toggles)
            {
                list.add(net.minecraft.nbt.StringTag.valueOf(id.toString()));
            }

            tag.put("Toggles", list);
        }

        if (!spellCooldowns.isEmpty())
        {
            CompoundTag cooldowns = new CompoundTag();
            spellCooldowns.forEach((id, until) -> cooldowns.putLong(id.toString(), until));
            tag.put("SpellCooldowns", cooldowns);
        }
    }

    public void readFrom(CompoundTag tag)
    {
        if (tag.contains("Hero"))
        {
            ResourceLocation id = ResourceLocation.tryParse(tag.getString("Hero"));

            if (id != null)
            {
                Hero value = Hero.REGISTRY.getHero(id);
                hero = value != null ? value.getIteration(tag.getString("HeroIteration")) : null;
            }
        }

        if (tag.contains("Data"))
        {
            data.readFrom(tag.getCompound("Data"));
        }

        toggles.clear();

        if (tag.contains("Toggles"))
        {
            net.minecraft.nbt.ListTag list = tag.getList("Toggles", net.minecraft.nbt.Tag.TAG_STRING);

            for (int i = 0; i < list.size(); ++i)
            {
                ResourceLocation id = ResourceLocation.tryParse(list.getString(i));

                if (id != null)
                {
                    toggles.add(id);
                }
            }
        }

        spellCooldowns.clear();
        if (tag.contains("SpellCooldowns"))
        {
            CompoundTag cooldowns = tag.getCompound("SpellCooldowns");
            for (String key : cooldowns.getAllKeys())
            {
                ResourceLocation id = ResourceLocation.tryParse(key);
                if (id != null)
                {
                    long until = cooldowns.getLong(key);
                    if (until > 0L) spellCooldowns.put(id, until);
                }
            }
        }
    }

    public void reset()
    {
        hero = null;
        prevHero = null;
        data.resetAll();
        toggles.clear();
        spellCooldowns.clear();
    }
}
