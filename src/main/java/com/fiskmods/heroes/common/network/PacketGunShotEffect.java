package com.fiskmods.heroes.common.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Sent only after the server accepts a shot; contains no client-authoritative gameplay data. */
public final class PacketGunShotEffect extends SHPacket
{
    private final int entityId;
    private final boolean offhand;

    public PacketGunShotEffect(int entityId, boolean offhand)
    {
        this.entityId = entityId;
        this.offhand = offhand;
    }

    public PacketGunShotEffect(FriendlyByteBuf buf)
    {
        entityId = buf.readVarInt();
        offhand = buf.readBoolean();
    }

    @Override
    public void encode(FriendlyByteBuf buf)
    {
        buf.writeVarInt(entityId);
        buf.writeBoolean(offhand);
    }

    @Override
    public void handle(NetworkEvent.Context context)
    {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.fiskmods.heroes.client.render.GunVisualEffects.onShot(entityId, offhand));
    }
}
