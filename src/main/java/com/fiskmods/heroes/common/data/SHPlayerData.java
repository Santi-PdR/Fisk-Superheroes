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

    private HeroIteration hero;
    private HeroIteration prevHero;
    private int heroChangeTime;
    private final java.util.Set<ResourceLocation> toggles = new java.util.LinkedHashSet<>();
    private final java.util.Map<ResourceLocation, Boolean> soundStates = new java.util.HashMap<>();

    /** Ticks the suit has been unusable (all pieces removed while powers were active). */
    private int suitDisabledTime;

    private boolean invulnerable;

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
    }

    public void reset()
    {
        hero = null;
        prevHero = null;
        data.resetAll();
    }
}
