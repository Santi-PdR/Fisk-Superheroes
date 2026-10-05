package com.fiskmods.heroes.client.render;

import com.fiskmods.heroes.common.entity.GravityWaveEntity;
import com.fiskmods.heroes.common.hero.HeroIteration;
import com.fiskmods.heroes.common.hero.HeroTracker;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/** Draws the short expanding ring emitted by the gravity manipulation ability. */
public final class GravityWaveRenderer extends EntityRenderer<GravityWaveEntity>
{
    public GravityWaveRenderer(EntityRendererProvider.Context context)
    {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(GravityWaveEntity wave, float yaw, float partialTick, PoseStack poseStack,
            MultiBufferSource buffers, int packedLight)
    {
        int side = wave.getSide();
        if (side == 2 || side == 3) poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        else if (side == 4 || side == 5) poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));

        float progress = Mth.clamp(wave.getProgress(partialTick), 0.0F, 1.0F);
        float radius = wave.getRadius() * 0.5F * progress;
        int alpha = Mth.clamp((int) ((1.0F - progress) * 127.5F), 0, 128);
        int color = color(wave);
        VertexConsumer consumer = buffers.getBuffer(RenderType.lightning());
        PoseStack.Pose pose = poseStack.last();
        // The original renderer draws a single annulus with the hero's configured color and
        // fades its outer edge to transparent. Keeping that gradient also avoids the fixed
        // cyan rings that made every hero's gravity effect look the same.
        float innerRadius = radius / 1.5F;
        for (int segment = 0; segment < 36; ++segment)
        {
            double a0 = Math.PI * 2.0D * segment / 36.0D;
            double a1 = Math.PI * 2.0D * (segment + 1) / 36.0D;
            vertex(consumer, pose, innerRadius * Math.cos(a0), innerRadius * Math.sin(a0), 0.0D, color, alpha);
            vertex(consumer, pose, radius * Math.cos(a0), radius * Math.sin(a0), 0.0D, color, 0);
            vertex(consumer, pose, radius * Math.cos(a1), radius * Math.sin(a1), 0.0D, color, 0);
            vertex(consumer, pose, innerRadius * Math.cos(a1), innerRadius * Math.sin(a1), 0.0D, color, alpha);
        }
        super.render(wave, yaw, partialTick, poseStack, buffers, packedLight);
    }

    private static int color(GravityWaveEntity wave)
    {
        if (wave.getCaster() instanceof Player player)
        {
            HeroIteration iteration = HeroTracker.getHero(player);
            HeroModelData model = HeroModelRegistry.get(iteration);
            com.google.gson.JsonObject effect = model != null
                    ? model.getCustom().get("fiskheroes:gravity_manipulation") : null;
            if (effect != null && effect.has("color"))
            {
                try
                {
                    return (int) Long.decode(effect.get("color").getAsString()).longValue() & 0xFFFFFF;
                }
                catch (NumberFormatException ignored)
                {
                    // Fall through to the original renderer's blue default.
                }
            }
        }
        return 0x32E0FF;
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, double x, double y, double z,
            int color, int alpha)
    {
        consumer.vertex(pose.pose(), (float) x, (float) y, (float) z)
                .color(color >> 16 & 255, color >> 8 & 255, color & 255, alpha).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(GravityWaveEntity entity)
    {
        return new ResourceLocation("minecraft", "textures/atlas/blocks.png");
    }
}
