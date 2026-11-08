package com.fiskmods.heroes.common.hero.ability;

import java.util.IdentityHashMap;
import java.util.Map;

import com.fiskmods.heroes.common.hero.modifier.ModifierFlight;

import net.minecraft.server.level.ServerPlayer;

/** Limits vanilla's floating counter for suit flight, wall contact and their immediate descent. */
public final class FlightKickGuard
{
    private static final Map<ServerPlayer, Descent> DESCENTS = new IdentityHashMap<>();
    private static final Map<ServerPlayer, Double> LAST_POSITIONS = new IdentityHashMap<>();

    private FlightKickGuard() { }

    /** Call at server tick END, after vanilla has restored the position reported by the client. */
    public static void tick(ServerPlayer player)
    {
        Double previousY = LAST_POSITIONS.put(player, player.getY());
        // In 1.20.1 the vanilla floating flag may remain set during a long ordinary
        // fall. Only reset it for very rapid *observed* descent, not slow descent,
        // hovering or ascent. A dropped player rapidly reaches this speed under
        // normal gravity; at two blocks/tick this cannot sustain hovering.
        if (previousY != null && isRapidDescent(previousY, player.getY(), player.onGround() || player.isPassenger())
                && !player.noPhysics && player.connection != null)
            player.connection.aboveGroundTickCount = 0;

        boolean active = SuitTravel.controlledFlightEntry(player) != null || ModifierFlight.hasActiveFlight(player)
                || WallCrawlContact.hasActiveContact(player);
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

    public static boolean isRapidDescent(double previousY, double currentY, boolean landedOrRiding)
    {
        return !landedOrRiding && Double.isFinite(previousY) && Double.isFinite(currentY)
                && currentY - previousY <= -2.0D;
    }

    public static void clear(ServerPlayer player)
    {
        DESCENTS.remove(player);
        LAST_POSITIONS.remove(player);
    }

    /** Transient, bounded grace only for observed downward displacement after flight or wall contact. */
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
