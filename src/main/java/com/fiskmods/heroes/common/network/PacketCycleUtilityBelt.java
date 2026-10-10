package com.fiskmods.heroes.common.network;

import com.fiskmods.heroes.common.hero.equipment.EquipmentHelper;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** Client request to cycle the held hero's configured utility-belt equipment. */
public class PacketCycleUtilityBelt extends SHPacket
{
    private final int direction;

    public PacketCycleUtilityBelt(int direction)
    {
        this.direction = Integer.signum(direction);
    }

    public PacketCycleUtilityBelt(FriendlyByteBuf buffer)
    {
        direction = Integer.signum(buffer.readByte());
    }

    @Override
    public void encode(FriendlyByteBuf buffer)
    {
        buffer.writeByte(direction);
    }

    @Override
    public void handle(NetworkEvent.Context context)
    {
        ServerPlayer player = context.getSender();
        if (player != null)
        {
            EquipmentHelper.cycleUtilityBelt(player, direction);
        }
    }
}
