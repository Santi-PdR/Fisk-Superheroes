package com.fiskmods.heroes.client.sound;

/**
 * The {@code sound} object a sound override function receives.
 * <p>
 * It is the counterpart of the original mod's {@code JSSound}: the script reads how far the sound
 * is through its fade and how long it has been playing, and writes a new volume and pitch.
 */
public class JSSound
{
    private final SHLoopingSound sound;

    public JSSound(SHLoopingSound sound)
    {
        this.sound = sound;
    }

    public void setVolume(float volume)
    {
        sound.setDynamicVolume(volume);
    }

    public void setPitch(float pitch)
    {
        sound.setDynamicPitch(pitch);
    }

    public float volume()
    {
        return sound.getBaseVolume();
    }

    public float pitch()
    {
        return sound.getBasePitch();
    }

    public int ticksPlaying()
    {
        return sound.getTicksPlaying();
    }

    public float fadeProgress()
    {
        return sound.getFadeProgress();
    }

    public boolean isLooping()
    {
        return sound.isLooping();
    }
}
