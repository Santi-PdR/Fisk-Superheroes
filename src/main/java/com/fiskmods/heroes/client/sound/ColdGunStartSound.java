package com.fiskmods.heroes.client.sound;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.annotation.Nullable;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.data.var.Vars;
import com.fiskmods.heroes.common.item.ItemColdGun;
import com.fiskmods.heroes.common.network.PacketPlaySound;
import com.fiskmods.heroes.common.sound.SHSounds;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** One-shot Cold Gun start clip, attached to its shooter while the beam is active. */
public final class ColdGunStartSound extends AbstractSoundInstance implements TickableSoundInstance
{
    private static final boolean DIAGNOSTICS = Boolean.getBoolean("fiskheroes.ciColdGunSoundDiagnostics");
    private static final int PENDING_TICKS = 20;
    private static final Map<Integer, Pending> PENDING = new HashMap<>();
    private static final Map<Integer, ColdGunStartSound> PLAYING = new HashMap<>();
    private static ClientLevel lastLevel;

    private record Pending(PacketPlaySound packet, int ticks) {}

    private final Player owner;
    private final float baseVolume;
    private final float range;
    private boolean stopped;
    private boolean stereo;
    private int observedTicks;
    private boolean movedLogged;
    private final double initialX;

    private ColdGunStartSound(Player owner, PacketPlaySound packet)
    {
        super(SHSounds.get(packet.getId()).getLocation(), packet.getSource(), RandomSource.create());
        this.owner = owner;
        baseVolume = packet.getVolume();
        range = Math.max(1.0F, packet.getRange());
        volume = baseVolume;
        pitch = packet.getPitch();
        relative = false;
        looping = false;
        x = initialX = owner.getX();
        y = owner.getY();
        z = owner.getZ();
    }

    private static boolean active(Player player)
    {
        var data = SHDataCapabilities.getPlayer(player);
        return player.isAlive() && player.getMainHandItem().getItem() instanceof ItemColdGun
                && data != null && data.getData().get(Vars.COLD_GUN_BEAM_ACTIVE);
    }

    private static void checkWorld(Minecraft mc)
    {
        if (lastLevel != mc.level || mc.level == null)
        {
            PLAYING.values().forEach(mc.getSoundManager()::stop);
            PLAYING.clear();
            PENDING.clear();
            lastLevel = mc.level;
        }
    }

    /** The sound packet can precede the active data sync by a tick. */
    public static void queue(PacketPlaySound packet)
    {
        Minecraft mc = Minecraft.getInstance();
        checkWorld(mc);
        if (mc.level == null || packet.getEntityId() == 0) return;
        stop(packet.getEntityId(), "restart");
        PENDING.put(packet.getEntityId(), new Pending(packet, 0));
    }

    /** Cancel even a queued start on receipt of the authoritative stop cue. */
    public static void stop(int entityId, String reason)
    {
        PENDING.remove(entityId);
        ColdGunStartSound sound = PLAYING.remove(entityId);
        if (sound == null) return;
        SoundManager manager = Minecraft.getInstance().getSoundManager();
        boolean playing = manager.isActive(sound);
        sound.stopped = true;
        manager.stop(sound);
        if (DIAGNOSTICS) FiskHeroes.LOGGER.info("Cold gun start stopped: {}, reason={}, playing={}",
                sound.owner.getName().getString(), reason, playing);
    }

    public static void tickClient()
    {
        Minecraft mc = Minecraft.getInstance();
        checkWorld(mc);
        if (mc.level == null) return;
        Iterator<Map.Entry<Integer, ColdGunStartSound>> running = PLAYING.entrySet().iterator();
        while (running.hasNext())
        {
            ColdGunStartSound sound = running.next().getValue();
            boolean invalid = mc.level.getEntity(sound.owner.getId()) != sound.owner || !active(sound.owner);
            if (invalid || sound.stopped || (++sound.observedTicks > 40 && !mc.getSoundManager().isActive(sound)))
            {
                boolean playing = mc.getSoundManager().isActive(sound);
                sound.stopped = true;
                mc.getSoundManager().stop(sound);
                running.remove();
                if (DIAGNOSTICS) FiskHeroes.LOGGER.info("Cold gun start stopped: {}, reason={}, playing={}",
                        sound.owner.getName().getString(), invalid ? "inactive" : "finished", playing);
            }
        }
        Iterator<Map.Entry<Integer, Pending>> pending = PENDING.entrySet().iterator();
        while (pending.hasNext())
        {
            var entry = pending.next();
            Pending request = entry.getValue();
            Entity entity = mc.level.getEntity(entry.getKey());
            if (entity instanceof Player owner && active(owner))
            {
                var event = SHSounds.get(request.packet().getId());
                if (event != null && mc.getSoundManager().getSoundEvent(event.getLocation()) != null)
                {
                    ColdGunStartSound sound = new ColdGunStartSound(owner, request.packet());
                    PLAYING.put(entry.getKey(), sound);
                    mc.getSoundManager().play(sound);
                    if (DIAGNOSTICS) FiskHeroes.LOGGER.info("Cold gun start attached: {}", owner.getName().getString());
                }
                pending.remove();
            }
            else if (request.ticks() >= PENDING_TICKS) pending.remove();
            else entry.setValue(new Pending(request.packet(), request.ticks() + 1));
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
            try (var in = Minecraft.getInstance().getResourceManager().open(getSound().getPath()))
            {
                stereo = com.fiskmods.heroes.common.sound.OggVorbisChannels.read(in.readNBytes(512)) > 1;
            }
            catch (java.io.IOException ignored) { }
        }
        updateVolume();
        return event;
    }

    @Override
    public void tick()
    {
        if (!active(owner)) { stopped = true; return; }
        x = owner.getX(); y = owner.getY(); z = owner.getZ();
        updateVolume();
        if (DIAGNOSTICS && !movedLogged && Math.abs(x - initialX) > 2.0D)
        {
            movedLogged = true;
            FiskHeroes.LOGGER.info("Cold gun start moved: {}, x={}, y={}, z={}", owner.getName().getString(), x, y, z);
        }
    }

    private void updateVolume()
    {
        volume = baseVolume;
        Minecraft mc = Minecraft.getInstance();
        Entity listener = mc.getCameraEntity() != null ? mc.getCameraEntity() : mc.player;
        if (stereo && listener != null && listener != owner)
            volume *= Mth.clamp(1.0F - (float) Math.sqrt(listener.distanceToSqr(owner)) / range, 0.0F, 1.0F);
    }

    @Override public SoundInstance.Attenuation getAttenuation()
    {
        return stereo ? SoundInstance.Attenuation.NONE : SoundInstance.Attenuation.LINEAR;
    }
    @Override public boolean isStopped() { return stopped; }
}
