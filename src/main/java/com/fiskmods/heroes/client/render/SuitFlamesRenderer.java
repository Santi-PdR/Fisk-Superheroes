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
    private static final String ICON = "fiskheroes:fire_layer_%s";
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
        // Direct primitive arguments avoid constructing five temporary JSON trees per rendered player per frame.
        plume(poseStack, buffer, player, playerModel, partialTick, "rightArm", 1, opacity * .7F, true,
                1.2F*ratio, 10*ratio, .15F*ratio, -12, 5, 173, 4.75F*ratio, 2*ratio);
        plume(poseStack, buffer, player, playerModel, partialTick, "rightArm", 1, opacity * .7F, true,
                1.2F*ratio, 10*ratio, -.15F*ratio, 12, -5, 173, 4.75F*ratio, 2*ratio);
        plume(poseStack, buffer, player, playerModel, partialTick, "rightArm", 1, opacity * .7F, true,
                -ratio, 8.4F*ratio, 0, 0, 0, 83, 4*ratio, 1.5F*ratio);
        plume(poseStack, buffer, player, playerModel, partialTick, "head", 1, opacity * .4F, false,
                0, ratio, 0, 0, 0, 180, (8 + 2*headTimer)*ratio, 1.75F*ratio);
        plume(poseStack, buffer, player, playerModel, partialTick, "head", headTimer, opacity * .3F, false,
                0, -5*ratio, 0, -25, 0, 180, 7.5F*ratio, 1.75F*ratio);
    }

    static void renderFirstPerson(JsonObject effect, PoseStack pose, MultiBufferSource buffer,
            AbstractClientPlayer player, PlayerModel<?> playerModel, HeroModelData model, boolean right, float partialTick)
    {
        if (!eligible(effect, player, model, 1)) return;
        float ratio = effect.has("scaleRatio") ? effect.get("scaleRatio").getAsFloat() : 1.0F;
        float opacity = effect.has("opacity") ? effect.get("opacity").getAsFloat() : 1.0F;
        String anchor = right ? "rightArm" : "leftArm";
        float side = right ? 1 : -1;
        diagnostic(player, model, right ? "first-right" : "first-left");
        plume(pose, buffer, player, playerModel, partialTick, anchor, 1, opacity * .7F, false,
                side*1.2F*ratio, 10*ratio, .15F*ratio, -12, 5, 173, 4.75F*ratio, 2*ratio);
        plume(pose, buffer, player, playerModel, partialTick, anchor, 1, opacity * .7F, false,
                side*1.2F*ratio, 10*ratio, -.15F*ratio, 12, -5, 173, 4.75F*ratio, 2*ratio);
        plume(pose, buffer, player, playerModel, partialTick, anchor, 1, opacity * .7F, false,
                -side*ratio, 8.4F*ratio, 0, 0, 0, 83, 4*ratio, 1.5F*ratio);
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

    private static void plume(PoseStack pose, MultiBufferSource buffer, AbstractClientPlayer player,
            PlayerModel<?> model, float partialTick, String anchor, float progress, float opacity, boolean mirror,
            float ox, float oy, float oz, float rx, float ry, float rz, float width, float length)
    {
        BoosterFlameRenderer.renderPlume(pose, buffer, player, model, partialTick, ICON, anchor, progress, opacity,
                mirror, ox, oy, oz, rx, ry, rz, width, length);
    }
}
