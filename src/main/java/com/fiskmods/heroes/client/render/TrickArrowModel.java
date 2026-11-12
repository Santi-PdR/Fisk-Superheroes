package com.fiskmods.heroes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** Original ModelArrow geometry and 64 by 32 texture layout. */
final class TrickArrowModel
{
    private final ModelPart stick;
    private final ModelPart head;
    private final ModelPart[] feathers = new ModelPart[3];

    TrickArrowModel()
    {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("stick", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-.5F, -9, -.5F, 1, 18, 1), PartPose.ZERO);
        PartDefinition headDef = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(4, 0)
                .addBox(-.5F, -3, -1, 1, 3, 2), PartPose.offset(0, -9, 0));
        headDef.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(8, 0)
                .addBox(-.5F, -.5F, -.5F, 1, 1, 1), PartPose.offsetAndRotation(0, -7.8F, 0, 0, 0, .7853982F));
        headDef.addOrReplaceChild("middle", CubeListBuilder.create().texOffs(8, 12)
                .addBox(-1, -2, -.5F, 2, 2, 1), PartPose.offset(0, -1, 0));
        headDef.addOrReplaceChild("front", CubeListBuilder.create().texOffs(4, 5)
                .addBox(-.5F, -5, -1, 1, 5, 1), PartPose.offsetAndRotation(0, -2.85F, 0, -.09599311F, 0, 0));
        headDef.addOrReplaceChild("back", CubeListBuilder.create().texOffs(4, 5)
                .addBox(-.5F, -5, 0, 1, 5, 1), PartPose.offsetAndRotation(0, -2.85F, 0, .09599311F, 0, 0));
        for (int side = 0; side < 2; ++side)
        {
            float direction = side == 0 ? -1 : 1;
            PartDefinition edge = headDef.addOrReplaceChild("edge" + side, CubeListBuilder.create().texOffs(8, 5)
                    .addBox(-.5F, -6, -.5F, 1, 6, 1),
                    PartPose.offsetAndRotation(direction * 2.3F, -2, 0, 0, 0, -direction * .34906584F));
            edge.addOrReplaceChild("lower", CubeListBuilder.create().texOffs(4, 11)
                    .addBox(-.5F, 0, -.5F, 1, 3, 1),
                    PartPose.offsetAndRotation(side == 0 ? -.22F : .28F, -.45F, 0, 0, 0, direction * 1.2217305F));
            edge.addOrReplaceChild("middle", CubeListBuilder.create().texOffs(11, 1)
                    .addBox(-.5F, -4, -.5F, 1, 4, 1),
                    PartPose.offsetAndRotation(-direction, 0, 0, 0, -.06981317F, direction * .06981317F));
        }
        for (int i = 0; i < feathers.length; i++)
        {
            float angle = i == 0 ? 0 : i == 1 ? 2.0943952F : -2.0943952F;
            PartDefinition feather = root.addOrReplaceChild("feather" + i,
                    CubeListBuilder.create().texOffs(4, 24).addBox(.65F, -.3F, -.5F, 3, 7, 1),
                    PartPose.offsetAndRotation(0, 4, 0, 0, angle, 0));
            feather.addOrReplaceChild("top", CubeListBuilder.create().texOffs(4, 15)
                    .addBox(-1.6F, -1, -.5F, 3, 1, 1),
                    PartPose.offsetAndRotation(1.95F, .15F, 0, 0, 0, .34906584F));
            feather.addOrReplaceChild("outer", CubeListBuilder.create().texOffs(4, 17)
                    .addBox(-.9F, 0, -.5F, 2, 6, 1),
                    PartPose.offsetAndRotation(2.6F, 0, 0, 0, 0, -.29670596F));
        }
        ModelPart model = LayerDefinition.create(mesh, 64, 32).bakeRoot();
        stick = model.getChild("stick");
        head = model.getChild("head");
        for (int i = 0; i < feathers.length; i++) feathers[i] = model.getChild("feather" + i);
    }

    void render(PoseStack pose, VertexConsumer out, int light, boolean inQuiver)
    {
        if (!inQuiver)
        {
            pose.pushPose();
            pose.translate(0, -9.0 / 16, 0);
            pose.scale(.3F, .4F, .15F);
            pose.translate(0, 9.0 / 16, 0);
            head.render(pose, out, light, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
        pose.pushPose();
        pose.scale(.3F, 1, .3F);
        stick.render(pose, out, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
        for (ModelPart feather : feathers)
        {
            pose.pushPose();
            // ModelArrow scaled each fletching around its four-pixel pivot.
            pose.translate(0, 4.0 / 16, 0);
            pose.scale(.2F, .5F, .2F);
            pose.translate(0, -4.0 / 16, 0);
            feather.render(pose, out, light, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
    }
}
