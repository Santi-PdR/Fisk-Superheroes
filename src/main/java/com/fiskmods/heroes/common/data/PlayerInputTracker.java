package com.fiskmods.heroes.common.data;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.world.entity.player.Player;

/** Raw movement keys, kept separately for the logical client and authoritative server. */
public final class PlayerInputTracker
{
    // Integrated client and server share a JVM and player UUID; they must not share input state.
    // Immutable snapshots also prevent the other thread from observing a partially updated input.
    private static final Map<UUID, Input> CLIENT_INPUTS = new ConcurrentHashMap<>();
    private static final Map<UUID, Input> SERVER_INPUTS = new ConcurrentHashMap<>();
    private static final Input EMPTY = new Input(false, false, 0, 0, false);

    private PlayerInputTracker() {}

    private static Map<UUID, Input> inputs(boolean clientSide)
    {
        return clientSide ? CLIENT_INPUTS : SERVER_INPUTS;
    }

    static void set(boolean clientSide, UUID id, boolean jump, boolean sneak, int forward, int strafe, boolean use)
    {
        inputs(clientSide).put(id, new Input(jump, sneak, Integer.signum(forward), Integer.signum(strafe), use));
    }

    static Input get(boolean clientSide, UUID id)
    {
        return inputs(clientSide).getOrDefault(id, EMPTY);
    }

    static void clear(boolean clientSide, UUID id)
    {
        inputs(clientSide).remove(id);
    }

    public static void set(Player player, boolean jump, boolean sneak, int forward, int strafe)
    {
        set(player, jump, sneak, forward, strafe, false);
    }

    public static void set(Player player, boolean jump, boolean sneak, int forward, int strafe, boolean use)
    {
        set(player.level().isClientSide, player.getUUID(), jump, sneak, forward, strafe, use);
    }

    private static Input get(Player player)
    {
        return get(player.level().isClientSide, player.getUUID());
    }

    public static boolean isUsing(Player player) { return get(player).use(); }
    public static int forward(Player player) { return get(player).forward(); }
    public static int strafe(Player player) { return get(player).strafe(); }
    public static boolean isJumping(Player player) { return get(player).jump(); }
    public static boolean isSneaking(Player player) { return get(player).sneak(); }

    public static void clear(Player player)
    {
        clear(player.level().isClientSide, player.getUUID());
    }

    /** Called when the client disconnects; never clears the integrated server's inputs. */
    public static void clearClient()
    {
        CLIENT_INPUTS.clear();
    }

    record Input(boolean jump, boolean sneak, int forward, int strafe, boolean use) {}
}
