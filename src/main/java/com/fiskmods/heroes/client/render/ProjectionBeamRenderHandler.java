package com.fiskmods.heroes.client.render;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.data.var.Vars;
import com.fiskmods.heroes.common.hero.HeroIteration;
import com.google.gson.JsonObject;
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

/** Draws the short-lived energy beams described by the wearer's hero model. */
@Mod.EventBusSubscriber(modid = FiskHeroes.MODID, value = Dist.CLIENT)
public final class ProjectionBeamRenderHandler
{
    private ProjectionBeamRenderHandler()
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
        Vec3 camera = event.getCamera().getPosition();
        boolean rendered = false;

        for (Player player : level.players())
        {
            SHPlayerData data = SHDataCapabilities.getPlayer(player);
            if (data == null)
            {
                continue;
            }

            float activity = Mth.clamp(data.getData().get(Vars.ENERGY_PROJECTION_TIMER), 0.0F, 1.0F);
            double length = data.getData().get(Vars.HEAT_VISION_LENGTH);
            if (activity <= 0.0F || length <= 0.0D)
            {
                continue;
            }

            HeroIteration iteration = data.getHero();
            HeroModelData model = HeroModelRegistry.get(iteration);
            if (model == null)
            {
                continue;
            }

            JsonObject effect = model.getCustom().get("fiskheroes:energy_projection");
            if (effect == null)
            {
                effect = model.getCustom().get("fiskheroes:charged_beam");
            }
            if (effect == null)
            {
                // Black Lightning and Shazam use the same hitscan ability, but their model
                // packs describe its visual as lightning_attack instead of energy_projection.
                effect = model.getCustom().get("fiskheroes:lightning_attack");
            }
            if (effect == null)
            {
                continue;
            }

            int color = parseColor(effect);
            float width = beamWidth(effect);
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

            Vec3 origin = origin(player, event.getPartialTick(), effect, direction, right, up);
            Vec3 start = origin.subtract(camera);
            Vec3 end = start.add(direction.scale(length));
            int alpha = Mth.clamp(Math.round(activity * 230.0F), 0, 230);

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

    private static int parseColor(JsonObject effect)
    {
        if (effect.has("color"))
        {
            try
            {
                return (int) Long.decode(effect.get("color").getAsString()).longValue();
            }
            catch (NumberFormatException ignored)
            {
                // Use the energy-beam default if an optional pack color is malformed.
            }
        }
        return 0xFF9A24;
    }

    private static float beamWidth(JsonObject effect)
    {
        if (effect.has("beams") && effect.get("beams").isJsonArray()
                && !effect.getAsJsonArray("beams").isEmpty())
        {
            JsonObject beam = effect.getAsJsonArray("beams").get(0).getAsJsonObject();
            if (beam.has("size") && beam.get("size").isJsonArray()
                    && !beam.getAsJsonArray("size").isEmpty())
            {
                return Mth.clamp(beam.getAsJsonArray("size").get(0).getAsFloat() / 32.0F, 0.02F, 0.18F);
            }
        }
        return 0.055F;
    }

    private static Vec3 origin(Player player, float partialTick, JsonObject effect, Vec3 direction,
            Vec3 right, Vec3 up)
    {
        Vec3 origin = player.getEyePosition(partialTick);
        String anchor = effect.has("anchor") ? effect.get("anchor").getAsString() : "head";
        if ("body".equals(anchor))
        {
            origin = player.getPosition(partialTick).add(0.0D, player.getBbHeight() * 0.5D, 0.0D);
        }
        else if ("rightArm".equals(anchor) || "leftArm".equals(anchor))
        {
            double sign = "rightArm".equals(anchor) ? 1.0D : -1.0D;
            origin = player.getPosition(partialTick).add(0.0D, player.getBbHeight() * 0.62D, 0.0D)
                    .add(right.scale(sign * 0.32D));
        }

        if (effect.has("beams") && effect.get("beams").isJsonArray()
                && !effect.getAsJsonArray("beams").isEmpty())
        {
            JsonObject beam = effect.getAsJsonArray("beams").get(0).getAsJsonObject();
            if (beam.has("offset") && beam.get("offset").isJsonArray()
                    && beam.getAsJsonArray("offset").size() >= 3)
            {
                var offset = beam.getAsJsonArray("offset");
                origin = origin.add(right.scale(offset.get(0).getAsDouble() / 16.0D))
                        .add(up.scale(offset.get(1).getAsDouble() / 16.0D))
                        .add(direction.scale(-offset.get(2).getAsDouble() / 16.0D));
            }
        }
        return origin.add(direction.scale(0.08D));
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
