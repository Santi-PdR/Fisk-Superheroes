package com.fiskmods.heroes.common.network;

import com.fiskmods.heroes.client.sound.SHSoundPlayer;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * Server -&gt; client: play one of the pack's sounds.
 * <p>
 * The mod's sounds are declared by the pack and only exist as real sound events on the client (the
 * audio itself comes from the downloaded sound repository), so the server cannot use the vanilla
 * sound packet. It sends the definition id and the client resolves and plays it, including the
 * loop/fade behaviour declared by the sound definition.
 */
public class PacketPlaySound extends SHPacket
{
    private final ResourceLocation id;
    private final int entityId;
    private final double x;
    private final double y;
    private final double z;
    private final SoundSource source;
    private final float volume;
    private final float pitch;
    private final boolean loop;
    private final int fadeIn;
    private final int fadeOut;
    private final int delay;
    private final float range;

    public PacketPlaySound(ResourceLocation id, int entityId, double x, double y, double z, SoundSource source,
            float volume, float pitch, boolean loop, int fadeIn, int fadeOut, int delay, float range)
    {
        this.id = id;
        this.entityId = entityId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.source = source;
        this.volume = volume;
        this.pitch = pitch;
        this.loop = loop;
        this.fadeIn = fadeIn;
        this.fadeOut = fadeOut;
        this.delay = delay;
        this.range = range;
    }

    public PacketPlaySound(FriendlyByteBuf buf)
    {
        id = buf.readResourceLocation();
        entityId = buf.readVarInt();
        x = buf.readDouble();
        y = buf.readDouble();
        z = buf.readDouble();
        source = buf.readEnum(SoundSource.class);
        volume = buf.readFloat();
        pitch = buf.readFloat();
        loop = buf.readBoolean();
        fadeIn = buf.readVarInt();
        fadeOut = buf.readVarInt();
        delay = buf.readVarInt();
        range = buf.readFloat();
    }

    @Override
    public void encode(FriendlyByteBuf buf)
    {
        buf.writeResourceLocation(id);
        buf.writeVarInt(entityId);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeEnum(source);
        buf.writeFloat(volume);
        buf.writeFloat(pitch);
        buf.writeBoolean(loop);
        buf.writeVarInt(fadeIn);
        buf.writeVarInt(fadeOut);
        buf.writeVarInt(delay);
        buf.writeFloat(range);
    }

    @Override
    public void handle(NetworkEvent.Context context)
    {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> SHSoundPlayer.play(this));
    }

    public ResourceLocation getId()
    {
        return id;
    }

    public int getEntityId()
    {
        return entityId;
    }

    public double getX()
    {
        return x;
    }

    public double getY()
    {
        return y;
    }

    public double getZ()
    {
        return z;
    }

    public SoundSource getSource()
    {
        return source;
    }

    public float getVolume()
    {
        return volume;
    }

    public float getPitch()
    {
        return pitch;
    }

    public boolean isLoop()
    {
        return loop;
    }

    public int getFadeIn()
    {
        return fadeIn;
    }

    public int getFadeOut()
    {
        return fadeOut;
    }

    public int getDelay()
    {
        return delay;
    }

    public float getRange()
    {
        return range;
    }
}
