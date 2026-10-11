package com.fiskmods.heroes.client.render;

import com.fiskmods.heroes.FiskHeroes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import com.fiskmods.heroes.common.item.ItemDualSword;

/** Renders the original UV-mapped Desert Eagle mesh from the 2.4.0 model. */
public final class DesertEagleItemRenderer extends BlockEntityWithoutLevelRenderer
{
    private static final ResourceLocation TEXTURE = FiskHeroes.id("textures/models/desert_eagle.png");
    private static DesertEagleItemRenderer instance;
    private final ModelPart root;

    private DesertEagleItemRenderer()
    {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        this.root = createModel();
    }

    public static DesertEagleItemRenderer getInstance()
    {
        if (instance == null) instance = new DesertEagleItemRenderer();
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
                pose.scale(2.0F, -2.0F, -2.0F);
            }
            case GROUND, FIXED ->
            {
                pose.translate(0.5D, 0.5D, 0.5D);
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
                pose.scale(1.5F, -1.5F, -1.5F);
            }
            case FIRST_PERSON_RIGHT_HAND, FIRST_PERSON_LEFT_HAND ->
            {
                pose.translate(context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND ? 0.68D : 0.32D, 0.52D, 0.42D);
                pose.mulPose(Axis.ZP.rotationDegrees(context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND ? 180.0F : 0.0F));
                pose.mulPose(Axis.YP.rotationDegrees(context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND ? 110.0F : -110.0F));
                pose.mulPose(Axis.XP.rotationDegrees(-10.0F));
                pose.scale(1.3F, -1.3F, -1.3F);
            }
            case THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND ->
            {
                pose.translate(context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND ? 0.7D : 0.3D, 0.48D, 0.55D);
                pose.mulPose(Axis.YP.rotationDegrees(context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND ? 90.0F : -90.0F));
                pose.scale(1.4F, -1.4F, -1.4F);
            }
            default ->
            {
                pose.translate(0.5D, 0.5D, 0.5D);
                pose.scale(1.0F, -1.0F, -1.0F);
            }
        }

        var consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        root.render(pose, consumer, light, overlay);
        if (ItemDualSword.isDual(stack))
        {
            pose.translate(-0.34D, 0.0D, 0.0D);
            root.render(pose, consumer, light, overlay);
        }
        pose.popPose();
    }

    private static ModelPart createModel()
    {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot().addOrReplaceChild("root", CubeListBuilder.create(),
                PartPose.offset(0.0F, -0.7F, 0.0F));
        PartDefinition s27 = part(root, "s27", 0, -0.25F, 2.0F, -0.7854F, 0, 0);
        box(s27, 13, 0, true, -0.9F, 0, -1, 1, 2, 1);
        box(part(s27, "s27a", 0, 0, 0), 13, 0, false, -0.2F, 0, -1, 1, 2, 1);

        PartDefinition s12 = part(root, "s12", 1, -2.05F, -2.3F);
        box(s12, 0, 22, false, -2, 0, -5, 2, 2, 1);
        PartDefinition s121 = part(s12, "s121", -0.5F, -0.1F, -5.5F);
        box(s121, 0, 17, false, -1, 0, 0, 1, 1, 8);
        box(s121, 7, 25, false, -1, 0, 6.5F, 1, 1, 0);
        box(part(s121, "s14", 0, 1, 0, 0.5236F, 0, 0), 0, 8, false, -1, 0, 0, 1, 1, 1);
        box(part(s121, "s122", -0.4F, 0.2F, 1.5F), 0, 6, true, -1, 0, 0, 1, 1, 5);
        box(part(s121, "s123", -0.6F, 0.2F, 1.5F), 0, 6, false, 0, 0, 0, 1, 1, 5);
        PartDefinition s124 = part(s121, "s124", 0, 1.4F, 1.0F);
        box(s124, 0, 0, false, -1, 0, 0, 1, 1, 4);
        box(s124, 0, 0, false, -1, -1, 0, 1, 1, 4);
        box(s124, 0, 0, false, -1, -0.4063F, 4, 1, 1, 4);

        PartDefinition s125 = part(s12, "s125", 0, 0, 1);
        PartDefinition hammer = part(s125, "hammer", -1, 1.45F, 3, -0.5236F, 0, 0);
        box(hammer, 13, 3, false, -0.5F, -0.8817F, -0.65F, 1, 1, 1);
        box(part(s125, "s291", -1, -0.4F, -4.97F, 0, 0, -0.7854F), 9, 0, false, -1, 0, -1, 1, 1, 1);
        part(s125, "s126", -2, 0, 0);

        PartDefinition s127 = part(s12, "s127", 0, 1, -4);
        PartDefinition chamber = part(s127, "chamber", -1, 24.8F, -0.7F);
        box(chamber, 10, 20, false, -0.95F, -25.8F, 5.7F, 2, 2, 3);
        box(chamber, 10, 20, true, -1.05F, -25.8F, 5.7F, 2, 2, 3);
        box(chamber, 7, 5, false, -1, -24.8F, 0.7F, 2, 1, 5);
        box(chamber, 20, 23, false, -0.5F, -25.8961F, 5.6883F, 1, 1, 2);
        PartDefinition s29 = part(chamber, "s29", 0.5F, -26.1F, 7.2F, 0.1745F, 0, 0);
        box(s29, 0, 6, false, -0.9922F, 0, 0, 1, 1, 1);
        box(s29, 0, 6, false, -1.0078F, 0, 0, 1, 1, 1);

        PartDefinition s18 = part(root, "s18", 1, -2.05F, -0.8F, -0.4363F, 0, 0);
        box(s18, 31, 19, false, -0.1F, 0, 0.3F, 0, 0, 0);
        PartDefinition s9 = part(s18, "s9", -0.5F, 1, 0.5F, 0.4189F, 0, 0);
        box(s9, 0, 12, false, -1, 0, -2, 1, 2, 2);
        box(part(s18, "s91", -0.5F, 2, 0, 0.1745F, 0, 0), 0, 0, false, -1, 0, 0, 1, 1, 0);
        PartDefinition s19 = part(s18, "s19", -2.02F, 1.7F, 0.3F, 0.6109F, 0, 0);
        box(s19, 0, 26, true, 0, 0, 0, 2, 4, 2);
        PartDefinition s191 = part(s19, "s191", 0.02F, 1.5F, 2.0F, 0.1745F, 0, 0);
        box(s191, 16, 29, true, 0, 0, -1, 2, 2, 1);
        PartDefinition s192 = part(s191, "s192", -0.03F, 2.0F, 0, -0.3491F, 0, 0);
        box(s192, 8, 29, true, 0, 0, -2, 2, 1, 2);
        box(part(s192, "s193", -0.01F, 0, -2.4F), 8, 27, true, 0, -0.01F, 0, 2, 1, 1);
        PartDefinition s194 = part(s18, "s194", 0.02F, 1.7F, 0.3F, 0.6109F, 0, 0);
        box(s194, 0, 26, false, -2, 0, 0, 2, 4, 2);
        PartDefinition s195 = part(s194, "s195", -0.02F, 1.5F, 2.0F, 0.1745F, 0, 0);
        box(s195, 16, 29, false, -2, 0, -1, 2, 2, 1);
        PartDefinition s196 = part(s195, "s196", 0.03F, 2.0F, 0, -0.3491F, 0, 0);
        box(s196, 8, 29, false, -2, 0, -2, 2, 1, 2);
        box(part(s196, "s197", 0.01F, 0, -2.4F), 8, 27, false, -2, -0.01F, 0, 2, 1, 1);
        return LayerDefinition.create(mesh, 32, 32).bakeRoot();
    }

    private static PartDefinition part(PartDefinition parent, String name, float x, float y, float z)
    {
        return part(parent, name, x, y, z, 0, 0, 0);
    }

    private static PartDefinition part(PartDefinition parent, String name, float x, float y, float z,
            float xRot, float yRot, float zRot)
    {
        return parent.addOrReplaceChild(name, CubeListBuilder.create(),
                PartPose.offsetAndRotation(x, y, z, xRot, yRot, zRot));
    }

    private static void box(PartDefinition part, int u, int v, boolean mirror,
            float x, float y, float z, float width, float height, float depth)
    {
        part.addOrReplaceChild("cube_" + u + "_" + v + "_" + x + "_" + y + "_" + z,
                CubeListBuilder.create().texOffs(u, v).mirror(mirror).addBox(x, y, z, width, height, depth), PartPose.ZERO);
    }
}
