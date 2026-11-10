package com.fiskmods.heroes.common.network;

import com.fiskmods.heroes.common.hero.equipment.EquipmentHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** Requests the server-selected gadget; the client never chooses an item or bypasses cooldowns. */
public final class PacketUseUtilityBelt extends SHPacket
{
    public PacketUseUtilityBelt() {}
    public PacketUseUtilityBelt(FriendlyByteBuf buffer) {}

    @Override public void encode(FriendlyByteBuf buffer) {}

    @Override
    public void handle(NetworkEvent.Context context)
    {
        ServerPlayer player = context.getSender();
        if (player != null && player.getMainHandItem().isEmpty() && player.getOffhandItem().isEmpty())
            EquipmentHelper.useUtilityBelt(player);
    }
}
