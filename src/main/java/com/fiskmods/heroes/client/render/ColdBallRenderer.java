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
        // Crossed quads remain visible even when the projectile is flying directly
        // toward or away from the camera; longitudinal ribbons collapsed to a line.
        VertexConsumer out = buffers.getBuffer(RenderType.lightning());
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees((entity.tickCount + partial) * 8.0F));
        quad(out, pose.last(), 0.18F, 70, 155, 255);
        pose.mulPose(Axis.YP.rotationDegrees(90.0F));
        quad(out, pose.last(), 0.18F, 90, 190, 255);
        pose.mulPose(Axis.XP.rotationDegrees(90.0F));
        quad(out, pose.last(), 0.13F, 150, 220, 255);
        pose.popPose();
        super.render(entity, yaw, partial, pose, buffers, light);
    }

    private static void quad(VertexConsumer out, PoseStack.Pose pose, float size, int r, int g, int b)
    {
        vertex(out, pose, -size, -size, r, g, b);
        vertex(out, pose, size, -size, r, g, b);
        vertex(out, pose, size, size, r, g, b);
        vertex(out, pose, -size, size, r, g, b);
    }

    private static void vertex(VertexConsumer out, PoseStack.Pose pose, float x, float y, int r, int g, int b)
    {
        out.vertex(pose.pose(), x, y, 0.0F).color(r, g, b, 255).endVertex();
    }

    @Override public ResourceLocation getTextureLocation(ColdBallEntity entity)
    { return new ResourceLocation("minecraft", "textures/atlas/blocks.png"); }
}
