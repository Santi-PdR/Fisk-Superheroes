package com.fiskmods.heroes.client.render;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.client.player.AbstractClientPlayer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

/** Draws the animated arm overlay used by Crossbones' punch mode. */
public final class ArmOverlaySuitRenderer
{
    private ArmOverlaySuitRenderer()
    {
    }

    public static void render(String key, JsonObject effect, PoseStack poseStack, MultiBufferSource buffer,
            int packedLight, AbstractClientPlayer player, PlayerModel<AbstractClientPlayer> playerModel,
            HeroModelData model, int slot, float opacity)
    {
        if (!HeroSuitLayer.appliesToSlot(effect, slot) || !HeroSuitLayer.passesConditionals(effect, model, player)) return;

        String textureKey = effect.has("texture") ? effect.get("texture").getAsString() : null;
        ResourceLocation texture = model.resolveCustomTexture(textureKey, player, slot);
        if (texture == null) return;

        String sideName = effect.has("side") ? effect.get("side").getAsString() : "RIGHT";
        boolean left = key.endsWith("|left") || sideName.equalsIgnoreCase("LEFT");
        ModelPart arm = left ? playerModel.leftArm : playerModel.rightArm;
        ModelPart sleeve = left ? playerModel.leftSleeve : playerModel.rightSleeve;
        float side = left ? 1.0F : -1.0F;
        float progress = Mth.clamp(model.evaluateRenderData(effect.get("data"), player, 0.0F), 0.0F, 1.0F);
        float eased = progress > 0.0F ? progress * progress * (3.0F - 2.0F * progress) : 0.0F;
        float[] offset = vector(effect.get("offset"));
        float[] rotation = vector(effect.get("rotation"));

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(texture));
        poseStack.pushPose();
        try
        {
            // Match the original effect's postRender, zeroed arm pivot, then animated shell.
            arm.translateAndRotate(poseStack);
            float x = arm.x, y = arm.y, z = arm.z;
            float xRot = arm.xRot, yRot = arm.yRot, zRot = arm.zRot;
            try
            {
                arm.x = arm.y = arm.z = 0.0F;
                arm.xRot = arm.yRot = arm.zRot = 0.0F;
                poseStack.translate(side * offset[0] * eased / 16.0D,
                        offset[1] * eased / 16.0D, offset[2] * eased / 16.0D);
                poseStack.mulPose(Axis.ZP.rotationDegrees(rotation[2] * eased));
                poseStack.mulPose(Axis.YP.rotationDegrees(rotation[1] * eased));
                poseStack.mulPose(Axis.XP.rotationDegrees(rotation[0] * eased));
                float scale = 1.0F + eased * 0.01F;
                poseStack.translate(2.0D * side / 16.0D, 4.0D / 16.0D, 0.0D);
                poseStack.scale(scale, scale, scale);
                poseStack.translate(-2.0D * side / 16.0D, -4.0D / 16.0D, 0.0D);
                arm.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY,
                        1.0F, 1.0F, 1.0F, opacity);
                sleeve.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY,
                        1.0F, 1.0F, 1.0F, opacity);
            }
            finally
            {
                arm.x = x; arm.y = y; arm.z = z;
                arm.xRot = xRot; arm.yRot = yRot; arm.zRot = zRot;
            }
        }
        finally
        {
            poseStack.popPose();
        }
    }

    private static float[] vector(JsonElement value)
    {
        float[] result = new float[3];
        if (value != null && value.isJsonArray())
        {
            for (int i = 0; i < Math.min(3, value.getAsJsonArray().size()); ++i)
                result[i] = value.getAsJsonArray().get(i).getAsFloat();
        }
        return result;
    }
}
