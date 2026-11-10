package com.fiskmods.heroes.client.render;

import com.fiskmods.heroes.common.entity.projectile.ColdBallEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import com.mojang.math.Axis;

/** Approximation of the original cold ball's bright animated blue shell. */
public final class ColdBallRenderer extends EntityRenderer<ColdBallEntity>
{
    private static final java.util.Set<ColdBallEntity> LOGGED =
            java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());

    public ColdBallRenderer(EntityRendererProvider.Context context) { super(context); shadowRadius = 0.0F; }

    @Override
    public void render(ColdBallEntity entity, float yaw, float partial, PoseStack pose,
            MultiBufferSource buffers, int light)
    {
        if (Boolean.getBoolean("fiskheroes.ciColdBallDiagnostics") && LOGGED.add(entity))
            com.fiskmods.heroes.FiskHeroes.LOGGER.info("Cold ball rendered: entity={}", entity.getId());
        // Crossed radial discs stay visible from every direction. The faded blue
        // rim approximates the original animated cryogenic shell without a square sprite.
        VertexConsumer out = buffers.getBuffer(RenderType.lightning());
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees((entity.tickCount + partial) * 8.0F));
        disc(out, pose.last(), 0.18F, 50, 120, 240);
        pose.mulPose(Axis.YP.rotationDegrees(90.0F));
        disc(out, pose.last(), 0.18F, 65, 150, 255);
        pose.mulPose(Axis.XP.rotationDegrees(90.0F));
        disc(out, pose.last(), 0.14F, 90, 170, 255);
        pose.popPose();
        super.render(entity, yaw, partial, pose, buffers, light);
    }

    private static void disc(VertexConsumer out, PoseStack.Pose pose, float radius, int r, int g, int b)
    {
        // RenderType.lightning uses quads: duplicate the centre for each radial triangle.
        for (int i = 0; i < 12; ++i)
        {
            double a = Math.PI * 2.0D * i / 12.0D;
            double bAngle = Math.PI * 2.0D * (i + 1) / 12.0D;
            vertex(out, pose, 0.0F, 0.0F, 165, 220, 255);
            vertex(out, pose, (float) Math.cos(a) * radius, (float) Math.sin(a) * radius, r, g, b);
            vertex(out, pose, (float) Math.cos(bAngle) * radius, (float) Math.sin(bAngle) * radius, r, g, b);
            vertex(out, pose, 0.0F, 0.0F, 165, 220, 255);
        }
    }

    private static void vertex(VertexConsumer out, PoseStack.Pose pose, float x, float y, int r, int g, int b)
    {
        out.vertex(pose.pose(), x, y, 0.0F).color(r, g, b, 255).endVertex();
    }

    @Override public ResourceLocation getTextureLocation(ColdBallEntity entity)
    { return new ResourceLocation("minecraft", "textures/atlas/blocks.png"); }
}
