package com.fiskmods.heroes.client.render;

import com.fiskmods.heroes.common.spell.EarthCrackEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

/** Renders the caster-colored lightning that connects Earth Swallowing to its trapped target. */
public final class EarthCrackRenderer extends EntityRenderer<EarthCrackEntity>
{
    private static final ResourceLocation TEXTURE = new ResourceLocation("minecraft", "textures/atlas/blocks.png");

    public EarthCrackRenderer(EntityRendererProvider.Context context)
    {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public ResourceLocation getTextureLocation(EarthCrackEntity entity)
    {
        return TEXTURE;
    }

    @Override
    public void render(EarthCrackEntity entity, float entityYaw, float partialTick, PoseStack pose,
            MultiBufferSource buffers, int packedLight)
    {
        LivingEntity target = entity.getTarget();
        if (target == null || !target.isAlive()) return;

        int color = SpellRenderColors.get(entity.getCaster(), "colorEarthCrack", 0xB366FF);
        float age = entity.tickCount + partialTick;
        float fade = Mth.clamp(Math.min(age / 5.0F, (60.0F - age) / 10.0F), 0.0F, 1.0F);
        if (fade <= 0.0F) return;

        Vec3 end = new Vec3(target.getX() - entity.getX(), target.getBbHeight() * 0.85D,
                target.getZ() - entity.getZ());
        Vec3 start = new Vec3(0.0D, 0.12D, 0.0D);
        VertexConsumer vertex = buffers.getBuffer(RenderType.lightning());
        for (int bolt = 0; bolt < 3; ++bolt)
        {
            Random random = new Random(entity.getId() * 1047228L + bolt * 1000L + entity.tickCount / 2);
            Vec3 previous = start;
            int segments = Math.max(6, Math.min(16, (int) (end.distanceTo(start) * 2.0D)));
            for (int i = 1; i <= segments; ++i)
            {
                double t = (double) i / segments;
                Vec3 next = new Vec3(end.x * t, start.y + (end.y - start.y) * t, end.z * t);
                if (i < segments)
                {
                    double jitter = 0.16D * (1.0D - t * 0.45D);
                    next = next.add((random.nextDouble() - 0.5D) * jitter,
                            (random.nextDouble() - 0.5D) * jitter,
                            (random.nextDouble() - 0.5D) * jitter);
                }
                emitSegment(vertex, pose, previous, next, color, fade * (bolt == 0 ? 1.0F : 0.58F), bolt == 0 ? 0.035F : 0.018F);
                previous = next;
            }
        }
        super.render(entity, entityYaw, partialTick, pose, buffers, packedLight);
    }

    private static void emitSegment(VertexConsumer out, PoseStack pose, Vec3 from, Vec3 to,
            int color, float alpha, float width)
    {
        Vec3 direction = to.subtract(from).normalize();
        Vec3 side = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (side.lengthSqr() < 1.0E-5D) side = direction.cross(new Vec3(1.0D, 0.0D, 0.0D));
        side = side.normalize().scale(width);

        Vec3 a = from.add(side);
        Vec3 b = from.subtract(side);
        Vec3 c = to.subtract(side);
        Vec3 d = to.add(side);
        var matrix = pose.last().pose();
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;
        quad(out, matrix, a, b, c, d, red, green, blue, alpha);
    }

    private static void quad(VertexConsumer out, org.joml.Matrix4f pose, Vec3 a, Vec3 b, Vec3 c, Vec3 d,
            int red, int green, int blue, float alpha)
    {
        vertex(out, pose, a, red, green, blue, alpha);
        vertex(out, pose, b, red, green, blue, alpha);
        vertex(out, pose, c, red, green, blue, alpha);
        vertex(out, pose, d, red, green, blue, alpha);
    }

    private static void vertex(VertexConsumer out, org.joml.Matrix4f pose, Vec3 position,
            int red, int green, int blue, float alpha)
    {
        out.vertex(pose, (float) position.x, (float) position.y, (float) position.z)
                .color(red, green, blue, Mth.clamp((int) (alpha * 255.0F), 0, 255))
                .uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0).normal(0.0F, 1.0F, 0.0F).endVertex();
    }
}
