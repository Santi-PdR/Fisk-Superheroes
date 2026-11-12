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

/** The original ModelArrow shaft and three fletchings, using its 64 by 32 texture layout.
 * The arrowhead is enclosed by the quiver and is deliberately not rendered here. */
final class QuiverArrowModel
{
    private final ModelPart stick;
    private final ModelPart[] feathers = new ModelPart[3];

    QuiverArrowModel()
    {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("stick", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-.5F, -9, -.5F, 1, 18, 1), PartPose.ZERO);
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
        for (int i = 0; i < feathers.length; i++) feathers[i] = model.getChild("feather" + i);
    }

    void render(PoseStack pose, VertexConsumer out, int light)
    {
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
