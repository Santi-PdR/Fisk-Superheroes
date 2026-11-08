package com.fiskmods.heroes.common.hero.ability;

import java.util.IdentityHashMap;
import java.util.Map;

import com.fiskmods.heroes.common.hero.modifier.ModifierFlight;

import net.minecraft.server.level.ServerPlayer;

/** Limits vanilla's floating counter for actual suit flight and the immediate descent after it. */
public final class FlightKickGuard
{
    private static final Map<ServerPlayer, Descent> DESCENTS = new IdentityHashMap<>();

    private FlightKickGuard() { }

    /** Call at server tick END, after vanilla has restored the position reported by the client. */
    public static void tick(ServerPlayer player)
    {
        boolean active = SuitTravel.controlledFlightEntry(player) != null || ModifierFlight.hasActiveFlight(player);
        Descent descent = DESCENTS.get(player);
        if (active && descent == null)
        {
            descent = new Descent(player.getY());
            DESCENTS.put(player, descent);
        }
        if (descent == null) return;
        boolean exempt = descent.update(active, player.getY(), player.onGround() || player.isPassenger());
        if (exempt && player.connection != null) player.connection.aboveGroundTickCount = 0;
        if (descent.isFinished()) DESCENTS.remove(player);
    }

    public static void clear(ServerPlayer player)
    {
        DESCENTS.remove(player);
    }

    /** Transient, bounded grace only for observed downward displacement after a suit flight. */
    public static final class Descent
    {
        private static final int GRACE_TICKS = 80;
        private double previousY;
        private int remaining;

        public Descent(double initialY) { previousY = initialY; }

        public boolean update(boolean active, double y, boolean landedOrRiding)
        {
            double delta = y - previousY;
            previousY = y;
            if (active)
            {
                remaining = GRACE_TICKS;
                return true;
            }
            if (landedOrRiding) remaining = 0;
            if (remaining <= 0) return false;
            --remaining;
            // Never grant the post-flight exemption for hovering or rising, and never extend it.
            return delta < -0.03125D;
        }

        public boolean isFinished() { return remaining <= 0; }
    }
}
