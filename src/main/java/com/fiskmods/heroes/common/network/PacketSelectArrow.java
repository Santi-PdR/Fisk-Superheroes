package com.fiskmods.heroes.common.network;

import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.data.var.Vars;
import com.fiskmods.heroes.common.item.ItemQuiver;
import com.fiskmods.heroes.common.item.ModItems;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public class PacketSelectArrow extends SHPacket
{
    private final byte slot;

    public PacketSelectArrow(byte slot)
    {
        this.slot = slot;
    }

    public PacketSelectArrow(FriendlyByteBuf buffer)
    {
        slot = buffer.readByte();
    }

    @Override
    public void encode(FriendlyByteBuf buffer)
    {
        buffer.writeByte(slot);
    }

    @Override
    public void handle(NetworkEvent.Context context)
    {
        ServerPlayer player = context.getSender();
        if (player == null || slot < 0 || slot >= 5
                || player.getMainHandItem().getItem() != ModItems.COMPOUND_BOW.get()
                || ItemQuiver.findQuiver(player).isEmpty()) return;

        SHPlayerData data = SHDataCapabilities.getPlayer(player);
        if (data != null) data.getData().set(Vars.SELECTED_ARROW, slot);
    }
}
