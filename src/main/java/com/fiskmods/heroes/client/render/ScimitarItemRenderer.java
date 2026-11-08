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

/** Original 3D Scimitar mesh ported from ModelScimitar & RenderItemScimitar. */
public final class ScimitarItemRenderer extends BlockEntityWithoutLevelRenderer
{
    private static final ResourceLocation TEXTURE = FiskHeroes.id("textures/models/scimitar.png");
    private static ScimitarItemRenderer instance;
    private final ModelPart root;

    private ScimitarItemRenderer()
    {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        this.root = createModel().getChild("root");
    }

    public static ScimitarItemRenderer getInstance()
    {
        if (instance == null) instance = new ScimitarItemRenderer();
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
                pose.translate(0.46D, 0.32D, 0.5D);
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
                pose.mulPose(Axis.XP.rotationDegrees(-42.0F));
                pose.mulPose(Axis.ZP.rotationDegrees(-20.0F));
                pose.scale(0.95F, -0.95F, -0.95F);
            }
            case GROUND, FIXED ->
            {
                pose.translate(0.5D, 0.5D, 0.5D);
                pose.mulPose(Axis.ZP.rotationDegrees(90.0F));
                pose.scale(0.85F, -0.85F, -0.85F);
            }
            case FIRST_PERSON_RIGHT_HAND, FIRST_PERSON_LEFT_HAND ->
            {
                pose.translate(context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND ? 0.68D : 0.32D, 0.52D, 0.42D);
                pose.mulPose(Axis.YP.rotationDegrees(context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND ? 80.0F : -80.0F));
                pose.mulPose(Axis.XP.rotationDegrees(150.0F));
                pose.scale(0.7F, -0.7F, -0.7F);
            }
            case THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND ->
            {
                pose.translate(context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND ? 0.68D : 0.32D, 0.52D, 0.48D);
                pose.mulPose(Axis.YP.rotationDegrees(context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND ? 85.0F : -85.0F));
                pose.mulPose(Axis.XP.rotationDegrees(150.0F));
                pose.scale(0.75F, -0.75F, -0.75F);
            }
            default ->
            {
                pose.translate(0.5D, 0.5D, 0.5D);
                pose.scale(0.85F, -0.85F, -0.85F);
            }
        }
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        root.render(pose, consumer, light, overlay);
        if (context != ItemDisplayContext.NONE && ItemDualSword.isDual(stack))
        {
            pose.translate(0.0D, 0.0D, 0.28D);
            root.render(pose, consumer, light, overlay);
        }
        pose.popPose();
    }

    private static ModelPart createModel()
    {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition meshRoot = mesh.getRoot();
        PartDefinition p_root = meshRoot.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.5F, 0.0F, -0.75F));
        PartDefinition p_sword = p_root.addOrReplaceChild("sword", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p_blade3 = p_sword.addOrReplaceChild("blade3", CubeListBuilder.create(), PartPose.offset(0.0F, 23.0F, 0.0F));
        PartDefinition p_bone2 = p_blade3.addOrReplaceChild("bone2", CubeListBuilder.create(), PartPose.offset(-0.5F, -24.0F, 1.0F));
        PartDefinition p_bone3 = p_bone2.addOrReplaceChild("bone3", CubeListBuilder.create()
                .texOffs(0, 24).mirror(false).addBox(0.0F, 20.5754F, -0.0467F, 0.0F, 1.0F, 1.0F), PartPose.offset(0.0F, -20.5754F, -0.9533F));
        PartDefinition p_bone13 = p_bone3.addOrReplaceChild("bone13", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 20.5754F, 0.9533F, 0.0436F, 0.0F, 0.0F));
        p_bone13.addOrReplaceChild("cube_r1", CubeListBuilder.create()
                .texOffs(0, 19).mirror(false).addBox(0.0F, -4.0F, -1.0F, 0.0F, 4.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0436F, 0.0F, 0.0F));
        PartDefinition p_bone4 = p_bone13.addOrReplaceChild("bone4", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, -3.909F, -1.1707F, -0.0873F, 0.0F, 0.0F));
        p_bone4.addOrReplaceChild("cube_r2", CubeListBuilder.create()
                .texOffs(0, 16).mirror(false).addBox(0.0F, -3.0F, 0.0F, 0.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0873F, 0.0F, 0.0F));
        PartDefinition p_bone5 = p_bone4.addOrReplaceChild("bone5", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, -3.0406F, 0.7348F, -0.1309F, 0.0F, 0.0F));
        p_bone5.addOrReplaceChild("cube_r3", CubeListBuilder.create()
                .texOffs(0, 12).mirror(false).addBox(0.0F, -3.0F, -1.0F, 0.0F, 3.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.1745F, 0.0F, 0.0F));
        PartDefinition p_bone6 = p_bone5.addOrReplaceChild("bone6", CubeListBuilder.create(), PartPose.offset(0.0F, -2.7808F, -1.5058F));
        p_bone6.addOrReplaceChild("cube_r4", CubeListBuilder.create()
                .texOffs(0, 10).mirror(false).addBox(0.0F, -2.0F, 0.0F, 0.0F, 2.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.1309F, 0.0F, 0.0F));
        PartDefinition p_bone7 = p_bone6.addOrReplaceChild("bone7", CubeListBuilder.create(), PartPose.offset(0.0F, -1.9829F, -0.2611F));
        p_bone7.addOrReplaceChild("cube_r5", CubeListBuilder.create()
                .texOffs(0, 7).mirror(false).addBox(0.0F, -2.0F, 0.0F, 0.0F, 2.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0436F, 0.0F, 0.0F));
        PartDefinition p_bone8 = p_bone7.addOrReplaceChild("bone8", CubeListBuilder.create(), PartPose.offset(0.0F, -1.9981F, -0.0872F));
        p_bone8.addOrReplaceChild("cube_r6", CubeListBuilder.create()
                .texOffs(0, 5).mirror(false).addBox(0.0F, -2.0F, 0.0F, 0.0F, 2.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.0436F, 0.0F, 0.0F));
        PartDefinition p_bone9 = p_bone8.addOrReplaceChild("bone9", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, -1.9981F, 0.0872F, 0.0611F, 0.0F, 0.0F));
        p_bone9.addOrReplaceChild("cube_r7", CubeListBuilder.create()
                .texOffs(0, 3).mirror(false).addBox(0.0F, -2.0F, 0.0F, 0.0F, 2.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.1745F, 0.0F, 0.0F));
        PartDefinition p_bone10 = p_bone9.addOrReplaceChild("bone10", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, -1.9696F, 0.3473F, -0.0436F, 0.0F, 0.0F));
        p_bone10.addOrReplaceChild("cube_r8", CubeListBuilder.create()
                .texOffs(0, 1).mirror(false).addBox(0.0F, -3.0F, 0.0F, 0.0F, 3.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.3054F, 0.0F, 0.0F));
        PartDefinition p_bone11 = p_bone10.addOrReplaceChild("bone11", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, -2.8612F, 0.9021F, -0.0436F, 0.0F, 0.0F));
        p_bone11.addOrReplaceChild("cube_r9", CubeListBuilder.create()
                .texOffs(0, -2).mirror(false).addBox(0.0F, -4.0F, 0.0F, 0.0F, 4.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.4363F, 0.0F, 0.0F));
        PartDefinition p_D5 = p_bone2.addOrReplaceChild("D5", CubeListBuilder.create()
                .texOffs(2, 25).mirror(false).addBox(-0.7578F, -5.0F, 0.0F, 1.0F, 6.0F, 1.0F), PartPose.offset(0.0F, 0.0F, -0.082F));
        PartDefinition p_bone20 = p_D5.addOrReplaceChild("bone20", CubeListBuilder.create(), PartPose.offset(-0.7578F, -5.0F, 0.0F));
        p_bone20.addOrReplaceChild("cube_r10", CubeListBuilder.create()
                .texOffs(6, 22).mirror(false).addBox(0.0F, -3.0F, 0.0F, 1.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.0436F, 0.0F, 0.0F));
        PartDefinition p_bone19 = p_bone20.addOrReplaceChild("bone19", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, -2.9971F, 0.1309F, -0.0873F, 0.0F, 0.0F));
        p_bone19.addOrReplaceChild("cube_r11", CubeListBuilder.create()
                .texOffs(6, 18).mirror(false).addBox(0.0F, -3.0F, 0.0F, 1.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.1309F, 0.0F, 0.0F));
        PartDefinition p_bone18 = p_bone19.addOrReplaceChild("bone18", CubeListBuilder.create(), PartPose.offset(0.0F, -2.9743F, 0.3916F));
        p_bone18.addOrReplaceChild("cube_r12", CubeListBuilder.create()
                .texOffs(8, 15).mirror(false).addBox(0.0F, -2.0F, 0.0F, 1.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.3054F, 0.0F, 0.0F));
        PartDefinition p_bone17 = p_bone18.addOrReplaceChild("bone17", CubeListBuilder.create(), PartPose.offset(-0.0039F, -1.4363F, 0.453F));
        p_bone17.addOrReplaceChild("cube_r13", CubeListBuilder.create()
                .texOffs(8, 12).mirror(false).addBox(0.0F, -2.0F, 0.0F, 1.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.48F, 0.0F, 0.0F));
        PartDefinition p_bone = p_bone17.addOrReplaceChild("bone", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, -1.3123F, 1.8105F, -0.0873F, 0.0F, 0.0F));
        p_bone.addOrReplaceChild("cube_r14", CubeListBuilder.create()
                .texOffs(8, 9).mirror(false).addBox(0.0039F, -2.0F, -1.0F, 1.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.5236F, 0.0F, 0.0F));
        PartDefinition p_bone12 = p_bone.addOrReplaceChild("bone12", CubeListBuilder.create(), PartPose.offsetAndRotation(-0.001F, -1.2321F, -1.866F, -0.0436F, 0.0F, 0.0F));
        p_bone12.addOrReplaceChild("cube_r15", CubeListBuilder.create()
                .texOffs(8, 6).mirror(false).addBox(0.001F, -2.0F, 0.0F, 1.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.3054F, 0.0F, 0.0F));
        PartDefinition p_bone14 = p_bone12.addOrReplaceChild("bone14", CubeListBuilder.create(), PartPose.offsetAndRotation(0.75F, -1.9074F, -0.6014F, -0.0436F, 0.0F, 0.0F));
        p_bone14.addOrReplaceChild("cube_r16", CubeListBuilder.create()
                .texOffs(7, 3).mirror(false).addBox(0.001F, -2.0F, 0.0F, 0.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.1309F, 0.0F, 0.0F));
        PartDefinition p_bone15 = p_bone14.addOrReplaceChild("bone15", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, -1.9829F, -0.2611F, 0.1309F, 0.0F, 0.0F));
        p_bone15.addOrReplaceChild("cube_r17", CubeListBuilder.create()
                .texOffs(7, 1).mirror(false).addBox(0.001F, -2.0F, 0.0F, 0.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.1309F, 0.0F, 0.0F));
        PartDefinition p_bone16 = p_bone15.addOrReplaceChild("bone16", CubeListBuilder.create(), PartPose.offset(0.0F, -1.9829F, 0.2611F));
        p_bone16.addOrReplaceChild("cube_r18", CubeListBuilder.create()
                .texOffs(7, -1).mirror(false).addBox(0.001F, -2.0F, 0.0F, 0.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.3491F, 0.0F, 0.0F));
        PartDefinition p_bone21 = p_blade3.addOrReplaceChild("bone21", CubeListBuilder.create(), PartPose.offset(-0.5195F, -24.0F, 1.0F));
        PartDefinition p_bone22 = p_bone21.addOrReplaceChild("bone22", CubeListBuilder.create()
                .texOffs(0, 24).mirror(true).addBox(0.0F, 20.5754F, -0.0467F, 0.0F, 1.0F, 1.0F), PartPose.offset(0.0F, -20.5754F, -0.9533F));
        PartDefinition p_bone23 = p_bone22.addOrReplaceChild("bone23", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 20.5754F, 0.9533F, 0.0436F, 0.0F, 0.0F));
        p_bone23.addOrReplaceChild("cube_r19", CubeListBuilder.create()
                .texOffs(0, 19).mirror(true).addBox(0.0F, -4.0F, -1.0F, 0.0F, 4.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0436F, 0.0F, 0.0F));
        PartDefinition p_bone24 = p_bone23.addOrReplaceChild("bone24", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, -3.909F, -1.1707F, -0.0873F, 0.0F, 0.0F));
        p_bone24.addOrReplaceChild("cube_r20", CubeListBuilder.create()
                .texOffs(0, 16).mirror(true).addBox(0.0F, -3.0F, 0.0F, 0.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0873F, 0.0F, 0.0F));
        PartDefinition p_bone25 = p_bone24.addOrReplaceChild("bone25", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, -3.0406F, 0.7348F, -0.1309F, 0.0F, 0.0F));
        p_bone25.addOrReplaceChild("cube_r21", CubeListBuilder.create()
                .texOffs(0, 12).mirror(true).addBox(0.0F, -3.0F, -1.0F, 0.0F, 3.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.1745F, 0.0F, 0.0F));
        PartDefinition p_bone26 = p_bone25.addOrReplaceChild("bone26", CubeListBuilder.create(), PartPose.offset(0.0F, -2.7808F, -1.5058F));
        p_bone26.addOrReplaceChild("cube_r22", CubeListBuilder.create()
                .texOffs(0, 10).mirror(true).addBox(0.0F, -2.0F, 0.0F, 0.0F, 2.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.1309F, 0.0F, 0.0F));
        PartDefinition p_bone27 = p_bone26.addOrReplaceChild("bone27", CubeListBuilder.create(), PartPose.offset(0.0F, -1.9829F, -0.2611F));
        p_bone27.addOrReplaceChild("cube_r23", CubeListBuilder.create()
                .texOffs(0, 7).mirror(true).addBox(0.0F, -2.0F, 0.0F, 0.0F, 2.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0436F, 0.0F, 0.0F));
        PartDefinition p_bone28 = p_bone27.addOrReplaceChild("bone28", CubeListBuilder.create(), PartPose.offset(0.0F, -1.9981F, -0.0872F));
        p_bone28.addOrReplaceChild("cube_r24", CubeListBuilder.create()
                .texOffs(0, 5).mirror(true).addBox(0.0F, -2.0F, 0.0F, 0.0F, 2.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.0436F, 0.0F, 0.0F));
        PartDefinition p_bone29 = p_bone28.addOrReplaceChild("bone29", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, -1.9981F, 0.0872F, 0.0611F, 0.0F, 0.0F));
        p_bone29.addOrReplaceChild("cube_r25", CubeListBuilder.create()
                .texOffs(0, 3).mirror(true).addBox(0.0F, -2.0F, 0.0F, 0.0F, 2.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.1745F, 0.0F, 0.0F));
        PartDefinition p_bone30 = p_bone29.addOrReplaceChild("bone30", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, -1.9696F, 0.3473F, -0.0436F, 0.0F, 0.0F));
        p_bone30.addOrReplaceChild("cube_r26", CubeListBuilder.create()
                .texOffs(0, 1).mirror(true).addBox(0.0F, -3.0F, 0.0F, 0.0F, 3.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.3054F, 0.0F, 0.0F));
        PartDefinition p_bone31 = p_bone30.addOrReplaceChild("bone31", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, -2.8612F, 0.9021F, -0.0436F, 0.0F, 0.0F));
        p_bone31.addOrReplaceChild("cube_r27", CubeListBuilder.create()
                .texOffs(0, -2).mirror(true).addBox(0.0F, -4.0F, 0.0F, 0.0F, 4.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.4363F, 0.0F, 0.0F));
        PartDefinition p_D2 = p_bone21.addOrReplaceChild("D2", CubeListBuilder.create()
                .texOffs(2, 25).mirror(true).addBox(-0.2422F, -5.0F, 0.0F, 1.0F, 6.0F, 1.0F), PartPose.offset(0.0F, 0.0F, -0.082F));
        PartDefinition p_bone32 = p_D2.addOrReplaceChild("bone32", CubeListBuilder.create(), PartPose.offset(0.7578F, -5.0F, 0.0F));
        p_bone32.addOrReplaceChild("cube_r28", CubeListBuilder.create()
                .texOffs(6, 22).mirror(true).addBox(-1.0F, -3.0F, 0.0F, 1.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.0436F, 0.0F, 0.0F));
        PartDefinition p_bone33 = p_bone32.addOrReplaceChild("bone33", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, -2.9971F, 0.1309F, -0.0873F, 0.0F, 0.0F));
        p_bone33.addOrReplaceChild("cube_r29", CubeListBuilder.create()
                .texOffs(6, 18).mirror(true).addBox(-1.0F, -3.0F, 0.0F, 1.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.1309F, 0.0F, 0.0F));
        PartDefinition p_bone34 = p_bone33.addOrReplaceChild("bone34", CubeListBuilder.create(), PartPose.offset(0.0F, -2.9743F, 0.3916F));
        p_bone34.addOrReplaceChild("cube_r30", CubeListBuilder.create()
                .texOffs(8, 15).mirror(true).addBox(-1.0F, -2.0F, 0.0F, 1.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.3054F, 0.0F, 0.0F));
        PartDefinition p_bone35 = p_bone34.addOrReplaceChild("bone35", CubeListBuilder.create(), PartPose.offset(0.0039F, -1.4363F, 0.453F));
        p_bone35.addOrReplaceChild("cube_r31", CubeListBuilder.create()
                .texOffs(8, 12).mirror(true).addBox(-1.0F, -2.0F, 0.0F, 1.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.48F, 0.0F, 0.0F));
        PartDefinition p_bone36 = p_bone35.addOrReplaceChild("bone36", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, -1.3123F, 1.8105F, -0.0873F, 0.0F, 0.0F));
        p_bone36.addOrReplaceChild("cube_r32", CubeListBuilder.create()
                .texOffs(8, 9).mirror(true).addBox(-1.0039F, -2.0F, -1.0F, 1.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.5236F, 0.0F, 0.0F));
        PartDefinition p_bone37 = p_bone36.addOrReplaceChild("bone37", CubeListBuilder.create(), PartPose.offsetAndRotation(0.001F, -1.2321F, -1.866F, -0.0436F, 0.0F, 0.0F));
        p_bone37.addOrReplaceChild("cube_r33", CubeListBuilder.create()
                .texOffs(8, 6).mirror(true).addBox(-1.001F, -2.0F, 0.0F, 1.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.3054F, 0.0F, 0.0F));
        PartDefinition p_bone38 = p_bone37.addOrReplaceChild("bone38", CubeListBuilder.create(), PartPose.offsetAndRotation(-0.75F, -1.9074F, -0.6014F, -0.0436F, 0.0F, 0.0F));
        p_bone38.addOrReplaceChild("cube_r34", CubeListBuilder.create()
                .texOffs(7, 3).mirror(true).addBox(-0.001F, -2.0F, 0.0F, 0.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.1309F, 0.0F, 0.0F));
        PartDefinition p_bone39 = p_bone38.addOrReplaceChild("bone39", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, -1.9829F, -0.2611F, 0.1309F, 0.0F, 0.0F));
        p_bone39.addOrReplaceChild("cube_r35", CubeListBuilder.create()
                .texOffs(7, 1).mirror(true).addBox(-0.001F, -2.0F, 0.0F, 0.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.1309F, 0.0F, 0.0F));
        PartDefinition p_bone40 = p_bone39.addOrReplaceChild("bone40", CubeListBuilder.create(), PartPose.offset(0.0F, -1.9829F, 0.2611F));
        p_bone40.addOrReplaceChild("cube_r36", CubeListBuilder.create()
                .texOffs(7, -1).mirror(true).addBox(-0.001F, -2.0F, 0.0F, 0.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.3491F, 0.0F, 0.0F));
        PartDefinition p_hilt = p_sword.addOrReplaceChild("hilt", CubeListBuilder.create()
                .texOffs(22, 0).mirror(false).addBox(-1.0F, 0.0F, 0.0F, 1.0F, 3.0F, 2.0F)
                .texOffs(22, 0).mirror(false).addBox(-1.0039F, 3.0F, 0.0F, 1.0F, 3.0F, 2.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        p_hilt.addOrReplaceChild("hilt_r1", CubeListBuilder.create()
                .texOffs(22, 0).mirror(false).addBox(-0.5039F, 0.0F, -2.0F, 1.0F, 4.0F, 2.0F), PartPose.offsetAndRotation(-0.5039F, 6.0F, 2.0F, -0.0873F, 0.0F, 0.0F));
        p_hilt.addOrReplaceChild("shape2", CubeListBuilder.create()
                .texOffs(6, 10).mirror(false).addBox(-4.0F, 0.0F, 0.0F, 5.0F, 0.0F, 6.0F), PartPose.offset(1.0F, -0.01F, -2.0F));
        return LayerDefinition.create(mesh, 64, 32).bakeRoot();
    }
}
