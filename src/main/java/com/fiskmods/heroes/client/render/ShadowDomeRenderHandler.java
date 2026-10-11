package com.fiskmods.heroes.client.render;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.data.var.Vars;
import com.fiskmods.heroes.common.hero.HeroIteration;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Draws the textured half-dome declared by the Obsidian suit model around its darkness cloud. */
@Mod.EventBusSubscriber(modid = FiskHeroes.MODID, value = Dist.CLIENT)
public final class ShadowDomeRenderHandler
{
    private ShadowDomeRenderHandler()
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
        Set<RenderType> batches = new HashSet<>();

        for (Player player : level.players())
        {
            SHPlayerData data = SHDataCapabilities.getPlayer(player);
            if (data == null)
            {
                continue;
            }

            int domeId = data.getData().get(Vars.LIGHTSOUT_ID);
            if (domeId < 0 || !(level.getEntity(domeId) instanceof AreaEffectCloud cloud))
            {
                continue;
            }

            HeroIteration iteration = data.getHero();
            HeroModelData model = HeroModelRegistry.get(iteration);
            if (model == null)
            {
                continue;
            }

            com.google.gson.JsonObject effect = model.getCustom().get("fiskheroes:shadowdome");
            if (effect == null || !effect.has("texture"))
            {
                continue;
            }

            ResourceLocation texture = ResourceLocation.tryParse(effect.get("texture").getAsString());
            if (texture == null)
            {
                continue;
            }

            int slices = 36;
            int stacks = 18;
            if (effect.has("shape") && effect.get("shape").isJsonArray()
                    && effect.getAsJsonArray("shape").size() >= 2)
            {
                slices = Math.max(8, Math.min(128, effect.getAsJsonArray("shape").get(0).getAsInt()));
                stacks = Math.max(4, Math.min(64, effect.getAsJsonArray("shape").get(1).getAsInt()));
            }

            float radius = Math.max(0.1F, cloud.getRadius());
            Vec3 center = cloud.getPosition(event.getPartialTick());
            RenderType renderType = RenderType.entityTranslucent(texture);
            VertexConsumer consumer = buffers.getBuffer(renderType);
            drawDome(consumer, event.getPoseStack().last(), center, radius, slices, stacks);
            batches.add(renderType);
        }

        for (RenderType renderType : batches)
        {
            buffers.endBatch(renderType);
        }
    }

    private static void drawDome(VertexConsumer consumer, PoseStack.Pose pose, Vec3 center,
            float radius, int slices, int stacks)
    {
        for (int stack = 0; stack < stacks; ++stack)
        {
            double top = Math.PI * 0.5D * stack / stacks;
            double bottom = Math.PI * 0.5D * (stack + 1) / stacks;
            float topV = (float) stack / stacks;
            float bottomV = (float) (stack + 1) / stacks;

            for (int slice = 0; slice < slices; ++slice)
            {
                double left = Math.PI * 2.0D * slice / slices;
                double right = Math.PI * 2.0D * (slice + 1) / slices;
                float leftU = (float) slice / slices;
                float rightU = (float) (slice + 1) / slices;

                Vec3 topLeft = point(center, radius, top, left);
                Vec3 topRight = point(center, radius, top, right);
                Vec3 bottomLeft = point(center, radius, bottom, left);
                Vec3 bottomRight = point(center, radius, bottom, right);

                vertex(consumer, pose, topLeft, normal(top, left), leftU, topV);
                vertex(consumer, pose, topRight, normal(top, right), rightU, topV);
                vertex(consumer, pose, bottomRight, normal(bottom, right), rightU, bottomV);
                vertex(consumer, pose, topLeft, normal(top, left), leftU, topV);
                vertex(consumer, pose, bottomRight, normal(bottom, right), rightU, bottomV);
                vertex(consumer, pose, bottomLeft, normal(bottom, left), leftU, bottomV);
            }
        }
    }

    private static Vec3 point(Vec3 center, float radius, double latitude, double longitude)
    {
        double ring = Math.sin(latitude) * radius;
        return center.add(Math.cos(longitude) * ring, Math.cos(latitude) * radius,
                Math.sin(longitude) * ring);
    }

    private static Vec3 normal(double latitude, double longitude)
    {
        return new Vec3(Math.sin(latitude) * Math.cos(longitude), Math.cos(latitude),
                Math.sin(latitude) * Math.sin(longitude));
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, Vec3 point,
            Vec3 normal, float u, float v)
    {
        consumer.vertex(pose.pose(), (float) point.x, (float) point.y, (float) point.z)
                .color(255, 255, 255, 220)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(pose.normal(), (float) normal.x, (float) normal.y, (float) normal.z)
                .endVertex();
    }
}
