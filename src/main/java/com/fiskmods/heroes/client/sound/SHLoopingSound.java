package com.fiskmods.heroes.client.sound;

import javax.annotation.Nullable;

import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.data.var.Vars;
import com.fiskmods.heroes.common.sound.SHSounds;
import com.fiskmods.heroes.common.sound.SoundDefinition;
import com.fiskmods.heroes.pack.js.SoundOverride;

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
 * A looping pack sound: follows the entity it belongs to and honours the definition it was started
 * from — its fade in / fade out / delay, its condition and its script override, which is what makes
 * the suit sounds respond to what the entity is doing (the original faded and modulated looping
 * sounds instead of cutting them).
 */
public class SHLoopingSound extends AbstractSoundInstance implements TickableSoundInstance
{
    private final ResourceLocation definitionId;
    private final int entityId;
    private final int fadeIn;
    private final int fadeOut;
    private final float range;

    @Nullable
    private SoundOverride override;
    private boolean overrideResolved;

    private float baseVolume;
    private float basePitch;
    private float dynamicVolume = 1.0F;
    private float dynamicPitch = 1.0F;
    private float rangeScale = 1.0F;

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
        this.basePitch = pitch;
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

    public int getTicksPlaying()
    {
        return ticks;
    }

    public float getBaseVolume()
    {
        return baseVolume;
    }

    public float getBasePitch()
    {
        return basePitch;
    }

    public void setDynamicVolume(float volume)
    {
        dynamicVolume = volume;
    }

    public void setDynamicPitch(float pitch)
    {
        dynamicPitch = pitch;
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

    /** How far the sound is through its fade in (1 = fully audible), as the pack scripts read it. */
    public float getFadeProgress()
    {
        if (fadingOut >= 0)
        {
            return fadeOut > 0 ? 1.0F - (float) fadingOut / fadeOut : 0.0F;
        }

        return fadeIn > 0 ? Mth.clamp((float) ticks / fadeIn, 0.0F, 1.0F) : 1.0F;
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

        Entity entity = entityId != 0 && Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getEntity(entityId) : null;

        if (entityId != 0)
        {
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

            volume = baseVolume * dynamicVolume * (1.0F - (float) fadingOut / fadeOut);
            return;
        }

        SoundDefinition definition = SHSounds.getDefinition(definitionId);

        if (definition != null)
        {
            // The definition decides whether the sound may keep playing
            if (ticks > 1 && !definition.getCondition().shouldContinue(entity))
            {
                fadeOut();
                return;
            }

            if (!overrideResolved)
            {
                overrideResolved = true;
                override = SoundOverride.compile(definition.getOverrideSource());
            }

            if (override != null && entity != null)
            {
                // The script sets the volume and pitch itself and reports whether to keep playing
                dynamicVolume = 1.0F;
                dynamicPitch = 1.0F;

                if (!override.continuePlaying(entity, new JSSound(this)))
                {
                    fadeOut();
                    return;
                }
            }
            else
            {
                dynamicVolume = definition.getVolume().getFloat(entity, 1.0F);
                dynamicPitch = definition.getPitch().getFloat(entity, 1.0F);
            }

            // Bigger or smaller entities carry the sound further, exactly like the original
            if (definition.isScale() && entity instanceof net.minecraft.world.entity.LivingEntity living && !Float.isInfinite(range))
            {
                float scale = Vars.getScale(living);
                rangeScale = scale < 1.0F ? (float) Math.pow(scale, 2.0D / 3.0D) : scale;
            }
        }

        float fade = getFadeProgress();
        volume = baseVolume * dynamicVolume * fade;
        pitch = basePitch * dynamicPitch;

        // Stop when the listener has walked out of the sound's range
        if (range > 0.0F && Minecraft.getInstance().player != null)
        {
            double distance = Minecraft.getInstance().player.distanceToSqr(x, y, z);

            if (distance > (double) range * range * rangeScale * rangeScale)
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
