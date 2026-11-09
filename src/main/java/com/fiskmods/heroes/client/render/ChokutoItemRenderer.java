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

/** Original 3D Chokuto mesh ported from ModelChokuto & RenderItemSword. */
public final class ChokutoItemRenderer extends BlockEntityWithoutLevelRenderer
{
    private static final ResourceLocation TEXTURE = FiskHeroes.id("textures/models/chokuto.png");
    private static ChokutoItemRenderer instance;
    private final ModelPart sword;

    private ChokutoItemRenderer()
    {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        this.sword = createModel().getChild("sword");
    }

    public static ChokutoItemRenderer getInstance()
    {
        if (instance == null) instance = new ChokutoItemRenderer();
        return instance;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
            MultiBufferSource buffers, int light, int overlay)
    {
        pose.pushPose();
        switch (context)
        {
            case NONE -> pose.translate(0.5D, 0.5D, 0.5D);
            case GUI ->
            {
                renderInventorySword(pose, buffers, light, overlay, false);
                if (ItemDualSword.isDual(stack)) renderInventorySword(pose, buffers, light, overlay, true);
                pose.popPose();
                return;
            }
            case GROUND, FIXED ->
            {
                pose.translate(0.5D, 0.5D, 0.5D);
                pose.mulPose(Axis.ZP.rotationDegrees(90.0F));
                pose.scale(1.0F, -1.0F, -1.0F);
            }
            case FIRST_PERSON_RIGHT_HAND, FIRST_PERSON_LEFT_HAND ->
            {
                pose.translate(context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND ? 0.36D : 0.64D, 0.75D, 0.18D);
                pose.mulPose(Axis.YP.rotationDegrees(context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND ? 80.0F : -80.0F));
                pose.mulPose(Axis.XP.rotationDegrees(-30.0F));
                pose.scale(0.55F, -0.55F, -0.55F);
            }
            case THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND ->
            {
                pose.translate(0.5D, 0.95D, 0.55D);
                pose.mulPose(Axis.XP.rotationDegrees(90.0F));
                pose.scale(0.75F, -0.75F, -0.75F);
            }
            default ->
            {
                pose.translate(0.5D, 0.5D, 0.5D);
                pose.scale(1.0F, -1.0F, -1.0F);
            }
        }
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(SuitItemRenderContext.textureOr(TEXTURE)));
        sword.render(pose, consumer, light, overlay);
        if (ItemDualSword.isDual(stack) && (context == ItemDisplayContext.GUI || context == ItemDisplayContext.GROUND || context == ItemDisplayContext.FIXED))
        {
            pose.translate(0.0D, 0.0D, 0.26D);
            sword.render(pose, consumer, light, overlay);
        }
        pose.popPose();
    }

    private void renderInventorySword(PoseStack pose, MultiBufferSource buffers, int light, int overlay, boolean mirrored)
    {
        pose.pushPose();
        try
        {
            pose.translate(0.5D, 0.5D, 0.5D);
            pose.mulPose(Axis.YP.rotationDegrees(mirrored ? -90.0F : 90.0F));
            pose.mulPose(Axis.XP.rotationDegrees(-42.0F));
            pose.mulPose(Axis.ZP.rotationDegrees(mirrored ? 20.0F : -20.0F));
            pose.scale(0.33F, -0.33F, -0.33F);
            sword.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(SuitItemRenderContext.textureOr(TEXTURE))), light, overlay);
        }
        finally
        {
            pose.popPose();
        }
    }

    private static ModelPart createModel()
    {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition meshRoot = mesh.getRoot();
        PartDefinition p_sword = meshRoot.addOrReplaceChild("sword", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p_blade1 = p_sword.addOrReplaceChild("blade1", CubeListBuilder.create()
                .texOffs(6, -1).mirror(false).addBox(0.0F, -29.0F, -1.0F, 0.0F, 29.0F, 1.0F), PartPose.offset(0.0F, 0.0F, 0.3F));
        PartDefinition p_blade4 = p_blade1.addOrReplaceChild("blade4", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(0.0F, -1.0F, 0.0F, 0.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.01F, -29.0F, -1.0F, -0.65798914F, 0.0F, 0.0F));
        PartDefinition p_blade5 = p_blade4.addOrReplaceChild("blade5", CubeListBuilder.create()
                .texOffs(0, -1).mirror(false).addBox(0.0F, -1.0F, 0.0F, 0.0F, 1.0F, 1.0F), PartPose.offset(0.0F, -0.5F, 0.0F));
        p_blade5.addOrReplaceChild("blade6", CubeListBuilder.create()
                .texOffs(4, -1).mirror(false).addBox(0.0F, 0.0F, 0.0F, 0.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.01F, -1.0F, 0.0F, 0.65798914F, 0.0F, 0.0F));
        p_blade1.addOrReplaceChild("blade3", CubeListBuilder.create()
                .texOffs(8, 0).mirror(false).addBox(-0.25F, -29.5F, -0.08F, 1.0F, 30.0F, 1.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        p_blade1.addOrReplaceChild("blade2", CubeListBuilder.create()
                .texOffs(8, 0).mirror(false).addBox(-0.25F, -29.5F, -0.42F, 1.0F, 30.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, (float) Math.PI, 0.0F));
        PartDefinition p_hilt = p_sword.addOrReplaceChild("hilt", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-0.5F, 0.0F, -1.0F, 1.0F, 13.0F, 2.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        p_hilt.addOrReplaceChild("guard", CubeListBuilder.create()
                .texOffs(6, 0).mirror(false).addBox(-2.5F, 0.0F, -3.0F, 5.0F, 0.0F, 6.0F), PartPose.offset(0.0F, -0.01F, 0.0F));
        return LayerDefinition.create(mesh, 32, 32).bakeRoot();
    }
}
