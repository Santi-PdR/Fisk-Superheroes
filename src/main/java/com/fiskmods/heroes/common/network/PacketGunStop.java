package com.fiskmods.heroes.common.network;

import com.fiskmods.heroes.common.item.ItemColdGun;
import com.fiskmods.heroes.common.item.ItemHeatGun;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/** An immediate release hint for the sender's held beam; the server still enforces timeouts. */
public final class PacketGunStop extends SHPacket
{
    public PacketGunStop() {}
    public PacketGunStop(FriendlyByteBuf buffer) {}
    @Override public void encode(FriendlyByteBuf buffer) {}

    @Override
    public void handle(NetworkEvent.Context context)
    {
        var player = context.getSender();
        if (player == null) return;
        ItemColdGun.stopBeam(player);
        ItemHeatGun.clearBeam(player);
    }
}
