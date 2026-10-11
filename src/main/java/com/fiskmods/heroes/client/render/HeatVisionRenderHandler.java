package com.fiskmods.heroes.client.render;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.data.var.Vars;
import com.fiskmods.heroes.common.hero.HeroIteration;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

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
            float width = 0.055F;
            if (effect != null)
            {
                if (effect.has("color"))
                {
                    try
                    {
                        color = (int) Long.decode(effect.get("color").getAsString()).longValue();
                    }
                    catch (NumberFormatException ignored)
                    {
                        // Keep the default red beam if the pack color is malformed.
                    }
                }

                if (effect.has("beams") && effect.get("beams").isJsonArray()
                        && !effect.getAsJsonArray("beams").isEmpty())
                {
                    var beam = effect.getAsJsonArray("beams").get(0).getAsJsonObject();
                    if (beam.has("size") && beam.get("size").isJsonArray()
                            && !beam.getAsJsonArray("size").isEmpty())
                    {
                        width = Mth.clamp(beam.getAsJsonArray("size").get(0).getAsFloat() / 32.0F,
                                0.02F, 0.18F);
                    }
                }
            }

            Vec3 direction = player.getLookAngle().normalize();
            Vec3 worldUp = new Vec3(0.0D, 1.0D, 0.0D);
            Vec3 right = direction.cross(worldUp);
            if (right.lengthSqr() < 1.0E-6D)
            {
                right = new Vec3(1.0D, 0.0D, 0.0D);
            }
            else
            {
                right = right.normalize();
            }
            Vec3 up = right.cross(direction).normalize();

            Vec3 start = player.getEyePosition(event.getPartialTick()).add(direction.scale(0.12D)).subtract(camera);
            Vec3 end = start.add(direction.scale(length));
            int alpha = Mth.clamp(Math.round(activity * 240.0F), 0, 240);

            PoseStack poseStack = event.getPoseStack();
            poseStack.pushPose();
            VertexConsumer consumer = buffers.getBuffer(RenderType.lightning());
            drawRibbon(consumer, poseStack.last(), start, end, right.scale(width), color, alpha);
            drawRibbon(consumer, poseStack.last(), start, end, up.scale(width * 0.55D), color, alpha);
            poseStack.popPose();
            rendered = true;
        }

        if (rendered)
        {
            buffers.endBatch(RenderType.lightning());
        }
    }

    private static void drawRibbon(VertexConsumer consumer, PoseStack.Pose pose, Vec3 start,
            Vec3 end, Vec3 side, int color, int alpha)
    {
        int red = color >> 16 & 255;
        int green = color >> 8 & 255;
        int blue = color & 255;
        vertex(consumer, pose, start.subtract(side), red, green, blue, alpha);
        vertex(consumer, pose, end.subtract(side), red, green, blue, alpha);
        vertex(consumer, pose, end.add(side), red, green, blue, alpha);
        vertex(consumer, pose, start.add(side), red, green, blue, alpha);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, Vec3 position,
            int red, int green, int blue, int alpha)
    {
        consumer.vertex(pose.pose(), (float) position.x, (float) position.y, (float) position.z)
                .color(red, green, blue, alpha)
                .endVertex();
    }
}
