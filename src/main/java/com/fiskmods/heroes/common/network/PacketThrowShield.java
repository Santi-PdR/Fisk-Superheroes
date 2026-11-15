package com.fiskmods.heroes.common.network;

import com.fiskmods.heroes.common.hero.ability.AbilityHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** Client primary-attack request; all equipment, ability and cooldown checks run on the server. */
public final class PacketThrowShield extends SHPacket
{
    public PacketThrowShield() {}
    public PacketThrowShield(FriendlyByteBuf buf) {}
    @Override public void encode(FriendlyByteBuf buf) {}

    @Override
    public void handle(NetworkEvent.Context context)
    {
        ServerPlayer player = context.getSender();
        if (player != null) AbilityHandler.throwShield(player);
    }
}
