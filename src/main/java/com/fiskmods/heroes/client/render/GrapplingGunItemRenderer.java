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

/** Original 3D Grappling Gun mesh ported from ModelGrapplingGun & RenderItemGrapplingGun. */
public final class GrapplingGunItemRenderer extends BlockEntityWithoutLevelRenderer
{
    private static final ResourceLocation TEXTURE = FiskHeroes.id("textures/models/grappling_gun.png");
    private static GrapplingGunItemRenderer instance;
    private final ModelPart shape1;

    private GrapplingGunItemRenderer()
    {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        this.shape1 = createModel().getChild("shape1");
    }

    public static GrapplingGunItemRenderer getInstance()
    {
        if (instance == null) instance = new GrapplingGunItemRenderer();
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
                pose.translate(0.52D, 0.55D, 0.5D);
                pose.mulPose(Axis.YP.rotationDegrees(45.0F));
                pose.mulPose(Axis.XP.rotationDegrees(-25.0F));
                pose.scale(1.6F, -1.6F, -1.6F);
            }
            case GROUND, FIXED ->
            {
                pose.translate(0.5D, 0.52D, 0.5D);
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
                pose.scale(1.2F, -1.2F, -1.2F);
            }
            case FIRST_PERSON_RIGHT_HAND, FIRST_PERSON_LEFT_HAND ->
            {
                pose.translate(context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND ? 0.68D : 0.32D, 0.54D, 0.44D);
                pose.mulPose(Axis.YP.rotationDegrees(context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND ? 180.0F : 0.0F));
                pose.scale(1.15F, -1.15F, -1.15F);
            }
            case THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND ->
            {
                pose.translate(0.5D, 0.52D, 0.5D);
                pose.mulPose(Axis.YP.rotationDegrees(180.0F));
                pose.scale(1.15F, -1.15F, -1.15F);
            }
            default ->
            {
                pose.translate(0.5D, 0.5D, 0.5D);
                pose.scale(1.1F, -1.1F, -1.1F);
            }
        }
        shape1.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light, overlay);
        pose.popPose();
    }

    private static ModelPart createModel()
    {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition meshRoot = mesh.getRoot();
        PartDefinition p_shape1 = meshRoot.addOrReplaceChild("shape1", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 5.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.6981317F, 0.0F, 0.0F));
        PartDefinition p_shape2 = p_shape1.addOrReplaceChild("shape2", CubeListBuilder.create()
                .texOffs(0, 12).mirror(false).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 2.0F, 1.0F), PartPose.offset(0.0F, 5.0F, 0.4F));
        p_shape2.addOrReplaceChild("shape4", CubeListBuilder.create()
                .texOffs(6, 0).mirror(false).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, -1.0F, -0.34906584F, 0.0F, 0.0F));
        p_shape2.addOrReplaceChild("shape3", CubeListBuilder.create()
                .texOffs(17, 6).mirror(false).addBox(-1.0F, -2.0F, 0.0F, 2.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.0F, -2.0F, -1.0F, -0.33161256F, 0.0F, 0.0F));
        p_shape1.addOrReplaceChild("shape5", CubeListBuilder.create()
                .texOffs(7, 6).mirror(false).addBox(-0.5F, 0.0F, 0.0F, 1.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.0F, -0.5F, -0.3F, -0.33161256F, 0.0F, 0.0F));
        PartDefinition p_shape7 = p_shape1.addOrReplaceChild("shape7", CubeListBuilder.create()
                .texOffs(6, 4).mirror(false).addBox(-1.5F, -3.0F, -3.9F, 3.0F, 3.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.6981317F, 0.0F, 0.0F));
        PartDefinition p_shape8 = p_shape7.addOrReplaceChild("shape8", CubeListBuilder.create()
                .texOffs(0, 21).mirror(false).addBox(-1.0F, -1.0F, -2.0F, 2.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.5F, 0.33161256F, 0.0F, 0.0F));
        p_shape8.addOrReplaceChild("shape9", CubeListBuilder.create()
                .texOffs(5, 17).mirror(false).addBox(-1.0F, -2.0F, -7.0F, 2.0F, 2.0F, 7.0F), PartPose.offsetAndRotation(0.0F, 0.0F, -2.0F, -0.33161256F, 0.0F, 0.0F));
        PartDefinition p_shape11 = p_shape7.addOrReplaceChild("shape11", CubeListBuilder.create()
                .texOffs(14, 12).mirror(false).addBox(-1.0F, 0.0F, -2.0F, 2.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(0.0F, -3.7F, 0.7F, 0.715585F, 0.0F, 0.0F));
        PartDefinition p_shape12 = p_shape11.addOrReplaceChild("shape12", CubeListBuilder.create()
                .texOffs(8, 12).mirror(false).addBox(-1.0F, -4.0F, -1.0F, 2.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.8552113F, 0.0F, 0.0F));
        PartDefinition p_shape13 = p_shape12.addOrReplaceChild("shape13", CubeListBuilder.create()
                .texOffs(10, 0).mirror(false).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.3F, -1.3F, 0.8552113F, 0.0F, 0.0F));
        PartDefinition p_shape14 = p_shape13.addOrReplaceChild("shape14", CubeListBuilder.create()
                .texOffs(10, 0).mirror(true).addBox(-0.8F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        p_shape14.addOrReplaceChild("shape15", CubeListBuilder.create()
                .texOffs(10, 0).mirror(false).addBox(-1.2F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        p_shape7.addOrReplaceChild("shape10", CubeListBuilder.create()
                .texOffs(0, 15).mirror(false).addBox(-1.0F, -1.0F, -4.0F, 2.0F, 2.0F, 4.0F), PartPose.offset(0.0F, -2.45F, -3.3F));
        p_shape1.addOrReplaceChild("shape6", CubeListBuilder.create()
                .texOffs(0, 7).mirror(false).addBox(-1.0F, -4.0F, -1.0F, 2.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 4.55F, 2.0F, -0.17453292F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 32, 32).bakeRoot();
    }
}
