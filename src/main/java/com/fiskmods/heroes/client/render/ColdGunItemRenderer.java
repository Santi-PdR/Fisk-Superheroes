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

/** Original 3D Cold Gun mesh + glowing lights pass ported from ModelColdGun & RenderItemColdGun. */
public final class ColdGunItemRenderer extends BlockEntityWithoutLevelRenderer
{
    private static final ResourceLocation TEXTURE = FiskHeroes.id("textures/models/cold_gun.png");
    private static final ResourceLocation TEXTURE_LIGHTS = FiskHeroes.id("textures/models/cold_gun_lights.png");
    private static ColdGunItemRenderer instance;
    private final ModelPart shape1;

    private ColdGunItemRenderer()
    {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        this.shape1 = createModel().getChild("shape1");
    }

    public static ColdGunItemRenderer getInstance()
    {
        if (instance == null) instance = new ColdGunItemRenderer();
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
                pose.translate(0.5D, 0.52D, 0.5D);
                pose.mulPose(Axis.YP.rotationDegrees(45.0F));
                pose.mulPose(Axis.XP.rotationDegrees(-25.0F));
                pose.scale(0.80F, -0.80F, -0.80F);
            }
            case GROUND, FIXED ->
            {
                pose.translate(0.5D, 0.5D, 0.5D);
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
                pose.scale(0.85F, -0.85F, -0.85F);
            }
            case FIRST_PERSON_RIGHT_HAND, FIRST_PERSON_LEFT_HAND ->
            {
                pose.translate(context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND ? 0.36D : 0.64D, 0.75D, 0.18D);
                pose.mulPose(Axis.YP.rotationDegrees(context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND ? 150.0F : -150.0F));
                pose.mulPose(Axis.XP.rotationDegrees(-10.0F));
                pose.scale(0.55F, -0.55F, -0.55F);
            }
            case THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND ->
            {
                pose.translate(0.5D, 0.95D, 0.55D);
                pose.mulPose(Axis.XP.rotationDegrees(90.0F));
                pose.scale(0.60F, -0.60F, -0.60F);
            }
            default ->
            {
                pose.translate(0.5D, 0.5D, 0.5D);
                pose.scale(0.85F, -0.85F, -0.85F);
            }
        }
        shape1.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(SuitItemRenderContext.textureOr(TEXTURE))), light, overlay);
        shape1.render(pose, buffers.getBuffer(RenderType.eyes(TEXTURE_LIGHTS)), LightTexture.FULL_BRIGHT, overlay, 0.45F, 0.85F, 1.0F, 1.0F);
        pose.popPose();
    }

    private static ModelPart createModel()
    {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition meshRoot = mesh.getRoot();
        PartDefinition p_shape1 = meshRoot.addOrReplaceChild("shape1", CubeListBuilder.create()
                .texOffs(0, 22).mirror(false).addBox(-1.0F, -3.0F, -3.41F, 2.0F, 3.0F, 6.0F), PartPose.offset(0.0F, -2.0F, 0.0F));
        p_shape1.addOrReplaceChild("shape55", CubeListBuilder.create()
                .texOffs(39, 0).mirror(false).addBox(-0.5F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(1.3F, -0.7F, -13.75F, 0.0F, 0.0F, 0.17453292F));
        p_shape1.addOrReplaceChild("shape56", CubeListBuilder.create()
                .texOffs(39, 2).mirror(false).addBox(-1.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.3F, -13.55F, 0.8552113F, 0.58817595F, 0.572468F));
        p_shape1.addOrReplaceChild("shape57", CubeListBuilder.create()
                .texOffs(39, 2).mirror(false).addBox(-1.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(-1.1F, -1.5F, -13.55F, 0.8552113F, 0.58817595F, 0.572468F));
        p_shape1.addOrReplaceChild("shape39", CubeListBuilder.create()
                .texOffs(31, 0).mirror(false).addBox(-2.0F, 0.0F, 0.0F, 2.0F, 2.0F, 2.0F), PartPose.offsetAndRotation(0.0F, -4.0F, -14.22F, 0.0F, 0.0F, -0.7853982F));
        p_shape1.addOrReplaceChild("shape6", CubeListBuilder.create()
                .texOffs(37, 0).mirror(false).addBox(0.0F, 0.0F, 0.0F, 1.0F, 4.0F, 6.0F), PartPose.offset(-1.9F, -4.5F, -9.11F));
        PartDefinition p_shape10 = p_shape1.addOrReplaceChild("shape10", CubeListBuilder.create()
                .texOffs(45, 0).mirror(false).addBox(0.0F, 0.0F, 0.0F, 4.0F, 3.0F, 1.0F), PartPose.offset(-2.0F, -4.6F, -2.8F));
        p_shape10.addOrReplaceChild("shape11", CubeListBuilder.create()
                .texOffs(18, 20).mirror(false).addBox(0.01F, -2.0F, -1.0F, 4.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 3.0F, 1.0F, -0.43633232F, 0.0F, 0.0F));
        PartDefinition p_shape12 = p_shape10.addOrReplaceChild("shape12", CubeListBuilder.create()
                .texOffs(18, 14).mirror(false).addBox(0.0F, 0.0F, 0.0F, 4.0F, 1.0F, 1.0F), PartPose.offset(0.0F, 0.0F, 0.94F));
        p_shape12.addOrReplaceChild("shape13", CubeListBuilder.create()
                .texOffs(18, 17).mirror(false).addBox(0.01F, 0.0F, -1.0F, 4.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 1.0F, 1.0F, -0.43633232F, 0.0F, 0.0F));
        PartDefinition p_shape3 = p_shape1.addOrReplaceChild("shape3", CubeListBuilder.create()
                .texOffs(0, 33).mirror(false).addBox(0.0F, -0.4F, 0.0F, 2.0F, 3.0F, 4.0F), PartPose.offset(-1.0F, -0.5F, -6.4F));
        PartDefinition p_shape4 = p_shape3.addOrReplaceChild("shape4", CubeListBuilder.create()
                .texOffs(0, 12).mirror(false).addBox(0.0F, 0.0F, 0.0F, 3.0F, 3.0F, 5.0F), PartPose.offset(-0.52F, 0.2F, 4.0F));
        p_shape4.addOrReplaceChild("shape5", CubeListBuilder.create()
                .texOffs(0, 12).mirror(false).addBox(0.0F, 0.0F, 0.0F, 3.0F, 3.0F, 5.0F), PartPose.offset(0.04F, 0.0F, 0.0F));
        p_shape1.addOrReplaceChild("shape2", CubeListBuilder.create()
                .texOffs(0, 57).mirror(false).addBox(0.0F, 0.0F, 0.0F, 3.0F, 4.0F, 3.0F), PartPose.offsetAndRotation(-1.5F, -2.7F, -3.5F, 0.34906584F, 0.0F, 0.0F));
        p_shape1.addOrReplaceChild("shape9", CubeListBuilder.create()
                .texOffs(22, 34).mirror(false).addBox(0.0F, 0.0F, 0.0F, 2.0F, 1.0F, 6.0F), PartPose.offsetAndRotation(-1.0F, -4.1F, -9.1F, (float) Math.PI / 90, 0.0F, 0.0F));
        p_shape1.addOrReplaceChild("shape58", CubeListBuilder.create()
                .texOffs(39, 2).mirror(false).addBox(-1.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(1.1F, -1.5F, -13.55F, 0.8552113F, 0.58817595F, 0.572468F));
        p_shape1.addOrReplaceChild("shape54", CubeListBuilder.create()
                .texOffs(39, 0).mirror(false).addBox(-0.5F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(-1.3F, -0.7F, -13.75F, 0.0F, 0.0F, -0.17453292F));
        PartDefinition p_shape26 = p_shape1.addOrReplaceChild("shape26", CubeListBuilder.create()
                .texOffs(48, 26).mirror(false).addBox(0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 5.0F), PartPose.offset(-0.5F, -4.0F, -14.0F));
        PartDefinition p_shape27 = p_shape26.addOrReplaceChild("shape27", CubeListBuilder.create()
                .texOffs(33, 26).mirror(true).addBox(-1.0F, 0.0F, 0.0F, 1.0F, 0.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.5235988F));
        PartDefinition p_shape28 = p_shape27.addOrReplaceChild("shape28", CubeListBuilder.create()
                .texOffs(18, 26).mirror(true).addBox(-1.0F, 0.0F, 0.0F, 1.0F, 0.0F, 5.0F), PartPose.offsetAndRotation(-1.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.5235988F));
        p_shape28.addOrReplaceChild("shape29", CubeListBuilder.create()
                .texOffs(0, 0).mirror(true).addBox(-2.0F, 0.01F, 0.0F, 2.0F, 0.0F, 5.0F), PartPose.offsetAndRotation(-1.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.5235988F));
        p_shape28.addOrReplaceChild("shape30", CubeListBuilder.create()
                .texOffs(0, 2).mirror(false).addBox(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F), PartPose.offset(-1.0F, 0.02F, 0.9F));
        p_shape27.addOrReplaceChild("shape31", CubeListBuilder.create()
                .texOffs(0, 2).mirror(false).addBox(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F), PartPose.offset(-1.0F, 0.02F, 0.9F));
        PartDefinition p_shape32 = p_shape26.addOrReplaceChild("shape32", CubeListBuilder.create()
                .texOffs(33, 26).mirror(false).addBox(0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 5.0F), PartPose.offsetAndRotation(1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.5235988F));
        PartDefinition p_shape33 = p_shape32.addOrReplaceChild("shape33", CubeListBuilder.create()
                .texOffs(18, 26).mirror(false).addBox(0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 5.0F), PartPose.offsetAndRotation(1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.5235988F));
        PartDefinition p_shape34 = p_shape33.addOrReplaceChild("shape34", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(0.0F, 0.0F, 0.0F, 2.0F, 0.0F, 5.0F), PartPose.offsetAndRotation(1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.5235988F));
        p_shape34.addOrReplaceChild("shape35", CubeListBuilder.create()
                .texOffs(0, 2).mirror(false).addBox(-1.02F, 0.0F, 0.0F, 1.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.9F, 0.0F, 0.0F, -1.5707964F));
        p_shape33.addOrReplaceChild("shape36", CubeListBuilder.create()
                .texOffs(0, 2).mirror(true).addBox(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F), PartPose.offset(0.0F, 0.02F, 0.9F));
        p_shape32.addOrReplaceChild("shape37", CubeListBuilder.create()
                .texOffs(0, 2).mirror(false).addBox(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F), PartPose.offset(0.0F, 0.02F, 0.9F));
        p_shape26.addOrReplaceChild("shape38", CubeListBuilder.create()
                .texOffs(0, 2).mirror(false).addBox(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F), PartPose.offset(0.0F, 0.02F, 0.9F));
        p_shape1.addOrReplaceChild("shape7", CubeListBuilder.create()
                .texOffs(23, 0).mirror(true).addBox(-1.0F, 0.0F, 0.0F, 1.0F, 4.0F, 6.0F), PartPose.offset(1.9F, -4.5F, -9.11F));
        PartDefinition p_shape18 = p_shape1.addOrReplaceChild("shape18", CubeListBuilder.create()
                .texOffs(31, 19).mirror(false).addBox(0.0F, 0.0F, 0.0F, 2.0F, 2.0F, 2.0F), PartPose.offsetAndRotation(-1.0F, -1.8F, 6.2F, -0.43633232F, 0.0F, 0.0F));
        p_shape18.addOrReplaceChild("shape25", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(0.0F, 0.0F, -1.0F, 1.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.5F, 0.4F, -0.4F, -0.08726646F, 0.0F, 0.0F));
        PartDefinition p_shape19 = p_shape18.addOrReplaceChild("shape19", CubeListBuilder.create()
                .texOffs(0, 50).mirror(false).addBox(-0.02F, 0.0F, 0.0F, 2.0F, 4.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 1.7F, 0.3F, 0.61086524F, 0.0F, 0.0F));
        PartDefinition p_shape20 = p_shape19.addOrReplaceChild("shape20", CubeListBuilder.create()
                .texOffs(23, 0).mirror(false).addBox(0.0F, -2.0F, -1.0F, 2.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 4.0F, 2.0F, 1.0471976F, 0.0F, 0.0F));
        PartDefinition p_shape21 = p_shape20.addOrReplaceChild("shape21", CubeListBuilder.create()
                .texOffs(20, 35).mirror(false).addBox(-0.01F, -4.0F, 0.0F, 2.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, -1.0F, 0.34906584F, 0.0F, 0.0F));
        PartDefinition p_shape22 = p_shape21.addOrReplaceChild("shape22", CubeListBuilder.create()
                .texOffs(13, 35).mirror(false).addBox(0.0F, -4.0F, 0.0F, 2.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(0.0F, -4.0F, 0.0F, -0.6981317F, 0.0F, 0.0F));
        p_shape22.addOrReplaceChild("shape23", CubeListBuilder.create()
                .texOffs(13, 35).mirror(false).addBox(0.01F, -4.0F, 0.0F, 2.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.2F, 0.3F, -0.17453292F, 0.0F, 0.0F));
        p_shape19.addOrReplaceChild("shape24", CubeListBuilder.create()
                .texOffs(0, 50).mirror(false).addBox(0.04F, 0.0F, 0.0F, 2.0F, 4.0F, 2.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        p_shape1.addOrReplaceChild("shape8", CubeListBuilder.create()
                .texOffs(31, 15).mirror(false).addBox(0.0F, -1.0F, 0.0F, 2.0F, 2.0F, 2.0F), PartPose.offsetAndRotation(-1.0F, -4.2F, -3.2F, 0.08726646F, 0.0F, 0.0F));
        p_shape1.addOrReplaceChild("shape53", CubeListBuilder.create()
                .texOffs(39, 0).mirror(false).addBox(-0.5F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F), PartPose.offset(0.0F, -1.4F, -13.75F));
        PartDefinition p_shape40 = p_shape1.addOrReplaceChild("shape40", CubeListBuilder.create()
                .texOffs(4, 0).mirror(false).addBox(0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 11.0F), PartPose.offset(-2.0F, -1.7F, -14.1F));
        p_shape40.addOrReplaceChild("shape49", CubeListBuilder.create()
                .texOffs(4, 0).mirror(false).addBox(0.01F, 0.0F, 0.0F, 0.0F, 1.0F, 11.0F), PartPose.offset(4.0F, 0.0F, 0.0F));
        PartDefinition p_shape41 = p_shape40.addOrReplaceChild("shape41", CubeListBuilder.create()
                .texOffs(0, 36).mirror(false).addBox(0.0F, 0.0F, 0.0F, 0.0F, 2.0F, 11.0F), PartPose.offsetAndRotation(0.0F, 1.0F, 0.0F, 0.0F, 0.0F, -0.25307274F));
        p_shape41.addOrReplaceChild("shape42", CubeListBuilder.create()
                .texOffs(0, 2).mirror(false).addBox(-1.0F, -2.0F, 0.0F, 1.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.02F, 0.0F, 1.0F, 0.0F, 0.0F, (float) Math.PI));
        PartDefinition p_shape50 = p_shape40.addOrReplaceChild("shape50", CubeListBuilder.create()
                .texOffs(0, 2).mirror(false).addBox(0.0F, 0.0F, 0.0F, 1.0F, 2.0F, 1.0F), PartPose.offset(0.2F, -1.0F, 1.0F));
        PartDefinition p_shape51 = p_shape50.addOrReplaceChild("shape51", CubeListBuilder.create()
                .texOffs(0, 2).mirror(false).addBox(0.0F, 0.0F, 0.0F, 1.0F, 2.0F, 1.0F), PartPose.offset(1.0F, 0.0F, 0.0F));
        p_shape51.addOrReplaceChild("shape52", CubeListBuilder.create()
                .texOffs(0, 2).mirror(false).addBox(0.0F, 0.0F, 0.0F, 1.0F, 2.0F, 1.0F), PartPose.offset(1.0F, 0.0F, 0.0F));
        PartDefinition p_shape45 = p_shape40.addOrReplaceChild("shape45", CubeListBuilder.create()
                .texOffs(41, 7).mirror(false).addBox(0.0F, -1.0F, 0.99F, 3.0F, 0.0F, 10.0F), PartPose.offset(0.5F, 3.94F, 0.0F));
        PartDefinition p_shape46 = p_shape45.addOrReplaceChild("shape46", CubeListBuilder.create()
                .texOffs(0, 2).mirror(false).addBox(0.0F, 0.0F, 0.0F, 1.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(3.0F, -1.02F, 1.0F, 0.0F, 0.0F, (float) Math.PI));
        PartDefinition p_shape47 = p_shape46.addOrReplaceChild("shape47", CubeListBuilder.create()
                .texOffs(0, 2).mirror(false).addBox(0.0F, 0.0F, 0.0F, 1.0F, 2.0F, 1.0F), PartPose.offset(1.0F, 0.0F, 0.0F));
        p_shape47.addOrReplaceChild("shape48", CubeListBuilder.create()
                .texOffs(0, 2).mirror(false).addBox(0.0F, 0.0F, 0.0F, 1.0F, 2.0F, 1.0F), PartPose.offset(1.0F, 0.0F, 0.0F));
        PartDefinition p_shape43 = p_shape40.addOrReplaceChild("shape43", CubeListBuilder.create()
                .texOffs(0, 36).mirror(true).addBox(0.0F, 0.0F, 0.0F, 0.0F, 2.0F, 11.0F), PartPose.offsetAndRotation(4.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.25307274F));
        p_shape43.addOrReplaceChild("shape44", CubeListBuilder.create()
                .texOffs(0, 2).mirror(true).addBox(0.0F, -2.0F, 0.0F, 1.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(-0.02F, 0.0F, 1.0F, 0.0F, 0.0F, -(float) Math.PI));
        PartDefinition p_shape14 = p_shape1.addOrReplaceChild("shape14", CubeListBuilder.create()
                .texOffs(28, 59).mirror(false).addBox(-0.02F, 0.0F, -1.0F, 3.0F, 2.0F, 3.0F), PartPose.offset(-1.5F, -4.0F, -2.4F));
        PartDefinition p_shape15 = p_shape14.addOrReplaceChild("shape15", CubeListBuilder.create()
                .texOffs(30, 10).mirror(false).addBox(0.0F, 0.0F, 0.0F, 3.0F, 1.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 2.0F, -0.17453292F, 0.0F, 0.0F));
        p_shape15.addOrReplaceChild("shape16", CubeListBuilder.create()
                .texOffs(13, 59).mirror(false).addBox(0.0F, 0.0F, 0.0F, 3.0F, 1.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 4.0F, -0.17453292F, 0.0F, 0.0F));
        p_shape14.addOrReplaceChild("shape17", CubeListBuilder.create()
                .texOffs(28, 59).mirror(false).addBox(0.04F, 0.0F, 0.0F, 3.0F, 2.0F, 3.0F), PartPose.offset(0.0F, 0.0F, -1.0F));
        return LayerDefinition.create(mesh, 64, 64).bakeRoot();
    }
}
