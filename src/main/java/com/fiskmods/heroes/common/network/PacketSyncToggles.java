package com.fiskmods.heroes.common.network;

import java.util.LinkedHashSet;
import java.util.Set;

import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.data.SHPlayerData;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Server to client snapshot of modifier toggles, which are stored outside the data-variable map. */
public final class PacketSyncToggles extends SHPacket
{
    private final int entityId;
    private final Set<ResourceLocation> enabled;

    public PacketSyncToggles(int entityId, Set<ResourceLocation> enabled)
    {
        this.entityId = entityId;
        this.enabled = Set.copyOf(enabled);
    }

    public PacketSyncToggles(FriendlyByteBuf buf)
    {
        entityId = buf.readVarInt();
        int size = buf.readVarInt();
        enabled = new LinkedHashSet<>();
        for (int i = 0; i < size; ++i)
        {
            enabled.add(buf.readResourceLocation());
        }
    }

    @Override
    public void encode(FriendlyByteBuf buf)
    {
        buf.writeVarInt(entityId);
        buf.writeVarInt(enabled.size());
        enabled.forEach(buf::writeResourceLocation);
    }

    @Override
    public void handle(NetworkEvent.Context context)
    {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleClient());
    }

    private void handleClient()
    {
        Entity entity = ClientEntityLookup.get(entityId);
        SHPlayerData data = SHDataCapabilities.getPlayer(entity);
        if (data != null)
        {
            data.replaceEnabledToggles(enabled);
        }
    }
}
