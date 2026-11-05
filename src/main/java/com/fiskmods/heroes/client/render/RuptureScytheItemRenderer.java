package com.fiskmods.heroes.client.render;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.item.ItemDualSword;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Original 3D Rupture's Scythe mesh ported from ModelRuptureScythe & RenderItemRuptureScythe. */
public final class RuptureScytheItemRenderer extends BlockEntityWithoutLevelRenderer
{
    private static final ResourceLocation TEXTURE = FiskHeroes.id("textures/models/ruptures_scythe.png");
    private static RuptureScytheItemRenderer instance;
    private final ModelPart handle1;

    private RuptureScytheItemRenderer()
    {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        this.handle1 = createModel().getChild("handle1");
    }

    public static RuptureScytheItemRenderer getInstance()
    {
        if (instance == null) instance = new RuptureScytheItemRenderer();
        return instance;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
            MultiBufferSource buffers, int light, int overlay)
    {
        pose.pushPose();
        switch (context)
        {
            case GUI ->
            {
                pose.translate(0.55D, 0.85D, 0.5D);
                pose.mulPose(Axis.YP.rotationDegrees(-45.0F));
                pose.mulPose(Axis.XP.rotationDegrees(-40.0F));
                pose.mulPose(Axis.YP.rotationDegrees(-25.0F));
                pose.scale(0.45F, -0.45F, -0.45F);
            }
            case GROUND, FIXED ->
            {
                pose.translate(0.5D, 0.5D, 0.5D);
                pose.mulPose(Axis.XP.rotationDegrees(90.0F));
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
                pose.scale(0.5F, -0.5F, -0.5F);
            }
            case FIRST_PERSON_RIGHT_HAND, FIRST_PERSON_LEFT_HAND ->
            {
                pose.translate(context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND ? 0.68D : 0.32D, 0.85D, 0.42D);
                pose.mulPose(Axis.YP.rotationDegrees(context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND ? 80.0F : -80.0F));
                pose.mulPose(Axis.XP.rotationDegrees(-20.0F));
                pose.scale(0.85F, -0.85F, -0.85F);
            }
            case THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND ->
            {
                pose.translate(0.5D, 0.95D, 0.55D);
                pose.mulPose(Axis.XP.rotationDegrees(15.0F));
                pose.mulPose(Axis.YP.rotationDegrees(180.0F));
                pose.scale(0.95F, -0.95F, -0.95F);
            }
            default ->
            {
                pose.translate(0.5D, 0.7D, 0.5D);
                pose.scale(0.6F, -0.6F, -0.6F);
            }
        }
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        handle1.render(pose, consumer, light, overlay);
        pose.popPose();
    }

