package com.fiskmods.heroes.common.sound;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.network.PacketPlaySound;
import com.fiskmods.heroes.common.network.PacketStopSound;
import com.fiskmods.heroes.common.network.SHNetwork;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
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
 * <p>
 * Playback is server-authoritative: the server decides what plays and sends
 * {@link PacketPlaySound}/{@link PacketStopSound} to the players that should hear it, while the
 * client resolves the definition and plays the audio.
 */
public final class SHSounds
{
    private static final Map<ResourceLocation, SoundDefinition> DEFINITIONS = new LinkedHashMap<>();
    private static final Map<ResourceLocation, SoundEvent> EVENTS = new HashMap<>();

    private static String repository = "FiskFille/Superheroes";
    private static int repositoryVersion = 3;

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
            ResourceLocation parentId = definition.getParent();

            if (parentId != null)
            {
                SoundDefinition inherited = DEFINITIONS.get(parentId);

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

    /** The sound repository and version declared by the loaded pack. */
    public static void setDownloadInfo(String repo, int version)
    {
        if (repo != null && !repo.isEmpty())
        {
            repository = repo;
        }

        if (version > 0)
        {
            repositoryVersion = version;
        }
    }

    public static String getRepository()
    {
        return repository;
    }

    public static int getRepositoryVersion()
    {
        return repositoryVersion;
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

    /* --- Playback --- */

    /** Plays a one-shot sound at a position, for everyone within the definition's range. */
    public static void play(Level level, double x, double y, double z, ResourceLocation id, SoundSource source, float volume, float pitch)
    {
        SoundDefinition definition = DEFINITIONS.get(id);
        float volumeScale = definition != null ? definition.getVolume().getFloat(null, 1.0F) : 1.0F;
        float pitchScale = definition != null ? definition.getPitch().getFloat(null, 1.0F) : 1.0F;
        send(level, null, id, x, y, z, source, volume * volumeScale, pitch * pitchScale, false);
    }

    /** Plays a one-shot sound on an entity. */
    public static void play(Entity entity, ResourceLocation id, SoundSource source, float volume, float pitch)
    {
        if (entity == null || entity.level().isClientSide)
        {
            return;
        }

        SoundDefinition definition = DEFINITIONS.get(id);
        float volumeScale = definition != null ? definition.getVolume().getFloat(null, 1.0F) : 1.0F;
        float pitchScale = definition != null ? definition.getPitch().getFloat(null, 1.0F) : 1.0F;

        send(entity.level(), entity, id, entity.getX(), entity.getY(), entity.getZ(), source, volume * volumeScale, pitch * pitchScale, false);
    }

    /** Plays a sound by its pack id, taking the volume and pitch declared by the definition. */
    public static void play(Entity entity, ResourceLocation id)
    {
        play(entity, id, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    /**
     * Starts (or restarts) a looping pack sound attached to an entity. Looping sounds follow the
     * entity on the client and fade in and out as their definition declares.
     */
    public static void playLoop(Entity entity, ResourceLocation id)
    {
        if (entity == null || entity.level().isClientSide)
        {
            return;
        }

        SoundDefinition definition = DEFINITIONS.get(id);
        send(entity.level(), entity, id, entity.getX(), entity.getY(), entity.getZ(), SoundSource.PLAYERS,
                definition != null ? definition.getVolume().getFloat(null, 1.0F) : 1.0F,
                definition != null ? definition.getPitch().getFloat(null, 1.0F) : 1.0F, true);
    }

    /** Stops a looping sound previously started with {@link #playLoop}. */
    public static void stopLoop(Entity entity, ResourceLocation id)
    {
        if (entity == null || entity.level().isClientSide)
        {
            return;
        }

        SHNetwork.sendToTracking(new PacketStopSound(id, entity.getId()), entity);
    }

    private static void send(Level level, Entity entity, ResourceLocation id, double x, double y, double z,
            SoundSource source, float volume, float pitch, boolean loop)
    {
        if (volume <= 0.0F || !(level instanceof ServerLevel))
        {
            return;
        }

        SoundDefinition definition = DEFINITIONS.get(id);
        float range = definition != null ? (float) definition.getRange() : 16.0F;
        int fadeIn = loop && definition != null ? definition.getFadeIn() : 0;
        int fadeOut = loop && definition != null ? definition.getFadeOut() : 0;
        int delay = loop && definition != null ? definition.getDelay() : 0;

        PacketPlaySound packet = new PacketPlaySound(id, entity != null ? entity.getId() : 0, x, y, z, source,
                volume, pitch, loop, fadeIn, fadeOut, delay, range);

        if (entity != null)
        {
            SHNetwork.sendToTracking(packet, entity);
        }
        else
        {
            SHNetwork.sendToNearby(packet, level, x, y, z, range);
        }
    }

    public static void logUnknown(ResourceLocation id)
    {
        FiskHeroes.LOGGER.debug("Reference to unknown sound {}", id);
    }
}
