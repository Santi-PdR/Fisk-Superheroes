package com.fiskmods.heroes.client.render;

import com.fiskmods.heroes.common.block.SuitFabricatorBlock;
import com.fiskmods.heroes.common.block.SuitFabricatorBlockEntity;
import com.fiskmods.heroes.common.hero.HeroIteration;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * 3D holographic suit projection renderer above Suit Fabricator & Cosmic Suit Fabricator blocks.
 * Core-profile 1.20.1 counterpart to 1.7.10 RenderSuitFabricator.
 */
public final class SuitFabricatorBlockEntityRenderer implements BlockEntityRenderer<SuitFabricatorBlockEntity>
{
    private final PlayerModel<?> playerModel;

    public SuitFabricatorBlockEntityRenderer(BlockEntityRendererProvider.Context ctx)
    {
        this.playerModel = new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false);
        this.playerModel.young = false;
    }

    @Override
    public void render(SuitFabricatorBlockEntity be, float partialTick, PoseStack pose,
            MultiBufferSource buffers, int packedLight, int packedOverlay)
    {
        HeroIteration iter = be.getTargetIteration();
        if (iter == null) return;

        HeroModelData modelData = HeroModelRegistry.get(iter);
        if (modelData == null) return;

        boolean printing = be.isPrinting();
        boolean finished = be.hasFinishedOutput();
        float progress = printing
                ? Mth.clamp((be.getPrintTime() + partialTick) / (float) SuitFabricatorBlockEntity.MAX_PRINT_TIME, 0.0F, 1.0F)
                : 1.0F;

        boolean cosmic = be.isCosmic();
        float tintR = finished ? 1.0F : (cosmic ? 0.92F : 0.47F);
        float tintG = finished ? 1.0F : (cosmic ? 0.68F : 0.82F);
        float tintB = finished ? 1.0F : 1.0F;
        float alpha = finished ? 0.95F : (printing ? 0.45F + 0.45F * progress : 0.38F);

        Direction facing = be.getBlockState().hasProperty(SuitFabricatorBlock.FACING)
                ? be.getBlockState().getValue(SuitFabricatorBlock.FACING)
                : Direction.NORTH;
        float yaw = -facing.toYRot();
        if (finished && be.getLevel() != null)
        {
            yaw = ((be.getLevel().getGameTime() + partialTick) * 2.0F) % 360.0F;
        }

        pose.pushPose();
        pose.translate(0.5D, 1.72D, 0.5D);
        pose.mulPose(Axis.YP.rotationDegrees(yaw + 180.0F));
        pose.scale(0.45F, -0.45F, -0.45F);

        var viewer = Minecraft.getInstance().player;
        for (int slot = 0; slot < 4; ++slot)
        {
            if (!iter.hasPieceOfSet(slot)) continue;
            // During printing, progressively reveal boots -> leggings -> chestplate -> helmet
            if (printing)
            {
                float slotThreshold = (3 - slot) * 0.22F;
                if (progress < slotThreshold) continue;
            }
            ResourceLocation baseTex = modelData.getTexture(slot, viewer);
            if (baseTex != null)
            {
                setVisibleForSlot(slot);
                playerModel.renderToBuffer(pose, buffers.getBuffer(RenderType.entityTranslucent(baseTex)),
                        LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, tintR, tintG, tintB, alpha);
            }
            ResourceLocation lightsTex = modelData.getLights(slot, viewer);
            if (lightsTex != null)
            {
                setVisibleForSlot(slot);
                playerModel.renderToBuffer(pose, buffers.getBuffer(RenderType.eyes(lightsTex)),
                        LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, alpha);
            }
        }
        pose.popPose();

        // Render active holographic laser scan plane while printing
        if (printing)
        {
            pose.pushPose();
            float scanY = 1.02F + progress * 0.85F;
            pose.translate(0.5D, scanY, 0.5D);
            Matrix4f mat = pose.last().pose();
            Matrix3f norm = pose.last().normal();
            VertexConsumer vc = buffers.getBuffer(RenderType.lightning());
            int r = cosmic ? 235 : 119;
            int g = cosmic ? 140 : 204;
            int b = 255;
            float hs = 0.34F;
            vc.vertex(mat, -hs, 0.0F, -hs).color(r, g, b, 130).uv(0, 0)
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(norm, 0, 1, 0).endVertex();
            vc.vertex(mat, -hs, 0.0F, hs).color(r, g, b, 130).uv(0, 1)
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(norm, 0, 1, 0).endVertex();
            vc.vertex(mat, hs, 0.0F, hs).color(r, g, b, 130).uv(1, 1)
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(norm, 0, 1, 0).endVertex();
            vc.vertex(mat, hs, 0.0F, -hs).color(r, g, b, 130).uv(1, 0)
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(norm, 0, 1, 0).endVertex();
            // Backface so visible from below too
            vc.vertex(mat, -hs, 0.0F, -hs).color(r, g, b, 130).uv(0, 0)
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(norm, 0, -1, 0).endVertex();
            vc.vertex(mat, hs, 0.0F, -hs).color(r, g, b, 130).uv(1, 0)
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(norm, 0, -1, 0).endVertex();
            vc.vertex(mat, hs, 0.0F, hs).color(r, g, b, 130).uv(1, 1)
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(norm, 0, -1, 0).endVertex();
            vc.vertex(mat, -hs, 0.0F, hs).color(r, g, b, 130).uv(0, 1)
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(norm, 0, -1, 0).endVertex();
            pose.popPose();
        }
    }

    private void setVisibleForSlot(int slot)
    {
        playerModel.setAllVisible(false);
        switch (slot)
        {
            case 0 ->
            {
                playerModel.head.visible = true;
                playerModel.hat.visible = true;
            }
            case 1 ->
            {
                playerModel.body.visible = true;
                playerModel.jacket.visible = true;
                playerModel.rightArm.visible = true;
                playerModel.rightSleeve.visible = true;
                playerModel.leftArm.visible = true;
                playerModel.leftSleeve.visible = true;
            }
            case 2 ->
            {
                playerModel.body.visible = true;
                playerModel.rightLeg.visible = true;
                playerModel.rightPants.visible = true;
                playerModel.leftLeg.visible = true;
                playerModel.leftPants.visible = true;
            }
            case 3 ->
            {
                playerModel.rightLeg.visible = true;
                playerModel.rightPants.visible = true;
                playerModel.leftLeg.visible = true;
                playerModel.leftPants.visible = true;
            }
        }
    }
}
