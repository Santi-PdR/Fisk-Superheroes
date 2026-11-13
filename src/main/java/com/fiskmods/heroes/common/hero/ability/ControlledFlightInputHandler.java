package com.fiskmods.heroes.common.hero.ability;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.data.PlayerInputTracker;
import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.data.var.Vars;
import com.fiskmods.heroes.common.hero.modifier.ModEffects;
import com.fiskmods.heroes.common.hero.power.ModifierEntry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/** Server-authoritative double-jump control for the original controlled-flight modifier. */
public final class ControlledFlightInputHandler
{
    private static final ResourceLocation CONTROLLED_FLIGHT = FiskHeroes.id("controlled_flight");
    private static final Map<UUID, Long> LAST_PRESS = new HashMap<>();
    private static final int DOUBLE_TAP_TICKS = 7;

    private ControlledFlightInputHandler() {}

    private static ModifierEntry enabledFlight(Player player, SHPlayerData data)
    {
        if (data == null || data.getHeroType() == null || player.hasEffect(ModEffects.TUTRIDIUM.get())) return null;
        for (ModifierEntry entry : data.getHeroType().getPowerContainer().getEntries())
            if (CONTROLLED_FLIGHT.equals(entry.getModifier().getId()) && entry.isEnabled()
                    && entry.isModifierEnabled(player, data)) return entry;
        return null;
    }

    public static void onJumpPressed(Player player, boolean sneak)
    {
        if (player.level().isClientSide) return;
        SHPlayerData data = SHDataCapabilities.getPlayer(player);
        if (enabledFlight(player, data) == null || sneak || player.isPassenger()
                || player.getAbilities().flying || player.isFallFlying())
        {
            clear(player);
            return;
        }

        long now = player.level().getGameTime();
        Long first = LAST_PRESS.get(player.getUUID());
        if (first != null && now >= first && now - first <= DOUBLE_TAP_TICKS && !player.onGround())
        {
            boolean flying = !data.getData().get(Vars.FLYING);
            data.getData().set(Vars.FLYING, flying);
            LAST_PRESS.remove(player.getUUID());
            FiskHeroes.LOGGER.info("Controlled flight input: player={}, active={}", player.getGameProfile().getName(), flying);
        }
        else if (first == null || now < first || now - first > DOUBLE_TAP_TICKS)
        {
            // Keep the initial ground jump: by the second press vanilla has lifted the player.
            LAST_PRESS.put(player.getUUID(), now);
        }
    }

    /** Original flight stops on landing, crouching or mounting; do not affect unrelated suits. */
    public static void tick(Player player)
    {
        if (player.level().isClientSide) return;
        SHPlayerData data = SHDataCapabilities.getPlayer(player);
        if (data == null || !data.getData().get(Vars.FLYING)) return;
        if ((player.onGround() || player.isPassenger() || player.getAbilities().flying || player.isFallFlying()
                || PlayerInputTracker.isSneaking(player)) && enabledFlight(player, data) != null)
        {
            data.getData().set(Vars.FLYING, false);
            clear(player);
        }
    }

    public static void clear(Player player)
    {
        LAST_PRESS.remove(player.getUUID());
    }
}
