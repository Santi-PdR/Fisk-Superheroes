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

        HeroModelData model = HeroModelRegistry.get(iteration.getHero().getRegistryName());

        if (model == null)
        {
            return;
        }

        PlayerModel<AbstractClientPlayer> playerModel = getParentModel();
        float scale = com.fiskmods.heroes.common.data.var.Vars.getScale(player);

        poseStack.pushPose();

        if (scale != 1.0F)
        {
            poseStack.scale(scale, scale, scale);
        }

        for (int slot = 0; slot < 4; ++slot)
        {
            if (iteration.getArmorType(slot) == null || !hasPiece(player, slot))
            {
                continue;
            }

            ResourceLocation texture = model.getTexture(slot, player);

            if (texture != null)
            {
                boolean[] hidden = hidePartsFor(playerModel, model, slot);
                VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(texture));
                playerModel.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
                restoreParts(playerModel, hidden);
            }

            renderOverlay(poseStack, buffer, packedLight, player, playerModel, model, slot);
            renderEquippedItems(poseStack, buffer, packedLight, player, playerModel, model, iteration, slot);

            // The glowing parts of the suit (reactor, lights, visor) are a second emissive pass
            ResourceLocation lights = model.getLights(slot, player);

            if (lights != null)
            {
                boolean[] hidden = hidePartsFor(playerModel, model, slot);

                if (model.shouldFixHatLayer(slot))
                {
                    // The hat layer is what draws the second skin layer; the helmet itself is
                    // already part of the suit texture
                    playerModel.hat.visible = false;
                }

                VertexConsumer consumer = buffer.getBuffer(RenderType.eyes(lights));
                playerModel.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
                restoreParts(playerModel, hidden);
            }
        }

        poseStack.popPose();
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
        if (selected == null || selected.length == 0 || selected[0] == null || selected[0].isEmpty())
        {
            return;
        }

        // In the 1.7 renderer this effect defaults to primary equipment slot zero. Its items array
        // holds one or more placements of that same stack (for example, a pair of dual tonfas).
        ItemStack stack = selected[0];
        for (com.google.gson.JsonElement itemElement : effect.getAsJsonArray("items"))
        {
            if (!itemElement.isJsonObject())
            {
                continue;
            }
            com.google.gson.JsonObject item = itemElement.getAsJsonObject();
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

    /** Draws the original model's second texture pass for visors, eyes and animated suit details. */
    private void renderOverlay(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
            AbstractClientPlayer player, PlayerModel<AbstractClientPlayer> playerModel, HeroModelData model, int slot)
    {
        com.google.gson.JsonObject overlay = model.getCustom().get("fiskheroes:overlay");
        if (overlay == null || !appliesToSlot(overlay, slot) || !passesConditionals(overlay, model, player))
        {
            return;
        }

        float data = model.evaluateRenderData(overlay.get("data"), player, 1.0F);
        float opacity = overlay.has("opacity") ? overlay.get("opacity").getAsFloat() : 1.0F;
        float alpha = Math.max(0.0F, Math.min(1.0F, data * opacity));
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
