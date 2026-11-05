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

/** Original 3D Rip Hunter's Gun mesh + blue energy lights ported from ModelRipHuntersGun. */
public final class RipHuntersGunItemRenderer extends BlockEntityWithoutLevelRenderer
{
    private static final ResourceLocation TEXTURE = FiskHeroes.id("textures/models/rip_hunters_gun.png");
    private static final ResourceLocation TEXTURE_LIGHTS = FiskHeroes.id("textures/models/rip_hunters_gun_lights.png");
    private static RipHuntersGunItemRenderer instance;
    private final ModelPart shape1;

    private RipHuntersGunItemRenderer()
    {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        this.shape1 = createModel().getChild("shape1");
    }

    public static RipHuntersGunItemRenderer getInstance()
    {
        if (instance == null) instance = new RipHuntersGunItemRenderer();
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
                pose.translate(0.55D, 0.42D, 0.5D);
                pose.mulPose(Axis.YP.rotationDegrees(45.0F));
                pose.mulPose(Axis.XP.rotationDegrees(-25.0F));
                pose.scale(0.75F, -0.75F, -0.75F);
            }
            case GROUND, FIXED ->
            {
                pose.translate(0.5D, 0.48D, 0.5D);
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
                pose.scale(0.55F, -0.55F, -0.55F);
            }
            case FIRST_PERSON_RIGHT_HAND, FIRST_PERSON_LEFT_HAND ->
            {
                pose.translate(context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND ? 0.68D : 0.32D, 0.48D, 0.45D);
                pose.mulPose(Axis.YP.rotationDegrees(context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND ? 180.0F : 0.0F));
                pose.scale(0.68F, -0.68F, -0.68F);
            }
            case THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND ->
            {
                pose.translate(0.5D, 0.5D, 0.48D);
                pose.mulPose(Axis.YP.rotationDegrees(180.0F));
                pose.scale(0.6F, -0.6F, -0.6F);
            }
            default ->
            {
                pose.translate(0.5D, 0.5D, 0.5D);
                pose.scale(0.6F, -0.6F, -0.6F);
            }
        }
        shape1.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light, overlay);
        shape1.render(pose, buffers.getBuffer(RenderType.eyes(TEXTURE_LIGHTS)), LightTexture.FULL_BRIGHT, overlay, 0.0F, 0.37F, 1.0F, 1.0F);
        pose.popPose();
    }

    private static ModelPart createModel()
    {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition meshRoot = mesh.getRoot();
        PartDefinition p_shape1 = meshRoot.addOrReplaceChild("shape1", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-1.0F, -1.0F, -14.0F, 2.0F, 2.0F, 14.0F), PartPose.offset(0.0F, -9.0F, -16.0F));
        p_shape1.addOrReplaceChild("shape68", CubeListBuilder.create()
                .texOffs(23, 3).mirror(false).addBox(-0.5F, 0.0F, 0.0F, 1.0F, 2.0F, 2.0F), PartPose.offsetAndRotation(0.0F, -1.0F, -13.0F, 0.61086524F, 0.0F, 0.0F));
        PartDefinition p_shape2 = p_shape1.addOrReplaceChild("shape2", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-1.5F, -1.5F, -3.0F, 3.0F, 3.0F, 3.0F), PartPose.offset(0.0F, 0.0F, 3.0F));
        PartDefinition p_shape3 = p_shape2.addOrReplaceChild("shape3", CubeListBuilder.create()
                .texOffs(41, 24).mirror(false).addBox(-0.5F, 0.0F, 0.0F, 1.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(0.0F, -1.5F, -3.0F, 0.43633232F, 0.0F, 0.0F));
        PartDefinition p_shape4 = p_shape3.addOrReplaceChild("shape4", CubeListBuilder.create()
                .texOffs(37, 15).mirror(false).addBox(-0.5F, 0.0F, 0.0F, 1.0F, 1.0F, 8.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 2.0F, -0.43633232F, 0.0F, 0.0F));
        PartDefinition p_shape25 = p_shape4.addOrReplaceChild("shape25", CubeListBuilder.create()
                .texOffs(40, 7).mirror(true).addBox(0.0F, 0.0F, -3.5F, 1.0F, 1.0F, 7.0F), PartPose.offsetAndRotation(0.5F, 0.0F, 4.5F, 0.0F, 0.0F, 0.5235988F));
        PartDefinition p_shape26 = p_shape25.addOrReplaceChild("shape26", CubeListBuilder.create()
                .texOffs(47, 15).mirror(true).addBox(-1.0F, 0.0F, -0.5F, 1.0F, 7.0F, 1.0F), PartPose.offsetAndRotation(1.0F, 0.0F, 3.0F, 0.0F, 0.0F, -0.5235988F));
        PartDefinition p_shape27 = p_shape26.addOrReplaceChild("shape27", CubeListBuilder.create()
                .texOffs(40, 9).mirror(true).addBox(-0.5F, 0.0F, -1.0F, 1.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(-0.5F, 0.0F, 0.5F, 0.31415927F, 0.0F, 0.0F));
        PartDefinition p_shape28 = p_shape27.addOrReplaceChild("shape28", CubeListBuilder.create()
                .texOffs(39, 4).mirror(true).addBox(-0.5F, -1.0F, 0.0F, 1.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 4.0F, -1.0F, 0.6632251F, 0.0F, 0.0F));
        p_shape28.addOrReplaceChild("shape30", CubeListBuilder.create()
                .texOffs(43, 7).mirror(true).addBox(-0.5F, 0.0F, -1.0F, 1.0F, 2.0F, 1.0F), PartPose.offset(0.0F, -0.5F, 0.0F));
        p_shape28.addOrReplaceChild("shape29", CubeListBuilder.create()
                .texOffs(0, 8).mirror(true).addBox(-0.5F, 0.0F, -2.0F, 1.0F, 3.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 3.0F, 0.0F, 0.61086524F, 0.0F, 0.0F));
        p_shape26.addOrReplaceChild("shape31", CubeListBuilder.create()
                .texOffs(43, 0).mirror(true).addBox(-0.5F, -3.0F, 0.0F, 1.0F, 3.0F, 4.0F), PartPose.offsetAndRotation(-0.5F, 7.0F, -0.5F, -0.38397244F, 0.0F, 0.0F));
        p_shape25.addOrReplaceChild("shape32", CubeListBuilder.create()
                .texOffs(28, 23).mirror(true).addBox(-1.0F, 0.0F, -3.0F, 1.0F, 1.0F, 6.0F), PartPose.offsetAndRotation(1.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5235988F));
        PartDefinition p_shape17 = p_shape4.addOrReplaceChild("shape17", CubeListBuilder.create()
                .texOffs(40, 7).mirror(false).addBox(-1.0F, 0.0F, -3.5F, 1.0F, 1.0F, 7.0F), PartPose.offsetAndRotation(-0.5F, 0.0F, 4.5F, 0.0F, 0.0F, -0.5235988F));
        p_shape17.addOrReplaceChild("shape24", CubeListBuilder.create()
                .texOffs(28, 23).mirror(false).addBox(0.0F, 0.0F, -3.0F, 1.0F, 1.0F, 6.0F), PartPose.offsetAndRotation(-1.0F, 0.0F, -0.5F, 0.0F, 0.0F, 0.5235988F));
        PartDefinition p_shape18 = p_shape17.addOrReplaceChild("shape18", CubeListBuilder.create()
                .texOffs(47, 15).mirror(false).addBox(0.0F, 0.0F, -0.5F, 1.0F, 7.0F, 1.0F), PartPose.offsetAndRotation(-1.0F, 0.0F, 3.0F, 0.0F, 0.0F, 0.5235988F));
        PartDefinition p_shape19 = p_shape18.addOrReplaceChild("shape19", CubeListBuilder.create()
                .texOffs(40, 9).mirror(false).addBox(-0.5F, 0.0F, -1.0F, 1.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(0.5F, 0.0F, 0.5F, 0.31415927F, 0.0F, 0.0F));
        PartDefinition p_shape20 = p_shape19.addOrReplaceChild("shape20", CubeListBuilder.create()
                .texOffs(39, 4).mirror(false).addBox(-0.5F, -1.0F, 0.0F, 1.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 4.0F, -1.0F, 0.6632251F, 0.0F, 0.0F));
        p_shape20.addOrReplaceChild("shape21", CubeListBuilder.create()
                .texOffs(0, 8).mirror(false).addBox(-0.5F, 0.0F, -2.0F, 1.0F, 3.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 3.0F, 0.0F, 0.61086524F, 0.0F, 0.0F));
        p_shape20.addOrReplaceChild("shape22", CubeListBuilder.create()
                .texOffs(43, 7).mirror(false).addBox(-0.5F, 0.0F, -1.0F, 1.0F, 2.0F, 1.0F), PartPose.offset(0.0F, -0.5F, 0.0F));
        p_shape18.addOrReplaceChild("shape23", CubeListBuilder.create()
                .texOffs(43, 0).mirror(false).addBox(-0.5F, -3.0F, 0.0F, 1.0F, 3.0F, 4.0F), PartPose.offsetAndRotation(0.5F, 7.0F, -0.5F, -0.38397244F, 0.0F, 0.0F));
        PartDefinition p_shape5 = p_shape4.addOrReplaceChild("shape5", CubeListBuilder.create()
                .texOffs(21, 16).mirror(false).addBox(-0.5F, 0.0F, -1.0F, 1.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 8.0F, 0.31415927F, 0.0F, 0.0F));
        PartDefinition p_shape8 = p_shape5.addOrReplaceChild("shape8", CubeListBuilder.create()
                .texOffs(0, 16).mirror(false).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 1.0F), PartPose.offset(0.0F, 0.86F, 0.0F));
        PartDefinition p_shape9 = p_shape8.addOrReplaceChild("shape9", CubeListBuilder.create()
                .texOffs(6, 16).mirror(false).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 3.4F, -0.9F, 0.6632251F, 0.0F, 0.0F));
        PartDefinition p_shape10 = p_shape9.addOrReplaceChild("shape10", CubeListBuilder.create()
                .texOffs(0, 20).mirror(false).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 3.0F, 0.0F, 0.61086524F, 0.0F, 0.0F));
        PartDefinition p_shape11 = p_shape10.addOrReplaceChild("shape11", CubeListBuilder.create()
                .texOffs(18, 18).mirror(false).addBox(-1.5F, 0.0F, -5.0F, 3.0F, 6.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 3.0F, 0.3F, -1.0297443F, 0.0F, 0.0F));
        PartDefinition p_shape12 = p_shape11.addOrReplaceChild("shape12", CubeListBuilder.create()
                .texOffs(29, 11).mirror(false).addBox(-1.5F, 0.0F, -5.0F, 3.0F, 7.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 6.0F, 0.0F, -0.2617994F, 0.0F, 0.0F));
        p_shape12.addOrReplaceChild("shape13", CubeListBuilder.create()
                .texOffs(29, 3).mirror(false).addBox(-1.5F, -6.0F, -2.0F, 3.0F, 6.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 7.0F, -5.0F, -0.3281219F, 0.0F, 0.0F));
        PartDefinition p_shape14 = p_shape9.addOrReplaceChild("shape14", CubeListBuilder.create()
                .texOffs(19, 18).mirror(false).addBox(-0.5F, -4.0F, -1.0F, 1.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 2.0F, 1.0F, -0.61086524F, 0.0F, 0.0F));
        p_shape14.addOrReplaceChild("shape16", CubeListBuilder.create()
                .texOffs(36, 24).mirror(false).addBox(-0.5F, 0.0F, 0.0F, 1.0F, 1.0F, 3.0F), PartPose.offset(0.0F, -2.5F, 0.0F));
        p_shape14.addOrReplaceChild("shape15", CubeListBuilder.create()
                .texOffs(6, 20).mirror(false).addBox(-0.5F, -1.0F, -1.0F, 1.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, -4.0F, 0.0F, 0.6457718F, 0.0F, 0.0F));
        p_shape5.addOrReplaceChild("shape6", CubeListBuilder.create()
                .texOffs(25, 16).mirror(true).addBox(-1.0F, 0.0F, -1.0F, 1.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.5F, 0.0F, 0.0F, 0.0F, 0.0F, -0.5267404F));
        p_shape5.addOrReplaceChild("shape7", CubeListBuilder.create()
                .texOffs(25, 16).mirror(false).addBox(0.0F, 0.0F, -1.0F, 1.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(-0.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.5267404F));
        PartDefinition p_shape33 = p_shape2.addOrReplaceChild("shape33", CubeListBuilder.create()
                .texOffs(0, 16).mirror(false).addBox(-0.5F, -0.5F, -9.0F, 1.0F, 1.0F, 13.0F), PartPose.offset(0.0F, 1.8F, 2.9F));
        PartDefinition p_shape34 = p_shape33.addOrReplaceChild("shape34", CubeListBuilder.create()
                .texOffs(49, 7).mirror(false).addBox(-1.0F, -1.0F, -2.0F, 2.0F, 4.0F, 2.0F), PartPose.offset(0.0F, 0.12F, -2.9F));
        PartDefinition p_shape35 = p_shape34.addOrReplaceChild("shape35", CubeListBuilder.create()
                .texOffs(0, 24).mirror(false).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 3.0F, -2.0F, -0.5235988F, 0.0F, 0.0F));
        PartDefinition p_shape36 = p_shape35.addOrReplaceChild("shape36", CubeListBuilder.create()
                .texOffs(40, 24).mirror(false).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 1.0F, 7.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 2.0F, 0.5235988F, 0.0F, 0.0F));
        PartDefinition p_shape47 = p_shape36.addOrReplaceChild("shape47", CubeListBuilder.create()
                .texOffs(41, 0).mirror(false).addBox(-0.5F, -0.5F, -1.0F, 1.0F, 2.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 4.2F, 0.4712389F, 0.0F, 0.0F));
        PartDefinition p_shape48 = p_shape47.addOrReplaceChild("shape48", CubeListBuilder.create()
                .texOffs(33, 1).mirror(false).addBox(-0.5F, 0.0F, -1.0F, 1.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 1.5F, 0.0F, -0.40142572F, 0.0F, 0.0F));
        p_shape48.addOrReplaceChild("shape49", CubeListBuilder.create()
                .texOffs(37, 1).mirror(false).addBox(-0.5F, 0.0F, -1.0F, 1.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 1.0F, 0.0F, -0.6632251F, 0.0F, 0.0F));
        PartDefinition p_shape37 = p_shape36.addOrReplaceChild("shape37", CubeListBuilder.create()
                .texOffs(5, 7).mirror(false).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 2.0F, 2.0F), PartPose.offsetAndRotation(0.0F, -1.0F, 6.5F, 0.5235988F, 0.0F, 0.0F));
        p_shape37.addOrReplaceChild("shape38", CubeListBuilder.create()
                .texOffs(8, 11).mirror(false).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 2.0F, 0.4F, 0.87266463F, 0.0F, 0.0F));
        PartDefinition p_shape39 = p_shape37.addOrReplaceChild("shape39", CubeListBuilder.create()
                .texOffs(29, 0).mirror(false).addBox(-0.5F, 0.0F, -1.0F, 1.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 2.0F, 0.0F, -0.5235988F, 0.0F, 0.0F));
        PartDefinition p_shape40 = p_shape39.addOrReplaceChild("shape40", CubeListBuilder.create()
                .texOffs(25, 0).mirror(false).addBox(-0.5F, 0.0F, -1.0F, 1.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 2.0F, 0.0F, -0.43633232F, 0.0F, 0.0F));
        PartDefinition p_shape41 = p_shape40.addOrReplaceChild("shape41", CubeListBuilder.create()
                .texOffs(21, 0).mirror(false).addBox(-0.5F, 0.0F, -1.0F, 1.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 2.0F, 0.0F, -0.6981317F, 0.0F, 0.0F));
        PartDefinition p_shape42 = p_shape41.addOrReplaceChild("shape42", CubeListBuilder.create()
                .texOffs(19, 3).mirror(false).addBox(-0.5F, 0.0F, -1.0F, 1.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 2.0F, 0.0F, -0.43633232F, 0.0F, 0.0F));
        PartDefinition p_shape43 = p_shape42.addOrReplaceChild("shape43", CubeListBuilder.create()
                .texOffs(9, 0).mirror(false).addBox(-0.5F, 0.0F, -1.0F, 1.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 3.0F, 0.0F, -0.54105204F, 0.0F, 0.0F));
        PartDefinition p_shape44 = p_shape43.addOrReplaceChild("shape44", CubeListBuilder.create()
                .texOffs(15, 16).mirror(false).addBox(-0.5F, 0.0F, -1.0F, 1.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 2.0F, 0.0F, -0.61086524F, 0.0F, 0.0F));
        PartDefinition p_shape45 = p_shape44.addOrReplaceChild("shape45", CubeListBuilder.create()
                .texOffs(15, 19).mirror(false).addBox(-0.5F, 0.0F, -1.0F, 1.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 2.0F, 0.0F, -0.43633232F, 0.0F, 0.0F));
        p_shape45.addOrReplaceChild("shape46", CubeListBuilder.create()
                .texOffs(0, 6).mirror(false).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 2.0F, 0.0F, -0.34906584F, 0.0F, 0.0F));
        PartDefinition p_shape50 = p_shape33.addOrReplaceChild("shape50", CubeListBuilder.create()
                .texOffs(18, 7).mirror(false).addBox(-1.5F, 0.6F, -2.5F, 3.0F, 2.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.5235988F));
        PartDefinition p_shape57 = p_shape50.addOrReplaceChild("shape57", CubeListBuilder.create()
                .texOffs(18, 7).mirror(false).addBox(-1.5F, 0.6F, -2.5F, 3.0F, 2.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, (float) Math.PI));
        PartDefinition p_shape58 = p_shape57.addOrReplaceChild("shape58", CubeListBuilder.create()
                .texOffs(18, 8).mirror(false).addBox(-0.5F, -2.4F, -2.7F, 1.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.5235988F));
        p_shape58.addOrReplaceChild("shape59", CubeListBuilder.create()
                .texOffs(18, 10).mirror(false).addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 1.0F), PartPose.offset(0.0F, -1.9F, 1.7F));
        PartDefinition p_shape63 = p_shape50.addOrReplaceChild("shape63", CubeListBuilder.create()
                .texOffs(18, 7).mirror(false).addBox(-1.5F, 0.6F, -2.5F, 3.0F, 2.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 5.2359877F));
        PartDefinition p_shape64 = p_shape63.addOrReplaceChild("shape64", CubeListBuilder.create()
                .texOffs(18, 8).mirror(false).addBox(-0.5F, -2.4F, -2.7F, 1.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.5235988F));
        p_shape64.addOrReplaceChild("shape65", CubeListBuilder.create()
                .texOffs(18, 10).mirror(false).addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 1.0F), PartPose.offset(0.0F, -1.9F, 1.7F));
        PartDefinition p_shape66 = p_shape50.addOrReplaceChild("shape66", CubeListBuilder.create()
                .texOffs(18, 8).mirror(false).addBox(-0.5F, -2.4F, -2.7F, 1.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.5235988F));
        p_shape66.addOrReplaceChild("shape67", CubeListBuilder.create()
                .texOffs(18, 10).mirror(false).addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 1.0F), PartPose.offset(0.0F, -1.9F, 1.7F));
        PartDefinition p_shape60 = p_shape50.addOrReplaceChild("shape60", CubeListBuilder.create()
                .texOffs(18, 7).mirror(false).addBox(-1.5F, 0.6F, -2.5F, 3.0F, 2.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 4.1887903F));
        PartDefinition p_shape61 = p_shape60.addOrReplaceChild("shape61", CubeListBuilder.create()
                .texOffs(18, 8).mirror(false).addBox(-0.5F, -2.4F, -2.7F, 1.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.5235988F));
        p_shape61.addOrReplaceChild("shape62", CubeListBuilder.create()
                .texOffs(18, 10).mirror(false).addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 1.0F), PartPose.offset(0.0F, -1.9F, 1.7F));
        PartDefinition p_shape54 = p_shape50.addOrReplaceChild("shape54", CubeListBuilder.create()
                .texOffs(18, 7).mirror(false).addBox(-1.5F, 0.6F, -2.5F, 3.0F, 2.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 2.0943952F));
        PartDefinition p_shape55 = p_shape54.addOrReplaceChild("shape55", CubeListBuilder.create()
                .texOffs(18, 8).mirror(false).addBox(-0.5F, -2.4F, -2.7F, 1.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.5235988F));
        p_shape55.addOrReplaceChild("shape56", CubeListBuilder.create()
                .texOffs(18, 10).mirror(false).addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 1.0F), PartPose.offset(0.0F, -1.9F, 1.7F));
        PartDefinition p_shape51 = p_shape50.addOrReplaceChild("shape51", CubeListBuilder.create()
                .texOffs(18, 7).mirror(false).addBox(-1.5F, 0.6F, -2.5F, 3.0F, 2.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0471976F));
        PartDefinition p_shape52 = p_shape51.addOrReplaceChild("shape52", CubeListBuilder.create()
                .texOffs(18, 8).mirror(false).addBox(-0.5F, -2.4F, -2.7F, 1.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.5235988F));
        p_shape52.addOrReplaceChild("shape53", CubeListBuilder.create()
                .texOffs(18, 10).mirror(false).addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 1.0F), PartPose.offset(0.0F, -1.9F, 1.7F));
        return LayerDefinition.create(mesh, 64, 32).bakeRoot();
    }
}
