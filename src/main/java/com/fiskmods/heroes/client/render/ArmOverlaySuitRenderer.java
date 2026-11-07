package com.fiskmods.heroes.client.render;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Draws custom arm overlays over the classic-width suit, without mutating the player's model. */
public final class ArmOverlaySuitRenderer
{
    private static PlayerModel<?> overlayModel;
    private static final java.util.Set<String> DIAGNOSTICS = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private ArmOverlaySuitRenderer() {}

    public static void render(String key, JsonObject effect, PoseStack poseStack, MultiBufferSource buffer,
            int packedLight, AbstractClientPlayer player, PlayerModel<AbstractClientPlayer> playerModel,
            HeroModelData model, int slot, float opacity)
    {
        render(key, effect, poseStack, buffer, packedLight, player, playerModel, model, slot, opacity, null);
    }

    /** firstPersonRight is null for the body layer, otherwise restricts drawing to the visible hand. */
    public static void render(String key, JsonObject effect, PoseStack poseStack, MultiBufferSource buffer,
            int packedLight, AbstractClientPlayer player, PlayerModel<AbstractClientPlayer> playerModel,
            HeroModelData model, int slot, float opacity, Boolean firstPersonRight)
    {
        if (!HeroSuitLayer.appliesToSlot(effect, slot) || !HeroSuitLayer.passesConditionals(effect, model, player)) return;
        String textureKey = effect.has("texture") ? effect.get("texture").getAsString() : null;
        ResourceLocation texture = model.resolveCustomTexture(textureKey, player, slot);
        if (texture == null) return;

        String side = effect.has("side") ? effect.get("side").getAsString() : "RIGHT";
        boolean left = side.equalsIgnoreCase("LEFT");
        boolean both = side.equalsIgnoreCase("BOTH");
        float progress = Mth.clamp(model.evaluateRenderData(effect.get("data"), player, 1.0F), 0.0F, 1.0F);
        float eased = progress * progress * (3.0F - 2.0F * progress);
        float[] offset = vector(effect.get("offset"));
        float[] rotation = vector(effect.get("rotation"));
        boolean drawRight = !left && (firstPersonRight == null || firstPersonRight);
        boolean drawLeft = (left || both) && (firstPersonRight == null || !firstPersonRight);
        if (!drawRight && !drawLeft) return;
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(texture));
        if (drawRight)
            renderArm(poseStack, consumer, packedLight, playerModel.rightArm, false, offset, rotation, eased, opacity);
        if (drawLeft)
            renderArm(poseStack, consumer, packedLight, playerModel.leftArm, true, offset, rotation, eased, opacity);
        String diagnostic = key + ":" + texture + ":" + firstPersonRight;
        if (DIAGNOSTICS.add(diagnostic))
            com.fiskmods.heroes.FiskHeroes.LOGGER.info("Suit arm overlay rendered: effect={}, side={}, firstPerson={}, texture={}, progress={}",
                    key, side, firstPersonRight, texture, progress);
    }

    private static void renderArm(PoseStack pose, VertexConsumer consumer, int light, ModelPart source,
            boolean left, float[] offset, float[] rotation, float eased, float opacity)
    {
        if (overlayModel == null)
        {
            // Slightly outside the suit's 0.55 deformation to avoid coplanar depth fighting.
            var mesh = PlayerModel.createMesh(new CubeDeformation(0.56F), false);
            overlayModel = new PlayerModel<>(LayerDefinition.create(mesh, 64, 64).bakeRoot(), false);
        }
        ModelPart arm = left ? overlayModel.leftArm : overlayModel.rightArm;
        ModelPart sleeve = left ? overlayModel.leftSleeve : overlayModel.rightSleeve;
        // The source's pose is applied once to the stack; the independent geometry remains at
        // the origin and unit scale. This also preserves any scale on the shared player model.
        arm.resetPose();
        sleeve.resetPose();
        arm.x = arm.y = arm.z = arm.xRot = arm.yRot = arm.zRot = 0.0F;
        sleeve.x = sleeve.y = sleeve.z = sleeve.xRot = sleeve.yRot = sleeve.zRot = 0.0F;
        arm.visible = sleeve.visible = true;
        arm.skipDraw = sleeve.skipDraw = false;
        float sign = left ? 1.0F : -1.0F;
        pose.pushPose();
        try
        {
            source.translateAndRotate(pose);
            pose.translate(sign * offset[0] * eased / 16.0D, offset[1] * eased / 16.0D, offset[2] * eased / 16.0D);
            pose.mulPose(Axis.ZP.rotationDegrees(rotation[2] * eased));
            pose.mulPose(Axis.YP.rotationDegrees(rotation[1] * eased));
            pose.mulPose(Axis.XP.rotationDegrees(rotation[0] * eased));
            float scale = 1.0F + eased * 0.01F;
            pose.translate(2.0D * sign / 16.0D, 4.0D / 16.0D, 0.0D);
            pose.scale(scale, scale, scale);
            pose.translate(-2.0D * sign / 16.0D, -4.0D / 16.0D, 0.0D);
            arm.render(pose, consumer, light, OverlayTexture.NO_OVERLAY, 1, 1, 1, opacity);
            sleeve.render(pose, consumer, light, OverlayTexture.NO_OVERLAY, 1, 1, 1, opacity);
        }
        finally { pose.popPose(); }
    }

    private static float[] vector(JsonElement value)
    {
        float[] result = new float[3];
        if (value != null && value.isJsonArray())
            for (int i = 0; i < Math.min(3, value.getAsJsonArray().size()); ++i)
                result[i] = value.getAsJsonArray().get(i).getAsFloat();
        return result;
    }
}
