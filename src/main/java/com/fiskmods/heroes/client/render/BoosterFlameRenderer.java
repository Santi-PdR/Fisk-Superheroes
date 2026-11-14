package com.fiskmods.heroes.client.render;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.util.AnimatedSpriteFrames;
import com.mojang.blaze3d.platform.NativeImage;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/** Faithful animated fire plumes for suit model booster effects. */
final class BoosterFlameRenderer
{
    private static final Map<ResourceLocation, AnimatedSpriteFrames> ANIMATIONS = new HashMap<>();
    private static final Map<String, ResourceLocation[]> ICONS = new HashMap<>();

    private BoosterFlameRenderer() { }

    static void clear()
    {
        ANIMATIONS.clear();
        ICONS.clear();
    }

    static void render(JsonObject effect, PoseStack pose, MultiBufferSource buffers, Player player,
            PlayerModel<?> model, HeroModelData suit, int slot, float partialTick)
    {
        if (!appliesToSlot(effect, slot) || !passesConditionals(effect, suit, player)) return;
        String iconTemplate = effect.has("icon") ? effect.get("icon").getAsString() : null;
        if (iconTemplate == null || !iconTemplate.contains("%s")) return;

        float progress = suit.evaluateRenderData(effect.get("data"), player, 0.0F);
        if (!(progress > 0.0F)) return;
        JsonArray size = effect.has("scale") && effect.get("scale").isJsonArray() ? effect.getAsJsonArray("scale") : null;
        JsonArray offset = effect.has("offset") && effect.get("offset").isJsonArray() ? effect.getAsJsonArray("offset") : null;
        JsonArray rotation = effect.has("rotation") && effect.get("rotation").isJsonArray() ? effect.getAsJsonArray("rotation") : null;
        renderPlume(pose, buffers, player, model, partialTick, iconTemplate,
                effect.has("anchor") ? effect.get("anchor").getAsString() : "body", progress,
                effect.has("opacity") ? effect.get("opacity").getAsFloat() : 1.0F,
                effect.has("mirror") && effect.get("mirror").getAsBoolean(),
                number(offset, 0, 0), number(offset, 1, 0), number(offset, 2, 0),
                number(rotation, 0, 0), number(rotation, 1, 0), number(rotation, 2, 0),
                number(size, 0, 2), number(size, 1, 4));
    }

    /** Shared primitive-based path avoids constructing temporary JSON and vector arrays per plume/frame. */
    static void renderPlume(PoseStack pose, MultiBufferSource buffers, Player player, PlayerModel<?> model,
            float partialTick, String iconTemplate, String anchorName, float progress, float opacity,
            boolean mirror, float ox, float oy, float oz, float rx, float ry, float rz, float sizeX, float sizeY)
    {
        if (!(progress > 0.0F)) return;
        ModelPart anchor = HeroSuitAnchor.get(model, anchorName);
        if (anchor == null) return;
        progress = Mth.clamp(progress, 0.0F, 1.0F);
        float eased = progress * progress * (3.0F - 2.0F * progress);
        double velocity = Math.min(8.0D, player.getDeltaMovement().length());
        float flutter = (float) Math.sin((player.tickCount + partialTick) * 3.0F) / 15.0F;
        float multiplier = eased * ((float) velocity * 2.0F + sizeY) - flutter / 3.0F;
        float width = Math.max(0.0F, (1.0F + flutter) * sizeX);
        // The legacy fire mesh scales every axis by its width, including height.
        float length = width * multiplier;
        if (length <= 0.0F) return;
        float alpha = Mth.clamp(opacity * Math.max(0.6F, 1.0F - (float) velocity / 5.0F), 0.0F, 1.0F);
        int tick = player.tickCount + (partialTick >= 0.5F ? 1 : 0);
        ResourceLocation[] icons = ICONS.computeIfAbsent(iconTemplate,
                template -> new ResourceLocation[] { icon(template, 0), icon(template, 1) });
        ResourceLocation layer0 = icons[0], layer1 = icons[1];
        drawAt(anchor, pose, buffers, layer0, width, length, alpha, tick, partialTick, ox, oy, oz, rx, ry, rz, false);
        if (mirror)
        {
            ModelPart opposite = HeroSuitAnchor.get(model, opposite(anchorName));
            if (opposite == anchor)
            {
                drawAt(anchor, pose, buffers, layer0, width, length, alpha, tick, partialTick, -ox, oy, oz, rx, -ry, -rz, true);
                if (layer1 != null) drawAt(anchor, pose, buffers, layer1, width * 0.88F, length, alpha * 0.8F,
                        tick, partialTick, -ox, oy, oz, rx, -ry, -rz, false);
            }
            else if (opposite != null)
            {
                drawAt(opposite, pose, buffers, layer0, width, length, alpha, tick, partialTick, -ox, oy, oz, rx, ry, rz, false);
                if (layer1 != null) drawAt(opposite, pose, buffers, layer1, width * 0.88F, length, alpha * 0.8F,
                        tick, partialTick, -ox, oy, oz, rx, ry, rz, true);
            }
        }
        if (layer1 != null) drawAt(anchor, pose, buffers, layer1, width * 0.88F, length, alpha * 0.8F,
                tick, partialTick, ox, oy, oz, rx, ry, rz, true);
    }

