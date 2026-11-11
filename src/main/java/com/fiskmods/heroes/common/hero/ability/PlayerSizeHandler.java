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
    // Integrated clients and servers have distinct Player objects with the same entity id;
    // Entity.equals compares ids, so a shared map would incorrectly treat them as the same key.
    private static final Map<Player, Float> LAST_CLIENT_SCALE = java.util.Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Player, Float> LAST_SERVER_SCALE = java.util.Collections.synchronizedMap(new WeakHashMap<>());

    private PlayerSizeHandler() {}

    public static float scale(Player player)
    {
        var data = SHDataCapabilities.getPlayer(player);
        float scale = data != null ? data.getData().get(Vars.SCALE) : 1.0F;
        return Float.isFinite(scale) && scale > 0.0F ? scale : 1.0F;
    }

    /** Used only for vanilla's prospective pose collision box, not the actual Size event. */
    public static net.minecraft.world.entity.EntityDimensions poseDimensions(net.minecraft.world.entity.Entity entity,
            net.minecraft.world.entity.EntityDimensions dimensions)
    {
        if (!(entity instanceof Player player)) return dimensions;
        float factor = scale(player);
        return factor == 1.0F ? dimensions : dimensions.scale(factor);
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
        Map<Player, Float> previousScales = player.level().isClientSide ? LAST_CLIENT_SCALE : LAST_SERVER_SCALE;
        Float previous = previousScales.put(player, scale);
        if (Boolean.getBoolean("fiskheroes.ciSizeDiagnostics") && player.tickCount % 20 == 0)
        {
            var data = SHDataCapabilities.getPlayer(player);
            var hero = data != null ? data.getHero() : null;
            com.fiskmods.heroes.FiskHeroes.LOGGER.info("Size pose: side={}, hero={}, scale={}, pose={}, x={}, y={}, z={}, height={}, eye={}",
                    player.level().isClientSide ? "client" : "server", hero != null ? hero.getRegistryName() : "none",
                    scale, player.getPose(), player.getX(), player.getY(), player.getZ(), player.getBbHeight(), player.getEyeHeight());
        }
        if (previous == null || Float.compare(previous, scale) != 0)
        {
            player.refreshDimensions();
            com.fiskmods.heroes.FiskHeroes.LOGGER.debug("Player dimensions entity={} side={} scale={} width={} eye={}",
                    player.getGameProfile().getName(), player.level().isClientSide ? "client" : "server",
                    scale, player.getBbWidth(), player.getEyeHeight());
        }
    }
}
