package com.fiskmods.heroes.client.render;

import com.fiskmods.heroes.common.entity.GravityWaveEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

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

        float radius = wave.getRadius() * wave.getProgress(partialTick);
        int alpha = Mth.clamp((int) ((1.0F - wave.getProgress(partialTick)) * 210.0F), 0, 210);
        VertexConsumer consumer = buffers.getBuffer(RenderType.lightning());
        PoseStack.Pose pose = poseStack.last();
        for (int ring = 0; ring < 2; ++ring)
        {
            float ringRadius = radius * (ring == 0 ? 0.94F : 1.0F);
            int color = ring == 0 ? 0xA8F8FF : 0x36BDF2;
            for (int segment = 0; segment < 32; ++segment)
            {
                double a0 = Math.PI * 2.0D * segment / 32.0D;
                double a1 = Math.PI * 2.0D * (segment + 1) / 32.0D;
                vertex(consumer, pose, ringRadius * Math.cos(a0), ringRadius * Math.sin(a0), 0.0D, color, alpha);
                vertex(consumer, pose, ringRadius * Math.cos(a1), ringRadius * Math.sin(a1), 0.0D, color, alpha);
            }
        }
        super.render(wave, yaw, partialTick, poseStack, buffers, packedLight);
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
