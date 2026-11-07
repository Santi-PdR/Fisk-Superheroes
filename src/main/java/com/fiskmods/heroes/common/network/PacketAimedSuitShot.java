package com.fiskmods.heroes.common.network;

import com.fiskmods.heroes.common.hero.ability.AbilityHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/** Request an aimed suit shot. All eligibility and cooldown checks are server-side. */
public final class PacketAimedSuitShot extends SHPacket
{
    public PacketAimedSuitShot() { }
    public PacketAimedSuitShot(FriendlyByteBuf buf) { }

    @Override
    public void encode(FriendlyByteBuf buf) { }

    @Override
    public void handle(NetworkEvent.Context context)
    {
        if (context.getSender() != null) AbilityHandler.fireAimedSuitShot(context.getSender());
    }
}
