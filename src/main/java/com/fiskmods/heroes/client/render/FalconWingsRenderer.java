package com.fiskmods.heroes.client.render;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Segmented, animated Falcon flight wings from the original ModelFalconWings. */
final class FalconWingsRenderer
{
    private static final float[] BASE_Z = {1.5707964F, 0.62831855F, 1.0821041F, 1.1693707F,
            1.012291F, 1.1868238F, 1.2740904F, 1.2566371F};

    private FalconWingsRenderer() { }

    static void render(JsonObject effect, PoseStack pose, MultiBufferSource buffers, int packedLight,
            Player player, PlayerModel<?> model, HeroModelData suit, int slot, float partialTick)
    {
        if (!appliesToSlot(effect, slot) || !passesConditionals(effect, suit, player)) return;
        String textureKey = effect.has("texture") && effect.get("texture").isJsonPrimitive()
                ? effect.get("texture").getAsString() : null;
        ResourceLocation texture = textureKey != null ? suit.resolveCustomTexture(textureKey, player, slot) : null;
        if (texture == null) return;

        float unfold = Mth.clamp(suit.evaluateRenderData(
                effect.has("dataWings") ? effect.get("dataWings") : effect.get("data"), player, 0.0F), 0.0F, 1.0F);
        float shield = Mth.clamp(suit.evaluateRenderData(effect.get("dataShield"), player, 0.0F), 0.0F, 1.0F);
        float yOffset = effect.has("yOffset") ? effect.get("yOffset").getAsFloat() : 0.0F;
        float opacity = Mth.clamp(effect.has("opacity") ? effect.get("opacity").getAsFloat() : 1.0F, 0.0F, 1.0F);
        if (opacity <= 0.0F) return;

        VertexConsumer vertex = buffers.getBuffer(RenderType.entityTranslucent(texture));
        Wings motion = wingMotion(player, unfold, partialTick);
        pose.pushPose();
        anchor(model, effect.has("anchor") ? effect.get("anchor").getAsString() : "body").translateAndRotate(pose);
        pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);

