package com.fiskmods.heroes.client.render;

import com.fiskmods.heroes.client.render.HeroModelData;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Draws the animated membrane between a hero's torso and arms. */
final class WingsuitRenderer
{
    private static final float TEXTURE_WIDTH = 0.390625F;
    private static final float TEXTURE_HEIGHT = 0.5F;

    private WingsuitRenderer() { }

    static void render(JsonObject effect, PoseStack pose, MultiBufferSource buffers, int packedLight,
            Player player, PlayerModel<?> model, HeroModelData suit, int slot)
    {
        if (!appliesToSlot(effect, slot) || !passesConditionals(effect, suit, player)) return;

        String textureKey = effect.has("texture") && effect.get("texture").isJsonPrimitive()
                ? effect.get("texture").getAsString() : null;
        ResourceLocation texture = textureKey != null ? suit.resolveCustomTexture(textureKey, player, slot) : null;
        if (texture == null) return;

        float unfold = Mth.clamp(suit.evaluateRenderData(effect.get("data"), player, 1.0F), 0.0F, 1.0F);
        float opacity = Mth.clamp(effect.has("opacity") ? effect.get("opacity").getAsFloat() : 1.0F, 0.0F, 1.0F);
        if (unfold <= 0.0F || opacity <= 0.0F) return;

        // Compute all six corners through the live body/arm poses, so the fabric follows
        // ordinary player animation as well as the suit's wing-fold timer.
        Vector3f[] body = {
                transformedPoint(pose, model.body, -4.0F, 17.0F, 0.0F),
                transformedPoint(pose, model.body, 4.0F, 17.0F, 0.0F)
        };
        ModelPart[] arms = {model.rightArm, model.leftArm};
        VertexConsumer vertex = buffers.getBuffer(RenderType.entityTranslucent(texture));
        Matrix4f identity = new Matrix4f();
        Matrix3f identityNormal = new Matrix3f();

        for (int i = 0; i < arms.length; ++i)
        {
            float side = i == 0 ? 1.0F : -1.0F;
            Vector3f shoulder = transformedPoint(pose, arms[i], side, -1.0F, 0.0F);
            Vector3f wrist = transformedPoint(pose, arms[i], side, 8.0F, 0.0F);
            float u = i == 0 ? 0.0F : TEXTURE_WIDTH;
            float uEnd = u + (i == 0 ? TEXTURE_WIDTH : -TEXTURE_WIDTH);

            if (unfold < 1.0F)
            {
                Vector3f middle = new Vector3f(wrist).add(body[i]).mul(0.5F);
                middle.lerp(shoulder, 1.0F - unfold);
                triangleBothSides(vertex, identity, identityNormal, shoulder, wrist, middle,
                        u, 0.0F, u, TEXTURE_HEIGHT, u + (uEnd - u) * 0.5F, TEXTURE_HEIGHT,
                        packedLight, opacity);
                triangleBothSides(vertex, identity, identityNormal, middle, body[i], shoulder,
                        u + (uEnd - u) * 0.5F, TEXTURE_HEIGHT, uEnd, TEXTURE_HEIGHT, u, 0.0F,
                        packedLight, opacity);
            }
            else
            {
                triangleBothSides(vertex, identity, identityNormal, shoulder, wrist, body[i],
                        u, 0.0F, u, TEXTURE_HEIGHT, uEnd, TEXTURE_HEIGHT, packedLight, opacity);
            }
        }
    }

    private static Vector3f transformedPoint(PoseStack parent, ModelPart part, float x, float y, float z)
    {
        parent.pushPose();
        part.translateAndRotate(parent);
        parent.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
        Vector3f point = new Vector3f(x, y, z);
        parent.last().pose().transformPosition(point);
        parent.popPose();
        return point;
    }

    private static void triangleBothSides(VertexConsumer vertex, Matrix4f pose, Matrix3f normalMatrix,
            Vector3f a, Vector3f b, Vector3f c, float au, float av, float bu, float bv, float cu, float cv,
            int packedLight, float opacity)
    {
        Vector3f normal = new Vector3f(b).sub(a).cross(new Vector3f(c).sub(a)).normalize();
        float alpha = opacity * 255.0F;
        point(vertex, pose, normalMatrix, a, au, av, normal, packedLight, alpha);
        point(vertex, pose, normalMatrix, b, bu, bv, normal, packedLight, alpha);
        point(vertex, pose, normalMatrix, c, cu, cv, normal, packedLight, alpha);
        normal.negate();
        point(vertex, pose, normalMatrix, c, cu, cv, normal, packedLight, alpha);
        point(vertex, pose, normalMatrix, b, bu, bv, normal, packedLight, alpha);
        point(vertex, pose, normalMatrix, a, au, av, normal, packedLight, alpha);
    }

    private static void point(VertexConsumer vertex, Matrix4f pose, Matrix3f normalMatrix, Vector3f point,
            float u, float v, Vector3f normal, int packedLight, float alpha)
    {
        vertex.vertex(pose, point.x, point.y, point.z)
                .color(255, 255, 255, Mth.clamp(Math.round(alpha), 0, 255))
                .uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight)
                .normal(normalMatrix, normal.x, normal.y, normal.z).endVertex();
    }

    private static boolean appliesToSlot(JsonObject effect, int slot)
    {
        if (!effect.has("applicable") || !effect.get("applicable").isJsonArray()) return true;
        for (JsonElement entry : effect.getAsJsonArray("applicable"))
        {
            if (HeroModelData.slot(entry.getAsString()) == slot) return true;
        }
        return false;
    }

    private static boolean passesConditionals(JsonObject effect, HeroModelData suit, Player player)
    {
        if (!effect.has("conditionals") || !effect.get("conditionals").isJsonArray()) return true;
        for (JsonElement entry : effect.getAsJsonArray("conditionals"))
        {
            if (entry.isJsonPrimitive() && entry.getAsString().startsWith("vars:")
                    && !suit.evaluate(entry.getAsString().substring("vars:".length()), player)) return false;
        }
        return true;
    }
}
