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

/** Original 3D Beretta 93R mesh ported from ModelBeretta93R & RenderItemBeretta93R. */
public final class Beretta93RItemRenderer extends BlockEntityWithoutLevelRenderer
{
    private static final ResourceLocation TEXTURE = FiskHeroes.id("textures/models/beretta_93r.png");
    private static Beretta93RItemRenderer instance;
    private final ModelPart root;

    private Beretta93RItemRenderer()
    {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        this.root = createModel().getChild("root");
    }

    public static Beretta93RItemRenderer getInstance()
    {
        if (instance == null) instance = new Beretta93RItemRenderer();
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
                pose.translate(0.52D, 0.52D, 0.5D);
                pose.mulPose(Axis.YP.rotationDegrees(45.0F));
                pose.mulPose(Axis.XP.rotationDegrees(-25.0F));
                pose.scale(1.85F, -1.85F, -1.85F);
            }
            case GROUND, FIXED ->
            {
                pose.translate(0.5D, 0.5D, 0.5D);
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
                pose.scale(1.35F, -1.35F, -1.35F);
            }
            case FIRST_PERSON_RIGHT_HAND, FIRST_PERSON_LEFT_HAND ->
            {
                pose.translate(context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND ? 0.68D : 0.32D, 0.52D, 0.42D);
                pose.mulPose(Axis.ZP.rotationDegrees(context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND ? 180.0F : 0.0F));
                pose.mulPose(Axis.YP.rotationDegrees(context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND ? 110.0F : -110.0F));
                pose.mulPose(Axis.XP.rotationDegrees(-10.0F));
                pose.scale(1.2F, -1.2F, -1.2F);
            }
            case THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND ->
            {
                pose.translate(context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND ? 0.7D : 0.3D, 0.48D, 0.55D);
                pose.mulPose(Axis.YP.rotationDegrees(context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND ? 90.0F : -90.0F));
                pose.scale(1.3F, -1.3F, -1.3F);
            }
            default ->
            {
                pose.translate(0.5D, 0.5D, 0.5D);
                pose.scale(1.1F, -1.1F, -1.1F);
            }
        }
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        root.render(pose, consumer, light, overlay);
        if (context != ItemDisplayContext.NONE && ItemDualSword.isDual(stack))
        {
            pose.translate(-0.34D, 0.0D, 0.0D);
            root.render(pose, consumer, light, overlay);
        }
        pose.popPose();
    }

    private static ModelPart createModel()
    {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition meshRoot = mesh.getRoot();
        PartDefinition p_root = meshRoot.addOrReplaceChild("root", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(0.0F, -0.25F, -6.9141F, 0.0F, 0.0F, 0.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p_shape2 = p_root.addOrReplaceChild("shape2", CubeListBuilder.create()
                .texOffs(10, 0).mirror(false).addBox(-0.9F, 0.0F, -1.0F, 1.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.0F, -0.25F, 2.0859F, -0.7854F, 0.0F, 0.0F));
        p_shape2.addOrReplaceChild("shape27", CubeListBuilder.create()
                .texOffs(27, 28).mirror(false).addBox(-0.1F, 0.0F, -1.0F, 1.0F, 2.0F, 1.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p_shape3 = p_root.addOrReplaceChild("shape3", CubeListBuilder.create()
                .texOffs(25, 12).mirror(false).addBox(-2.0F, 0.0F, -5.0F, 2.0F, 2.0F, 1.0F), PartPose.offset(1.0F, -2.05F, -2.2141F));
        PartDefinition p_shape4 = p_shape3.addOrReplaceChild("shape4", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-1.0F, 0.25F, -0.4805F, 1.0F, 1.0F, 8.0F), PartPose.offset(-0.5F, -0.1F, -5.5F));
        p_shape4.addOrReplaceChild("Box_r1", CubeListBuilder.create()
                .texOffs(18, 1).mirror(false).addBox(-0.5F, -0.5F, -1.5F, 1.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(-0.5F, 0.85F, 0.0117F, 0.0F, 0.0F, 0.7854F));
        p_shape4.addOrReplaceChild("shape5", CubeListBuilder.create()
                .texOffs(8, 12).mirror(false).addBox(-1.0F, 0.25F, 0.0F, 1.0F, 1.0F, 5.0F), PartPose.offset(-0.4F, 0.2F, 1.5F));
        p_shape4.addOrReplaceChild("shape6", CubeListBuilder.create()
                .texOffs(10, 1).mirror(false).addBox(0.0F, 0.25F, 0.0F, 1.0F, 1.0F, 5.0F), PartPose.offset(-0.6F, 0.2F, 1.5F));
        p_shape4.addOrReplaceChild("shape7", CubeListBuilder.create()
                .texOffs(7, 19).mirror(false).addBox(-1.0F, -0.3086F, 0.5F, 1.0F, 1.0F, 4.0F)
                .texOffs(0, 0).mirror(false).addBox(-1.0F, -1.15F, 5.5185F, 1.0F, 1.0F, 0.0F), PartPose.offset(0.0F, 1.4F, 1.0F));
        PartDefinition p_shape8 = p_shape3.addOrReplaceChild("shape8", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 1.0F));
        PartDefinition p_hammer = p_shape8.addOrReplaceChild("hammer", CubeListBuilder.create(), PartPose.offsetAndRotation(-1.0F, 1.8483F, 2.5747F, -0.5236F, 0.0F, 0.0F));
        p_hammer.addOrReplaceChild("Box_r2", CubeListBuilder.create()
                .texOffs(9, 10).mirror(false).addBox(-0.5F, -0.75F, 0.25F, 1.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.0F, -1.1401F, -0.1058F, -0.6981F, 0.0F, 0.0F));
        PartDefinition p_shape12 = p_shape3.addOrReplaceChild("shape12", CubeListBuilder.create(), PartPose.offset(0.0F, 1.0F, -4.0F));
        PartDefinition p_chamber = p_shape12.addOrReplaceChild("chamber", CubeListBuilder.create()
                .texOffs(17, 7).mirror(false).addBox(-0.95F, -1.1519F, -1.962F, 2.0F, 2.0F, 3.0F)
                .texOffs(0, 17).mirror(false).addBox(-1.05F, -1.1519F, -1.962F, 2.0F, 2.0F, 3.0F)
                .texOffs(0, 10).mirror(false).addBox(-1.0F, -0.1558F, -6.462F, 2.0F, 1.0F, 5.0F), PartPose.offset(-1.0F, 0.1519F, 6.462F));
        p_chamber.addOrReplaceChild("cube_r1", CubeListBuilder.create()
                .texOffs(0, 30).mirror(false).addBox(0.0F, 0.9063F, -1.0156F, 2.0F, 1.0F, 1.0F)
                .texOffs(15, 30).mirror(false).addBox(0.0F, 0.0F, -1.0F, 2.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(-1.0F, -1.1519F, 1.038F, 0.3272F, 0.0F, 0.0F));
        PartDefinition p_shape18 = p_root.addOrReplaceChild("shape18", CubeListBuilder.create()
                .texOffs(0, 1).mirror(false).addBox(-0.1F, 0.0F, 0.3F, 0.0F, 0.0F, 0.0F), PartPose.offsetAndRotation(1.0F, -2.05F, -0.7141F, -0.4363F, 0.0F, 0.0F));
        PartDefinition p_shape10 = p_shape18.addOrReplaceChild("shape10", CubeListBuilder.create()
                .texOffs(18, 12).mirror(false).addBox(-1.0039F, 0.0F, -3.0F, 1.0F, 2.0F, 3.0F), PartPose.offsetAndRotation(-0.5F, 1.0F, 0.5F, 0.4189F, 0.0F, 0.0F));
        p_shape10.addOrReplaceChild("cube_r2", CubeListBuilder.create()
                .texOffs(26, 15).mirror(false).addBox(-0.9883F, 0.7612F, -4.2473F, 1.0F, 1.0F, 1.0F)
                .texOffs(26, 15).mirror(false).addBox(-1.0117F, 0.7612F, -4.2473F, 1.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 2.7071F, 0.5355F, -0.7854F, 0.0F, 0.0F));
        PartDefinition p_handle2 = p_shape10.addOrReplaceChild("handle2", CubeListBuilder.create(), PartPose.offset(-0.5F, 0.9572F, -3.7054F));
        p_handle2.addOrReplaceChild("cube_r3", CubeListBuilder.create()
                .texOffs(0, 10).mirror(false).addBox(-0.5F, -0.6548F, 0.0123F, 1.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(0.0F, -0.0195F, 0.0039F, -0.7854F, 0.0F, 0.0F));
        p_shape18.addOrReplaceChild("shape9", CubeListBuilder.create()
                .texOffs(5, 0).mirror(false).addBox(-1.0F, 0.0F, 0.0F, 1.0F, 1.0F, 0.0F), PartPose.offsetAndRotation(-0.5F, 2.0F, 0.0F, 0.1745F, 0.0F, 0.0F));
        PartDefinition p_shape11 = p_shape18.addOrReplaceChild("shape11", CubeListBuilder.create()
                .texOffs(0, 23).mirror(false).addBox(0.0F, 0.0F, 0.0F, 2.0F, 4.0F, 2.0F), PartPose.offsetAndRotation(-2.02F, 1.7F, 0.3F, 0.6109F, 0.0F, 0.0F));
        PartDefinition p_shape13 = p_shape11.addOrReplaceChild("shape13", CubeListBuilder.create()
                .texOffs(25, 7).mirror(false).addBox(0.0F, 0.0F, -1.0F, 2.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.02F, 1.5F, 2.0F, 0.1745F, 0.0F, 0.0F));
        PartDefinition p_shape14 = p_shape13.addOrReplaceChild("shape14", CubeListBuilder.create()
                .texOffs(22, 3).mirror(false).addBox(0.0F, 0.0F, -2.0F, 2.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(-0.03F, 2.0F, 0.0F, -0.3491F, 0.0F, 0.0F));
        p_shape14.addOrReplaceChild("shape15", CubeListBuilder.create()
                .texOffs(22, 0).mirror(false).addBox(0.0F, -0.01F, 0.0F, 2.0F, 1.0F, 1.0F), PartPose.offset(-0.01F, 0.0F, -2.4F));
        PartDefinition p_shape16 = p_shape18.addOrReplaceChild("shape16", CubeListBuilder.create()
                .texOffs(17, 19).mirror(false).addBox(-2.0F, 0.0F, 0.0F, 2.0F, 4.0F, 2.0F), PartPose.offsetAndRotation(0.02F, 1.7F, 0.3F, 0.6109F, 0.0F, 0.0F));
        PartDefinition p_shape17 = p_shape16.addOrReplaceChild("shape17", CubeListBuilder.create(), PartPose.offsetAndRotation(-0.02F, 1.5F, 2.0F, 0.1745F, 0.0F, 0.0F));
        PartDefinition p_shape20 = p_shape17.addOrReplaceChild("shape20", CubeListBuilder.create()
                .texOffs(23, 24).mirror(false).addBox(-2.0F, 0.0F, -2.0F, 2.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(0.03F, 2.0F, 0.0F, -0.3491F, 0.0F, 0.0F));
        p_shape20.addOrReplaceChild("shape19", CubeListBuilder.create()
                .texOffs(15, 13).mirror(false).addBox(-2.0F, -0.01F, 0.0F, 2.0F, 1.0F, 1.0F), PartPose.offset(0.01F, 0.0F, -2.4F));
        PartDefinition p_bone = p_root.addOrReplaceChild("bone", CubeListBuilder.create()
                .texOffs(25, 19).mirror(false).addBox(-0.15F, 0.1202F, -0.0587F, 1.0F, 1.0F, 2.0F)
                .texOffs(9, 3).mirror(false).addBox(-0.1461F, 0.128F, -0.3087F, 1.0F, 1.0F, 1.0F)
                .texOffs(7, 30).mirror(false).addBox(-0.857F, 0.128F, -0.3087F, 1.0F, 1.0F, 1.0F)
                .texOffs(16, 26).mirror(false).addBox(-0.85F, 0.1202F, -0.0587F, 1.0F, 1.0F, 2.0F), PartPose.offset(0.0F, 5.25F, 0.0859F));
        p_bone.addOrReplaceChild("cube_r4", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-0.5F, -0.8594F, 0.0F, 1.0F, 3.0F, 2.0F)
                .texOffs(9, 25).mirror(false).addBox(0.0F, -0.8594F, 0.0F, 1.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(-0.25F, -1.6406F, -0.5F, 0.1745F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 32, 32).bakeRoot();
    }
}
