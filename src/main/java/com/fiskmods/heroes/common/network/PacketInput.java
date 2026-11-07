package com.fiskmods.heroes.common.network;

import com.fiskmods.heroes.common.data.PlayerInputTracker;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/**
 * Client -> server: movement and use-key state. Only sent when the state changes.
 */
public class PacketInput extends SHPacket
{
    public static boolean lastJump;
    public static boolean lastSneak;
    public static boolean lastUse;
    public static boolean lastSent;
    public static int lastForward;
    public static int lastStrafe;

    private final boolean jump;
    private final boolean sneak;
    private final boolean use;
    private final int forward;
    private final int strafe;

    public PacketInput(boolean jump, boolean sneak, int forward, int strafe)
    {
        this(jump, sneak, forward, strafe, false);
    }

    public PacketInput(boolean jump, boolean sneak, int forward, int strafe, boolean use)
    {
        this.jump = jump;
        this.sneak = sneak;
        this.use = use;
        this.forward = Integer.signum(forward);
        this.strafe = Integer.signum(strafe);
    }

    public PacketInput(FriendlyByteBuf buf)
    {
        jump = buf.readBoolean();
        sneak = buf.readBoolean();
        forward = Integer.signum(buf.readByte());
        strafe = Integer.signum(buf.readByte());
        use = buf.readBoolean();
    }

    @Override
    public void encode(FriendlyByteBuf buf)
    {
        buf.writeBoolean(jump);
        buf.writeBoolean(sneak);
        buf.writeByte(forward);
        buf.writeByte(strafe);
        buf.writeBoolean(use);
    }

    @Override
    public void handle(NetworkEvent.Context context)
    {
        ServerPlayer player = context.getSender();

        if (player != null)
        {
            PlayerInputTracker.set(player, jump, sneak, forward, strafe, use);
        }
    }
}
