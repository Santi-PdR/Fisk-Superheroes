package com.fiskmods.heroes.client.sound;

import com.fiskmods.heroes.common.sound.SHSounds;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;

/**
 * A looping pack sound: follows the entity it belongs to and honours the fade in / fade out / delay
 * properties declared by the sound definition (the original mod faded looping suit sounds in and
 * out instead of cutting them).
 */
public class SHLoopingSound extends AbstractSoundInstance implements TickableSoundInstance
{
    private final ResourceLocation definitionId;
    private final int entityId;
    private final float baseVolume;
    private final int fadeIn;
    private final int fadeOut;
    private final float range;

    private int ticks;
    private int fadingOut = -1;
    private boolean stopped;

    public SHLoopingSound(ResourceLocation definitionId, int entityId, double x, double y, double z, SoundSource source,
            float volume, float pitch, int fadeIn, int fadeOut, int delay, float range)
    {
        super(definitionId, source, RandomSource.create());
        this.definitionId = definitionId;
        this.entityId = entityId;
        this.baseVolume = volume;
        this.fadeIn = Math.max(0, fadeIn);
        this.fadeOut = Math.max(0, fadeOut);
        this.range = range;

        this.x = x;
        this.y = y;
        this.z = z;
        this.volume = fadeIn > 0 ? 0.0F : volume;
        this.pitch = pitch;
        this.looping = true;
        this.delay = Math.max(0, delay);

        if (entityId != 0)
        {
            this.relative = false;
        }
    }

    public ResourceLocation getDefinitionId()
    {
        return definitionId;
    }

    public int getEntityId()
    {
        return entityId;
    }

    public boolean isStopped()
    {
        return stopped;
    }

    /** Starts the fade-out; the instance goes away once it has finished. */
    public void fadeOut()
    {
        if (fadingOut < 0)
        {
            fadingOut = 0;
        }
    }

    @Override
    public boolean canPlaySound()
    {
        return SHSounds.isDefined(definitionId) || Minecraft.getInstance().getSoundManager() != null;
    }

    @Override
    public void tick()
    {
        if (stopped)
        {
            return;
        }

        ++ticks;

        if (entityId != 0)
        {
            Entity entity = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getEntity(entityId) : null;

            if (entity == null || !entity.isAlive())
            {
                stopped = true;
                return;
            }

            x = entity.getX();
            y = entity.getY();
            z = entity.getZ();
        }

        if (fadingOut >= 0)
        {
            fadingOut++;

            if (fadeOut <= 0 || fadingOut >= fadeOut)
            {
                stopped = true;
                return;
            }

            volume = baseVolume * (1.0F - (float) fadingOut / fadeOut);
            return;
        }

        if (fadeIn > 0)
        {
            float progress = Mth.clamp((float) ticks / fadeIn, 0.0F, 1.0F);
            volume = baseVolume * progress;
        }

        // Stop when the listener has walked out of the sound's range
        if (range > 0.0F && Minecraft.getInstance().player != null)
        {
            double distance = Minecraft.getInstance().player.distanceToSqr(x, y, z);

            if (distance > (double) range * range)
            {
                fadeOut();
            }
        }
    }

    @Override
    public boolean isLooping()
    {
        return !stopped;
    }

    @Override
    public boolean canStartSilent()
    {
        return true;
    }

    @Override
    public SoundInstance.Attenuation getAttenuation()
    {
        return SoundInstance.Attenuation.LINEAR;
    }
}
