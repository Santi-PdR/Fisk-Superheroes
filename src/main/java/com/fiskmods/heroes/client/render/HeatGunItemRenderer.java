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

/** Original 3D Heat Gun mesh + glowing lights pass ported from ModelHeatGun & RenderItemHeatGun. */
public final class HeatGunItemRenderer extends BlockEntityWithoutLevelRenderer
{
    private static final ResourceLocation TEXTURE = FiskHeroes.id("textures/models/heat_gun.png");
    private static final ResourceLocation TEXTURE_LIGHTS = FiskHeroes.id("textures/models/heat_gun_lights.png");
    private static HeatGunItemRenderer instance;
    private final ModelPart gun1;
    private final ModelPart canister1;
    private final ModelPart wire1_1;
    private final ModelPart wire2_1;
    private final ModelPart wire3_1;

    private HeatGunItemRenderer()
    {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        ModelPart baked = createModel();
        this.gun1 = baked.getChild("gun1");
        this.canister1 = baked.getChild("canister1");
        this.wire1_1 = baked.getChild("wire1_1");
        this.wire2_1 = baked.getChild("wire2_1");
        this.wire3_1 = baked.getChild("wire3_1");
    }

    public static HeatGunItemRenderer getInstance()
    {
        if (instance == null) instance = new HeatGunItemRenderer();
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
                pose.translate(0.52D, 0.45D, 0.5D);
                pose.mulPose(Axis.YP.rotationDegrees(45.0F));
                pose.mulPose(Axis.XP.rotationDegrees(-25.0F));
                pose.scale(1.15F, -1.15F, -1.15F);
            }
            case GROUND, FIXED ->
            {
                pose.translate(0.5D, 0.5D, 0.5D);
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
                pose.scale(0.85F, -0.85F, -0.85F);
            }
            case FIRST_PERSON_RIGHT_HAND, FIRST_PERSON_LEFT_HAND ->
            {
                pose.translate(context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND ? 0.68D : 0.32D, 0.48D, 0.45D);
                pose.mulPose(Axis.YP.rotationDegrees(context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND ? 180.0F : 0.0F));
                pose.scale(0.95F, -0.95F, -0.95F);
            }
            case THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND ->
            {
                pose.translate(0.5D, 0.5D, 0.48D);
                pose.mulPose(Axis.YP.rotationDegrees(180.0F));
                pose.scale(0.9F, -0.9F, -0.9F);
            }
            default ->
            {
                pose.translate(0.5D, 0.5D, 0.5D);
                pose.scale(0.85F, -0.85F, -0.85F);
            }
        }
        renderPass(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light, overlay);
        renderPass(pose, buffers.getBuffer(RenderType.eyes(TEXTURE_LIGHTS)), LightTexture.FULL_BRIGHT, overlay);
        pose.popPose();
    }

    private void renderPass(PoseStack pose, VertexConsumer consumer, int light, int overlay)
    {
        renderScaled(pose, consumer, light, overlay, wire1_1, 0.5F);
        renderScaled(pose, consumer, light, overlay, canister1, 0.7F);
        renderScaled(pose, consumer, light, overlay, wire2_1, 0.5F);
        gun1.render(pose, consumer, light, overlay);
        renderScaled(pose, consumer, light, overlay, wire3_1, 0.5F);
    }

    private static void renderScaled(PoseStack pose, VertexConsumer consumer, int light, int overlay, ModelPart part, float scale)
    {
        pose.pushPose();
        pose.translate(part.x * 0.0625F, part.y * 0.0625F, part.z * 0.0625F);
        pose.scale(scale, scale, scale);
        pose.translate(-part.x * 0.0625F, -part.y * 0.0625F, -part.z * 0.0625F);
        part.render(pose, consumer, light, overlay);
        pose.popPose();
    }

    private static ModelPart createModel()
    {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition meshRoot = mesh.getRoot();
        PartDefinition p_wire1_1 = meshRoot.addOrReplaceChild("wire1_1", CubeListBuilder.create()
                .texOffs(0, 8).mirror(false).addBox(-0.5F, -0.5F, -3.0F, 1.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(-3.0F, -2.0F, -3.7F, 0.08726646F, -0.12217305F, 0.0F));
        p_wire1_1.addOrReplaceChild("wire1_2", CubeListBuilder.create()
                .texOffs(5, 5).mirror(false).addBox(0.0F, -0.5F, -3.0F, 1.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(-0.5F, 0.0F, -3.0F, 0.0F, -0.5235988F, 0.0F));
        PartDefinition p_canister1 = meshRoot.addOrReplaceChild("canister1", CubeListBuilder.create()
                .texOffs(25, 14).mirror(false).addBox(-0.5F, -0.5F, -8.0F, 1.0F, 1.0F, 15.0F), PartPose.offset(0.0F, -5.1F, -0.2F));
        p_canister1.addOrReplaceChild("canister2", CubeListBuilder.create()
                .texOffs(0, 18).mirror(false).addBox(-1.0F, -1.0F, -2.5F, 2.0F, 2.0F, 4.0F), PartPose.offset(0.0F, 0.0F, -4.4F));
        p_canister1.addOrReplaceChild("canister3", CubeListBuilder.create()
                .texOffs(0, 18).mirror(false).addBox(-1.0F, -1.0F, -2.5F, 2.0F, 2.0F, 4.0F), PartPose.offset(0.0F, 0.0F, 0.3F));
        p_canister1.addOrReplaceChild("canister4", CubeListBuilder.create()
                .texOffs(0, 18).mirror(false).addBox(-1.0F, -1.0F, -2.5F, 2.0F, 2.0F, 4.0F), PartPose.offset(0.0F, 0.0F, 5.0F));
        PartDefinition p_wire2_1 = meshRoot.addOrReplaceChild("wire2_1", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 4.0F), PartPose.offsetAndRotation(0.0F, -5.1F, 4.6F, 0.0F, 0.13962634F, 0.0F));
        PartDefinition p_wire2_2 = p_wire2_1.addOrReplaceChild("wire2_2", CubeListBuilder.create()
                .texOffs(6, 0).mirror(false).addBox(0.0F, -0.5F, 0.0F, 1.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(-0.5F, 0.0F, 4.0F, 0.0F, 0.5934119F, 0.0F));
        PartDefinition p_wire2_3 = p_wire2_2.addOrReplaceChild("wire2_3", CubeListBuilder.create()
                .texOffs(11, 0).mirror(false).addBox(0.0F, -0.5F, 0.0F, 1.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 3.0F, 0.0F, 0.7853982F, 0.0F));
        PartDefinition p_wire2_4 = p_wire2_3.addOrReplaceChild("wire2_4", CubeListBuilder.create()
                .texOffs(11, 0).mirror(false).addBox(-0.5F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.5F, -0.5F, 1.0F, -0.34906584F, 0.0F, 0.0F));
        PartDefinition p_wire2_5 = p_wire2_4.addOrReplaceChild("wire2_5", CubeListBuilder.create()
                .texOffs(11, 0).mirror(false).addBox(-0.5F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 1.0F, -0.34906584F, 0.0F, 0.0F));
        PartDefinition p_wire2_6 = p_wire2_5.addOrReplaceChild("wire2_6", CubeListBuilder.create()
                .texOffs(11, 0).mirror(false).addBox(-0.5F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 1.0F, -0.34906584F, 0.0F, 0.0F));
        PartDefinition p_wire2_7 = p_wire2_6.addOrReplaceChild("wire2_7", CubeListBuilder.create()
                .texOffs(11, 0).mirror(false).addBox(-0.5F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 1.0F, -0.34906584F, 0.0F, 0.0F));
        PartDefinition p_wire2_8 = p_wire2_7.addOrReplaceChild("wire2_8", CubeListBuilder.create()
                .texOffs(0, 5).mirror(false).addBox(-0.5F, 0.0F, 0.0F, 1.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 1.0F, -0.2617994F, 0.0F, 0.0F));
        PartDefinition p_wire2_9 = p_wire2_8.addOrReplaceChild("wire2_9", CubeListBuilder.create()
                .texOffs(11, 0).mirror(false).addBox(-0.5F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 2.0F, -0.43633232F, 0.0F, 0.0F));
        PartDefinition p_wire2_10 = p_wire2_9.addOrReplaceChild("wire2_10", CubeListBuilder.create()
                .texOffs(11, 0).mirror(false).addBox(-0.5F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 1.0F, -0.43633232F, 0.0F, 0.0F));
        p_wire2_10.addOrReplaceChild("wire2_11", CubeListBuilder.create()
                .texOffs(11, 0).mirror(false).addBox(-0.5F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 1.0F, -0.43633232F, 0.0F, 0.0F));
        PartDefinition p_gun1 = meshRoot.addOrReplaceChild("gun1", CubeListBuilder.create()
                .texOffs(0, 32).mirror(false).addBox(-1.0F, -1.0F, -8.0F, 2.0F, 1.0F, 15.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        p_gun1.addOrReplaceChild("gun11", CubeListBuilder.create()
                .texOffs(19, 37).mirror(false).addBox(-0.5F, 0.0F, -1.0F, 1.0F, 2.0F, 3.0F), PartPose.offset(0.0F, -0.8F, -1.0F));
        PartDefinition p_gun5 = p_gun1.addOrReplaceChild("gun5", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-1.5F, -2.0F, -8.0F, 3.0F, 2.0F, 16.0F), PartPose.offset(0.0F, -1.0F, 0.0F));
        p_gun5.addOrReplaceChild("gun9", CubeListBuilder.create()
                .texOffs(16, 23).mirror(false).addBox(0.0F, -0.5F, -0.5F, 1.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(-1.5F, -0.7F, -7.7F, 0.05235988F, -0.6806784F, 0.0F));
        p_gun5.addOrReplaceChild("gun8", CubeListBuilder.create()
                .texOffs(19, 30).mirror(true).addBox(-1.0F, -1.0F, -6.2F, 1.0F, 1.0F, 12.0F), PartPose.offsetAndRotation(1.5F, -2.0F, 0.0F, 0.0F, 0.0F, -0.52534413F));
        p_gun5.addOrReplaceChild("gun10", CubeListBuilder.create()
                .texOffs(0, 24).mirror(false).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 4.0F), PartPose.offset(0.0F, -1.86F, 4.6F));
        p_gun5.addOrReplaceChild("gun7", CubeListBuilder.create()
                .texOffs(19, 30).mirror(false).addBox(0.0F, -1.0F, -6.2F, 1.0F, 1.0F, 12.0F), PartPose.offsetAndRotation(-1.5F, -2.0F, 0.0F, 0.0F, 0.0F, 0.52534413F));
        p_gun5.addOrReplaceChild("gun6", CubeListBuilder.create()
                .texOffs(0, 18).mirror(false).addBox(-1.0F, -2.0F, -6.2F, 2.0F, 2.0F, 12.0F), PartPose.offset(0.0F, -1.4F, 0.0F));
        PartDefinition p_gun3 = p_gun1.addOrReplaceChild("gun3", CubeListBuilder.create()
                .texOffs(19, 32).mirror(false).addBox(-1.0F, 0.0F, -1.5F, 2.0F, 2.0F, 3.0F), PartPose.offset(0.0F, -0.1F, -3.5F));
        p_gun3.addOrReplaceChild("gun4", CubeListBuilder.create()
                .texOffs(24, 37).mirror(false).addBox(-1.0F, -2.0F, 0.0F, 2.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 2.0F, -1.5F, 0.2617994F, 0.0F, 0.0F));
        PartDefinition p_gun12 = p_gun1.addOrReplaceChild("gun12", CubeListBuilder.create()
                .texOffs(0, 32).mirror(false).addBox(-1.0F, -3.0F, 0.0F, 2.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, -0.29F, -8.95F, -0.29670596F, 0.0F, 0.0F));
        PartDefinition p_gun16 = p_gun12.addOrReplaceChild("gun16", CubeListBuilder.create()
                .texOffs(8, 35).mirror(true).addBox(-0.5F, -2.0F, -0.5F, 1.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(1.2F, 0.0F, 0.8F, 0.12217305F, 0.0F, 0.0F));
        p_gun16.addOrReplaceChild("gun17", CubeListBuilder.create()
                .texOffs(10, 4).mirror(true).addBox(-1.0F, -0.5F, -2.0F, 1.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(0.6F, -0.3F, 0.0F, -0.2617994F, 0.17453292F, 0.0F));
        p_gun12.addOrReplaceChild("gun13", CubeListBuilder.create()
                .texOffs(6, 32).mirror(false).addBox(-1.0F, -2.0F, 0.0F, 2.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.0F, -3.0F, 0.0F, -0.9075712F, 0.0F, 0.0F));
        PartDefinition p_gun14 = p_gun12.addOrReplaceChild("gun14", CubeListBuilder.create()
                .texOffs(8, 35).mirror(false).addBox(-0.5F, -2.0F, -0.5F, 1.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(-1.2F, 0.0F, 0.8F, 0.12217305F, 0.0F, 0.0F));
        p_gun14.addOrReplaceChild("gun15", CubeListBuilder.create()
                .texOffs(10, 4).mirror(false).addBox(0.0F, -0.5F, -2.0F, 1.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(-0.6F, -0.3F, 0.0F, -0.2617994F, -0.17453292F, 0.0F));
        p_gun12.addOrReplaceChild("gun18", CubeListBuilder.create()
                .texOffs(0, 36).mirror(false).addBox(-1.5F, -0.5F, -0.5F, 3.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(0.0F, -0.1F, 0.3F, 0.43633232F, 0.0F, 0.0F));
        p_gun1.addOrReplaceChild("gun2", CubeListBuilder.create()
                .texOffs(30, 18).mirror(false).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 5.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.34906584F, 0.0F, 0.0F));
        PartDefinition p_gun19 = p_gun1.addOrReplaceChild("gun19", CubeListBuilder.create()
                .texOffs(16, 18).mirror(false).addBox(-3.0F, -1.5F, -4.0F, 3.0F, 3.0F, 8.0F), PartPose.offset(-1.5F, -2.0F, 1.8F));
        PartDefinition p_gun20 = p_gun19.addOrReplaceChild("gun20", CubeListBuilder.create()
                .texOffs(16, 18).mirror(false).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 1.0F), PartPose.offset(-1.5F, 0.0F, -3.6F));
        p_gun20.addOrReplaceChild("gun21", CubeListBuilder.create()
                .texOffs(16, 21).mirror(false).addBox(-0.5F, -0.5F, -1.0F, 1.0F, 1.0F, 1.0F), PartPose.offset(0.0F, 0.0F, -1.0F));
        PartDefinition p_wire3_1 = meshRoot.addOrReplaceChild("wire3_1", CubeListBuilder.create()
                .texOffs(11, 0).mirror(false).addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(-3.0F, -2.0F, 5.8F, 0.0F, 0.0F, -0.05235988F));
        PartDefinition p_wire3_2 = p_wire3_1.addOrReplaceChild("wire3_2", CubeListBuilder.create()
                .texOffs(11, 0).mirror(false).addBox(0.0F, -0.5F, 0.0F, 1.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(-0.5F, 0.0F, 1.0F, 0.0F, 0.36651915F, 0.0F));
        PartDefinition p_wire3_3 = p_wire3_2.addOrReplaceChild("wire3_3", CubeListBuilder.create()
                .texOffs(0, 5).mirror(false).addBox(0.0F, -0.5F, 0.0F, 1.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 1.0F, 0.0F, 0.36651915F, 0.0F));
        PartDefinition p_wire3_4 = p_wire3_3.addOrReplaceChild("wire3_4", CubeListBuilder.create()
                .texOffs(11, 0).mirror(false).addBox(0.0F, -0.5F, 0.0F, 1.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 2.0F, 0.0F, 0.4537856F, 0.0F));
        p_wire3_4.addOrReplaceChild("wire3_5", CubeListBuilder.create()
                .texOffs(11, 0).mirror(false).addBox(0.0F, -0.5F, 0.0F, 1.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 1.0F, 0.0F, 0.36651915F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64).bakeRoot();
    }
}
