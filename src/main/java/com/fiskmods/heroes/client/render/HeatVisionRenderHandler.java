package com.fiskmods.heroes.client.render;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.data.var.Vars;
import com.fiskmods.heroes.common.hero.HeroIteration;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Draws the active heat-vision ray from the synced state and per-hero beam settings. */
@Mod.EventBusSubscriber(modid = FiskHeroes.MODID, value = Dist.CLIENT)
public final class HeatVisionRenderHandler
{
    private HeatVisionRenderHandler()
    {
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event)
    {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES)
        {
            return;
        }

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null)
        {
            return;
        }

        var buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        boolean rendered = false;
        Vec3 camera = event.getCamera().getPosition();

        for (Player player : level.players())
        {
            SHPlayerData data = SHDataCapabilities.getPlayer(player);
            if (data == null)
            {
                continue;
            }

            float activity = Mth.clamp(data.getData().get(Vars.HEAT_VISION_TIMER), 0.0F, 1.0F);
            double length = data.getData().get(Vars.HEAT_VISION_LENGTH);
            if (activity <= 0.0F || length <= 0.0D)
            {
                continue;
            }

            HeroIteration iteration = data.getHero();
            HeroModelData model = HeroModelRegistry.get(iteration);
            var effect = model != null ? model.getCustom().get("fiskheroes:heat_vision") : null;

            int color = 0xFF0000;
            if (effect != null && effect.has("color"))
            {
                try { color = (int) Long.decode(effect.get("color").getAsString()).longValue(); }
                catch (NumberFormatException ignored) { }
            }
            PoseStack poseStack = event.getPoseStack();
            poseStack.pushPose();
            BeamConstellationRenderer.render(poseStack, buffers, camera, player, effect, length,
                    activity, event.getPartialTick(), true, color);
            poseStack.popPose();
            rendered = true;
        }

        if (rendered)
        {
            buffers.endBatch(RenderType.lightning());
        }
    }

 }
