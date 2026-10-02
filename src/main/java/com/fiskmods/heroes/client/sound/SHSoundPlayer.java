package com.fiskmods.heroes.client.sound;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.fiskmods.heroes.common.network.PacketPlaySound;
import com.fiskmods.heroes.common.network.PacketStopSound;
import com.fiskmods.heroes.common.sound.SHSounds;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

/**
 * Client-side playback of the pack's sounds. One-shot sounds are played as simple sound instances;
 * looping sounds are tracked so they can follow their entity and fade out when the server says so.
 */
public final class SHSoundPlayer
{
    private static final List<SHLoopingSound> LOOPS = new ArrayList<>();

    private SHSoundPlayer()
    {
    }

    public static void play(PacketPlaySound packet)
    {
        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null)
        {
            return;
        }

        SoundEvent event = SHSounds.get(packet.getId());

        if (event == null || packet.getVolume() <= 0.0F)
        {
            return;
        }

        if (packet.isLoop())
        {
            for (SHLoopingSound existing : LOOPS)
            {
                if (existing.getDefinitionId().equals(packet.getId()) && existing.getEntityId() == packet.getEntityId())
                {
                    return;
                }
            }

            SHLoopingSound sound = new SHLoopingSound(packet.getId(), packet.getEntityId(), packet.getX(), packet.getY(), packet.getZ(),
                    packet.getSource(), packet.getVolume(), packet.getPitch(), packet.getFadeIn(), packet.getFadeOut(),
                    packet.getDelay(), packet.getRange());

            LOOPS.add(sound);
            mc.getSoundManager().play(sound);
        }
        else
        {
            mc.getSoundManager().play(new SimpleSoundInstance(event, packet.getSource(), packet.getVolume(),
                    packet.getPitch(), net.minecraft.util.RandomSource.create(), false, packet.getDelay(),
                    net.minecraft.client.resources.sounds.SoundInstance.Attenuation.LINEAR, packet.getX(), packet.getY(), packet.getZ()));
        }
    }

    public static void stop(PacketStopSound packet)
    {
        for (SHLoopingSound sound : LOOPS)
        {
            if (sound.getDefinitionId().equals(packet.getId())
                    && (packet.getEntityId() == 0 || sound.getEntityId() == packet.getEntityId()))
            {
                sound.fadeOut();
            }
        }
    }

    /** Called from the client tick: retires finished looping sounds. */
    public static void tick()
    {
        Iterator<SHLoopingSound> iterator = LOOPS.iterator();

        while (iterator.hasNext())
        {
            SHLoopingSound sound = iterator.next();

            if (sound.isStopped())
            {
                Minecraft.getInstance().getSoundManager().stop(sound);
                iterator.remove();
            }
        }
    }

    /** Resolves a sound event by its pack id, for client-driven sounds (HUD, first person, ...). */
    public static SoundEvent resolve(ResourceLocation id)
    {
        return SHSounds.get(id);
    }
}
