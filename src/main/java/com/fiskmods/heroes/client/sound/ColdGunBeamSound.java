package com.fiskmods.heroes.client.sound;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.data.var.Vars;
import com.fiskmods.heroes.common.item.ItemColdGun;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** A client-side moving loop driven by the server's validated Cold Gun use state. */
public final class ColdGunBeamSound extends AbstractSoundInstance implements TickableSoundInstance
{
    private static final ResourceLocation EVENT = FiskHeroes.id("item.coldgun.beam.loop");
    private static final double RANGE_SQUARED = 64.0D * 64.0D;
    private static final boolean DIAGNOSTICS = Boolean.getBoolean("fiskheroes.ciColdGunSoundDiagnostics");
    private static final Map<UUID, ColdGunBeamSound> SOUNDS = new HashMap<>();
    private static ClientLevel lastLevel;

    private final Player owner;
    private boolean stopped;
    private boolean stereo;
    private int ticks;
    private int observedTicks;

    private ColdGunBeamSound(Player owner)
    {
        super(EVENT, SoundSource.PLAYERS, RandomSource.create());
        this.owner = owner;
        this.looping = true;
        this.relative = false;
        this.volume = 0.0F;
        this.pitch = 1.0F;
        this.x = owner.getX();
        this.y = owner.getY();
        this.z = owner.getZ();
    }

    private static boolean active(Player player)
    {
        var data = SHDataCapabilities.getPlayer(player);
        return player.isAlive() && player.getMainHandItem().getItem() instanceof ItemColdGun
                && data != null && data.getData().get(Vars.COLD_GUN_BEAM_ACTIVE);
    }

    /** Also discovers players who begin tracking an already-firing gun or enter its range. */
    public static void tickClient()
    {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level != lastLevel || level == null || mc.player == null)
        {
            SOUNDS.values().forEach(mc.getSoundManager()::stop);
            SOUNDS.clear();
            lastLevel = level;
            if (level == null || mc.player == null) return;
        }

        Entity listener = mc.getCameraEntity() != null ? mc.getCameraEntity() : mc.player;
        Iterator<ColdGunBeamSound> iterator = SOUNDS.values().iterator();
        while (iterator.hasNext())
        {
            ColdGunBeamSound sound = iterator.next();
            Player owner = sound.owner;
            ++sound.observedTicks;
            // Resource reloads and audio device resets can invalidate a sound without
            // changing beam state. Retry at a bounded rate, never every frame.
            if (level.getEntity(owner.getId()) != owner || !active(owner)
                    || listener.distanceToSqr(owner) > RANGE_SQUARED
                    || (sound.observedTicks >= 100 && !mc.getSoundManager().isActive(sound)))
            {
                sound.stopped = true;
                mc.getSoundManager().stop(sound);
                iterator.remove();
                if (DIAGNOSTICS) FiskHeroes.LOGGER.info("Cold gun loop stopped: {}", owner.getName().getString());
            }
        }

        // A missing pack must not leave dead sound instances or log a warning every tick.
        if (mc.getSoundManager().getSoundEvent(EVENT) == null) return;
        for (Player owner : level.players())
        {
            if (active(owner) && listener.distanceToSqr(owner) <= RANGE_SQUARED && !SOUNDS.containsKey(owner.getUUID()))
            {
                ColdGunBeamSound sound = new ColdGunBeamSound(owner);
                SOUNDS.put(owner.getUUID(), sound);
                mc.getSoundManager().play(sound);
                if (DIAGNOSTICS) FiskHeroes.LOGGER.info("Cold gun loop started: {}", owner.getName().getString());
            }
        }
    }

    @Override
    @Nullable
    public WeighedSoundEvents resolve(SoundManager manager)
    {
        WeighedSoundEvents event = super.resolve(manager);
        stereo = false;
        if (event != null && getSound() != null)
        {
            // OpenAL does not spatialize stereo buffers. The original Cold Gun recording is
            // stereo, so apply distance gain ourselves; mono replacements retain native panning.
            try (var in = Minecraft.getInstance().getResourceManager().open(getSound().getPath()))
            {
                stereo = com.fiskmods.heroes.common.sound.OggVorbisChannels.read(in.readNBytes(512)) > 1;
            }
            catch (java.io.IOException ignored)
            {
                // A missing or non-Vorbis replacement still follows the normal sound pipeline.
            }
        }
        return event;
    }

    @Override
    public void tick()
    {
        if (!active(owner))
        {
            stopped = true;
            return;
        }
        x = owner.getX();
        y = owner.getY();
        z = owner.getZ();
        // Original GUN_SHOOTING_TIMER rises over three ticks while the gun is in use.
        volume = Mth.clamp(++ticks / 3.0F, 0.0F, 1.0F);
        Minecraft mc = Minecraft.getInstance();
        Entity listener = mc.getCameraEntity() != null ? mc.getCameraEntity() : mc.player;
        if (stereo && listener != null && listener != owner)
            volume *= Mth.clamp(1.0F - (float) Math.sqrt(listener.distanceToSqr(owner)) / 64.0F, 0.0F, 1.0F);
        if (DIAGNOSTICS && (ticks == 3 || ticks % 40 == 0))
            FiskHeroes.LOGGER.info("Cold gun loop ticked: {}, volume={}, active={}",
                    owner.getName().getString(), volume, Minecraft.getInstance().getSoundManager().isActive(this));
    }

    @Override
    public SoundInstance.Attenuation getAttenuation()
    {
        return stereo ? SoundInstance.Attenuation.NONE : SoundInstance.Attenuation.LINEAR;
    }

    @Override public boolean isStopped() { return stopped; }
    @Override public boolean canStartSilent() { return true; }
}
