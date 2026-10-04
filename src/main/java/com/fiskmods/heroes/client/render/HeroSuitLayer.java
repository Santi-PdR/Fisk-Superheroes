package com.fiskmods.heroes.client.render;

import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.hero.HeroTracker;
import com.fiskmods.heroes.common.hero.HeroIteration;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Draws the suit a player is wearing on top of the vanilla player model.
 * <p>
 * Each armour piece of the suit has its own texture (the original mod's {@code layer1}/{@code layer2}
 * scheme) and its own set of visible model parts, both taken from the hero's model JSON. The result
 * is the same layered look the 1.7.10 version produced, built here on top of the modern render
 * pipeline.
 */
public class HeroSuitLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>
{
    private static final ResourceLocation[] SLOT_NAMES = {
            new ResourceLocation("fiskheroes", "helmet"),
            new ResourceLocation("fiskheroes", "chestplate"),
            new ResourceLocation("fiskheroes", "leggings"),
            new ResourceLocation("fiskheroes", "boots")
    };

    public HeroSuitLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent)
    {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch)
    {
        SHPlayerData data = SHDataCapabilities.getPlayer(player);

        if (data == null)
        {
            return;
        }

        HeroIteration iteration = HeroTracker.getWornSuit(player);

        if (iteration == null)
        {
            iteration = data.getHero();
        }

        if (iteration == null)
        {
            return;
        }

        HeroModelData model = HeroModelRegistry.get(iteration);

        if (model == null)
        {
            return;
        }

        PlayerModel<AbstractClientPlayer> playerModel = getParentModel();
        float scale = com.fiskmods.heroes.common.data.var.Vars.getScale(player);
        ParticleEmitterRenderer.tick(player, playerModel, model, scale);

        poseStack.pushPose();

        if (scale != 1.0F)
        {
            poseStack.scale(scale, scale, scale);
        }

        for (int slot = 0; slot < 4; ++slot)
        {
            // Resolve each worn piece independently. The chestplate still identifies the active
            // hero for powers, but must not lend its texture/model to a helmet or leg piece from
            // another set (creative inventory and old mixed sets can contain those).
            ItemStack piece = player.getInventory().armor.get(3 - slot);
            HeroIteration pieceIteration = com.fiskmods.heroes.common.hero.ItemHeroArmor.getHero(piece);
            if (pieceIteration == null || pieceIteration.getArmorType(slot) == null)
            {
                continue;
            }

            HeroModelData pieceModel = HeroModelRegistry.get(pieceIteration);
            if (pieceModel == null)
            {
                continue;
            }

            float opacity = suitOpacity(pieceModel, player, slot);
            if (opacity <= 0.001F)
            {
                continue;
            }

            ResourceLocation texture = pieceModel.getTexture(slot, player);

            if (texture != null)
            {
                boolean[] hidden = hidePartsFor(playerModel, pieceModel, slot);
                VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(texture));
                playerModel.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY,
                        1.0F, 1.0F, 1.0F, opacity);
                restoreParts(playerModel, hidden);
            }

            renderOverlay(poseStack, buffer, packedLight, player, playerModel, pieceModel, slot, opacity);
            renderEquippedItems(poseStack, buffer, packedLight, player, playerModel, pieceModel, pieceIteration, slot);
            renderCape(poseStack, buffer, packedLight, player, playerModel, pieceModel, slot, opacity);
            renderAttachedModel(poseStack, buffer, packedLight, player, playerModel, pieceModel, slot, opacity);
            com.google.gson.JsonObject tentacles = pieceModel.getCustom().get("fiskheroes:tentacles");
            if (tentacles != null)
            {
                TentacleSuitRenderer.render(tentacles, poseStack, buffer, packedLight, player, playerModel, pieceModel, slot);
            }
            for (java.util.Map.Entry<String, com.google.gson.JsonObject> entry : pieceModel.getCustom().entrySet())
            {
                if (entry.getKey().startsWith("fiskheroes:booster"))
                    BoosterFlameRenderer.render(entry.getValue(), poseStack, buffer, player, playerModel, pieceModel, slot, partialTicks);
                else if (entry.getKey().equals("fiskheroes:wingsuit"))
                    WingsuitRenderer.render(entry.getValue(), poseStack, buffer, packedLight, player, playerModel, pieceModel, slot);
                else if (entry.getKey().equals("fiskheroes:wings"))
                    FalconWingsRenderer.render(entry.getValue(), poseStack, buffer, packedLight, player, playerModel, pieceModel, slot, partialTicks);
            }

            // The glowing parts of the suit (reactor, lights, visor) are a second emissive pass
            ResourceLocation lights = pieceModel.getLights(slot, player);

            if (lights != null)
            {
                boolean[] hidden = hidePartsFor(playerModel, pieceModel, slot);

                if (pieceModel.shouldFixHatLayer(slot))
                {
                    // The hat layer is what draws the second skin layer; the helmet itself is
                    // already part of the suit texture
                    playerModel.hat.visible = false;
                }

                VertexConsumer consumer = buffer.getBuffer(RenderType.eyes(lights));
                playerModel.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY,
                        1.0F, 1.0F, 1.0F, opacity);
                restoreParts(playerModel, hidden);
            }
        }

        QuiverSuitRenderer.render(poseStack, buffer, packedLight, player, playerModel, model, 1);

        poseStack.popPose();
    }

    /** Draws a Tabula model attachment declared by the original suit model JSON. */
    private void renderAttachedModel(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
            AbstractClientPlayer player, PlayerModel<AbstractClientPlayer> playerModel, HeroModelData model, int slot,
            float suitOpacity)
    {
        com.google.gson.JsonObject effect = model.getCustom().get("fiskheroes:model");
        if (effect == null || !appliesToSlot(effect, slot) || !effect.has("modelType")) return;

        ModelPart anchor = anchor(playerModel, effect.has("anchor") ? effect.get("anchor").getAsString() : "body");
        if (anchor == null) return;
        float opacity = suitOpacity * Math.max(0.0F, Math.min(1.0F,
                model.evaluateRenderData(effect.get("opacity"), player, 1.0F)));
        if (opacity <= 0.0F) return;

        for (int pass = 0; pass < 2; ++pass)
        {
            String key = textureForPass(effect.get("texture"), pass);
            if (key == null || key.equals("null")) continue;
            ResourceLocation texture = model.resolveCustomTexture(key, player, slot);
            if (texture == null) continue;

            poseStack.pushPose();
            anchor.translateAndRotate(poseStack);
            if (effect.has("mirror") && effect.get("mirror").getAsBoolean()) poseStack.scale(-1.0F, 1.0F, 1.0F);
            RenderType renderType = pass == 0 ? RenderType.entityTranslucent(texture) : RenderType.eyes(texture);
            VertexConsumer vertex = buffer.getBuffer(renderType);
            TabulaModelCache.render(effect.get("modelType").getAsString(), poseStack, vertex, packedLight,
                    1.0F, 1.0F, 1.0F, opacity);
            poseStack.popPose();
        }
    }

    /** Draws the selected primary equipment at the anchors declared in the suit model. */
    private void renderEquippedItems(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
            AbstractClientPlayer player, PlayerModel<AbstractClientPlayer> playerModel, HeroModelData model,
            HeroIteration iteration, int slot)
    {
        com.google.gson.JsonObject effect = model.getCustom().get("fiskheroes:equipped_item");
        if (effect == null || !appliesToSlot(effect, slot) || !effect.has("items") || !effect.get("items").isJsonArray())
        {
            return;
        }

        int coreSlot = iteration.getHero().getCorePieceOfSet();
        if (coreSlot < 0 || coreSlot > 3)
        {
            return;
        }
        ItemStack core = player.getInventory().armor.get(3 - coreSlot);
        ItemStack[] selected = com.fiskmods.heroes.common.hero.ItemHeroArmor.getWeapons(core);
        if (selected == null || selected.length == 0)
        {
            return;
        }

        // In the 1.7 renderer each equipped_item placement defaults to equipment slot zero, while
        // slotIndex selects another configured weapon (for example, Deathstroke's belt weapon).
        for (com.google.gson.JsonElement itemElement : effect.getAsJsonArray("items"))
        {
            if (!itemElement.isJsonObject())
            {
                continue;
            }
            com.google.gson.JsonObject item = itemElement.getAsJsonObject();
            int equipmentSlot = item.has("slotIndex") ? item.get("slotIndex").getAsInt() : 0;
            if (equipmentSlot < 0 || equipmentSlot >= selected.length || selected[equipmentSlot] == null || selected[equipmentSlot].isEmpty())
            {
                continue;
            }
            ItemStack stack = selected[equipmentSlot];
            ModelPart anchor = anchor(playerModel, item.has("anchor") ? item.get("anchor").getAsString() : "body");
            if (anchor == null)
            {
                continue;
            }

            poseStack.pushPose();
            anchor.translateAndRotate(poseStack);
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            translate(poseStack, item.get("offset"));
            rotate(poseStack, item.get("rotation"));
            float scale = item.has("scale") ? item.get("scale").getAsFloat() : 1.0F;
            poseStack.scale(scale, scale, scale);

            // Match the original EQUIPPED_SUIT transform and keep rendering delegated to Minecraft
            // so item model overrides, NBT variants and custom item renderers are honored.
            poseStack.translate(0.9375F, 0.0625F, 0.0F);
            poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-335.0F));
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-50.0F));
            poseStack.scale(2.0F / 3.0F, 2.0F / 3.0F, 2.0F / 3.0F);
            poseStack.translate(0.0F, 0.3F, 0.0F);
            Minecraft.getInstance().getItemRenderer().renderStatic(player, stack,
                    net.minecraft.world.item.ItemDisplayContext.NONE, false, poseStack, buffer,
                    player.level(), packedLight, OverlayTexture.NO_OVERLAY, player.getId());
            poseStack.popPose();
        }
    }

    private static ModelPart anchor(PlayerModel<?> model, String name)
    {
        return switch (name.toLowerCase(java.util.Locale.ROOT))
        {
            case "head" -> model.head;
            case "headwear", "hat" -> model.hat;
            case "rightarm" -> model.rightArm;
            case "leftarm" -> model.leftArm;
            case "rightleg" -> model.rightLeg;
            case "leftleg" -> model.leftLeg;
            case "body", "torso" -> model.body;
            default -> model.body;
        };
    }

    private static void translate(PoseStack poseStack, com.google.gson.JsonElement value)
    {
        if (value != null && value.isJsonArray() && value.getAsJsonArray().size() >= 3)
        {
            var vector = value.getAsJsonArray();
            poseStack.translate(vector.get(0).getAsDouble(), vector.get(1).getAsDouble(), vector.get(2).getAsDouble());
        }
    }

    private static void rotate(PoseStack poseStack, com.google.gson.JsonElement value)
    {
        if (value != null && value.isJsonArray() && value.getAsJsonArray().size() >= 3)
        {
            var vector = value.getAsJsonArray();
            poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(vector.get(2).getAsFloat()));
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(vector.get(1).getAsFloat()));
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(vector.get(0).getAsFloat()));
        }
    }

    /** Renders the segmented, motion-reactive cape effect declared by the original hero models. */
    private void renderCape(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
            AbstractClientPlayer player, PlayerModel<AbstractClientPlayer> playerModel, HeroModelData model, int slot,
            float suitOpacity)
    {
        com.google.gson.JsonObject effect = model.getCustom().get("fiskheroes:cape");
        if (effect == null || !appliesToSlot(effect, slot))
        {
            return;
        }

        float animation = model.evaluateRenderData(effect.get("data"), player, 0.0F);
        float open = 1.0F - Math.min(Math.max(animation, 0.0F) * 2.0F, 1.0F);
        float flare = Math.max(0.0F, Math.min(1.0F, (animation - 0.5F) * 2.0F));
        float length = effect.has("length") ? effect.get("length").getAsFloat() : 24.0F;
        boolean wide = effect.has("wide") && effect.get("wide").getAsBoolean();
        float width = wide ? 16.0F : 14.0F;

        for (int pass = 0; pass < 2; ++pass)
        {
            String key = textureForPass(effect.get("texture"), pass);
            if (key == null || key.equals("null"))
            {
                continue;
            }
            ResourceLocation texture = model.resolveCustomTexture(key, player, slot);
            if (texture == null)
            {
                continue;
            }

            boolean[] hidden = hidePartsFor(playerModel, model, slot);
            ModelPart body = playerModel.body;
            poseStack.pushPose();
            body.translateAndRotate(poseStack);
            poseStack.translate(0.0D, -0.02D, 0.1575D);

            float scale = com.fiskmods.heroes.common.data.var.Vars.getScale(player);
            net.minecraft.world.phys.Vec3 velocity = player.getDeltaMovement();
            float horizontalSpeed = (float) Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z) / Math.max(scale, 0.001F);
            float verticalSpeed = (float) velocity.y / Math.max(scale, 0.001F);
            float swing = Math.max(0.0F, Math.min(110.0F, horizontalSpeed * 22.0F + Math.max(0.0F, verticalSpeed * 10.0F)));
            float lean = (6.0F + swing) * open + 6.0F * flare;
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(lean));

            RenderType type = pass == 0 ? RenderType.entityTranslucent(texture) : RenderType.eyes(texture);
            VertexConsumer consumer = buffer.getBuffer(type);
            renderCapeMesh(poseStack, consumer, packedLight, player.tickCount, width, length, open, suitOpacity);
            poseStack.popPose();
            restoreParts(playerModel, hidden);
        }
    }

    private static void renderCapeMesh(PoseStack poseStack, VertexConsumer consumer, int packedLight,
            int ticks, float widthPixels, float lengthPixels, float open, float opacity)
    {
        final int segments = 24;
        float pixelScale = 1.0F / 16.0F;
        float width = widthPixels * pixelScale;
        float segmentLength = lengthPixels * pixelScale / segments;
        float uWidth = widthPixels / 64.0F;
        float vLength = lengthPixels / 32.0F;
        float u0 = 0.0F;
        float u1 = uWidth;
        float flex = (1.0F - open) * 0.65F;

        for (int i = 0; i < segments; ++i)
        {
            float top = segmentLength * i;
            float bottom = segmentLength * (i + 1);
            float topV = vLength * i / segments;
            float bottomV = vLength * (i + 1) / segments;
            float wave = (float) Math.sin((ticks * 0.09F) + i * 0.16F) * flex * (i / (float) segments);
            float left = -width * 0.5F - wave;
            float right = width * 0.5F + wave;
            float bend = (float) Math.sin((ticks * 0.07F) + i * 0.12F) * flex * 0.012F * (i + 1);

            poseStack.pushPose();
            poseStack.translate(0.0D, top, bend);
            vertex(consumer, poseStack, left, 0, 0, u0, topV, packedLight, opacity);
            vertex(consumer, poseStack, right, 0, 0, u1, topV, packedLight, opacity);
            vertex(consumer, poseStack, right, segmentLength, 0, u1, bottomV, packedLight, opacity);
            vertex(consumer, poseStack, left, segmentLength, 0, u0, bottomV, packedLight, opacity);
            // The reverse face uses the back half of the original cape texture region.
            vertex(consumer, poseStack, left, segmentLength, 0, uWidth, bottomV, packedLight, opacity);
            vertex(consumer, poseStack, right, segmentLength, 0, uWidth * 2.0F, bottomV, packedLight, opacity);
            vertex(consumer, poseStack, right, 0, 0, uWidth * 2.0F, topV, packedLight, opacity);
            vertex(consumer, poseStack, left, 0, 0, uWidth, topV, packedLight, opacity);
            poseStack.popPose();
            poseStack.translate(0.0D, segmentLength, 0.0D);
        }
    }

    private static void vertex(VertexConsumer consumer, PoseStack poseStack, float x, float y, float z,
            float u, float v, int packedLight, float opacity)
    {
        consumer.vertex(poseStack.last().pose(), x, y, z)
                .color(255, 255, 255, Math.round(255.0F * opacity))
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(poseStack.last().normal(), 0.0F, 0.0F, 1.0F)
                .endVertex();
    }

    /** Draws the original model's second texture pass for visors, eyes and animated suit details. */
    private void renderOverlay(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
            AbstractClientPlayer player, PlayerModel<AbstractClientPlayer> playerModel, HeroModelData model, int slot,
            float suitOpacity)
    {
        com.google.gson.JsonObject overlay = model.getCustom().get("fiskheroes:overlay");
        if (overlay == null || !appliesToSlot(overlay, slot) || !passesConditionals(overlay, model, player))
        {
            return;
        }

        float data = model.evaluateRenderData(overlay.get("data"), player, 1.0F);
        float opacity = overlay.has("opacity") ? overlay.get("opacity").getAsFloat() : 1.0F;
        float alpha = suitOpacity * Math.max(0.0F, Math.min(1.0F, data * opacity));
        if (alpha <= 0.0F)
        {
            return;
        }

        com.google.gson.JsonElement textures = overlay.get("texture");
        for (int pass = 0; pass < 2; ++pass)
        {
            String key = textureForPass(textures, pass);
            if (key == null || key.equals("null"))
            {
                continue;
            }

            ResourceLocation texture = model.resolveCustomTexture(key, player, slot);
            if (texture == null)
            {
                continue;
            }

            boolean[] hidden = hidePartsFor(playerModel, model, slot);
            RenderType renderType = pass == 0 ? RenderType.entityTranslucent(texture) : RenderType.eyes(texture);
            VertexConsumer consumer = buffer.getBuffer(renderType);
            playerModel.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY,
                    1.0F, 1.0F, 1.0F, alpha);
            restoreParts(playerModel, hidden);
        }
    }

    private static String textureForPass(com.google.gson.JsonElement textures, int pass)
    {
        if (textures == null || textures.isJsonNull())
        {
            return null;
        }
        if (textures.isJsonPrimitive())
        {
            return pass == 0 ? textures.getAsString() : null;
        }
        if (textures.isJsonArray() && textures.getAsJsonArray().size() > pass)
        {
            return textures.getAsJsonArray().get(pass).getAsString();
        }
        return null;
    }

    private static boolean appliesToSlot(com.google.gson.JsonObject effect, int slot)
    {
        if (!effect.has("applicable") || !effect.get("applicable").isJsonArray())
        {
            return false;
        }
        String slotName = switch (slot)
        {
            case 0 -> "HELMET";
            case 1 -> "CHESTPLATE";
            case 2 -> "LEGGINGS";
            default -> "BOOTS";
        };
        for (com.google.gson.JsonElement applicable : effect.getAsJsonArray("applicable"))
        {
            if (applicable.isJsonPrimitive() && applicable.getAsString().equalsIgnoreCase(slotName))
            {
                return true;
            }
        }
        return false;
    }

    /** Applies the original model's per-piece invisibility/intangibility opacity curve. */
    private static float suitOpacity(HeroModelData model, AbstractClientPlayer player, int slot)
    {
        com.google.gson.JsonObject effect = model.getCustom().get("fiskheroes:invisibility");
        if (effect == null || !appliesToSlot(effect, slot))
        {
            return 1.0F;
        }

        float data = model.evaluateRenderData(effect.get("data"), player, 0.0F);
        float min = effect.has("opacityMin") ? effect.get("opacityMin").getAsFloat() : 0.0F;
        float max = effect.has("opacityMax") ? effect.get("opacityMax").getAsFloat() : 1.0F;
        return net.minecraft.util.Mth.clamp(max + (min - max) * data, 0.0F, 1.0F);
    }

    private static boolean passesConditionals(com.google.gson.JsonObject effect, HeroModelData model, AbstractClientPlayer player)
    {
        if (!effect.has("conditionals") || !effect.get("conditionals").isJsonArray())
        {
            return true;
        }
        for (com.google.gson.JsonElement conditional : effect.getAsJsonArray("conditionals"))
        {
            if (!conditional.isJsonPrimitive())
            {
                return false;
            }
            String condition = conditional.getAsString();
            if (condition.startsWith("vars:") && !model.evaluate(condition.substring("vars:".length()), player))
            {
                return false;
            }
        }
        return true;
    }

    public static boolean hasPiece(AbstractClientPlayer player, int slot)
    {
        return com.fiskmods.heroes.common.hero.ItemHeroArmor.isSuitPiece(player.getInventory().armor.get(3 - slot));
    }

    /** Temporarily hides the parts the given piece must not draw and returns the previous state. */
    private boolean[] hidePartsFor(PlayerModel<AbstractClientPlayer> model, HeroModelData data, int slot)
    {
        ModelPart[] parts = {
                model.head, model.hat, model.body, model.rightArm, model.leftArm, model.rightLeg, model.leftLeg,
                model.jacket, model.rightSleeve, model.leftSleeve, model.rightPants, model.leftPants
        };
        String[] names = { "head", "headwear", "body", "rightArm", "leftArm", "rightLeg", "leftLeg" };
        boolean[] previous = new boolean[parts.length];

        for (int i = 0; i < names.length; ++i)
        {
            previous[i] = parts[i].visible;
            parts[i].visible = previous[i] && data.showsModelPart(slot, names[i]);
        }

        // The jacket/sleeve overlays follow the body pieces of the suit
        parts[7].visible = previous[7] && model.body.visible;
        parts[8].visible = previous[8] && model.rightArm.visible;
        parts[9].visible = previous[9] && model.leftArm.visible;
        parts[10].visible = previous[10] && model.rightLeg.visible;
        parts[11].visible = previous[11] && model.leftLeg.visible;

        return previous;
    }

    private void restoreParts(PlayerModel<AbstractClientPlayer> model, boolean[] previous)
    {
        ModelPart[] parts = {
                model.head, model.hat, model.body, model.rightArm, model.leftArm, model.rightLeg, model.leftLeg,
                model.jacket, model.rightSleeve, model.leftSleeve, model.rightPants, model.leftPants
        };

        for (int i = 0; i < parts.length && i < previous.length; ++i)
        {
            parts[i].visible = previous[i];
        }
    }
}
