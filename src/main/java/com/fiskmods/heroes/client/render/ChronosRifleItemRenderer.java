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

/** Original 3D Chronos Rifle mesh + glowing lights pass ported from ModelChronosRifle. */
public final class ChronosRifleItemRenderer extends BlockEntityWithoutLevelRenderer
{
    private static final ResourceLocation TEXTURE = FiskHeroes.id("textures/models/chronos_rifle.png");
    private static final ResourceLocation TEXTURE_LIGHTS = FiskHeroes.id("textures/models/chronos_rifle_lights.png");
    private static ChronosRifleItemRenderer instance;
    private final ModelPart shape1;

    private ChronosRifleItemRenderer()
    {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        this.shape1 = createModel().getChild("shape1");
    }

    public static ChronosRifleItemRenderer getInstance()
    {
        if (instance == null) instance = new ChronosRifleItemRenderer();
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
                pose.translate(0.45D, 0.45D, 0.5D);
                pose.mulPose(Axis.YP.rotationDegrees(45.0F));
                pose.mulPose(Axis.XP.rotationDegrees(-25.0F));
                pose.scale(0.75F, -0.75F, -0.75F);
            }
            case GROUND, FIXED ->
            {
                pose.translate(0.5D, 0.5D, 0.5D);
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
                pose.scale(0.65F, -0.65F, -0.65F);
            }
            case FIRST_PERSON_RIGHT_HAND, FIRST_PERSON_LEFT_HAND ->
            {
                pose.translate(context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND ? 0.65D : 0.35D, 0.48D, 0.4D);
                pose.mulPose(Axis.YP.rotationDegrees(context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND ? 180.0F : 0.0F));
                pose.scale(0.85F, -0.85F, -0.85F);
            }
            case THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND ->
            {
                pose.translate(0.5D, 0.5D, 0.42D);
                pose.mulPose(Axis.YP.rotationDegrees(180.0F));
                pose.scale(0.85F, -0.85F, -0.85F);
            }
            default ->
            {
                pose.translate(0.5D, 0.5D, 0.5D);
                pose.scale(0.75F, -0.75F, -0.75F);
            }
        }
        shape1.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light, overlay);
        shape1.render(pose, buffers.getBuffer(RenderType.eyes(TEXTURE_LIGHTS)), LightTexture.FULL_BRIGHT, overlay, 0.35F, 1.0F, 0.45F, 1.0F);
        pose.popPose();
    }

    private static ModelPart createModel()
    {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition meshRoot = mesh.getRoot();
        PartDefinition p_shape1 = meshRoot.addOrReplaceChild("shape1", CubeListBuilder.create()
                .texOffs(0, 25).mirror(false).addBox(-10.0F, -1.0F, -1.0F, 10.0F, 4.0F, 2.0F), PartPose.offsetAndRotation(0.0F, -3.0F, -10.0F, 0.0F, 1.5707964F, 0.0F));
        PartDefinition p_shape7 = p_shape1.addOrReplaceChild("shape7", CubeListBuilder.create()
                .texOffs(24, 28).mirror(false).addBox(-2.5F, 0.0F, -1.0F, 5.0F, 2.0F, 2.0F), PartPose.offset(0.0F, 2.5F, 0.0F));
        p_shape7.addOrReplaceChild("shape8", CubeListBuilder.create()
                .texOffs(6, 20).mirror(false).addBox(-2.5F, 0.0F, -0.5F, 5.0F, 1.0F, 1.0F), PartPose.offset(-5.0F, 0.5F, 0.0F));
        p_shape1.addOrReplaceChild("shape23", CubeListBuilder.create()
                .texOffs(0, 6).mirror(false).addBox(-3.0F, 0.0F, -1.0F, 3.0F, 5.0F, 2.0F), PartPose.offsetAndRotation(-7.0F, 3.0F, 0.0F, 0.0F, 0.0F, 0.34906584F));
        PartDefinition p_shape9 = p_shape1.addOrReplaceChild("shape9", CubeListBuilder.create()
                .texOffs(18, 13).mirror(false).addBox(-6.0F, -1.0F, -1.0F, 6.0F, 2.0F, 2.0F), PartPose.offset(-10.0F, 0.0F, 0.0F));
        PartDefinition p_shape15 = p_shape9.addOrReplaceChild("shape15", CubeListBuilder.create()
                .texOffs(0, 13).mirror(false).addBox(-2.0F, -1.0F, -1.5F, 2.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(1.2F, 0.86F, 0.0F, 0.0F, 0.0F, 0.40142572F));
        p_shape15.addOrReplaceChild("shape16", CubeListBuilder.create()
                .texOffs(8, 6).mirror(false).addBox(0.0F, -1.0F, -0.5F, 10.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -0.40142572F));
        PartDefinition p_shape18 = p_shape15.addOrReplaceChild("shape18", CubeListBuilder.create()
                .texOffs(12, 10).mirror(false).addBox(-7.0F, 0.0F, -1.0F, 7.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(-4.96F, -1.0F, 0.0F, 0.0F, 0.0F, -0.40142572F));
        p_shape18.addOrReplaceChild("shape19", CubeListBuilder.create()
                .texOffs(0, 20).mirror(false).addBox(0.0F, 0.0F, -1.0F, 2.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.40142572F));
        p_shape15.addOrReplaceChild("shape21", CubeListBuilder.create()
                .texOffs(0, 18).mirror(false).addBox(-3.0F, -0.5F, -1.0F, 3.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(-2.0F, -0.5F, 1.5F, 0.0F, -0.16929694F, 0.0F));
        p_shape15.addOrReplaceChild("shape17", CubeListBuilder.create()
                .texOffs(8, 6).mirror(false).addBox(0.0F, -1.0F, -0.5F, 10.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 1.0F, 0.0F, 0.0F, -0.40142572F));
        p_shape15.addOrReplaceChild("shape20", CubeListBuilder.create()
                .texOffs(0, 18).mirror(false).addBox(-3.0F, -0.5F, 0.0F, 3.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(-2.0F, -0.5F, -1.5F, 0.0F, 0.16929694F, 0.0F));
        PartDefinition p_shape10 = p_shape9.addOrReplaceChild("shape10", CubeListBuilder.create()
                .texOffs(36, 0).mirror(false).addBox(-5.0F, -1.0F, -1.0F, 5.0F, 3.0F, 2.0F), PartPose.offset(-6.0F, 0.0F, 0.0F));
        p_shape10.addOrReplaceChild("shape13", CubeListBuilder.create()
                .texOffs(33, 17).mirror(false).addBox(-1.5F, -2.0F, -0.2F, 3.0F, 4.0F, 1.0F), PartPose.offset(-3.0F, 1.5F, -1.0F));
        PartDefinition p_shape11 = p_shape10.addOrReplaceChild("shape11", CubeListBuilder.create()
                .texOffs(33, 5).mirror(false).addBox(-2.0F, 0.0F, -1.0F, 4.0F, 2.0F, 2.0F), PartPose.offset(-3.0F, 2.0F, 0.0F));
        p_shape11.addOrReplaceChild("shape12", CubeListBuilder.create()
                .texOffs(27, 17).mirror(false).addBox(-1.0F, -4.0F, -1.0F, 1.0F, 4.0F, 2.0F), PartPose.offsetAndRotation(2.0F, 2.0F, 0.0F, 0.0F, 0.0F, 0.4886922F));
        p_shape10.addOrReplaceChild("shape14", CubeListBuilder.create()
                .texOffs(33, 17).mirror(false).addBox(-1.5F, -2.0F, -0.8F, 3.0F, 4.0F, 1.0F), PartPose.offset(-3.0F, 1.5F, 1.0F));
        PartDefinition p_shape24 = p_shape1.addOrReplaceChild("shape24", CubeListBuilder.create()
                .texOffs(0, 23).mirror(false).addBox(-15.0F, -1.0F, -0.5F, 21.0F, 1.0F, 1.0F), PartPose.offset(-6.0F, -0.5F, 0.0F));
        PartDefinition p_shape25 = p_shape24.addOrReplaceChild("shape25", CubeListBuilder.create()
                .texOffs(8, 18).mirror(false).addBox(-2.0F, -1.0F, -0.5F, 4.0F, 1.0F, 1.0F), PartPose.offset(0.0F, -1.0F, 0.0F));
        PartDefinition p_shape26 = p_shape25.addOrReplaceChild("shape26", CubeListBuilder.create()
                .texOffs(34, 13).mirror(false).addBox(-2.5F, -2.0F, -1.0F, 5.0F, 2.0F, 2.0F), PartPose.offset(-0.4F, -0.9F, 0.0F));
        PartDefinition p_shape29 = p_shape26.addOrReplaceChild("shape29", CubeListBuilder.create()
                .texOffs(46, 27).mirror(false).addBox(-0.17F, -1.0F, -1.0F, 3.0F, 1.0F, 2.0F), PartPose.offset(3.2F, 0.0F, 0.0F));
        p_shape29.addOrReplaceChild("shape30", CubeListBuilder.create()
                .texOffs(38, 28).mirror(false).addBox(0.0F, 0.0F, -1.0F, 3.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(0.0F, -2.0F, 0.0F, 0.0F, 0.0F, 0.33964106F));
        PartDefinition p_shape27 = p_shape26.addOrReplaceChild("shape27", CubeListBuilder.create()
                .texOffs(44, 23).mirror(false).addBox(-1.0F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(-2.5F, -1.0F, 0.0F));
        p_shape27.addOrReplaceChild("shape28", CubeListBuilder.create()
                .texOffs(46, 23).mirror(false).addBox(-2.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F), PartPose.offset(-0.4F, 0.0F, 0.0F));
        p_shape26.addOrReplaceChild("shape31", CubeListBuilder.create()
                .texOffs(12, 13).mirror(false).addBox(0.0F, 0.0F, -0.5F, 2.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(2.5F, -2.0F, 0.0F, 0.0F, 0.0F, 0.40142572F));
        PartDefinition p_shape32 = p_shape1.addOrReplaceChild("shape32", CubeListBuilder.create()
                .texOffs(0, 3).mirror(false).addBox(0.0F, -1.0F, -0.5F, 16.0F, 2.0F, 1.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p_shape33 = p_shape32.addOrReplaceChild("shape33", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(0.0F, -0.5F, -1.0F, 16.0F, 1.0F, 2.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p_shape34 = p_shape33.addOrReplaceChild("shape34", CubeListBuilder.create()
                .texOffs(41, 17).mirror(false).addBox(-1.0F, -3.0F, -1.2F, 1.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(15.5F, -0.5F, 0.0F, 0.0F, 0.0F, -0.6981317F));
        p_shape34.addOrReplaceChild("shape35", CubeListBuilder.create()
                .texOffs(41, 17).mirror(false).addBox(-1.0F, -3.0F, 0.2F, 1.0F, 3.0F, 1.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        p_shape32.addOrReplaceChild("shape37", CubeListBuilder.create()
                .texOffs(43, 19).mirror(false).addBox(0.0F, -1.0F, -1.0F, 1.0F, 2.0F, 2.0F), PartPose.offset(16.0F, 0.0F, 0.0F));
        p_shape32.addOrReplaceChild("shape36", CubeListBuilder.create()
                .texOffs(12, 8).mirror(false).addBox(-5.0F, -0.5F, -0.5F, 10.0F, 1.0F, 1.0F), PartPose.offset(7.0F, -1.0F, 0.0F));
        PartDefinition p_shape2 = p_shape1.addOrReplaceChild("shape2", CubeListBuilder.create()
                .texOffs(32, 9).mirror(false).addBox(0.0F, -1.0F, -1.0F, 5.0F, 2.0F, 2.0F), PartPose.offset(0.0F, 1.5F, 0.0F));
        PartDefinition p_shape3 = p_shape2.addOrReplaceChild("shape3", CubeListBuilder.create()
                .texOffs(36, 25).mirror(false).addBox(-0.12F, -1.0F, -1.0F, 4.0F, 1.0F, 2.0F), PartPose.offset(5.0F, 0.0F, 0.0F));
        p_shape3.addOrReplaceChild("shape4", CubeListBuilder.create()
                .texOffs(24, 25).mirror(false).addBox(0.0F, -1.0F, -1.0F, 4.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 1.0F, 0.0F, 0.0F, 0.0F, -0.25481808F));
        p_shape2.addOrReplaceChild("shape6", CubeListBuilder.create()
                .texOffs(1, 8).mirror(false).addBox(0.0F, -0.2F, -7.0F, 1.0F, 1.0F, 9.0F), PartPose.offsetAndRotation(-1.0F, -0.7F, 1.0F, 1.5707964F, 0.61086524F, 1.5707964F));
        p_shape2.addOrReplaceChild("shape5", CubeListBuilder.create()
                .texOffs(1, 8).mirror(true).addBox(-1.0F, -0.8F, -2.0F, 1.0F, 1.0F, 9.0F), PartPose.offsetAndRotation(-1.0F, -0.7F, -1.0F, 1.5707964F, 0.61086524F, -1.5707964F));
        p_shape1.addOrReplaceChild("shape22", CubeListBuilder.create()
                .texOffs(19, 17).mirror(false).addBox(0.0F, -4.0F, -1.0F, 2.0F, 4.0F, 2.0F), PartPose.offsetAndRotation(-10.0F, 3.0F, 0.0F, 0.0F, 0.0F, -0.9948377F));
        return LayerDefinition.create(mesh, 64, 32).bakeRoot();
    }
}