        renderSide(pose, vertex, packedLight, opacity, unfold, 1.0F + unfold * 0.5F,
                shield, yOffset, 1, motion.left());
        renderSide(pose, vertex, packedLight, opacity, unfold,
                Math.max(1.0F + unfold * 0.5F, 1.0F + curve(shield) * 0.5F),
                shield, yOffset, -1, motion.right());
        pose.popPose();
    }

    private static void renderSide(PoseStack pose, VertexConsumer vertex, int light, float opacity,
            float wings, float scale, float shieldInput, float yOffset, int side, Wing motion)
    {
        float unfold = wings * wings;
        float shield = side < 0 ? curve(shieldInput) : 0.0F;
        float rootX = (yOffset + 2.0F) * side;
        float rootY = 2.0F;
        float rootZ = 1.8F;

        if (side < 0 && shield > 0.0F)
        {
            rootX -= 0.5F * shield;
            rootY += 2.0F * shield;
            rootZ += shield;
        }

        pose.pushPose();
        pose.translate(rootX, rootY - 1.5F * (1.0F - wings), rootZ + 1.5F * wings);
        pose.scale(scale, scale, scale);
        renderSegment(pose, vertex, light, opacity, unfold, shield, side, 0, yOffset, motion);
        pose.popPose();
    }

    private static void renderSegment(PoseStack pose, VertexConsumer vertex, int light, float opacity,
            float unfold, float shield, int side, int segment, float yOffset, Wing motion)
    {
        int sign = side;
        int width = (segment == 0 || segment == 7 ? 4 : 3) * sign;
        int progress = textureProgress(segment, side);
        float y = segment == 0 ? yOffset - 2.0F : 0.0F;

        pose.pushPose();
        if (segment > 0)
        {
            pose.translate((segment == 1 ? 4.0F : 3.0F) * sign, segment == 1 ? yOffset - 2.0F : 0.0F, 0.0F);
        }

        float angleX = -0.01F;
        float angleY = 0.0F;
        float angleZ = BASE_Z[segment] * sign;
        if (segment > 0 && side < 0)
        {
            float[] shieldFold = {0.0F, -1.2F, -1.1F, -0.4F, -0.3F, -0.1F, -0.1F, -0.1F};
            angleY += shieldFold[segment] * shield;
        }
        angleX *= 1.0F - unfold;
        angleZ *= 1.0F - unfold;
        if (side < 0 && shield > 0.0F)
        {
            float damp = 1.0F - shield;
            angleX *= damp;
            angleZ *= damp;
        }
        if (segment == 0)
        {
            angleX = motion.rotX() * (float) Math.sqrt(unfold);
            angleY += motion.rotY() * unfold * sign;
            angleZ = lerp(BASE_Z[0], motion.rotZ(), (float) Math.sqrt(unfold)) * sign;
            if (side < 0 && shield > 0.0F)
            {
                angleX -= 0.1F * shield;
                angleY += 0.1F * shield + 0.8F * Mth.sin((float) Math.PI * shield);
                angleZ += 0.3F * shield;
            }
        }
        else
        {
            float flexY = motion.flexY() / 8.0F * 1.8F * unfold * sign;
            float flexZ = motion.flexZ() / 8.0F * 1.8F;
            if (flexZ < 0.0F)
            {
                float flexProgress = 1.0F - segment / 8.0F;
                pose.translate(Mth.sin(flexZ * (float) Math.PI) * 3.5F * flexProgress * unfold * sign, 0.0F, 0.0F);
            }
            float flexCurve = Mth.sin((1.0F - segment / 8.0F) * (float) Math.PI / 2.0F);
            angleY += flexY * flexCurve;
            angleZ += flexZ * flexCurve * unfold * sign;
            angleX += flexZ * flexCurve * unfold * 0.04F;
        }
        pose.mulPose(Axis.ZP.rotation(angleZ));
        pose.mulPose(Axis.YP.rotation(angleY));
        pose.mulPose(Axis.XP.rotation(angleX));

        float topY = segment == 0 ? y + 4.0F : 4.0F;
        quadBothSides(pose, vertex, light, opacity,
                0.0F, topY, 0.0F, width, topY, 0.0F, width, y, 0.0F, 0.0F, y, 0.0F,
                (progress - width) / 32.0F, (8.0F * (side < 0 ? 0 : 1) + 4.0F) / 16.0F,
                progress / 32.0F, (8.0F * (side < 0 ? 0 : 1) + 4.0F) / 16.0F,
                progress / 32.0F, (8.0F * (side < 0 ? 0 : 1)) / 16.0F,
                (progress - width) / 32.0F, (8.0F * (side < 0 ? 0 : 1)) / 16.0F);

        renderBottom(pose, vertex, light, opacity, unfold, shield, side, segment, yOffset, width, progress);

        if (segment < 7) renderSegment(pose, vertex, light, opacity, unfold, shield, side,
                segment + 1, yOffset, motion);
        pose.popPose();
    }

    private static void renderBottom(PoseStack parent, VertexConsumer vertex, int light, float opacity,
            float unfold, float shield, int side, int segment, float yOffset, int width, int progress)
    {
        parent.pushPose();
        float sign = side;
        float pivotX = segment == 0 ? 4.0F * sign : 3.0F * sign;
        float pivotY = segment == 0 ? yOffset - 2.0F : 0.0F;
        float pivotZ = 0.0F;
        if (segment == 7 && side > 0) pivotX += sign;
        pivotY += 4.0F * unfold;
        float fold = side < 0 ? shield : 0.0F;
        float angleX = 0.1F * (side < 0 ? 1.0F - fold : 1.0F);
        if (side < 0 && shield > 0.0F)
        {
            float targetY = segment == 0 ? 2.0F : 4.0F;
            pivotY += (targetY - pivotY) * shield;
        }
        parent.translate(pivotX, pivotY, pivotZ);
        parent.mulPose(Axis.XP.rotation(angleX));
        int bottomWidth = -width;
        int vStart = 8 * (side < 0 ? 0 : 1) + 4;
        int vEnd = 8 * (side < 0 ? 0 : 1) + 8;
        quadBothSides(parent, vertex, light, opacity,
                0.0F, 4.0F, 0.0F, bottomWidth, 4.0F, 0.0F,
                bottomWidth, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
                progress / 32.0F, vEnd / 16.0F, (progress - width) / 32.0F, vEnd / 16.0F,
                (progress - width) / 32.0F, vStart / 16.0F, progress / 32.0F, vStart / 16.0F);
        parent.popPose();
    }

    private static void quadBothSides(PoseStack pose, VertexConsumer vertex, int light, float opacity,
            float x0, float y0, float z0, float x1, float y1, float z1,
            float x2, float y2, float z2, float x3, float y3, float z3,
            float u0, float v0, float u1, float v1, float u2, float v2, float u3, float v3)
    {
        var matrix = pose.last();
        int alpha = Mth.clamp(Math.round(opacity * 255.0F), 0, 255);
        vertex(matrix, vertex, x0, y0, z0, u0, v0, light, alpha, 0.0F, 0.0F, 1.0F);
        vertex(matrix, vertex, x1, y1, z1, u1, v1, light, alpha, 0.0F, 0.0F, 1.0F);
        vertex(matrix, vertex, x2, y2, z2, u2, v2, light, alpha, 0.0F, 0.0F, 1.0F);
        vertex(matrix, vertex, x3, y3, z3, u3, v3, light, alpha, 0.0F, 0.0F, 1.0F);
        vertex(matrix, vertex, x3, y3, z3, u3, v3, light, alpha, 0.0F, 0.0F, -1.0F);
        vertex(matrix, vertex, x2, y2, z2, u2, v2, light, alpha, 0.0F, 0.0F, -1.0F);
        vertex(matrix, vertex, x1, y1, z1, u1, v1, light, alpha, 0.0F, 0.0F, -1.0F);
        vertex(matrix, vertex, x0, y0, z0, u0, v0, light, alpha, 0.0F, 0.0F, -1.0F);
    }

    private static void vertex(PoseStack.Pose matrix, VertexConsumer vertex,
            float x, float y, float z, float u, float v, int light, int alpha, float nx, float ny, float nz)
    {
        vertex.vertex(matrix.pose(), x, y, z).color(255, 255, 255, alpha).uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(matrix.normal(), nx, ny, nz).endVertex();
    }

    private static int textureProgress(int segment, int side)
    {
        int progress = side < 0 ? 26 : 0;
        for (int i = 0; i <= segment; ++i)
        {
            int width = (i == 0 || i == 7 ? 4 : 3) * side;
            progress += width;
        }
        return progress;
    }

    private static Wings wingMotion(Player player, float wings, float partialTick)
    {
        Vec3 velocity = player.getDeltaMovement();
        float yaw = (Mth.rotLerp(partialTick, player.yRotO, player.getYRot())
                - Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot)) / 180.0F;
        float active = player.isFallFlying() || wings > 0.0F ? 1.0F - wings : 1.0F;
        float vertical = 0.0F;
        float horizontal = (float) Math.sqrt(curve(Math.min((float) Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z) / 2.0F, 1.0F)));
        if (velocity.y != 0.0D)
        {
            float direction = velocity.y < 0.0D ? -1.0F : 1.0F;
            vertical = (float) Math.sqrt(curve(Math.min((float) Math.abs(velocity.y) / 2.0F, 1.0F))) / 3.0F * direction;
        }
        float down = 1.0F;
        if (velocity.y < 0.0D)
        {
            Vec3 normalized = velocity.normalize();
            down -= (float) Math.pow(Math.max(0.0D, -normalized.y), 1.5D) * active;
        }
        float swing = Mth.sin((float) Math.sqrt(player.getAttackAnim(partialTick)) * (float) Math.PI * 2.0F) * 0.18F;
        float rotX = lerp(0.2F + Math.abs(vertical) * 0.8F, -0.2F, active);
        float rotZ = (float) (Math.PI / 2.0D) * (1.0F - down) - lerp(0.15F - vertical * 0.9F, 0.4F, active);
        float rotYRight = ((1.0F - active) * horizontal * 5.0F - 1.0F + yaw * 2.0F) * 0.1F + swing;
        float rotYLeft = ((1.0F - active) * horizontal * 5.0F - 1.0F - yaw * 2.0F) * 0.1F - swing;
        float flexRight = lerp(0.22222F + vertical * 1.33333F + horizontal * 0.88888F,
                -0.333333F + yaw * 0.88888F, active);
        float flexLeft = lerp(0.22222F + vertical * 1.33333F + horizontal * 0.88888F,
                -0.333333F - yaw * 0.88888F, active);
        float flexZ = lerp(Math.max(vertical * 4.44444F, 0.0F) - 0.333333F, 0.444444F, active);
        return new Wings(new Wing(rotX, rotYRight, rotZ, flexRight, flexZ),
                new Wing(rotX, rotYLeft, rotZ, flexLeft, flexZ));
    }

    private static float curve(float value)
    {
        return (Mth.sin((value * 2.0F - 1.0F) * (float) Math.PI / 2.0F) + 1.0F) / 2.0F;
    }

    private static float lerp(float from, float to, float amount)
    {
        return from + (to - from) * amount;
    }

    private static ModelPart anchor(PlayerModel<?> model, String name)
    {
        return switch (name.toLowerCase(java.util.Locale.ROOT))
        {
            case "head" -> model.head;
            case "rightarm" -> model.rightArm;
            case "leftarm" -> model.leftArm;
            case "rightleg" -> model.rightLeg;
            case "leftleg" -> model.leftLeg;
            default -> model.body;
        };
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

    private record Wing(float rotX, float rotY, float rotZ, float flexY, float flexZ) { }
    private record Wings(Wing right, Wing left) { }
}
