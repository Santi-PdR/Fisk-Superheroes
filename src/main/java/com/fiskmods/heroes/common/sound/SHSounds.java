package com.fiskmods.heroes.common.sound;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fiskmods.heroes.FiskHeroes;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * The mod's sound registry.
 * <p>
 * 1.7.10 allowed the mod to name a sound at any moment; in 1.20.1 a {@link SoundEvent} has to be a
 * registry object. Pack-defined sounds are therefore resolved through this class, which hands out
 * fixed-range events created from the pack's definitions (the range comes straight from
 * {@code heropack.json}) and falls back to the vanilla registry for anything the game already
 * knows.
 */
public final class SHSounds
{
    private static final Map<ResourceLocation, SoundDefinition> DEFINITIONS = new LinkedHashMap<>();
    private static final Map<ResourceLocation, SoundEvent> EVENTS = new HashMap<>();

    private SHSounds()
    {
    }

    public static void clear()
    {
        DEFINITIONS.clear();
        EVENTS.clear();
    }

    public static void register(SoundDefinition definition)
    {
        DEFINITIONS.put(definition.getId(), definition);
    }

    /** Resolves inheritance for every definition of the current pack. */
    public static void resolveInheritance()
    {
        for (SoundDefinition definition : DEFINITIONS.values())
        {
            SoundDefinition parent = definition.getParent();

            if (parent != null)
            {
                SoundDefinition inherited = DEFINITIONS.get(parent);

                if (inherited != null)
                {
                    definition.inherit(inherited);
                }
            }
        }
    }

    public static int size()
    {
        return DEFINITIONS.size();
    }

    public static SoundDefinition getDefinition(ResourceLocation id)
    {
        return DEFINITIONS.get(id);
    }

    public static boolean isDefined(ResourceLocation id)
    {
        return DEFINITIONS.containsKey(id);
    }

    /**
     * Returns the sound event for the given id: the pack definition when the pack declares it, and
     * otherwise whatever the vanilla registry holds (so scripts may reference vanilla sounds too).
     */
    public static SoundEvent get(ResourceLocation id)
    {
        if (id == null)
        {
            return null;
        }

        SoundEvent cached = EVENTS.get(id);

        if (cached != null)
        {
            return cached;
        }

        SoundDefinition definition = DEFINITIONS.get(id);
        SoundEvent event;

        if (definition != null)
        {
            event = SoundEvent.createFixedRangeEvent(id, (float) definition.getRange());
        }
        else if (ForgeRegistries.SOUND_EVENTS.containsKey(id))
        {
            event = ForgeRegistries.SOUND_EVENTS.getValue(id);
        }
        else
        {
            event = SoundEvent.createVariableRangeEvent(id);
        }

        EVENTS.put(id, event);
        return event;
    }

    /** Plays a pack sound across the level (server side; the clients receive the sound packet). */
    public static void play(Level level, double x, double y, double z, ResourceLocation id, SoundSource source, float volume, float pitch)
    {
        SoundEvent event = get(id);

        if (event != null && volume > 0.0F)
        {
            level.playSound(null, x, y, z, event, source, volume, pitch);
        }
    }

    public static void play(Entity entity, ResourceLocation id, SoundSource source, float volume, float pitch)
    {
        if (entity == null || entity.level().isClientSide)
        {
            return;
        }

        SoundDefinition definition = DEFINITIONS.get(id);
        float volumeScale = definition != null ? definition.getVolume() : 1.0F;
        float pitchScale = definition != null ? definition.getPitch() : 1.0F;

        play(entity.level(), entity.getX(), entity.getY(), entity.getZ(), id, source, volume * volumeScale, pitch * pitchScale);
    }

    /** Plays a sound by its pack id, taking the volume and pitch declared by the definition. */
    public static void play(Entity entity, ResourceLocation id)
    {
        play(entity, id, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    public static void logUnknown(ResourceLocation id)
    {
        FiskHeroes.LOGGER.debug("Reference to unknown sound {}", id);
    }
}
