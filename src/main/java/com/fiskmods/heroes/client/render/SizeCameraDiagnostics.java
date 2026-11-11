package com.fiskmods.heroes.client.render;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.hero.ability.PlayerSizeHandler;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Opt-in CI diagnostics for the actual rendered first-person camera. */
@Mod.EventBusSubscriber(modid = FiskHeroes.MODID, value = Dist.CLIENT)
public final class SizeCameraDiagnostics
{
    private static int lastTick = Integer.MIN_VALUE;
    private SizeCameraDiagnostics() {}

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event)
    {
        if (!Boolean.getBoolean("fiskheroes.ciSizeDiagnostics")
                || event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null || event.getCamera().getEntity() != player || !mc.options.getCameraType().isFirstPerson()
                || player.tickCount == lastTick || player.tickCount % 20 != 0) return;
        lastTick = player.tickCount;
        var data = SHDataCapabilities.getPlayer(player);
        var hero = data != null ? data.getHero() : null;
        FiskHeroes.LOGGER.info("Size camera: hero={}, scale={}, pose={}, x={}, y={}, z={}, cameraY={}, eye={}",
                hero != null ? hero.getRegistryName() : "none", PlayerSizeHandler.scale(player), player.getPose(),
                player.getX(), player.getY(), player.getZ(), event.getCamera().getPosition().y, player.getEyeHeight());
    }
}
