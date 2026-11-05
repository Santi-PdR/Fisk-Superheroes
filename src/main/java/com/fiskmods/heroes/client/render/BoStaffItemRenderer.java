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

/** Original telescoping 3D Bo Staff mesh ported from ModelBoStaff & RenderItemBoStaff. */
public final class BoStaffItemRenderer extends BlockEntityWithoutLevelRenderer
{
    private static final ResourceLocation TEXTURE = FiskHeroes.id("textures/models/bo_staff.png");
    private static BoStaffItemRenderer instance;
    private final ModelPart shape1;
    private final ModelPart shape2;
    private final ModelPart shape3;

    private BoStaffItemRenderer()
    {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        ModelPart baked = createModel();
        this.shape1 = baked.getChild("shape1");
        this.shape2 = baked.getChild("shape2");
        this.shape3 = baked.getChild("shape3");
    }

    public static BoStaffItemRenderer getInstance()
    {
        if (instance == null) instance = new BoStaffItemRenderer();
        return instance;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
            MultiBufferSource buffers, int light, int overlay)
    {
        float extend = context == ItemDisplayContext.GUI ? 0.0F : 1.0F;
        pose.pushPose();
        switch (context)
        {
            case GUI ->
            {
                pose.translate(0.5D, 0.5D, 0.5D);
                pose.mulPose(Axis.YP.rotationDegrees(-45.0F));
                pose.mulPose(Axis.XP.rotationDegrees(-40.0F));
                pose.mulPose(Axis.YP.rotationDegrees(-25.0F));
                pose.scale(1.6F, -1.6F, -1.6F);
            }
            case GROUND, FIXED ->
            {
                pose.translate(0.5D, 0.5D, 0.5D);
                pose.mulPose(Axis.ZP.rotationDegrees(45.0F));
                pose.scale(0.85F, -0.85F, -0.85F);
            }
            case FIRST_PERSON_RIGHT_HAND, FIRST_PERSON_LEFT_HAND ->
            {
                pose.translate(context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND ? 0.68D : 0.32D, 0.52D, 0.45D);
                pose.mulPose(Axis.YP.rotationDegrees(context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND ? 70.0F : -70.0F));
                pose.mulPose(Axis.XP.rotationDegrees(-25.0F));
                pose.scale(1.15F, -1.15F, -1.15F);
            }
            case THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND ->
            {
                pose.translate(0.5D, 0.55D, 0.5D);
                pose.mulPose(Axis.XP.rotationDegrees(15.0F));
                pose.scale(1.35F, -1.35F, -1.35F);
            }
            default ->
            {
                pose.translate(0.5D, 0.5D, 0.5D);
                pose.scale(1.0F, -1.0F, -1.0F);
            }
        }
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        shape2.y = Mth.lerp(extend, 0.5F, 8.0F);
        shape3.y = Mth.lerp(extend, 1.0F, 16.0F);

        renderScaledPart(pose, consumer, light, overlay, shape2, 0.85F, 1.0F, 0.85F);
        renderScaledPart(pose, consumer, light, overlay, shape3, 0.7F, 1.0F, 0.7F);
        shape1.render(pose, consumer, light, overlay);
        pose.popPose();
    }

    private static void renderScaledPart(PoseStack pose, VertexConsumer consumer, int light, int overlay,
            ModelPart part, float sx, float sy, float sz)
    {
        pose.pushPose();
        pose.translate(part.x * 0.0625F, part.y * 0.0625F, part.z * 0.0625F);
        pose.scale(sx, sy, sz);
        pose.translate(-part.x * 0.0625F, -part.y * 0.0625F, -part.z * 0.0625F);
        part.render(pose, consumer, light, overlay);
        pose.popPose();
    }

    private static ModelPart createModel()
    {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition meshRoot = mesh.getRoot();
        meshRoot.addOrReplaceChild("shape1", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 8.0F, 1.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        meshRoot.addOrReplaceChild("shape2", CubeListBuilder.create()
                .texOffs(4, 0).mirror(false).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 8.0F, 1.0F), PartPose.offset(0.0F, 8.0F, 0.0F));
        meshRoot.addOrReplaceChild("shape3", CubeListBuilder.create()
                .texOffs(8, 0).mirror(false).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 8.0F, 1.0F), PartPose.offset(0.0F, 16.0F, 0.0F));
        return LayerDefinition.create(mesh, 16, 16).bakeRoot();
    }
}
