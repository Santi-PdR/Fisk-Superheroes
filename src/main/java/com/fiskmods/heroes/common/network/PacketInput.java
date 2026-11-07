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
    // Separate from PlayerInputTracker: an integrated client and server share static fields.
    private static final java.util.Map<java.util.UUID, Boolean> SERVER_JUMP = new java.util.concurrent.ConcurrentHashMap<>();

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

        if (player != null) applyServerInput(player, jump, sneak, forward, strafe, use);
    }

    public static void applyServerInput(net.minecraft.world.entity.player.Player player, boolean jump, boolean sneak,
            int forward, int strafe, boolean use)
    {
        boolean previousJump = Boolean.TRUE.equals(SERVER_JUMP.put(player.getUUID(), jump));
        // The local player's jump runs client-side; consume the post-swing window on the server
        // too, so a later authoritative data sync cannot bring it back.
        if (jump && !previousJump)
        {
            com.fiskmods.heroes.common.hero.HeroTracker.update(player);
            com.fiskmods.heroes.common.hero.ability.GlidingInputHandler.onJumpPressed(player);
            var data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(player);
            if (data != null && data.getHeroType() != null
                    && data.getHeroType().getPowerContainer().has(com.fiskmods.heroes.FiskHeroes.id("web_swinging"))
                    && data.getData().get(com.fiskmods.heroes.common.data.var.Vars.TICKS_SINCE_SWINGING) < 5)
                data.getData().set(com.fiskmods.heroes.common.data.var.Vars.TICKS_SINCE_SWINGING, (short) 5);
        }
        PlayerInputTracker.set(player, jump, sneak, forward, strafe, use);
    }

    public static void clearServerState(net.minecraft.world.entity.player.Player player)
    {
        SERVER_JUMP.remove(player.getUUID());
        com.fiskmods.heroes.common.hero.ability.GlidingInputHandler.clear(player);
    }
}
