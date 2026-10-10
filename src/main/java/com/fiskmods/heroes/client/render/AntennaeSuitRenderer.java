package com.fiskmods.heroes.client.render;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/** Renders the segmented antenna geometry configured by the original {@code fiskheroes:antennae} effect. */
final class AntennaeSuitRenderer
{
    private AntennaeSuitRenderer()
    {
    }

    static void render(JsonObject effect, PoseStack pose, MultiBufferSource buffers, int packedLight,
            Player player, PlayerModel<?> playerModel, HeroModelData model, int slot, float suitOpacity)
    {
        if (slot != 0 || !appliesToHelmet(effect) || !passesConditionals(effect, model, player)) return;

        ResourceLocation texture = model.getTexture(slot, player);
        if (texture == null) return;

        int segments = Math.max(1, Math.min(64, integer(effect, "segments", 8)));
        float segmentHeight = 8.0F / segments;
        float angle = number(effect, "angle", 90.0F) / segments;
        float offset = number(effect, "offset", 0.0F);
        float alpha = Math.max(0.0F, Math.min(1.0F, suitOpacity * number(effect, "opacity", 1.0F)));
        if (alpha <= 0.001F) return;

        VertexConsumer vertex = buffers.getBuffer(RenderType.entityTranslucent(texture));
        pose.pushPose();
        playerModel.head.translateAndRotate(pose);
        // Model resources express offset in blocks; the original renderer applies it in model pixels.
        pose.translate(0.0D, -0.5D, -offset);
        pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
        pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(angle));

        for (int i = 0; i < segments; ++i)
        {
            float vTop = 8.0F - segmentHeight * i;
            float vBottom = vTop - segmentHeight;
            emitQuad(vertex, pose, packedLight, alpha, segmentHeight, vTop, vBottom, false);
            emitQuad(vertex, pose, packedLight, alpha, segmentHeight, vTop, vBottom, true);

            if (i + 1 < segments)
            {
                pose.translate(0.0D, -segmentHeight, 0.0D);
                pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(angle));
            }
        }

        pose.popPose();
    }

    private static void emitQuad(VertexConsumer vertex, PoseStack pose, int light, float alpha,
            float height, float vTop, float vBottom, boolean backFace)
    {
        if (backFace)
        {
            vertex(vertex, pose, 4.0F, -height, 0.0F, 56.0F, vBottom, light, alpha, 0.0F, 0.0F, -1.0F);
            vertex(vertex, pose, -4.0F, -height, 0.0F, 64.0F, vBottom, light, alpha, 0.0F, 0.0F, -1.0F);
            vertex(vertex, pose, -4.0F, 0.0F, 0.0F, 64.0F, vTop, light, alpha, 0.0F, 0.0F, -1.0F);
            vertex(vertex, pose, 4.0F, 0.0F, 0.0F, 56.0F, vTop, light, alpha, 0.0F, 0.0F, -1.0F);
        }
        else
        {
            vertex(vertex, pose, 4.0F, -height, 0.0F, 56.0F, vBottom, light, alpha, 0.0F, 0.0F, 1.0F);
            vertex(vertex, pose, 4.0F, 0.0F, 0.0F, 56.0F, vTop, light, alpha, 0.0F, 0.0F, 1.0F);
            vertex(vertex, pose, -4.0F, 0.0F, 0.0F, 64.0F, vTop, light, alpha, 0.0F, 0.0F, 1.0F);
            vertex(vertex, pose, -4.0F, -height, 0.0F, 64.0F, vBottom, light, alpha, 0.0F, 0.0F, 1.0F);
        }
    }

    private static void vertex(VertexConsumer vertex, PoseStack pose, float x, float y, float z,
            float u, float v, int light, float alpha, float nx, float ny, float nz)
    {
        vertex.vertex(pose.last().pose(), x, y, z)
                .color(255, 255, 255, Math.round(alpha * 255.0F))
                .uv(u / 64.0F, v / 64.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(pose.last().normal(), nx, ny, nz)
                .endVertex();
    }

    private static boolean appliesToHelmet(JsonObject effect)
    {
        if (!effect.has("applicable") || !effect.get("applicable").isJsonArray()) return true;
        for (JsonElement entry : effect.getAsJsonArray("applicable"))
        {
            if (entry.isJsonPrimitive() && "HELMET".equalsIgnoreCase(entry.getAsString())) return true;
        }
        return false;
    }

    private static boolean passesConditionals(JsonObject effect, HeroModelData model, Player player)
    {
        if (!effect.has("conditionals") || !effect.get("conditionals").isJsonArray()) return true;
        for (JsonElement conditional : effect.getAsJsonArray("conditionals"))
        {
            if (!conditional.isJsonPrimitive()) return false;
            String expression = conditional.getAsString();
            if (expression.startsWith("vars:") && !model.evaluate(expression.substring("vars:".length()), player)) return false;
        }
        return true;
    }

    private static float number(JsonObject object, String key, float fallback)
    {
        try { return object.has(key) ? object.get(key).getAsFloat() : fallback; }
        catch (RuntimeException ignored) { return fallback; }
    }

    private static int integer(JsonObject object, String key, int fallback)
    {
        try { return object.has(key) ? object.get(key).getAsInt() : fallback; }
        catch (RuntimeException ignored) { return fallback; }
    }
}
