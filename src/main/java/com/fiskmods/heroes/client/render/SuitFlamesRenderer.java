package com.fiskmods.heroes.client.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemStack;

/** Ports the animated hand and head plumes configured by the original Firestorm renderer. */
final class SuitFlamesRenderer
{
    private static final String ICON = "fiskheroes:fire_layer_%s";
    private static final String MASK_TIMER = "1 - entity.getInterpolatedData('fiskheroes:mask_open_timer2')";

    private SuitFlamesRenderer()
    {
    }

    static void render(JsonObject effect, PoseStack poseStack, MultiBufferSource buffer, int packedLight,
            AbstractClientPlayer player, PlayerModel<AbstractClientPlayer> playerModel, HeroModelData model,
            int slot, float partialTick)
    {
        if (slot != 1 || !HeroSuitLayer.appliesToSlot(effect, slot)
                || !HeroSuitLayer.passesConditionals(effect, model, player)) return;
        if (effect.has("requiresFullSuit") && effect.get("requiresFullSuit").getAsBoolean()
                && !hasFullSuit(player, model)) return;

        float scaleRatio = effect.has("scaleRatio") ? effect.get("scaleRatio").getAsFloat() : 1.0F;
        float headTimer = model.evaluateRenderData(new com.google.gson.JsonPrimitive(MASK_TIMER), player, 1.0F);
        float opacity = effect.has("opacity") ? effect.get("opacity").getAsFloat() : 1.0F;

        // external/flames.js:createHands(renderer, fire, true)
        renderPlume(poseStack, buffer, packedLight, player, playerModel, model, slot, partialTick,
                "rightArm", ICON, 1.0F, opacity * 0.7F, true,
                scaled(new float[] { 1.2F, 10.0F, 0.15F }, scaleRatio),
                new float[] { -12.0F, 5.0F, 173.0F }, new float[] { 4.75F, 2.0F }, scaleRatio);
        renderPlume(poseStack, buffer, packedLight, player, playerModel, model, slot, partialTick,
                "rightArm", ICON, 1.0F, opacity * 0.7F, true,
                scaled(new float[] { 1.2F, 10.0F, -0.15F }, scaleRatio),
                new float[] { 12.0F, -5.0F, 173.0F }, new float[] { 4.75F, 2.0F }, scaleRatio);
        renderPlume(poseStack, buffer, packedLight, player, playerModel, model, slot, partialTick,
                "rightArm", ICON, 1.0F, opacity * 0.7F, true,
                scaled(new float[] { -1.0F, 8.4F, 0.0F }, scaleRatio),
                new float[] { 0.0F, 0.0F, 83.0F }, new float[] { 4.0F, 1.5F }, scaleRatio);

        // The base flame remains while masked; the tapered top retracts with the mask animation.
        renderPlume(poseStack, buffer, packedLight, player, playerModel, model, slot, partialTick,
                "head", ICON, 1.0F, opacity * 0.4F, false,
                scaled(new float[] { 0.0F, 1.0F, 0.0F }, scaleRatio),
                new float[] { 0.0F, 0.0F, 180.0F }, new float[] { 8.0F + 2.0F * headTimer, 1.75F }, scaleRatio);
        renderPlume(poseStack, buffer, packedLight, player, playerModel, model, slot, partialTick,
                "head", ICON, headTimer, opacity * 0.3F, false,
                scaled(new float[] { 0.0F, -5.0F, 0.0F }, scaleRatio),
                new float[] { -25.0F, 0.0F, 180.0F }, new float[] { 7.5F, 1.75F }, scaleRatio);
    }

    private static void renderPlume(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
            AbstractClientPlayer player, PlayerModel<AbstractClientPlayer> playerModel, HeroModelData model,
            int slot, float partialTick, String anchor, String icon, float progress, float opacity,
            boolean mirror, float[] offset, float[] rotation, float[] size, float ratio)
    {
        JsonObject plume = new JsonObject();
        plume.addProperty("icon", icon);
        plume.addProperty("data", progress);
        plume.addProperty("anchor", anchor);
        plume.addProperty("opacity", opacity);
        plume.addProperty("mirror", mirror);
        plume.add("offset", vector(offset));
        plume.add("rotation", vector(rotation));
        plume.add("scale", vector(scaled(size, ratio)));
        BoosterFlameRenderer.render(plume, poseStack, buffer, player, playerModel, model, slot, partialTick);
    }

    private static boolean hasFullSuit(AbstractClientPlayer player, HeroModelData model)
    {
        for (int slot = 0; slot < 4; ++slot)
        {
            ItemStack piece = player.getInventory().armor.get(3 - slot);
            com.fiskmods.heroes.common.hero.HeroIteration iteration =
                    com.fiskmods.heroes.common.hero.ItemHeroArmor.getHero(piece);
            if (iteration == null || iteration.getArmorType(slot) == null
                    || !iteration.getHero().getRegistryName().equals(model.getHero())) return false;
        }
        return true;
    }

    private static float[] scaled(float[] values, float ratio)
    {
        for (int i = 0; i < values.length; ++i) values[i] *= ratio;
        return values;
    }

    private static JsonArray vector(float[] values)
    {
        JsonArray result = new JsonArray();
        for (float value : values) result.add(value);
        return result;
    }
}
