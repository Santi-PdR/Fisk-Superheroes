package com.fiskmods.heroes.common.network;

import com.fiskmods.heroes.common.block.SuitIteratorMenu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** Client -> server packet selecting a suit iteration inside SuitIteratorMenu. */
public final class PacketSuitIteration extends SHPacket
{
    private final String iterationKey;

    public PacketSuitIteration(String iterationKey)
    {
        this.iterationKey = iterationKey != null ? iterationKey : "";
    }

    public PacketSuitIteration(FriendlyByteBuf buf)
    {
        this.iterationKey = buf.readUtf(128);
    }

    @Override
    public void encode(FriendlyByteBuf buf)
    {
        buf.writeUtf(iterationKey, 128);
    }

    @Override
    public void handle(NetworkEvent.Context context)
    {
        ServerPlayer sender = context.getSender();
        if (sender != null && sender.containerMenu instanceof SuitIteratorMenu menu)
        {
            menu.applyIteration(iterationKey);
        }
    }
}
