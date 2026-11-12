package com.fiskmods.heroes.client.render;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.entity.projectile.CactusSpikeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Pointed, directional 3D spike based on the original ModelSpike, not a cactus item sprite. */
public final class CactusSpikeRenderer extends EntityRenderer<CactusSpikeEntity>
{
    private static final ResourceLocation TEXTURE = FiskHeroes.id("textures/block/cactus_spike.png");
    private static boolean logged;
    public CactusSpikeRenderer(EntityRendererProvider.Context context) { super(context); }

    @Override
    public void render(CactusSpikeEntity spike, float yaw, float partial, PoseStack pose,
            MultiBufferSource buffers, int light)
    {
        if (Boolean.getBoolean("fiskheroes.ciCactusDiagnostics") && !logged)
        {
            logged = true;
            FiskHeroes.LOGGER.info("Cactus spike 3D model rendered");
        }
        pose.pushPose();
        pose.translate(0, spike.getBbHeight() / 2.0F, 0);
        pose.mulPose(Axis.YP.rotationDegrees(Mth.rotLerp(partial, spike.yRotO, spike.getYRot()) - 90));
        pose.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partial, spike.xRotO, spike.getXRot()) - 90));
        pose.translate(0, -.1F, 0);
        pose.scale(.4F, .4F, .4F);
        var out = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        float w = .085F / 2, length = .2F, u = .125F, maxV = u / .085F;
        float[][] corners = {{-w,-w}, {w,-w}, {w,w}, {-w,w}};
        for (int i = 0; i < 4; ++i)
        {
            float[] a = corners[i], b = corners[(i + 1) % 4];
            float nx = a[0] + b[0], nz = a[1] + b[1];
            vertex(out, pose.last(), a[0], 0, a[1], 0, 0, nx, 0, nz, light);
            vertex(out, pose.last(), a[0], length, a[1], 0, maxV * length, nx, 0, nz, light);
            vertex(out, pose.last(), b[0], length, b[1], u, maxV * length, nx, 0, nz, light);
            vertex(out, pose.last(), b[0], 0, b[1], u, 0, nx, 0, nz, light);
            float s = maxV * (length - .8F);
            vertex(out, pose.last(), a[0], length, a[1], 0, s, nx, w, nz, light);
            vertex(out, pose.last(), 0, 1, 0, u / 2, 1, nx, w, nz, light);
            vertex(out, pose.last(), 0, 1, 0, u / 2, 1, nx, w, nz, light);
            vertex(out, pose.last(), b[0], length, b[1], u, s, nx, w, nz, light);
        }
        for (float[] c : corners) vertex(out, pose.last(), c[0], 0, c[1], c[0] > 0 ? u : 0,
                c[1] > 0 ? u : 0, 0, -1, 0, light);
        pose.popPose();
        super.render(spike, yaw, partial, pose, buffers, light);
    }

    private static void vertex(VertexConsumer out, PoseStack.Pose pose, float x, float y, float z, float u, float v,
            float nx, float ny, float nz, int light)
    {
        float norm = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        out.vertex(pose.pose(), x, y, z).color(255, 255, 255, 255).uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(pose.normal(), nx / norm, ny / norm, nz / norm).endVertex();
    }
    @Override public ResourceLocation getTextureLocation(CactusSpikeEntity spike) { return TEXTURE; }
}
