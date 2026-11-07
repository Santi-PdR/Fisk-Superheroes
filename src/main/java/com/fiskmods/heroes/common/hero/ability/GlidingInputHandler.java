package com.fiskmods.heroes.common.hero.ability;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.data.var.Vars;
import com.fiskmods.heroes.common.hero.power.PowerProperty;

import net.minecraft.world.entity.player.Player;

/** Server validation for the original jump/double-jump glide control. */
public final class GlidingInputHandler
{
    private static final Map<UUID, Long> LAST_PRESS = new HashMap<>();
    private GlidingInputHandler() {}

    public static void onJumpPressed(Player player)
    {
        var data = SHDataCapabilities.getPlayer(player);
        var hero = data != null ? data.getHeroType() : null;
        var entry = hero != null ? hero.getPowerContainer().getEntry(FiskHeroes.id("gliding")) : null;
        if (entry == null || !entry.isEnabled() || !entry.isModifierEnabled(player, data)
                || player.hasEffect(com.fiskmods.heroes.common.hero.modifier.ModEffects.TUTRIDIUM.get())
                || player.onGround() || player.isShiftKeyDown() || player.isPassenger()
                || player.getAbilities().flying || player.isInWater() && !entry.getBoolean(player, PowerProperty.IS_POWERED))
        {
            LAST_PRESS.remove(player.getUUID());
            return;
        }
        long now = player.level().getGameTime();
        Long previous = LAST_PRESS.put(player.getUUID(), now);
        boolean doubleTap = previous != null && now >= previous && now - previous <= 7;
        boolean gliding = data.getData().get(Vars.GLIDING);
        if (doubleTap || !gliding && entry.getBoolean(player, PowerProperty.CAN_JUMP_ACTIVATE)
                && (player.getDeltaMovement().y < 0.0D || player.isInWater()))
        {
            boolean next = !gliding;
            data.getData().set(Vars.GLIDING, next);
            entry.setToggled(player, next);
            FiskHeroes.LOGGER.info("Gliding input: player={}, active={}", player.getGameProfile().getName(), next);
        }
    }

    public static void clear(Player player)
    {
        LAST_PRESS.remove(player.getUUID());
    }
}
