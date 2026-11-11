package com.fiskmods.heroes.client.render;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.entity.projectile.GrapplingHookEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Original three-claw hook mesh, rather than a flat vanilla tripwire-hook sprite. */
public final class GrapplingHookRenderer extends EntityRenderer<GrapplingHookEntity>
{
    private static final ResourceLocation TEXTURE = FiskHeroes.id("textures/models/grappling_hook.png");
    private final ModelPart model;
    private final ModelPart[] claws;

    public GrapplingHookRenderer(EntityRendererProvider.Context context)
    {
        super(context);
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition main = mesh.getRoot().addOrReplaceChild("main", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-.5F, 0, -.5F, 1, 5, 1), PartPose.ZERO);
        for (int i = 0; i < 3; ++i)
        {
            float rotation = (float) (i * Math.PI * 2 / 3);
            PartDefinition arm = main.addOrReplaceChild("claw" + i, CubeListBuilder.create()
                    .texOffs(4, 0).addBox(-.5F, -1, -4, 1, 1, 4),
                    PartPose.offsetAndRotation(0, 1.7F, 0, 1.7F, rotation, 0));
            arm.addOrReplaceChild("right", CubeListBuilder.create().texOffs(0, 8)
                    .addBox(-1, 0, 0, 1, 1, 2),
                    PartPose.offsetAndRotation(.5F, -.8F, -4, .5235988F, .61086524F, .5235988F));
            arm.addOrReplaceChild("left", CubeListBuilder.create().texOffs(0, 8).mirror()
                    .addBox(0, 0, 0, 1, 1, 2),
                    PartPose.offsetAndRotation(-.5F, -.8F, -4, .5235988F, -.61086524F, -.5235988F));
        }
        model = LayerDefinition.create(mesh, 64, 32).bakeRoot();
        ModelPart body = model.getChild("main");
        claws = new ModelPart[] {body.getChild("claw0"), body.getChild("claw1"), body.getChild("claw2")};
    }

    @Override
    public void render(GrapplingHookEntity hook, float yaw, float partial, PoseStack pose,
            MultiBufferSource buffers, int light)
    {
        float angle = .5F + 1.2F * (1.0F - hook.grappleProgress(partial));
        for (ModelPart claw : claws) claw.xRot = angle;
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(Mth.rotLerp(partial, hook.yRotO, hook.getYRot()) - 90));
        pose.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partial, hook.xRotO, hook.getXRot()) + 90));
        pose.mulPose(Axis.YP.rotationDegrees(180));
        pose.translate(0, -.3125, 0);
        pose.scale(1, -1, -1);
        model.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
        super.render(hook, yaw, partial, pose, buffers, light);
    }

    @Override public ResourceLocation getTextureLocation(GrapplingHookEntity hook) { return TEXTURE; }
}
