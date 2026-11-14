package com.fiskmods.heroes.client.render;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.hero.HeroTracker;
import com.fiskmods.heroes.common.hero.ItemHeroArmor;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;

/** Animated hand and head plumes configured by Firestorm's suit renderer. */
final class SuitFlamesRenderer
{
    private static final java.util.Set<String> DIAGNOSTICS = new java.util.HashSet<>();
    private static final JsonPrimitive MASK_TIMER = new JsonPrimitive("1 - entity.getInterpolatedData('fiskheroes:mask_open_timer2')");

    private SuitFlamesRenderer() { }

    static void render(JsonObject effect, PoseStack poseStack, MultiBufferSource buffer, int packedLight,
            AbstractClientPlayer player, PlayerModel<AbstractClientPlayer> playerModel, HeroModelData model,
            int slot, float partialTick)
    {
        if (!eligible(effect, player, model, slot)) return;
        diagnostic(player, model, "third");
        float ratio = effect.has("scaleRatio") ? effect.get("scaleRatio").getAsFloat() : 1.0F;
        float headTimer = model.evaluateRenderData(MASK_TIMER, player, 1.0F);
        float opacity = effect.has("opacity") ? effect.get("opacity").getAsFloat() : 1.0F;
        // HeroEffectFlames uses the original fire mesh, measured in blocks, on each arm.
        if (ratio > 0)
        {
            fire(poseStack, buffer, player, playerModel, partialTick, "rightArm", 10.4F, .325F / ratio, 1.9F * ratio, opacity);
            fire(poseStack, buffer, player, playerModel, partialTick, "leftArm", 10.4F, .325F / ratio, 1.9F * ratio, opacity);
        }
        if (headTimer > 0)
        {
            fire(poseStack, buffer, player, playerModel, partialTick, "head", 1.6F, .6F, 2 * headTimer, .3F);
            fire(poseStack, buffer, player, playerModel, partialTick, "head", 0, .65F, 1.1F * headTimer, .3F);
            fire(poseStack, buffer, player, playerModel, partialTick, "body", 3.2F, .5F, 1.8F * headTimer, .3F);
        }
    }

    static void renderFirstPerson(JsonObject effect, PoseStack pose, MultiBufferSource buffer,
            AbstractClientPlayer player, PlayerModel<?> playerModel, HeroModelData model, boolean right, float partialTick)
    {
        if (!eligible(effect, player, model, 1)) return;
        float ratio = effect.has("scaleRatio") ? effect.get("scaleRatio").getAsFloat() : 1.0F;
        if (ratio <= 0) return;
        float opacity = effect.has("opacity") ? effect.get("opacity").getAsFloat() : 1.0F;
        diagnostic(player, model, right ? "first-right" : "first-left");
        fire(pose, buffer, player, playerModel, partialTick, right ? "rightArm" : "leftArm",
                10.4F, .325F / ratio, 1.9F * ratio, opacity);
    }

    private static boolean eligible(JsonObject effect, AbstractClientPlayer player, HeroModelData model, int slot)
    {
        if (slot != 1 || !HeroSuitLayer.appliesToSlot(effect, slot)
                || !HeroSuitLayer.passesConditionals(effect, model, player)) return false;
        if (!effect.has("requiresFullSuit") || !effect.get("requiresFullSuit").getAsBoolean()) return true;
        // Many suits deliberately have no helmet: only their defined pieces are required.
        var iteration = ItemHeroArmor.getHero(player.getInventory().armor.get(2));
        boolean complete = iteration != null && iteration.getHero().getRegistryName().equals(model.getHero())
                && HeroTracker.hasCompleteSuit(player, iteration);
        if (!complete && Boolean.getBoolean("fiskheroes.ciParticleDiagnostics")
                && DIAGNOSTICS.add(player.getUUID() + "|" + model.getHero() + "|incomplete"))
            FiskHeroes.LOGGER.info("Suit flames suppressed: hero={}, wearer={}, reason=incomplete_suit", model.getHero(), player.getName().getString());
        return complete;
    }

    private static void diagnostic(AbstractClientPlayer player, HeroModelData model, String view)
    {
        if (Boolean.getBoolean("fiskheroes.ciParticleDiagnostics") && DIAGNOSTICS.add(player.getUUID() + "|" + model.getHero() + "|" + view))
            FiskHeroes.LOGGER.info("Suit flames rendered: hero={}, wearer={}, view={}", model.getHero(), player.getName().getString(), view);
    }

    static void clear() { DIAGNOSTICS.clear(); }

    private static void fire(PoseStack pose, MultiBufferSource buffer, AbstractClientPlayer player,
            PlayerModel<?> model, float partialTick, String anchor, float offsetY, float widthBlocks,
            float heightFactor, float opacity)
    {
        BoosterFlameRenderer.renderSuitFire(pose, buffer, player, model, partialTick, anchor, offsetY,
                widthBlocks * 16, widthBlocks * heightFactor * 16, opacity);
    }
}
