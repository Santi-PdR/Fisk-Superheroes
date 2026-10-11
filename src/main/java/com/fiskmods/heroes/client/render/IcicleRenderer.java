package com.fiskmods.heroes.client.render;

import com.fiskmods.heroes.common.entity.projectile.IcicleEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** Textured spike renderer corresponding to the original packed-ice icicle. */
public final class IcicleRenderer extends EntityRenderer<IcicleEntity>
{
    private static final ResourceLocation TEXTURE = new ResourceLocation("minecraft", "textures/block/packed_ice.png");

    public IcicleRenderer(EntityRendererProvider.Context context)
    {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(IcicleEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource buffers, int packedLight)
    {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(entity.getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(entity.getXRot()));
        poseStack.scale(0.7F, 0.7F, 0.7F);

        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        PoseStack.Pose pose = poseStack.last();
        float half = 0.055F;
        float back = -0.19F;
        float tip = 0.25F;
        float[][] base = { { -half, -half }, { half, -half }, { half, half }, { -half, half } };
        for (int i = 0; i < 4; ++i)
        {
            float[] a = base[i];
            float[] b = base[(i + 1) % 4];
            vertex(consumer, pose, a[0], a[1], back, 0.0F, 1.0F, packedLight);
            vertex(consumer, pose, b[0], b[1], back, 1.0F, 1.0F, packedLight);
            vertex(consumer, pose, 0.0F, 0.0F, tip, 0.5F, 0.0F, packedLight);
        }
        super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);
        poseStack.popPose();
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z,
            float u, float v, int packedLight)
    {
        consumer.vertex(pose.pose(), x, y, z).color(255, 255, 255, 255).uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight == 0 ? LightTexture.FULL_BRIGHT : packedLight)
                .normal(pose.normal(), 0.0F, 1.0F, 0.0F).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(IcicleEntity entity)
    {
        return TEXTURE;
    }
}
