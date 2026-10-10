package com.fiskmods.heroes.common.network;

import com.fiskmods.heroes.common.item.ItemGun;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** Client request to fire the gun in the main hand; ItemGun validates permission and cadence. */
public final class PacketGunFire extends SHPacket
{
    public PacketGunFire()
    {
    }

    public PacketGunFire(FriendlyByteBuf buf)
    {
    }

    @Override
    public void encode(FriendlyByteBuf buf)
    {
    }

    @Override
    public void handle(NetworkEvent.Context context)
    {
        ServerPlayer player = context.getSender();
        if (player != null && player.getMainHandItem().getItem() instanceof ItemGun gun)
        {
            gun.fireFromAttack(player, player.getMainHandItem());
        }
    }
}
