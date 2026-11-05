package com.fiskmods.heroes.client.gui;

import com.fiskmods.heroes.client.render.HeroModelData;
import com.fiskmods.heroes.client.render.HeroModelRegistry;
import com.fiskmods.heroes.common.hero.HeroIteration;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** Renders a live rotating 3D hero suit preview inside Suit Iterator & Suit Fabricator GUIs. */
public final class SuitGuiPreviewRenderer
{
    private static PlayerModel<?> cachedModel;

    private SuitGuiPreviewRenderer()
    {
    }

    private static PlayerModel<?> model()
    {
        if (cachedModel == null)
        {
            cachedModel = new PlayerModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
            cachedModel.young = false;
        }
        return cachedModel;
    }

    public static void render(GuiGraphics graphics, HeroIteration iter, int centerX, int bottomY, float scale, float partialTick)
    {
        if (iter == null) return;
        HeroModelData modelData = HeroModelRegistry.get(iter);
        if (modelData == null) return;

        PlayerModel<?> m = model();
        Minecraft mc = Minecraft.getInstance();
        float spin = ((mc.player != null ? mc.player.tickCount : 0) + partialTick) * 2.0F;

        Lighting.setupForEntityInInventory();
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(centerX, bottomY - scale * 1.15F, 120.0F);
        pose.scale(scale, scale, -scale);
        pose.mulPose(Axis.XP.rotationDegrees(10.0F));
        pose.mulPose(Axis.YP.rotationDegrees(180.0F + spin));

        for (int slot = 0; slot < 4; ++slot)
        {
            if (!iter.hasPieceOfSet(slot)) continue;
            ResourceLocation tex = modelData.getTexture(slot, mc.player);
            if (tex != null)
            {
                setVisibleForSlot(m, slot);
                m.renderToBuffer(pose, graphics.bufferSource().getBuffer(RenderType.entityTranslucent(tex)),
                        LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
            }
            ResourceLocation lights = modelData.getLights(slot, mc.player);
            if (lights != null)
            {
                setVisibleForSlot(m, slot);
                m.renderToBuffer(pose, graphics.bufferSource().getBuffer(RenderType.eyes(lights)),
                        LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
            }
        }
        m.setAllVisible(true);
        graphics.flush();
        pose.popPose();
        Lighting.setupFor3DItems();
    }

    private static void setVisibleForSlot(PlayerModel<?> m, int slot)
    {
        m.setAllVisible(false);
        switch (slot)
        {
            case 0 ->
            {
                m.head.visible = true;
                m.hat.visible = true;
            }
            case 1 ->
            {
                m.body.visible = true;
                m.jacket.visible = true;
                m.rightArm.visible = true;
                m.rightSleeve.visible = true;
                m.leftArm.visible = true;
                m.leftSleeve.visible = true;
            }
            case 2 ->
            {
                m.body.visible = true;
                m.rightLeg.visible = true;
                m.rightPants.visible = true;
                m.leftLeg.visible = true;
                m.leftPants.visible = true;
            }
            case 3 ->
            {
                m.rightLeg.visible = true;
                m.rightPants.visible = true;
                m.leftLeg.visible = true;
                m.leftPants.visible = true;
            }
        }
    }
}
