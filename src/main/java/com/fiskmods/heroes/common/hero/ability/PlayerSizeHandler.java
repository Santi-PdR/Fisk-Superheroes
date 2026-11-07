package com.fiskmods.heroes.common.hero.ability;

import java.util.Map;
import java.util.WeakHashMap;

import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.data.var.Vars;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Applies the synchronized suit scale to collision dimensions and eye height on both sides. */
public final class PlayerSizeHandler
{
    private static final Map<Player, Float> LAST_SCALE = java.util.Collections.synchronizedMap(new WeakHashMap<>());

    private PlayerSizeHandler() {}

    public static float scale(Player player)
    {
        var data = SHDataCapabilities.getPlayer(player);
        float scale = data != null ? data.getData().get(Vars.SCALE) : 1.0F;
        return Float.isFinite(scale) && scale > 0.0F ? scale : 1.0F;
    }

    @SubscribeEvent
    public static void size(EntityEvent.Size event)
    {
        if (!(event.getEntity() instanceof Player player)) return;
        float scale = scale(player);
        if (scale == 1.0F) return;
        event.setNewSize(event.getNewSize().scale(scale));
        event.setNewEyeHeight(event.getNewEyeHeight() * scale);
    }

    public static void refreshIfChanged(Player player)
    {
        float scale = scale(player);
        Float previous = LAST_SCALE.put(player, scale);
        if (previous == null || Float.compare(previous, scale) != 0)
        {
            player.refreshDimensions();
            com.fiskmods.heroes.FiskHeroes.LOGGER.debug("Player dimensions entity={} side={} scale={} width={} eye={}",
                    player.getGameProfile().getName(), player.level().isClientSide ? "client" : "server",
                    scale, player.getBbWidth(), player.getEyeHeight());
        }
    }
}
