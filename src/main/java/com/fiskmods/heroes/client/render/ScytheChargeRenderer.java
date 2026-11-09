package com.fiskmods.heroes.client.render;

import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.data.var.Vars;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;

/** Short-lived red arcs around the blade and shaft while energy is charged. */
final class ScytheChargeRenderer
{
    private static final Set<String> DIAGNOSTICS = ConcurrentHashMap.newKeySet();
    // Points are in the same block-space as ModelPart geometry, not model pixels.
    private static final Vec3[][] PATHS = {
            {new Vec3(0.0D, -1.38D, 0.1D), new Vec3(0.0D, -1.36D, -1.05D)},
            {new Vec3(0.0D, -1.25D, -0.05D), new Vec3(0.0D, 1.75D, -0.05D)}
    };

    private ScytheChargeRenderer() {}

    static void render(PoseStack pose, MultiBufferSource buffers, ItemDisplayContext context)
    {
        LivingEntity wearer = SuitItemRenderContext.wearer();
        if (wearer == null || (context != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                && context != ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                && context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
                && context != ItemDisplayContext.THIRD_PERSON_LEFT_HAND)) return;
        var data = SHDataCapabilities.getPlayer(wearer);
        if (data == null) return;
        float rawCharge = data.getData().get(Vars.ENERGY_CHARGE);
        if (!Float.isFinite(rawCharge)) return;
        float charge = Mth.clamp(rawCharge, 0.0F, 1.0F);
        if (charge <= 0.01F) return;

        var vertices = buffers.getBuffer(RenderType.lightning());
        Random random = new Random((long) wearer.getId() * 31L + wearer.tickCount / 2);
        float alpha = 0.3F + charge * 0.55F;
        double width = 0.012D + charge * 0.012D;
        double jitter = 0.035D + charge * 0.09D;
        for (Vec3[] path : PATHS)
        {
            for (int strand = 0; strand < 2; ++strand)
            {
                Vec3 previous = path[0].add((strand == 0 ? -1 : 1) * 0.1D, 0, 0);
                for (int part = 1; part <= 6; ++part)
                {
                    double t = part / 6.0D;
                    Vec3 point = path[0].lerp(path[1], t).add((strand == 0 ? -1 : 1) * 0.1D
                            + (random.nextDouble() - 0.5D) * jitter,
                            (random.nextDouble() - 0.5D) * jitter,
                            (random.nextDouble() - 0.5D) * jitter);
                    bolt(vertices, pose.last(), previous, point, width, alpha);
                    previous = point;
                }
            }
        }
        if (DIAGNOSTICS.add(wearer.getUUID() + "|" + context))
            FiskHeroes.LOGGER.info("Scythe charge arcs: wearer={}, context={}, charge={}",
                    wearer.getName().getString(), context, charge);
    }

    private static void bolt(VertexConsumer vertex, PoseStack.Pose pose, Vec3 start, Vec3 end, double width, float alpha)
    {
        Vec3 direction = end.subtract(start).normalize();
        Vec3 side = direction.cross(new Vec3(0, 1, 0));
        if (side.lengthSqr() < 1.0E-5D) side = direction.cross(new Vec3(1, 0, 0));
        side = side.normalize().scale(width);
        Vec3 up = direction.cross(side).normalize().scale(width);
        Vec3[] offsets = {side.add(up), side.subtract(up), side.scale(-1).subtract(up), side.scale(-1).add(up)};
        for (int i = 0; i < 4; ++i)
        {
            Vec3 a = offsets[i], b = offsets[(i + 1) % 4];
            point(vertex, pose, start.add(a), alpha);
            point(vertex, pose, end.add(a), alpha);
            point(vertex, pose, end.add(b), alpha);
            point(vertex, pose, start.add(b), alpha);
        }
    }

    private static void point(VertexConsumer vertex, PoseStack.Pose pose, Vec3 point, float alpha)
    {
        vertex.vertex(pose.pose(), (float) point.x, (float) point.y, (float) point.z)
                .color(1.0F, 0.08F, 0.05F, alpha).endVertex();
    }
}
