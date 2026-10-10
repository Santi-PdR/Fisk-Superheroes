package com.fiskmods.heroes.client.render;

import com.fiskmods.heroes.FiskHeroes;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    private record SpriteAnimation(List<Integer> frames, int sheetFrames) { }
    private static final Map<ResourceLocation, SpriteAnimation> ANIMATIONS = new HashMap<>();

    private BoosterFlameRenderer() { }

    static void clear()
    {
        ANIMATIONS.clear();
    }

    static void render(JsonObject effect, PoseStack pose, MultiBufferSource buffers, Player player,
            PlayerModel<?> model, HeroModelData suit, int slot, float partialTick)
    {
        if (!appliesToSlot(effect, slot) || !passesConditionals(effect, suit, player)) return;
        String iconTemplate = effect.has("icon") ? effect.get("icon").getAsString() : null;
        if (iconTemplate == null || !iconTemplate.contains("%s")) return;

        float progress = suit.evaluateRenderData(effect.get("data"), player, 0.0F);
        if (!(progress > 0.0F)) return;
        progress = Mth.clamp(progress, 0.0F, 1.0F);
        float eased = progress * progress * (3.0F - 2.0F * progress);
        float opacity = effect.has("opacity") ? effect.get("opacity").getAsFloat() : 1.0F;
        JsonArray size = vector(effect.get("scale"), 2, 2.0F, 4.0F);
        JsonArray offset = vector(effect.get("offset"), 3, 0.0F, 0.0F, 0.0F);
        JsonArray rotation = vector(effect.get("rotation"), 3, 0.0F, 0.0F, 0.0F);
        String anchorName = effect.has("anchor") ? effect.get("anchor").getAsString() : "body";
        ModelPart anchor = HeroSuitAnchor.get(model, anchorName);
        if (anchor == null) return;

        double velocity = Math.min(8.0D, player.getDeltaMovement().length());
        float flutter = (float) Math.sin((player.tickCount + partialTick) * 3.0F) / 15.0F;
        float multiplier = eased * ((float) velocity * 2.0F + number(size, 1, 4.0F)) - flutter / 3.0F;
        if (multiplier <= 0.0F) return;
        float width = Math.max(0.0F, (1.0F + flutter) * number(size, 0, 2.0F));
        float alpha = Mth.clamp(opacity * Math.max(0.6F, 1.0F - (float) velocity / 5.0F), 0.0F, 1.0F);

        int tick = player.tickCount + (partialTick >= 0.5F ? 1 : 0);
        ResourceLocation layer0 = icon(iconTemplate, 0);
        ResourceLocation layer1 = icon(iconTemplate, 1);
        drawAt(anchor, pose, buffers, layer0, width, multiplier, alpha, tick, partialTick,
                offset, rotation, false);

        if (effect.has("mirror") && effect.get("mirror").getAsBoolean())
        {
            String oppositeName = opposite(anchorName);
            ModelPart opposite = HeroSuitAnchor.get(model, oppositeName);
            if (opposite == anchor)
            {
                JsonArray mirroredOffset = copyVector(offset);
                mirroredOffset.set(0, new com.google.gson.JsonPrimitive(-number(offset, 0, 0.0F)));
                JsonArray mirroredRotation = copyVector(rotation);
                mirroredRotation.set(1, new com.google.gson.JsonPrimitive(-number(rotation, 1, 0.0F)));
                mirroredRotation.set(2, new com.google.gson.JsonPrimitive(-number(rotation, 2, 0.0F)));
                drawAt(anchor, pose, buffers, layer0, width, multiplier, alpha, tick, partialTick,
                        mirroredOffset, mirroredRotation, true);
                if (layer1 != null) drawAt(anchor, pose, buffers, layer1, width * 0.88F, multiplier, alpha * 0.8F,
                        tick, partialTick, mirroredOffset, mirroredRotation, false);
            }
            else if (opposite != null)
            {
                JsonArray mirroredOffset = copyVector(offset);
                mirroredOffset.set(0, new com.google.gson.JsonPrimitive(-number(offset, 0, 0.0F)));
                drawAt(opposite, pose, buffers, layer0, width, multiplier, alpha, tick, partialTick,
                        mirroredOffset, rotation, false);
                if (layer1 != null) drawAt(opposite, pose, buffers, layer1, width * 0.88F, multiplier, alpha * 0.8F,
                        tick, partialTick, mirroredOffset, rotation, true);
            }
        }
        if (layer1 != null) drawAt(anchor, pose, buffers, layer1, width * 0.88F, multiplier, alpha * 0.8F,
                tick, partialTick, offset, rotation, true);
    }

    private static void drawAt(ModelPart anchor, PoseStack pose, MultiBufferSource buffers, ResourceLocation texture,
            float width, float length, float alpha, int tick, float partialTick,
            JsonArray offset, JsonArray rotation, boolean crossPlane)
    {
        if (texture == null || width <= 0.0F || length <= 0.0F) return;
        SpriteAnimation animation = animation(texture);
        int frame = animation.frames().get(Math.floorMod(tick, animation.frames().size()));
        float v0 = (float) frame / animation.sheetFrames();
        float v1 = (float) (frame + 1) / animation.sheetFrames();

        pose.pushPose();
        anchor.translateAndRotate(pose);
        pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
        pose.translate(-number(offset, 0, 0.0F), number(offset, 1, 0.0F), number(offset, 2, 0.0F));
        pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(number(rotation, 2, 0.0F)));
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(number(rotation, 1, 0.0F)));
        pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(number(rotation, 0, 0.0F)));
        if (crossPlane) pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F + (tick + partialTick) * 9.0F));

        VertexConsumer vertex = buffers.getBuffer(RenderType.entityTranslucent(texture));
        float half = width * 0.5F;
        quad(vertex, pose, -half, 0.0F, 0.0F, half, 0.0F, 0.0F,
                half * 0.25F, length, 0.0F, -half * 0.25F, length, 0.0F, v0, v1, alpha);
        pose.popPose();
    }

    private static void quad(VertexConsumer vertex, PoseStack pose, float x0, float y0, float z0,
            float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3,
            float v0, float v1, float alpha)
    {
        var matrix = pose.last();
        int a = Math.round(255.0F * alpha);
        vertex.vertex(matrix.pose(), x0, y0, z0).color(255, 255, 255, a).uv(0.0F, v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(matrix.normal(), 0, 0, 1).endVertex();
        vertex.vertex(matrix.pose(), x1, y1, z1).color(255, 255, 255, a).uv(1.0F, v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(matrix.normal(), 0, 0, 1).endVertex();
        vertex.vertex(matrix.pose(), x2, y2, z2).color(255, 255, 255, a).uv(1.0F, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(matrix.normal(), 0, 0, 1).endVertex();
        vertex.vertex(matrix.pose(), x3, y3, z3).color(255, 255, 255, a).uv(0.0F, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(matrix.normal(), 0, 0, 1).endVertex();
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

    private static SpriteAnimation animation(ResourceLocation texture)
    {
        return ANIMATIONS.computeIfAbsent(texture, BoosterFlameRenderer::loadAnimation);
    }

    private static SpriteAnimation loadAnimation(ResourceLocation texture)
    {
        ResourceLocation metadata = new ResourceLocation(texture.getNamespace(), texture.getPath() + ".mcmeta");
        try
        {
            Resource resource = Minecraft.getInstance().getResourceManager().getResource(metadata).orElseThrow();
            try (InputStreamReader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8))
            {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                JsonArray source = json.getAsJsonObject("animation").getAsJsonArray("frames");
                List<Integer> frames = new ArrayList<>();
                int max = 0;
                for (JsonElement frame : source)
                {
                    int index = frame.isJsonObject() ? frame.getAsJsonObject().get("index").getAsInt() : frame.getAsInt();
                    frames.add(index);
                    max = Math.max(max, index);
                }
                if (!frames.isEmpty()) return new SpriteAnimation(List.copyOf(frames), max + 1);
            }
        }
        catch (Exception ignored)
        {
            // Static custom icons still render as a single full-height frame.
        }
        return new SpriteAnimation(List.of(0), 1);
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

    private static JsonArray vector(JsonElement element, int size, float... defaults)
    {
        if (element != null && element.isJsonArray() && element.getAsJsonArray().size() >= size) return element.getAsJsonArray();
        JsonArray result = new JsonArray();
        for (int i = 0; i < size; ++i) result.add(defaults[i]);
        return result;
    }

    private static JsonArray copyVector(JsonArray value)
    {
        return value.deepCopy();
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

    private static final class HeroSuitAnchor
    {
        private static ModelPart get(PlayerModel<?> model, String name)
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