    /** Legacy suit fire is a wrapping mesh in block units, not a tiny booster billboard. */
    static void renderSuitFire(PoseStack pose, MultiBufferSource buffers, Player player, PlayerModel<?> model,
            float partialTick, String anchorName, float offsetY, float width, float height, float opacity)
    {
        ModelPart anchor = HeroSuitAnchor.get(model, anchorName);
        if (anchor == null || width <= 0 || height <= 0 || opacity <= 0) return;
        ResourceLocation[] icons = ICONS.computeIfAbsent("fiskheroes:fire_layer_%s",
                template -> new ResourceLocation[] { icon(template, 0), icon(template, 1) });
        int tick = player.tickCount + (partialTick >= .5F ? 1 : 0);
        pose.pushPose();
        anchor.translateAndRotate(pose);
        pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
        pose.translate(0, offsetY, 0);
        for (int layer = 0; layer < 2; ++layer)
        {
            ResourceLocation texture = icons[layer];
            AnimatedSpriteFrames animation = animation(texture);
            int frame = animation.frameAt(tick);
            VertexConsumer vertex = buffers.getBuffer(RenderType.entityTranslucentEmissive(texture));
            float u0 = animation.u0(frame), u1 = animation.u1(frame);
            float v0 = animation.v0(frame), v1 = animation.v1(frame);
            float a = width * .2F, b = width * .3F, edge = width * .5F;
            // Four offset, slanted planes per layer wrap around the model instead of
            // disappearing inside its opaque head or arm. Negative Y rises in model space.
            fireQuad(vertex, pose, a, edge, -b, edge, -edge, height, u0, u1, v0, v1, opacity, false);
            fireQuad(vertex, pose, -a, -edge, b, -edge, edge, height, u0, u1, v0, v1, opacity, false);
            fireQuad(vertex, pose, a, edge, -b, edge, -edge, height, u0, u1, v0, v1, opacity, true);
            fireQuad(vertex, pose, -a, -edge, b, -edge, edge, height, u0, u1, v0, v1, opacity, true);
        }
        pose.popPose();
    }

    private static void fireQuad(VertexConsumer vertex, PoseStack pose, float lower, float start, float upper,
            float end, float otherEnd, float height, float u0, float u1, float v0, float v1, float alpha, boolean swap)
    {
        if (!swap)
            quad(vertex, pose, lower, 0, start, lower, 0, otherEnd, upper, -height, otherEnd, upper, -height, end,
                    u0, u1, v0, v1, alpha);
        else
            quad(vertex, pose, start, 0, lower, otherEnd, 0, lower, otherEnd, -height, upper, end, -height, upper,
                    u0, u1, v0, v1, alpha);
    }

