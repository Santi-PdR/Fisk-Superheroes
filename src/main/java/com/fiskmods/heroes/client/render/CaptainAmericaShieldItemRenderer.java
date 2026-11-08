package com.fiskmods.heroes.client.render;

import java.util.ArrayList;
import java.util.List;

import com.fiskmods.heroes.FiskHeroes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Procedural 48-segment curved Vibranium shield disc + 5-point star + wrist/grip straps
 * ported directly from ModelCapsShield & RenderItemCapsShield.
 */
public final class CaptainAmericaShieldItemRenderer extends BlockEntityWithoutLevelRenderer
{
    private static final ResourceLocation TEXTURE = FiskHeroes.id("textures/models/captain_americas_shield.png");
    private static final float TEX_W = 32.0F;
    private static final float TEX_H = 32.0F;
    private static CaptainAmericaShieldItemRenderer instance;

    private final List<Quad> shapeQuads = new ArrayList<>();
    private final List<Quad> strapQuads = new ArrayList<>();
    private final ModelPart strapWrist;
    private final ModelPart strapGrip;

    private CaptainAmericaShieldItemRenderer()
    {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        buildShieldGeometry();
        ModelPart baked = createStrapBoxes();
        this.strapWrist = baked.getChild("strapWrist");
        this.strapGrip = baked.getChild("strapGrip");
    }

