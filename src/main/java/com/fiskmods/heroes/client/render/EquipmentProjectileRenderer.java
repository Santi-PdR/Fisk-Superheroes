package com.fiskmods.heroes.client.render;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.entity.projectile.EquipmentProjectileEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Original grenade meshes; other equipment retains its item renderer. */
public final class EquipmentProjectileRenderer extends ThrownItemRenderer<EquipmentProjectileEntity>
{
    private static final ResourceLocation TIMED_GRENADE = FiskHeroes.id("textures/models/grenade.png");
    private static final ResourceLocation BODY = FiskHeroes.id("textures/models/freeze_grenade.png");
    private static final ResourceLocation LIGHTS = FiskHeroes.id("textures/models/freeze_grenade_lights.png");
    private static final java.util.Set<EquipmentProjectileEntity> LOGGED =
            java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());
    private final ModelPart grenade;
    private final ModelPart timedGrenade;

    public EquipmentProjectileRenderer(EntityRendererProvider.Context context)
    {
        super(context);
        grenade = createModel();
        timedGrenade = createTimedGrenadeModel();
    }

    @Override
    public void render(EquipmentProjectileEntity entity, float yaw, float partial, PoseStack pose,
            MultiBufferSource buffers, int light)
    {
        if (entity.isTimedGrenade())
        {
            renderTimedGrenade(entity, partial, pose, buffers, light);
            return;
        }
        if (!entity.isFreezeGrenade())
        {
            super.render(entity, yaw, partial, pose, buffers, light);
            return;
        }
        if (Boolean.getBoolean("fiskheroes.ciEquipmentDiagnostics") && LOGGED.add(entity))
            FiskHeroes.LOGGER.info("Freeze grenade model rendered: entity={}", entity.getId());
        pose.pushPose();
        pose.translate(0.0D, entity.getBbHeight() / 2.0D, 0.0D);
        pose.mulPose(Axis.YP.rotationDegrees(-90.0F + Mth.rotLerp(partial, entity.yRotO, entity.getYRot())));
        pose.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partial, entity.xRotO, entity.getXRot())
                - (entity.tickCount + partial) * 20.0F));
        pose.translate(0.0D, 0.025D, 0.0D);
        pose.scale(0.5F, -0.5F, -0.5F);
        grenade.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(BODY)), light, OverlayTexture.NO_OVERLAY);
        grenade.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(LIGHTS)), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }

    private void renderTimedGrenade(EquipmentProjectileEntity entity, float partial, PoseStack pose,
            MultiBufferSource buffers, int light)
    {
        if (Boolean.getBoolean("fiskheroes.ciEquipmentDiagnostics") && LOGGED.add(entity))
            FiskHeroes.LOGGER.info("Timed grenade model rendered: entity={}", entity.getId());
        pose.pushPose();
        pose.translate(0.0D, entity.getBbHeight() / 2.0D, 0.0D);
        pose.mulPose(Axis.YP.rotationDegrees(-90.0F + Mth.rotLerp(partial, entity.yRotO, entity.getYRot())));
        pose.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partial, entity.xRotO, entity.getXRot()) + entity.grenadeSpin(partial)));
        // The original model is centred at y=24 pixels, with a 2.8-block offset and scale 2.
        pose.translate(0.0D, 2.8D, 0.0D);
        pose.scale(2.0F, -2.0F, -2.0F);
        timedGrenade.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TIMED_GRENADE)), light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }

    private static ModelPart createTimedGrenadeModel()
    {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition body = mesh.getRoot().addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-0.5F, -3, -0.5F, 1, 3, 1)
                .texOffs(0, 4).addBox(-0.5039F, -1.8F, -0.5039F, 1, 2, 1, new CubeDeformation(0.1F)), PartPose.offset(0, 24, 0));
        body.addOrReplaceChild("lever", CubeListBuilder.create().texOffs(2, 1).addBox(-0.5F, 0, 0, 1, 0, 2),
                PartPose.offsetAndRotation(0, -3.3834F, 0.6725F, -1.3963F, 0, 0));
        body.addOrReplaceChild("pin", CubeListBuilder.create().texOffs(2, 0).addBox(-0.5F, 0, 0, 1, 0, 1),
                PartPose.offsetAndRotation(0, -3, -0.25F, 0.3927F, 0, 0));
        body.addOrReplaceChild("upper", CubeListBuilder.create().texOffs(0, 4)
                .addBox(-0.5F, -0.8F, -0.5F, 1, 2, 1, new CubeDeformation(0.1F)),
                PartPose.offsetAndRotation(0, -2, 0, 0, 0, -3.1416F));
        return LayerDefinition.create(mesh, 8, 8).bakeRoot();
    }

    private static ModelPart createModel()
    {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition body = mesh.getRoot().addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-1.5F, 0, -1.5F, 3, 8, 3), PartPose.ZERO);
        PartDefinition lever = body.addOrReplaceChild("lever", CubeListBuilder.create()
                .texOffs(0, 11).addBox(-0.8F, -5.5F, -1, 1, 12, 2), PartPose.offset(-1.5F, 0, 0));
        PartDefinition grip = lever.addOrReplaceChild("grip", CubeListBuilder.create()
                .texOffs(6, 11).addBox(-1, -3, -0.5F, 1, 6, 1), PartPose.offset(-0.5F, -0.5F, 0));
        grip.addOrReplaceChild("top", CubeListBuilder.create().texOffs(9, 0).addBox(0, -1, -1, 1, 1, 2),
                PartPose.offsetAndRotation(-1, -3, 0, 0, 0, 0.7853982F));
        grip.addOrReplaceChild("bottom", CubeListBuilder.create().texOffs(9, 0).addBox(0, 0, -1, 1, 1, 2),
                PartPose.offsetAndRotation(-1, 3, 0, 0, 0, -0.7853982F));
        PartDefinition cap = body.addOrReplaceChild("cap", CubeListBuilder.create()
                .texOffs(12, 4).addBox(-1, -3, -1, 2, 3, 2), PartPose.ZERO);
        cap.addOrReplaceChild("ring", CubeListBuilder.create().texOffs(12, 0).addBox(-1.5F, -0.5F, -1.5F, 3, 1, 3), PartPose.offset(0, -1.2F, 0));
        PartDefinition upper = cap.addOrReplaceChild("upper", CubeListBuilder.create()
                .texOffs(10, 9).addBox(-1.5F, -3, -1.5F, 3, 3, 3), PartPose.offset(0, -2, 0));
        upper.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(10, 15).addBox(-1, -1, -1, 2, 1, 2), PartPose.offset(0, -2.7F, 0));
        body.addOrReplaceChild("panel", CubeListBuilder.create().texOffs(6, 18).addBox(-1, 0, -2, 2, 3, 4), PartPose.offset(0, -0.4F, 0));
        body.addOrReplaceChild("side", CubeListBuilder.create().texOffs(0, 25).addBox(0, 0, -1, 2, 3, 2), PartPose.offset(0, -0.4F, 0));
        return LayerDefinition.create(mesh, 64, 32).bakeRoot();
    }
}
