package com.fiskmods.heroes.common.network;

import com.fiskmods.heroes.common.hero.ability.WebSwingHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/** Requests one web interaction; the server resolves its own target and validates the suit. */
public final class PacketWebSwing extends SHPacket
{
    public PacketWebSwing() { }
    public PacketWebSwing(FriendlyByteBuf buf) { }

    @Override
    public void encode(FriendlyByteBuf buf) { }

    @Override
    public void handle(NetworkEvent.Context context)
    {
        if (context.getSender() != null) WebSwingHandler.request(context.getSender());
    }
}
