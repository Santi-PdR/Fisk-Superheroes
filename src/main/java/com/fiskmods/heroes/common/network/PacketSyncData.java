package com.fiskmods.heroes.common.network;

import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.data.SHPlayerData;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * Server -> client: the changed data variables of an entity. Only values flagged dirty since the
 * previous tick are sent.
 */
public class PacketSyncData extends SHPacket
{
    private final int entityId;
    private final CompoundTag tag;

    public PacketSyncData(int entityId, CompoundTag tag)
    {
        this.entityId = entityId;
        this.tag = tag;
    }

    public PacketSyncData(FriendlyByteBuf buf)
    {
        entityId = buf.readVarInt();
        tag = buf.readNbt();
    }

    @Override
    public void encode(FriendlyByteBuf buf)
    {
        buf.writeVarInt(entityId);
        buf.writeNbt(tag);
    }

    @Override
    public void handle(NetworkEvent.Context context)
    {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleClient());
    }

    private void handleClient()
    {
        Entity entity = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getEntity(entityId) : null;

        if (entity != null)
        {
            SHPlayerData data = SHDataCapabilities.getPlayer(entity);

            if (data != null && tag != null)
            {
                data.getData().readUpdate(tag);
            }
        }
    }
}
