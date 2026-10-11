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
import net.minecraft.client.renderer.MultiBufferSource;
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

            if (renderModelBeam(event.getPoseStack(), buffers, camera, player, model, event.getPartialTick()))
            {
                rendered = true;
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
            if (effect == null) effect = model.getCustom().get("fiskheroes:cold_gun");
            if (effect == null) effect = model.getCustom().get("fiskheroes:repulsor_blast");
            if (effect == null) effect = model.getCustom().get("fiskheroes:energy_manipulation");
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

    /** Draws component beams such as the articulated glow inside Iron Man's Mk 50 cannon. */
    private static boolean renderModelBeam(PoseStack poseStack, MultiBufferSource.BufferSource buffers,
            Vec3 camera, Player player, HeroModelData model, float partialTick)
    {
        JsonObject effect = model.getCustom().get("fiskheroes:beam");
        if (effect == null || !effect.has("componentBeams") || !effect.get("componentBeams").isJsonArray())
        {
            return false;
        }

        float amount = Mth.clamp(model.evaluateRenderData(effect.get("length"), player, 0.0F), 0.0F, 1.0F);
        if (amount <= 0.001F)
        {
            return false;
        }

        Vec3 direction = player.getLookAngle().normalize();
        Vec3 right = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (right.lengthSqr() < 1.0E-6D)
        {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        }
        else
        {
            right = right.normalize();
        }
        Vec3 up = right.cross(direction).normalize();
        Vec3 origin = player.getPosition(partialTick).add(0.0D, player.getBbHeight() * 0.62D, 0.0D)
                .add(right.scale(0.32D));
        if (effect.has("offset") && effect.get("offset").isJsonArray()
                && effect.getAsJsonArray("offset").size() >= 3)
        {
            var offset = effect.getAsJsonArray("offset");
            origin = origin.add(right.scale(-offset.get(0).getAsDouble() / 16.0D))
                    .add(up.scale(offset.get(1).getAsDouble() / 16.0D))
                    .add(direction.scale(-offset.get(2).getAsDouble() / 16.0D));
        }

        int color = parseColor(effect);
        int alpha = Mth.clamp(Math.round(amount * 230.0F), 0, 230);
        VertexConsumer consumer = buffers.getBuffer(RenderType.lightning());
        Vec3 startBase = origin.subtract(camera);
        for (var element : effect.getAsJsonArray("componentBeams"))
        {
            if (!element.isJsonObject()) continue;
            JsonObject component = element.getAsJsonObject();
            JsonObject beam = component.has("beam") && component.get("beam").isJsonObject()
                    ? component.getAsJsonObject("beam") : null;
            if (beam == null || !component.has("direction") || !component.get("direction").isJsonArray()) continue;

            double x = 0.0D, y = 0.0D, z = 0.0D;
            if (beam.has("offset") && beam.get("offset").isJsonArray()
                    && beam.getAsJsonArray("offset").size() >= 3)
            {
                var offset = beam.getAsJsonArray("offset");
                x = offset.get(0).getAsDouble();
                y = offset.get(1).getAsDouble();
                z = offset.get(2).getAsDouble();
            }
            var vector = component.getAsJsonArray("direction");
            if (vector.size() < 3) continue;
            Vec3 start = startBase.add(right.scale(x / 16.0D)).add(up.scale(y / 16.0D))
                    .add(direction.scale(-z / 16.0D));
            Vec3 end = start.add(right.scale(vector.get(0).getAsDouble() * amount / 16.0D))
                    .add(up.scale(vector.get(1).getAsDouble() * amount / 16.0D))
                    .add(direction.scale(-vector.get(2).getAsDouble() * amount / 16.0D));
            double width = 0.0125D;
            if (beam.has("size") && beam.get("size").isJsonArray() && !beam.getAsJsonArray("size").isEmpty())
            {
                width = Math.max(0.006D, beam.getAsJsonArray("size").get(0).getAsDouble() / 32.0D);
            }

            Vec3 side = end.subtract(start).cross(up);
            if (side.lengthSqr() < 1.0E-6D) side = right;
            else side = side.normalize();
            poseStack.pushPose();
            drawRibbon(consumer, poseStack.last(), start, end, side.scale(width), color, alpha);
            poseStack.popPose();
        }
        return true;
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
            boolean firstPerson = player == Minecraft.getInstance().player
                    && Minecraft.getInstance().options.getCameraType().isFirstPerson();
            String offsetKey = firstPerson && beam.has("firstPerson") ? "firstPerson" : "offset";
            if (beam.has(offsetKey) && beam.get(offsetKey).isJsonArray()
                    && beam.getAsJsonArray(offsetKey).size() >= 3)
            {
                var offset = beam.getAsJsonArray(offsetKey);
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
