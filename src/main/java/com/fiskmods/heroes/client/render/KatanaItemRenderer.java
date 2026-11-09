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

/** Original multi-segment katana mesh used by the weapon and equipment-wheel item. */
public final class KatanaItemRenderer extends BlockEntityWithoutLevelRenderer
{
    private static final ResourceLocation TEXTURE = FiskHeroes.id("textures/models/katana.png");
    private static KatanaItemRenderer instance;
    private static boolean loggedStowedDual;
    private final ModelPart sword;

    private KatanaItemRenderer()
    {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        sword = LayerDefinition.create(createMesh(), 32, 32).bakeRoot().getChild("sword");
    }

    public static KatanaItemRenderer getInstance()
    {
        if (instance == null) instance = new KatanaItemRenderer();
        return instance;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
            MultiBufferSource buffers, int light, int overlay)
    {
        pose.pushPose();
        switch (context)
        {
            case NONE ->
            {
                // The suit layer already applies the pack's anchor and rotation.
                // Cancel ItemRenderer's half-block origin shift without adding a GUI pose.
                pose.translate(0.5D, 0.5D, 0.5D);
            }
            case GUI ->
            {
                // The legacy dual icon used opposed rotations, not two coincident blades.
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

        var consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(SuitItemRenderContext.textureOr(TEXTURE)));
        sword.render(pose, consumer, light, overlay);
        boolean secondCopy = ItemDualSword.isDual(stack) && (context == ItemDisplayContext.GUI || context == ItemDisplayContext.GROUND || context == ItemDisplayContext.FIXED);
        if (context == ItemDisplayContext.NONE && ItemDualSword.isDual(stack) && !loggedStowedDual)
        {
            loggedStowedDual = true;
            FiskHeroes.LOGGER.info("Stowed dual katana meshes per anchor: {}", secondCopy ? 2 : 1);
        }
        if (secondCopy)
        {
            pose.translate(0.0D, 0.0D, 0.26D);
            sword.render(pose, consumer, light, overlay);
        }
        pose.popPose();
    }

    private void renderInventorySword(PoseStack pose, MultiBufferSource buffers,
            int light, int overlay, boolean mirrored)
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

    private static MeshDefinition createMesh()
    {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition sword = mesh.getRoot().addOrReplaceChild("sword", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition hilt = part(sword, "hilt", 0, 0, 0);
        box(hilt, 0, 0, -0.5F, 0, -1, 1, 13, 2);
        box(part(hilt, "guard", 0, -0.01F, 0), 6, 0, -2.5F, 0, -3, 5, 0, 6);

        PartDefinition b1 = part(sword, "blade1", 0, 0, 0.3F);
        box(b1, 6, 23, 0, -7, -1, 0, 7, 1);
        box(part(b1, "blade2", 0, 0, 0, 0, (float) Math.PI, 0), 0, 15, -0.25F, -7, -0.42F, 1, 7, 1);
        box(part(b1, "blade3", 0, 0, 0), 0, 15, -0.25F, -7, -0.08F, 1, 7, 1);
        PartDefinition b4 = part(b1, "blade4", 0, -7, -1, -(float) Math.PI / 180.0F, 0, 0);
        box(b4, 6, 18, 0, -5, 0, 0, 5, 1);
        box(part(b4, "blade5", 0, 0, 1, 0, (float) Math.PI, 0), 8, 22, -0.25F, -5, -0.42F, 1, 5, 1);
        box(part(b4, "blade6", 0, 0, 1), 8, 22, -0.25F, -5, -0.08F, 1, 5, 1);
        PartDefinition b7 = part(b4, "blade7", 0, -5, 0, -(float) Math.PI / 180.0F, 0, 0);
        box(b7, 6, 13, 0, -5, 0, 0, 5, 1);
        box(part(b7, "blade8", 0, 0, 1, 0, (float) Math.PI, 0), 8, 16, -0.25F, -5, -0.42F, 1, 5, 1);
        box(part(b7, "blade9", 0, 0, 1), 8, 16, -0.25F, -5, -0.08F, 1, 5, 1);
        PartDefinition b10 = part(b7, "blade10", 0, -5, 0, -(float) Math.PI / 90.0F, 0, 0);
        box(b10, 6, 8, 0, -5, 0, 0, 5, 1);
        box(part(b10, "blade11", 0, 0, 1, 0, (float) Math.PI, 0), 8, 10, -0.25F, -5, -0.42F, 1, 5, 1);
        box(part(b10, "blade12", 0, 0, 1), 8, 10, -0.25F, -5, -0.08F, 1, 5, 1);
        PartDefinition b13 = part(b10, "blade13", 0, -5, 0, -0.05235988F, 0, 0);
        box(b13, 6, 3, 0, -5, 0, 0, 5, 1);
        box(part(b13, "blade14", 0, 0, 1, 0, (float) Math.PI, 0), 8, 4, -0.25F, -5, -0.42F, 1, 5, 1);
        box(part(b13, "blade15", 0, 0, 1), 8, 4, -0.25F, -5, -0.08F, 1, 5, 1);
        PartDefinition b16 = part(b13, "blade16", 0, -5, 0, -0.05235988F, 0, 0);
        box(b16, 6, 2, 0, -1, 0, 0, 1, 1);
        PartDefinition b17 = part(b16, "blade17", 0.01F, -1, 0, -0.17453292F, 0, 0);
        box(b17, 6, 0, 0, -2, 0, 0, 2, 1);
        PartDefinition b18 = part(b17, "blade18", 0.01F, -2, 0, -0.43633232F, 0, 0);
        box(b18, 6, -1, 0, -1, 0, 0, 1, 1);
        box(part(b18, "blade19", 0.01F, -1, 0, 0.61086524F, 0, 0), 4, -1, 0, 0, 0, 0, 2, 1);
        box(part(b16, "blade20", 0, 0, 1, 0, (float) Math.PI, 0), 8, 0, -0.25F, -3, -0.42F, 1, 3, 1);
        box(part(b16, "blade21", 0, 0, 1), 8, 0, -0.25F, -3, -0.08F, 1, 3, 1);
        return mesh;
    }

    private static PartDefinition part(PartDefinition parent, String name, float x, float y, float z)
    {
        return part(parent, name, x, y, z, 0, 0, 0);
    }

    private static PartDefinition part(PartDefinition parent, String name, float x, float y, float z,
            float xRot, float yRot, float zRot)
    {
        return parent.addOrReplaceChild(name, CubeListBuilder.create(), PartPose.offsetAndRotation(x, y, z, xRot, yRot, zRot));
    }

    private static void box(PartDefinition part, int u, int v,
            float x, float y, float z, float width, float height, float depth)
    {
        part.addOrReplaceChild("cube_" + u + "_" + v + "_" + x + "_" + y + "_" + z,
                CubeListBuilder.create().texOffs(u, v).addBox(x, y, z, width, height, depth), PartPose.ZERO);
    }
}
