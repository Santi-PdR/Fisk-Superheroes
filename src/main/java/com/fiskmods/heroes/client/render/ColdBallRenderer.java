package com.fiskmods.heroes.client.render;

import com.fiskmods.heroes.common.entity.projectile.ColdBallEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/** Approximation of the original cold ball's bright animated blue shell. */
public final class ColdBallRenderer extends EntityRenderer<ColdBallEntity>
{
    public ColdBallRenderer(EntityRendererProvider.Context context) { super(context); shadowRadius = 0.0F; }

    @Override
    public void render(ColdBallEntity entity, float yaw, float partial, PoseStack pose,
            MultiBufferSource buffers, int light)
    {
        Vec3 forward = entity.getDeltaMovement().normalize();
        if (forward.lengthSqr() < 1.0E-6D) forward = new Vec3(0, 0, 1);
        Vec3 right = forward.cross(new Vec3(0, 1, 0));
        right = right.lengthSqr() < 1.0E-6D ? new Vec3(1, 0, 0) : right.normalize();
        Vec3 up = right.cross(forward).normalize();
        double angle = (entity.tickCount + partial) * 0.28D;
        Vec3 side = right.scale(Math.cos(angle)).add(up.scale(Math.sin(angle))).scale(0.16D);
        Vec3 other = right.scale(-Math.sin(angle)).add(up.scale(Math.cos(angle))).scale(0.16D);
        VertexConsumer out = buffers.getBuffer(RenderType.lightning());
        ribbon(out, pose.last(), forward.scale(-0.18D), forward.scale(0.18D), side, 70, 160, 255);
        ribbon(out, pose.last(), forward.scale(-0.18D), forward.scale(0.18D), other, 140, 220, 255);
        ribbon(out, pose.last(), forward.scale(-0.1D), forward.scale(0.1D), side.scale(0.3D), 235, 250, 255);
        super.render(entity, yaw, partial, pose, buffers, light);
    }

    private static void ribbon(VertexConsumer out, PoseStack.Pose pose, Vec3 start, Vec3 end, Vec3 side,
            int r, int g, int b)
    {
        vertex(out, pose, start.subtract(side), r, g, b);
        vertex(out, pose, end.subtract(side), r, g, b);
        vertex(out, pose, end.add(side), r, g, b);
        vertex(out, pose, start.add(side), r, g, b);
    }

    private static void vertex(VertexConsumer out, PoseStack.Pose pose, Vec3 p, int r, int g, int b)
    {
        out.vertex(pose.pose(), (float) p.x, (float) p.y, (float) p.z).color(r, g, b, 255).endVertex();
    }

    @Override public ResourceLocation getTextureLocation(ColdBallEntity entity)
    { return new ResourceLocation("minecraft", "textures/atlas/blocks.png"); }
}
