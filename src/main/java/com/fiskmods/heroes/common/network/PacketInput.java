package com.fiskmods.heroes.common.network;

import com.fiskmods.heroes.common.data.PlayerInputTracker;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/**
 * Client -> server: the state of the jump and sneak keys. Only sent when the state changes.
 */
public class PacketInput extends SHPacket
{
    public static boolean lastJump;
    public static boolean lastSneak;
    public static boolean lastSent;

    private final boolean jump;
    private final boolean sneak;

    public PacketInput(boolean jump, boolean sneak)
    {
        this.jump = jump;
        this.sneak = sneak;
    }

    public PacketInput(FriendlyByteBuf buf)
    {
        jump = buf.readBoolean();
        sneak = buf.readBoolean();
    }

    @Override
    public void encode(FriendlyByteBuf buf)
    {
        buf.writeBoolean(jump);
        buf.writeBoolean(sneak);
    }

    @Override
    public void handle(NetworkEvent.Context context)
    {
        ServerPlayer player = context.getSender();

        if (player != null)
        {
            PlayerInputTracker.set(player, jump, sneak);
        }
    }
}
