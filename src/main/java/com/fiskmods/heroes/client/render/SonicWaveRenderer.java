package com.fiskmods.heroes.client.render;

import com.fiskmods.heroes.common.entity.projectile.SonicWaveEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** The original cry is a translucent white, open-ended cylindrical ring. */
public final class SonicWaveRenderer extends EntityRenderer<SonicWaveEntity>
{
    // Untextured, unlit alpha blending, double-sided and depth-tested without writing
    // depth, like the old tessellator. Lightning uses additive blending and writes depth.
    private static final RenderType WAVE = new RenderType("fiskheroes_sonic_wave",
            DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 1536, false, true,
            () -> {
                RenderSystem.setShader(GameRenderer::getPositionColorShader);
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.disableCull();
                RenderSystem.depthMask(false);
            },
            () -> {
                RenderSystem.depthMask(true);
                RenderSystem.enableCull();
                RenderSystem.disableBlend();
            }) {};
    private static boolean loggedDiagnostic;

    public SonicWaveRenderer(EntityRendererProvider.Context context)
    {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(SonicWaveEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource buffers, int packedLight)
    {
        if (!loggedDiagnostic && Boolean.getBoolean("fiskheroes.ciSonicWaveDiagnostics"))
        {
            loggedDiagnostic = true;
            com.fiskmods.heroes.FiskHeroes.LOGGER.info("Sonic wave ring rendered");
        }
        poseStack.pushPose();
        poseStack.translate(0.0D, entity.getBbHeight() / 2.0D, 0.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot())));
        poseStack.mulPose(Axis.XP.rotationDegrees(-Mth.lerp(partialTick, entity.xRotO, entity.getXRot())));
        float radius = entity.getRadius(partialTick);
        float inner = radius / 1.25F;
        float length = Math.max(radius / 5.0F, 0.1F);
        int alpha = Mth.clamp((int) (entity.getOpacity(partialTick) * 255.0F), 0, 255);
        VertexConsumer out = buffers.getBuffer(WAVE);
        PoseStack.Pose pose = poseStack.last();
        for (int i = 0; i < 36; ++i)
        {
            double a = Math.PI * 2.0D * i / 36.0D;
            double b = Math.PI * 2.0D * (i + 1) / 36.0D;
            float x0 = (float) Math.sin(a), y0 = (float) Math.cos(a);
            float x1 = (float) Math.sin(b), y1 = (float) Math.cos(b);
            // Outer cylindrical wall and the flat annular face at its base.
            vertex(out, pose, x1 * radius, y1 * radius, 0.0F, alpha);
            vertex(out, pose, x1 * radius, y1 * radius, length, alpha);
            vertex(out, pose, x0 * radius, y0 * radius, length, alpha);
            vertex(out, pose, x0 * radius, y0 * radius, 0.0F, alpha);
            vertex(out, pose, x1 * inner, y1 * inner, 0.0F, alpha);
            vertex(out, pose, x1 * radius, y1 * radius, 0.0F, alpha);
            vertex(out, pose, x0 * radius, y0 * radius, 0.0F, alpha);
            vertex(out, pose, x0 * inner, y0 * inner, 0.0F, alpha);
        }
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);
    }

    private static void vertex(VertexConsumer out, PoseStack.Pose pose, float x, float y, float z, int alpha)
    {
        out.vertex(pose.pose(), x, y, z).color(255, 255, 255, alpha).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(SonicWaveEntity entity)
    {
        return new ResourceLocation("minecraft", "textures/atlas/blocks.png");
    }
}
