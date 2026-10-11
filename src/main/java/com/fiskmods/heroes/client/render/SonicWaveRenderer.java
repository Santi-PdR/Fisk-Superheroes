package com.fiskmods.heroes.client.render;

import com.fiskmods.heroes.common.entity.projectile.SonicWaveEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Renders the original Canary cry as an expanding translucent spherical pulse. */
public final class SonicWaveRenderer extends EntityRenderer<SonicWaveEntity>
{
    public SonicWaveRenderer(EntityRendererProvider.Context context)
    {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(SonicWaveEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource buffers, int packedLight)
    {
        poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(entity.getXRot()));
        float radius = entity.getRadius(partialTick);
        int alpha = Mth.clamp((int) (entity.getOpacity(partialTick) * 255.0F), 0, 255);
        VertexConsumer consumer = buffers.getBuffer(RenderType.lightning());
        PoseStack.Pose pose = poseStack.last();
        for (int ring = 0; ring < 3; ++ring)
        {
            float ringRadius = radius * (0.72F + ring * 0.14F);
            int color = ring == 0 ? 0xD9F8FF : 0x53CFFF;
            for (int segment = 0; segment < 16; ++segment)
            {
                double a0 = Math.PI * 2.0D * segment / 16.0D;
                double a1 = Math.PI * 2.0D * (segment + 1) / 16.0D;
                vertex(consumer, pose, ringRadius * Math.cos(a0), ringRadius * Math.sin(a0), 0.0D, color, alpha);
                vertex(consumer, pose, ringRadius * Math.cos(a1), ringRadius * Math.sin(a1), 0.0D, color, alpha);
            }
        }
        super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, double x, double y, double z,
            int color, int alpha)
    {
        consumer.vertex(pose.pose(), (float) x, (float) y, (float) z)
                .color(color >> 16 & 255, color >> 8 & 255, color & 255, alpha).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(SonicWaveEntity entity)
    {
        return new ResourceLocation("minecraft", "textures/atlas/blocks.png");
    }
}
