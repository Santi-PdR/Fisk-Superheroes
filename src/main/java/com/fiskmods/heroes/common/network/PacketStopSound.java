package com.fiskmods.heroes.common.network;

import com.fiskmods.heroes.client.sound.SHSoundPlayer;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Server -&gt; client: stop a looping pack sound (fading it out as its definition declares). */
public class PacketStopSound extends SHPacket
{
    private final ResourceLocation id;
    private final int entityId;

    public PacketStopSound(ResourceLocation id, int entityId)
    {
        this.id = id;
        this.entityId = entityId;
    }

    public PacketStopSound(FriendlyByteBuf buf)
    {
        id = buf.readResourceLocation();
        entityId = buf.readVarInt();
    }

    @Override
    public void encode(FriendlyByteBuf buf)
    {
        buf.writeResourceLocation(id);
        buf.writeVarInt(entityId);
    }

    @Override
    public void handle(NetworkEvent.Context context)
    {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> SHSoundPlayer.stop(this));
    }

    public ResourceLocation getId()
    {
        return id;
    }

    public int getEntityId()
    {
        return entityId;
    }
}