    public static CaptainAmericaShieldItemRenderer getInstance()
    {
        if (instance == null) instance = new CaptainAmericaShieldItemRenderer();
        return instance;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
            MultiBufferSource buffers, int light, int overlay)
    {
        Player player = Minecraft.getInstance().player;
        boolean blocking = player != null && player.isUsingItem() && player.getUseItem() == stack;
        boolean showStraps = false;

        pose.pushPose();
        switch (context)
        {
            case NONE ->
            {
                // ItemRenderer subtracts half a block before invoking a custom renderer.
                // Cancel that translation; the suit layer supplies the back anchor and rotation.
                pose.translate(0.5D, 0.5D, 0.5D);
            }
            case GUI ->
            {
                pose.translate(0.5D, 0.5D, 0.5D);
                pose.mulPose(Axis.XP.rotationDegrees(60.0F));
                pose.mulPose(Axis.ZP.rotationDegrees(-35.0F));
                pose.scale(0.95F, -0.95F, -0.95F);
            }
            case GROUND, FIXED ->
            {
                pose.translate(0.5D, 0.5D, 0.5D);
                pose.mulPose(Axis.XP.rotationDegrees(90.0F));
                pose.scale(0.95F, -0.95F, -0.95F);
            }
            case FIRST_PERSON_RIGHT_HAND, FIRST_PERSON_LEFT_HAND ->
            {
                boolean right = context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
                if (blocking)
                {
                    pose.translate(right ? 0.42D : 0.58D, 0.48D, 0.35D);
                    pose.mulPose(Axis.XP.rotationDegrees(-85.0F));
                    pose.mulPose(Axis.YP.rotationDegrees(right ? 15.0F : -15.0F));
                }
                else
                {
                    pose.translate(right ? 0.65D : 0.35D, 0.42D, 0.45D);
                    pose.mulPose(Axis.ZP.rotationDegrees(right ? 130.0F : -130.0F));
                    pose.mulPose(Axis.XP.rotationDegrees(-50.0F));
                    pose.mulPose(Axis.YP.rotationDegrees(right ? 90.0F : -90.0F));
                }
                pose.scale(0.95F, 0.95F, 0.95F);
            }
            case THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND ->
            {
                showStraps = !blocking;
                if (blocking)
                {
                    pose.translate(0.5D, 0.48D, 0.38D);
                    pose.mulPose(Axis.XP.rotationDegrees(-90.0F));
                }
                else
                {
                    pose.translate(0.5D, 0.45D, 0.5D);
                    pose.mulPose(Axis.ZP.rotationDegrees(-75.0F));
                    pose.mulPose(Axis.YP.rotationDegrees(-77.5F));
                }
                pose.scale(1.05F, 1.05F, 1.05F);
            }
            default ->
            {
                pose.translate(0.5D, 0.5D, 0.5D);
                pose.mulPose(Axis.XP.rotationDegrees(90.0F));
                pose.scale(0.95F, -0.95F, -0.95F);
            }
        }

        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));

        pose.pushPose();
        float discScale = 3.0874999F * 0.0625F;
        pose.scale(discScale, discScale, discScale);
        for (Quad q : shapeQuads)
        {
            q.emit(pose, consumer, light, overlay);
        }
        pose.popPose();

        pose.pushPose();
        pose.scale(0.0625F, 0.0625F, 0.0625F);
        for (Quad q : strapQuads)
        {
            q.emit(pose, consumer, light, overlay);
        }
        pose.popPose();

        if (showStraps)
        {
            strapWrist.render(pose, consumer, light, overlay);
            strapGrip.render(pose, consumer, light, overlay);
        }

        pose.popPose();
    }

    private void buildShieldGeometry()
    {
        float thickness = 0.033749998F;
        float angle = 72.0F;
        float res = 0.59259266F;
        Vec3 vec0 = new Vec3(0.3374999761581421D, -0.4F - thickness, 0.0D);
        float minU = 4.0F;
        float minV = 16.0F;

        for (int i = 0; i < 5; ++i)
        {
            Vec3 v30 = rotateY(new Vec3(0.8859374523162842D, vec0.y + thickness * 0.75F, 0.0D), Math.toRadians(angle * i - 90.0F));
            Vec3 v31 = rotateY(vec0, Math.toRadians(angle * (i - 0.5F) - 90.0F));
            Vec3 v32 = rotateY(vec0, Math.toRadians(angle * (i + 0.5F) - 90.0F));
            addTri(shapeQuads,
                    v31, (float) (v31.x * res * 8.0 + minU), (float) (-v31.z * res * 8.0 + minV + 0.5),
                    new Vec3(0.0D, vec0.y, 0.0D), minU, minV + 0.5F,
                    v32, (float) (v32.x * res * 8.0 + minU), (float) (-v32.z * res * 8.0 + minV + 0.5));
            addTri(shapeQuads,
                    v30, (float) (v30.x * res * 8.0 + minU), (float) (-v30.z * res * 8.0 + minV + 0.5),
                    v31, (float) (v31.x * res * 8.0 + minU), (float) (-v31.z * res * 8.0 + minV + 0.5),
                    v32, (float) (v32.x * res * 8.0 + minU), (float) (-v32.z * res * 8.0 + minV + 0.5));
        }

        for (int i = 0; i < 5; ++i)
        {
            Vec3 v30 = rotateY(new Vec3(0.8859374523162842D, vec0.y + thickness * 0.75F, 0.0D), Math.toRadians(angle * i - 90.0F));
            Vec3 v31 = rotateY(vec0, Math.toRadians(angle * (i - 0.5F) - 90.0F));
            Vec3 v32 = rotateY(vec0, Math.toRadians(angle * (i + 0.5F) - 90.0F));
            Vec3 v33 = v30.add(0.0D, thickness * 0.5D, 0.0D);
            addQuad(shapeQuads, false,
                    v33, (float) (v33.x * res * 8.0 + minU), (float) (-v33.z * res * 8.0 + minV + 0.5),
                    v31.add(0.0D, thickness, 0.0D), (float) (v31.x * res * 8.0 + minU), (float) (-v31.z * res * 8.0 + minV + 0.5),
                    v31, (float) (v31.x * res * 8.0 + minU), (float) (-v31.z * res * 8.0 + minV + 0.5),
                    v30, (float) (v30.x * res * 8.0 + minU), (float) (-v30.z * res * 8.0 + minV + 0.5));
            addQuad(shapeQuads, false,
                    v32, (float) (v32.x * res * 8.0 + minU), (float) (-v32.z * res * 8.0 + minV + 0.5),
                    v32.add(0.0D, thickness, 0.0D), (float) (v32.x * res * 8.0 + minU), (float) (-v32.z * res * 8.0 + minV + 0.5),
                    v33, (float) (v33.x * res * 8.0 + minU), (float) (-v33.z * res * 8.0 + minV + 0.5),
                    v30, (float) (v30.x * res * 8.0 + minU), (float) (-v30.z * res * 8.0 + minV + 0.5));
        }

        vec0 = new Vec3(0.9D, -0.4D, 0.0D);
        angle = 7.5F;
        thickness = 0.5F;
        res = 0.5F;
        float half = thickness * 0.5F;
        for (int i = 0; i < 48; ++i)
        {
            Vec3 v0 = rotateY(vec0, Math.toRadians(angle * i - 90.0F));
            Vec3 v1 = rotateY(vec0, Math.toRadians(angle * (i + 1) - 90.0F));
            minU = 12.0F;
            minV = 16.0F;
            addTri(shapeQuads,
                    v0, (float) (v0.x * res * 8.0 + minU), (float) (-v0.z * res * 8.0 + minV),
                    new Vec3(0.0D, -0.4D, 0.0D), minU, minV,
                    v1, (float) (v1.x * res * 8.0 + minU), (float) (-v1.z * res * 8.0 + minV));
            minU = 20.0F;
            addTri(shapeQuads,
                    v0.add(0.0D, half, 0.0D), (float) (-v0.x * res * 8.0 + minU), (float) (-v0.z * res * 8.0 + minV),
                    v1.add(0.0D, half, 0.0D), (float) (-v1.x * res * 8.0 + minU), (float) (-v1.z * res * 8.0 + minV),
                    new Vec3(0.0D, -0.4D + half, 0.0D), minU, minV);
        }

        float texW = 24.0F;
        float texH = 6.0F;
        for (int i = 0; i < 48; ++i)
        {
            double a0 = Math.toRadians(angle * i - 90.0F);
            double a1 = Math.toRadians(angle * (i + 1) - 90.0F);
            Vec3 v0 = rotateY(vec0.add(0.0D, half, 0.0D), a0);
            Vec3 v1 = rotateY(vec0.add(0.0D, half, 0.0D), a1);
            float u0 = texW * i / 48.0F;
            float u1 = texW * (i + 1) / 48.0F;
            float texY = 1.0F;
            minU = 0.0F;
            minV = 6.0F;
            for (int j = 1; j <= 6; ++j)
            {
                float f1 = j / 6.0F;
                Vec3 p0 = v0;
                Vec3 p1 = v1;
                float prevV = minV + texY * texH;
                Vec3 nextBase = new Vec3(0.9D + Math.sin(f1) * 1.5D / 0.9D, -0.4D + f1 * half + half, 0.0D);
                v1 = rotateY(nextBase, a1);
                v0 = rotateY(nextBase, a0);
                texY -= (j + 1) / 6.0F - f1;
                float nextV = minV + texY * texH;
                addQuad(shapeQuads, true,
                        p0, minU + u0, prevV,
                        p1, minU + u1, prevV,
                        v1, minU + u1, nextV,
                        v0, minU + u0, nextV);
            }

            v0 = rotateY(vec0, a0);
            v1 = rotateY(vec0, a1);
            texY = 1.0F;
            minV = 0.0F;
            for (int j = 1; j <= 6; ++j)
            {
                float f1 = j / 6.0F;
                Vec3 p0 = v0;
                Vec3 p1 = v1;
                float prevV = minV + texY * texH;
                Vec3 nextBase = new Vec3(0.9D + Math.sin(f1) * 1.5D / 0.9D, -0.4D + f1 * thickness, 0.0D);
                v1 = rotateY(nextBase, a1);
                v0 = rotateY(nextBase, a0);
                texY -= (j + 1) / 6.0F - f1;
                float nextV = minV + texY * texH;
                addQuad(shapeQuads, false,
                        p0, minU + u0, prevV,
                        p1, minU + u1, prevV,
                        v1, minU + u1, nextV,
                        v0, minU + u0, nextV);
            }
        }

        float edg = 7.0F;
        float w = 4.5F;
        float indent = 0.1396F;
        float yOff = 0.3F;
        minU = 0.0F;
        minV = 20.5F;
        float maxU = 1.0F;
        float maxV = minV + w;
        for (int i = 0; i < 2; ++i)
        {
            for (int j = 0; j < 2; ++j)
            {
                double rot = Math.toRadians(90 + (j * 2 - 1) * 40 + i * 180);
                double x = edg * Math.sin(rot);
                double z = edg * Math.cos(rot);
                rot += Math.PI * 0.5D;
                addQuad(strapQuads, false,
                        rotateY(rotateZ(new Vec3(w, 0.0D, -0.5D), indent), rot).add(x, yOff, z), minU, minV,
                        rotateY(new Vec3(0.0D, 0.0D, -0.5D), rot).add(x, yOff, z), minU, maxV,
                        rotateY(new Vec3(0.0D, 0.0D, 0.5D), rot).add(x, yOff, z), maxU, maxV,
                        rotateY(rotateZ(new Vec3(w, 0.0D, 0.5D), indent), rot).add(x, yOff, z), maxU, minV);
            }
        }
        float sx = -2.625F;
        yOff = -0.37F;
        minU = 1.0F;
        maxU = 5.0F;
        minV = 20.0F;
        maxV = 21.0F;
        w = 2.0F;
        addQuad(strapQuads, false,
                new Vec3(sx, yOff, w), minU, minV,
                new Vec3(sx + 1.0F, yOff, w), minU, maxV,
                new Vec3(sx + 1.0F, yOff, -w), maxU, maxV,
                new Vec3(sx, yOff, -w), maxU, minV);
        sx = -sx - 1.0F;
        addQuad(strapQuads, false,
                new Vec3(sx, yOff, w), minU, minV,
                new Vec3(sx + 1.0F, yOff, w), minU, maxV,
                new Vec3(sx + 1.0F, yOff, -w), maxU, maxV,
                new Vec3(sx, yOff, -w), maxU, minV);
    }

    private static ModelPart createStrapBoxes()
    {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        float x = 1.625F;
        root.addOrReplaceChild("strapWrist",
                CubeListBuilder.create().texOffs(12, 20).addBox(-0.5F, 0.0F, -2.5F, 1.0F, 3.0F, 5.0F),
                PartPose.offsetAndRotation(x + 0.55F, -0.5F, 0.0F, 0.0F, 0.0F, 0.0873F));
        root.addOrReplaceChild("strapGrip",
                CubeListBuilder.create().texOffs(0, 20).addBox(-0.5F, 0.0F, -2.5F, 1.0F, 5.0F, 5.0F),
                PartPose.offsetAndRotation(-x - 0.55F, -0.5F, 0.0F, 0.0F, 0.0F, -0.1396F));
        return LayerDefinition.create(mesh, 32, 32).bakeRoot();
    }

    private static Vec3 rotateY(Vec3 v, double rad)
    {
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        return new Vec3(v.x * cos + v.z * sin, v.y, v.z * cos - v.x * sin);
    }

    private static Vec3 rotateZ(Vec3 v, double rad)
    {
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        return new Vec3(v.x * cos - v.y * sin, v.y * cos + v.x * sin, v.z);
    }

    private static void addTri(List<Quad> out, Vec3 p0, float u0, float v0, Vec3 p1, float u1, float v1, Vec3 p2, float u2, float v2)
    {
        addQuad(out, false, p0, u0, v0, p1, u1, v1, p2, u2, v2, p2, u2, v2);
    }

    private static void addQuad(List<Quad> out, boolean mirror, Vec3 p0, float u0, float v0, Vec3 p1, float u1, float v1,
            Vec3 p2, float u2, float v2, Vec3 p3, float u3, float v3)
    {
        if (mirror)
        {
            out.add(Quad.of(p3, u3, v3, p2, u2, v2, p1, u1, v1, p0, u0, v0));
        }
        else
        {
            out.add(Quad.of(p0, u0, v0, p1, u1, v1, p2, u2, v2, p3, u3, v3));
        }
    }

    private record Quad(Vec3 p0, float u0, float v0, Vec3 p1, float u1, float v1, Vec3 p2, float u2, float v2,
            Vec3 p3, float u3, float v3, float nx, float ny, float nz)
    {
        static Quad of(Vec3 p0, float u0, float v0, Vec3 p1, float u1, float v1, Vec3 p2, float u2, float v2, Vec3 p3, float u3, float v3)
        {
            Vec3 e1 = p1.subtract(p0);
            Vec3 e2 = p2.subtract(p0);
            Vec3 n = e1.cross(e2).normalize();
            if (n.lengthSqr() < 1.0E-6D) n = new Vec3(0.0D, -1.0D, 0.0D);
            return new Quad(p0, u0 / TEX_W, v0 / TEX_H, p1, u1 / TEX_W, v1 / TEX_H,
                    p2, u2 / TEX_W, v2 / TEX_H, p3, u3 / TEX_W, v3 / TEX_H,
                    (float) n.x, (float) n.y, (float) n.z);
        }

        void emit(PoseStack pose, VertexConsumer consumer, int light, int overlay)
        {
            vert(pose, consumer, p0, u0, v0, light, overlay);
            vert(pose, consumer, p1, u1, v1, light, overlay);
            vert(pose, consumer, p2, u2, v2, light, overlay);
            vert(pose, consumer, p3, u3, v3, light, overlay);
        }

        private void vert(PoseStack pose, VertexConsumer consumer, Vec3 p, float u, float v, int light, int overlay)
        {
            consumer.vertex(pose.last().pose(), (float) p.x, (float) p.y, (float) p.z)
                    .color(255, 255, 255, 255)
                    .uv(u, v)
                    .overlayCoords(overlay)
                    .uv2(light)
                    .normal(pose.last().normal(), nx, ny, nz)
                    .endVertex();
        }
    }
}
