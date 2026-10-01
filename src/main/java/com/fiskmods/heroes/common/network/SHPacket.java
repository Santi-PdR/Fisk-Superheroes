package com.fiskmods.heroes.common.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/**
 * Base contract for every packet of the mod. Encoding/decoding lives with the packet itself; the
 * channel handles dispatch and thread hopping.
 */
public abstract class SHPacket
{
    public abstract void encode(FriendlyByteBuf buf);

    public abstract void handle(NetworkEvent.Context context);
}
