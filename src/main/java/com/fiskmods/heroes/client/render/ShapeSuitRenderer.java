package com.fiskmods.heroes.client.render;

import com.fiskmods.heroes.common.hero.HeroIteration;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Draws the original SHAPE effects (spell glyphs, mandalas and energy blades) from pack JSON. */
public final class ShapeSuitRenderer
{
    private ShapeSuitRenderer() {}

    public static void render(String effectId, com.google.gson.JsonObject effect, PoseStack poseStack,
            MultiBufferSource buffers, Player player, net.minecraft.client.model.PlayerModel<?> playerModel,
            HeroModelData model, int slot, float suitOpacity)
    {
        if (!effect.has("shape") || !effect.has("data")) return;
        ResourceLocation shapeId = ResourceLocation.tryParse(effect.get("shape").getAsString());
        ShapeEffectRegistry.Shape shape = shapeId != null ? ShapeEffectRegistry.get(shapeId) : null;
        if (shape == null) return;

        float progress = net.minecraft.util.Mth.clamp(model.evaluateRenderData(effect.get("data"), player, 0.0F), 0.0F, 1.0F);
        if (progress <= 0.001F) return;

        String anchorName = effect.has("anchor") ? effect.get("anchor").getAsString() : "rightArm";
        ModelPart anchor = anchor(playerModel, anchorName);
        if (anchor == null) return;

        double[] offset = vector(effect.get("offset"));
        double[] rotation = vector(effect.get("rotation"));
        float scale = effect.has("scale") ? effect.get("scale").getAsFloat() : 1.0F;
        if (scale <= 0.0F) return;
        if (effect.has("mirror") && effect.get("mirror").getAsBoolean()) scale = -scale;
        int color = color(effect.has("color") ? effect.get("color").getAsString() : "0xFFFFFF");
        float alpha = Math.max(0.0F, Math.min(1.0F, progress * suitOpacity));
        VertexConsumer consumer = buffers.getBuffer(RenderType.lines());

        poseStack.pushPose();
        anchor.translateAndRotate(poseStack);
        poseStack.translate(offset[0] / 16.0D, offset[1] / 16.0D, offset[2] / 16.0D);
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees((float) rotation[0]));
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees((float) rotation[1]));
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees((float) rotation[2]));
        poseStack.scale(scale, Math.abs(scale), Math.abs(scale));

        PoseStack.Pose pose = poseStack.last();
        for (ShapeEffectRegistry.Path path : shape.paths()) drawPath(consumer, pose, path.points(), progress, color, alpha);
        poseStack.popPose();
    }

    private static void drawPath(VertexConsumer consumer, PoseStack.Pose pose, java.util.List<Vec3> points,
            float progress, int color, float alpha)
    {
        int segments = points.size() - 1;
        if (segments <= 0) return;
        float segmentLimit = progress * segments;
        int complete = Math.min(segments, (int) segmentLimit);
        for (int i = 0; i < complete; ++i)
        {
            vertex(consumer, pose, points.get(i), color, alpha);
            vertex(consumer, pose, points.get(i + 1), color, alpha);
        }
        if (complete < segments)
        {
            float fraction = segmentLimit - complete;
            if (fraction > 0.0F)
            {
                Vec3 from = points.get(complete);
                Vec3 to = from.lerp(points.get(complete + 1), fraction);
                vertex(consumer, pose, from, color, alpha);
                vertex(consumer, pose, to, color, alpha);
            }
        }
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, Vec3 point, int color, float alpha)
    {
        consumer.vertex(pose.pose(), (float) point.x, (float) point.y, (float) point.z)
                .color((color >> 16) & 255, (color >> 8) & 255, color & 255, (int) (alpha * 255.0F))
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(pose.normal(), 0.0F, 0.0F, 1.0F).endVertex();
    }

    private static ModelPart anchor(net.minecraft.client.model.PlayerModel<?> model, String name)
    {
        return switch (name.toLowerCase(java.util.Locale.ROOT))
        {
            case "head" -> model.head;
            case "body" -> model.body;
            case "leftarm" -> model.leftArm;
            case "leftleg" -> model.leftLeg;
            case "rightleg" -> model.rightLeg;
            default -> model.rightArm;
        };
    }

    private static double[] vector(com.google.gson.JsonElement value)
    {
        double[] result = new double[3];
        if (value != null && value.isJsonArray())
        {
            var array = value.getAsJsonArray();
            for (int i = 0; i < Math.min(3, array.size()); ++i) result[i] = array.get(i).getAsDouble();
        }
        return result;
    }

    private static int color(String value)
    {
        try { return (int) Long.decode(value).longValue() & 0xFFFFFF; }
        catch (NumberFormatException ignored) { return 0xFFFFFF; }
    }
}