    private static void drawAt(ModelPart anchor, PoseStack pose, MultiBufferSource buffers, ResourceLocation texture,
            float width, float length, float alpha, int tick, float partialTick,
            float ox, float oy, float oz, float rx, float ry, float rz, boolean crossPlane)
    {
        if (texture == null || width <= 0.0F || length <= 0.0F) return;
        AnimatedSpriteFrames animation = animation(texture);
        int frame = animation.frameAt(tick);
        float u0 = animation.u0(frame), u1 = animation.u1(frame);
        float v0 = animation.v0(frame), v1 = animation.v1(frame);
        pose.pushPose();
        anchor.translateAndRotate(pose);
        pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
        pose.translate(-ox, oy, oz);
        pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(rz));
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(ry));
        pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(rx));
        if (crossPlane) pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F + (tick + partialTick) * 9.0F));
        VertexConsumer vertex = buffers.getBuffer(RenderType.entityTranslucent(texture));
        float half = width * 0.5F;
        quad(vertex, pose, -half, 0.0F, 0.0F, half, 0.0F, 0.0F,
                half * 0.25F, length, 0.0F, -half * 0.25F, length, 0.0F, u0, u1, v0, v1, alpha);
        pose.popPose();
    }

    private static void quad(VertexConsumer vertex, PoseStack pose, float x0, float y0, float z0,
            float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3,
            float u0, float u1, float v0, float v1, float alpha)
    {
        var matrix = pose.last();
        int a = Math.round(255.0F * alpha);
        vertex.vertex(matrix.pose(), x0, y0, z0).color(255, 255, 255, a).uv(u0, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(matrix.normal(), 0, 0, 1).endVertex();
        vertex.vertex(matrix.pose(), x1, y1, z1).color(255, 255, 255, a).uv(u1, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(matrix.normal(), 0, 0, 1).endVertex();
        vertex.vertex(matrix.pose(), x2, y2, z2).color(255, 255, 255, a).uv(u1, v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(matrix.normal(), 0, 0, 1).endVertex();
        vertex.vertex(matrix.pose(), x3, y3, z3).color(255, 255, 255, a).uv(u0, v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(matrix.normal(), 0, 0, 1).endVertex();
    }

    private static ResourceLocation icon(String template, int layer)
    {
        String value = template.replace("%s", Integer.toString(layer));
        ResourceLocation parsed = ResourceLocation.tryParse(value);
        if (parsed == null) parsed = FiskHeroes.id(value);
        String path = parsed.getPath();
        if (!path.startsWith("textures/")) path = "textures/icons/" + path;
        if (!path.endsWith(".png")) path += ".png";
        return new ResourceLocation(parsed.getNamespace(), path);
    }

    private static AnimatedSpriteFrames animation(ResourceLocation texture)
    {
        return ANIMATIONS.computeIfAbsent(texture, BoosterFlameRenderer::loadAnimation);
    }

    private static AnimatedSpriteFrames loadAnimation(ResourceLocation texture)
    {
        ResourceLocation metadata = new ResourceLocation(texture.getNamespace(), texture.getPath() + ".mcmeta");
        try
        {
            var manager = Minecraft.getInstance().getResourceManager();
            Resource resource = manager.getResource(metadata).orElseThrow();
            try (InputStreamReader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8);
                    var imageStream = manager.getResource(texture).orElseThrow().open();
                    NativeImage image = NativeImage.read(imageStream))
            {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                JsonObject metadataAnimation = root.has("animation") && root.get("animation").isJsonObject()
                        ? root.getAsJsonObject("animation") : null;
                AnimatedSpriteFrames result = AnimatedSpriteFrames.parse(metadataAnimation, image.getWidth(), image.getHeight());
                if (Boolean.getBoolean("fiskheroes.ciParticleDiagnostics"))
                    FiskHeroes.LOGGER.info("Booster sprite animation: texture={}, columns={}, rows={}, frames={}, first={}, second={}",
                            texture, result.columns(), result.rows(), result.frameCount(), result.frameAt(0), result.frameAt(1));
                return result;
            }
        }
        catch (Exception ignored)
        {
            // Static custom icons still render as a single full-image frame.
        }
        return AnimatedSpriteFrames.single();
    }

    private static boolean appliesToSlot(JsonObject effect, int slot)
    {
        if (!effect.has("applicable") || !effect.get("applicable").isJsonArray()) return true;
        for (JsonElement item : effect.getAsJsonArray("applicable"))
        {
            if (HeroModelData.slot(item.getAsString()) == slot) return true;
        }
        return false;
    }

    private static boolean passesConditionals(JsonObject effect, HeroModelData model, Player player)
    {
        if (!effect.has("conditionals") || !effect.get("conditionals").isJsonArray()) return true;
        for (JsonElement conditional : effect.getAsJsonArray("conditionals"))
        {
            if (conditional.isJsonPrimitive() && conditional.getAsString().startsWith("vars:")
                    && !model.evaluate(conditional.getAsString().substring("vars:".length()), player)) return false;
        }
        return true;
    }

    private static float number(JsonArray array, int index, float fallback)
    {
        return array != null && index < array.size() && array.get(index).isJsonPrimitive() ? array.get(index).getAsFloat() : fallback;
    }

    private static String opposite(String part)
    {
        return switch (part.toLowerCase(java.util.Locale.ROOT))
        {
            case "rightarm" -> "leftArm";
            case "leftarm" -> "rightArm";
            case "rightleg" -> "leftLeg";
            case "leftleg" -> "rightLeg";
            default -> part;
        };
    }

    static final class HeroSuitAnchor
    {
        static ModelPart get(PlayerModel<?> model, String name)
        {
            return switch (name.toLowerCase(java.util.Locale.ROOT))
            {
                case "head" -> model.head;
                case "headwear", "hat" -> model.hat;
                case "rightarm" -> model.rightArm;
                case "leftarm" -> model.leftArm;
                case "rightleg" -> model.rightLeg;
                case "leftleg" -> model.leftLeg;
                default -> model.body;
            };
        }
    }
}
