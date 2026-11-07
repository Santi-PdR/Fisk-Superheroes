package com.fiskmods.heroes.common.data;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.world.entity.player.Player;

/**
 * Per-player client input that the server cannot otherwise observe.
 * <p>
 * 1.7.10 let the server read {@code EntityPlayer.movementInput} directly. That field is client-only
 * in 1.20.1 ({@code LocalPlayer.input}) and the vanilla protocol does not carry the jump/sneak keys
 * to the server, so the flight modifier needs them forwarded explicitly. This is the same
 * information vanilla used to expose, not a gameplay shortcut: the client only reports the raw keys
 * and the server still owns every movement decision.
 */
public final class PlayerInputTracker
{
    private static final Map<UUID, Input> INPUTS = new ConcurrentHashMap<>();

    private PlayerInputTracker()
    {
    }

    public static void set(Player player, boolean jump, boolean sneak, int forward, int strafe)
    {
        set(player, jump, sneak, forward, strafe, false);
    }

    public static void set(Player player, boolean jump, boolean sneak, int forward, int strafe, boolean use)
    {
        INPUTS.computeIfAbsent(player.getUUID(), k -> new Input()).set(jump, sneak, forward, strafe, use);
    }

    public static boolean isUsing(Player player)
    {
        Input input = INPUTS.get(player.getUUID());
        return input != null && input.use;
    }

    public static int forward(Player player)
    {
        Input input = INPUTS.get(player.getUUID());
        return input != null ? input.forward : 0;
    }

    public static int strafe(Player player)
    {
        Input input = INPUTS.get(player.getUUID());
        return input != null ? input.strafe : 0;
    }

    public static boolean isJumping(Player player)
    {
        Input input = INPUTS.get(player.getUUID());
        return input != null && input.jump;
    }

    public static boolean isSneaking(Player player)
    {
        Input input = INPUTS.get(player.getUUID());
        return input != null && input.sneak;
    }

    public static void clear(Player player)
    {
        INPUTS.remove(player.getUUID());
    }

    private static final class Input
    {
        private boolean jump;
        private boolean sneak;
        private boolean use;
        private int forward;
        private int strafe;

        void set(boolean jump, boolean sneak, int forward, int strafe, boolean use)
        {
            this.jump = jump;
            this.sneak = sneak;
            this.use = use;
            this.forward = Integer.signum(forward);
            this.strafe = Integer.signum(strafe);
        }
    }
}