    private static ModelPart createModel()
    {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition meshRoot = mesh.getRoot();
        PartDefinition p_handle1 = meshRoot.addOrReplaceChild("handle1", CubeListBuilder.create()
                .texOffs(28, 13).mirror(false).addBox(0.0F, 4.0F, 0.0F, 1.0F, 7.0F, 1.0F), PartPose.offset(-0.5F, -19.0F, -1.5F));
        p_handle1.addOrReplaceChild("handle3", CubeListBuilder.create()
                .texOffs(8, 23).mirror(false).addBox(0.0F, 0.0F, 0.0F, 1.0F, 3.0F, 3.0F), PartPose.offset(-0.02F, 1.0F, -0.5F));
        PartDefinition p_handle4 = p_handle1.addOrReplaceChild("handle4", CubeListBuilder.create()
                .texOffs(25, 0).mirror(false).addBox(0.0F, 0.0F, 0.0F, 3.0F, 3.0F, 3.0F), PartPose.offset(-1.0F, -2.8F, 1.0F));
        p_handle4.addOrReplaceChild("handle5", CubeListBuilder.create()
                .texOffs(18, 13).mirror(false).addBox(0.0F, 0.0F, 0.0F, 2.0F, 2.0F, 2.0F), PartPose.offset(0.5F, 0.8F, -1.9F));
        PartDefinition p_handle13 = p_handle1.addOrReplaceChild("handle13", CubeListBuilder.create()
                .texOffs(8, 12).mirror(false).addBox(0.0F, 0.0F, 0.0F, 1.0F, 10.0F, 1.0F), PartPose.offset(0.2F, 41.0F, 0.7F));
        PartDefinition p_handle14 = p_handle13.addOrReplaceChild("handle14", CubeListBuilder.create()
                .texOffs(8, 12).mirror(false).addBox(0.0F, 0.0F, 0.0F, 1.0F, 10.0F, 1.0F), PartPose.offset(-0.4F, 0.0F, 0.0F));
        p_handle14.addOrReplaceChild("handle15", CubeListBuilder.create()
                .texOffs(8, 12).mirror(false).addBox(0.0F, 0.0F, 0.0F, 1.0F, 10.0F, 1.0F), PartPose.offset(0.0F, 0.0F, 0.5F));
        p_handle13.addOrReplaceChild("handle16", CubeListBuilder.create()
                .texOffs(8, 12).mirror(false).addBox(0.0F, 0.0F, 0.0F, 1.0F, 10.0F, 1.0F), PartPose.offset(0.0F, 0.0F, 0.5F));
        PartDefinition p_blade1 = p_handle1.addOrReplaceChild("blade1", CubeListBuilder.create()
                .texOffs(14, 0).mirror(false).addBox(0.0F, 0.0F, -3.0F, 1.0F, 4.0F, 9.0F), PartPose.offset(0.0F, -4.0F, -3.0F));
        p_blade1.addOrReplaceChild("blade2", CubeListBuilder.create()
                .texOffs(12, 13).mirror(false).addBox(0.0F, 0.0F, 0.0F, 1.0F, 4.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 6.0F, -0.08726646F, 0.0F, 0.0F));
        PartDefinition p_blade3 = p_blade1.addOrReplaceChild("blade3", CubeListBuilder.create()
                .texOffs(14, 0).mirror(false).addBox(0.0F, 0.0F, 0.0F, 1.0F, 4.0F, 1.0F), PartPose.offset(0.0F, 0.0F, -4.0F));
        PartDefinition p_blade4 = p_blade3.addOrReplaceChild("blade4", CubeListBuilder.create()
                .texOffs(14, 0).mirror(false).addBox(0.0F, 0.0F, -1.0F, 1.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.06981317F, 0.0F, 0.0F));
        PartDefinition p_blade5 = p_blade4.addOrReplaceChild("blade5", CubeListBuilder.create()
                .texOffs(14, 0).mirror(false).addBox(0.0F, 0.0F, -1.0F, 1.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, -1.0F, 0.08726646F, 0.0F, 0.0F));
        PartDefinition p_blade6 = p_blade5.addOrReplaceChild("blade6", CubeListBuilder.create()
                .texOffs(14, 0).mirror(false).addBox(0.0F, 0.0F, -1.0F, 1.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, -1.0F, 0.08726646F, 0.0F, 0.0F));
        PartDefinition p_blade7 = p_blade6.addOrReplaceChild("blade7", CubeListBuilder.create()
                .texOffs(14, 0).mirror(false).addBox(0.0F, 0.0F, -1.0F, 1.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, -1.0F, 0.08726646F, 0.0F, 0.0F));
        PartDefinition p_blade8 = p_blade7.addOrReplaceChild("blade8", CubeListBuilder.create()
                .texOffs(14, 0).mirror(false).addBox(0.0F, 0.0F, -1.0F, 1.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, -1.0F, 0.08726646F, 0.0F, 0.0F));
        PartDefinition p_blade9 = p_blade8.addOrReplaceChild("blade9", CubeListBuilder.create()
                .texOffs(14, 0).mirror(false).addBox(0.0F, 0.0F, -1.0F, 1.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, -1.0F, 0.08726646F, 0.0F, 0.0F));
        PartDefinition p_blade10 = p_blade9.addOrReplaceChild("blade10", CubeListBuilder.create()
                .texOffs(14, 0).mirror(false).addBox(0.0F, 0.0F, -1.0F, 1.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, -1.0F, 0.08726646F, 0.0F, 0.0F));
        PartDefinition p_blade11 = p_blade10.addOrReplaceChild("blade11", CubeListBuilder.create()
                .texOffs(14, 0).mirror(false).addBox(0.0F, 0.0F, -1.0F, 1.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, -1.0F, 0.08726646F, 0.0F, 0.0F));
        PartDefinition p_blade12 = p_blade11.addOrReplaceChild("blade12", CubeListBuilder.create()
                .texOffs(14, 6).mirror(false).addBox(0.0F, 0.0F, -2.0F, 1.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, -1.0F, 0.08726646F, 0.0F, 0.0F));
        PartDefinition p_blade13 = p_blade12.addOrReplaceChild("blade13", CubeListBuilder.create()
                .texOffs(8, 21).mirror(false).addBox(0.0F, -1.0F, 0.0F, 1.0F, 1.0F, 8.0F), PartPose.offsetAndRotation(-0.03F, 1.0F, -2.0F, -0.6457718F, 0.0F, 0.0F));
        p_blade13.addOrReplaceChild("blade14", CubeListBuilder.create()
                .texOffs(18, 13).mirror(false).addBox(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 8.0F), PartPose.offsetAndRotation(0.0F, -1.0F, 8.0F, -0.12217305F, 0.0F, 0.0F));
        p_blade12.addOrReplaceChild("blade18", CubeListBuilder.create()
                .texOffs(18, 22).mirror(true).addBox(-0.99F, 0.0F, 0.0F, 1.0F, 1.0F, 4.0F), PartPose.offsetAndRotation(1.0F, 0.3F, -1.0F, -0.4712389F, 0.0F, 0.0F));
        PartDefinition p_blade15 = p_blade12.addOrReplaceChild("blade15", CubeListBuilder.create()
                .texOffs(8, 21).mirror(true).addBox(-1.0F, -1.0F, 0.0F, 1.0F, 1.0F, 8.0F), PartPose.offsetAndRotation(1.03F, 1.0F, -2.0F, -0.6457718F, 0.0F, 0.0F));
        p_blade15.addOrReplaceChild("blade16", CubeListBuilder.create()
                .texOffs(18, 13).mirror(true).addBox(-1.0F, 0.0F, 0.0F, 1.0F, 1.0F, 8.0F), PartPose.offsetAndRotation(0.0F, -1.0F, 8.0F, -0.12217305F, 0.0F, 0.0F));
        p_blade12.addOrReplaceChild("blade17", CubeListBuilder.create()
                .texOffs(18, 22).mirror(false).addBox(-0.01F, 0.0F, 0.0F, 1.0F, 1.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 0.3F, -1.0F, -0.4712389F, 0.0F, 0.0F));
        p_blade3.addOrReplaceChild("blade19", CubeListBuilder.create()
                .texOffs(25, 6).mirror(false).addBox(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 2.0F), PartPose.offset(0.0F, 3.0F, -2.0F));
        PartDefinition p_handle6 = p_handle1.addOrReplaceChild("handle6", CubeListBuilder.create()
                .texOffs(4, 0).mirror(false).addBox(0.0F, 0.0F, 0.0F, 1.0F, 24.0F, 1.0F), PartPose.offset(0.2F, 11.0F, 0.7F));
        p_handle6.addOrReplaceChild("handle9", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(0.0F, 0.0F, 0.0F, 1.0F, 31.0F, 1.0F), PartPose.offset(-0.02F, -7.0F, 0.5F));
        PartDefinition p_handle7 = p_handle6.addOrReplaceChild("handle7", CubeListBuilder.create()
                .texOffs(4, 0).mirror(false).addBox(0.0F, 0.0F, 0.0F, 1.0F, 24.0F, 1.0F), PartPose.offset(-0.4F, 0.0F, 0.0F));
        p_handle7.addOrReplaceChild("handle8", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(0.0F, 0.0F, 0.0F, 1.0F, 31.0F, 1.0F), PartPose.offset(0.02F, -7.0F, 0.5F));
        p_handle1.addOrReplaceChild("handle18", CubeListBuilder.create()
                .texOffs(8, 23).mirror(false).addBox(0.0F, 0.0F, 0.0F, 1.0F, 3.0F, 3.0F), PartPose.offset(0.03F, -1.0F, 1.0F));
        PartDefinition p_handle10 = p_handle1.addOrReplaceChild("handle10", CubeListBuilder.create()
                .texOffs(18, 0).mirror(false).addBox(0.0F, 0.0F, 0.0F, 1.0F, 7.0F, 1.0F), PartPose.offset(0.0F, 5.0F, 3.0F));
        p_handle10.addOrReplaceChild("handle12", CubeListBuilder.create()
                .texOffs(24, 22).mirror(false).addBox(0.0F, -1.0F, -3.0F, 1.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 7.0F, 1.0F, 0.5235988F, 0.0F, 0.0F));
        p_handle10.addOrReplaceChild("handle11", CubeListBuilder.create()
                .texOffs(24, 22).mirror(false).addBox(0.0F, 0.0F, -3.0F, 1.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 1.0F, -0.5235988F, 0.0F, 0.0F));
        p_handle1.addOrReplaceChild("handle17", CubeListBuilder.create()
                .texOffs(4, 25).mirror(false).addBox(0.0F, 0.0F, 0.0F, 1.0F, 6.0F, 1.0F), PartPose.offset(0.0F, 35.0F, 0.95F));
        p_handle1.addOrReplaceChild("handle2", CubeListBuilder.create()
                .texOffs(8, 0).mirror(false).addBox(0.0F, 0.0F, 0.0F, 2.0F, 11.0F, 1.0F), PartPose.offset(-0.5F, 0.0F, 0.5F));
        return LayerDefinition.create(mesh, 64, 32).bakeRoot();
    }
}
