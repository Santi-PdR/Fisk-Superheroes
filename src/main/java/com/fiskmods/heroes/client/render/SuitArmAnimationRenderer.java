package com.fiskmods.heroes.client.render;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/** Applies the player-arm subset of the pack's FSK animations to the worn suit model. */
public final class SuitArmAnimationRenderer
{
    private static final float HALF_PI = (float) (Math.PI / 2.0D);

    private SuitArmAnimationRenderer()
    {
    }

    /** Called after vanilla setupAnim, before the player skin and all armor layers are drawn. */
    public static void onPlayerModelSetup(PlayerModel<?> model, net.minecraft.world.entity.LivingEntity entity, float limbSwingAmount)
    {
        if (!(entity instanceof net.minecraft.client.player.AbstractClientPlayer player)) return;
        var iteration = com.fiskmods.heroes.common.hero.HeroTracker.getWornSuit(player);
        if (iteration == null) return;
        HeroModelData suit = HeroModelRegistry.get(iteration);
        if (suit == null) return;
        float before = model.rightArm.xRot;
        apply(suit, model, player, limbSwingAmount);
        model.rightSleeve.copyFrom(model.rightArm);
        model.leftSleeve.copyFrom(model.leftArm);
        model.hat.copyFrom(model.head);
        model.rightPants.copyFrom(model.rightLeg);
        model.leftPants.copyFrom(model.leftLeg);
        if (Math.abs(model.rightArm.xRot - before) > 0.01F
                && DIAGNOSTICS.add(iteration.getRegistryName().toString()))
            com.fiskmods.heroes.FiskHeroes.LOGGER.info("Shared suit arm pose applied: hero={}, sleeveAligned={}",
                    iteration.getRegistryName(), model.rightSleeve.xRot == model.rightArm.xRot
                            && model.leftSleeve.xRot == model.leftArm.xRot);
    }

    private static final java.util.Set<String> DIAGNOSTICS = java.util.concurrent.ConcurrentHashMap.newKeySet();

    static void apply(HeroModelData suit, PlayerModel<?> model, Player player, float limbSwingAmount)
    {
        applyGlidingPose(suit, model, player);
        JsonObject animationEffect = suit.getCustom().get("fiskheroes:animation");
        if (animationEffect != null && animationEffect.has("animation")
                && "fiskheroes:hat_tip_player".equals(animationEffect.get("animation").getAsString()))
        {
            float data = Mth.clamp(suit.evaluateRenderData(animationEffect.get("data"), player, 0.0F), 0.0F, 1.0F);
            float progress = Mth.sin((float) Math.PI * (1.0F - data));
            progress *= progress;
            model.rightArm.xRot = lerp(model.rightArm.xRot, -2.4F, progress);
        }

        JsonObject armAnimation = suit.getCustom().get("fiskheroes:arm_animation");
        float rightPose = armAnimation != null && armAnimation.has("dataRArmPose")
                ? suit.evaluateRenderData(armAnimation.get("dataRArmPose"), player, 0.0F)
                : data(suit, player, "fiskheroes:aiming_timer");
        float leftPose = armAnimation != null && armAnimation.has("dataLArmPose")
                ? suit.evaluateRenderData(armAnimation.get("dataLArmPose"), player, 0.0F) : 0.0F;

        ResourceLocation blocking = animation(suit, "BLOCKING");
        if (blocking != null) apply(blocking, model, data(suit, player, "fiskheroes:shield_blocking_timer"), limbSwingAmount);

        ResourceLocation projection = animation(suit, "ENERGY_PROJECTION");
        if (projection != null) apply(projection, model, data(suit, player, "fiskheroes:energy_projection_timer"), limbSwingAmount);

        ResourceLocation chargedBeam = animation(suit, "CHARGED_BEAM");
        if (chargedBeam != null) apply(chargedBeam, model, data(suit, player, "fiskheroes:beam_shooting_timer"), limbSwingAmount);

        if (armAnimation != null && armAnimation.has("dataSwordPose"))
        {
            ResourceLocation swordPose = animation(suit, "SWORD_POSE");
            if (swordPose != null)
            {
                apply(swordPose, model, suit.evaluateRenderData(armAnimation.get("dataSwordPose"), player, 0.0F), limbSwingAmount);
            }
        }

        ResourceLocation aiming = animation(suit, "AIMING");
        if (aiming != null) apply(aiming, model, rightPose, limbSwingAmount);
        ResourceLocation aimingLeft = animation(suit, "AIMING_LEFT");
        if (aimingLeft != null) apply(aimingLeft, model, leftPose, limbSwingAmount);
    }

    /** Mirrors the original gliding pose for capes and wing membranes. */
    private static void applyGlidingPose(HeroModelData suit, PlayerModel<?> model, Player player)
    {
        boolean wingsuit = suit.getCustom().containsKey("fiskheroes:wingsuit");
        boolean cape = suit.getCustom().containsKey("fiskheroes:cape");
        if (!wingsuit && !cape) return;
        float timer = data(suit, player, "fiskheroes:gliding_timer");
        if (timer <= 0.0F) return;
        float progress = com.fiskmods.heroes.common.util.GlidingPoseMath.curve(timer);
        var motion = player.getDeltaMovement();
        float spread = com.fiskmods.heroes.common.util.GlidingPoseMath.spread(timer, motion.y, motion.length());
        float swing = player.getAttackAnim(net.minecraft.client.Minecraft.getInstance().getFrameTime());
        float rightSwing = 1.0F - Math.min(swing, 1.0F - swing) * 2.0F;
        model.head.xRot = lerp(model.head.xRot, -(float) Math.PI / 4.0F, progress);
        if (wingsuit || player.getMainHandItem().isEmpty())
        {
            model.rightArm.zRot += spread * 0.8F * rightSwing;
            model.rightArm.xRot += progress * (wingsuit ? 0.25F : -0.025F) * rightSwing;
        }
        if (wingsuit || player.getOffhandItem().isEmpty())
        {
            model.leftArm.zRot -= spread * 0.8F;
            model.leftArm.xRot += progress * (wingsuit ? 0.25F : -0.025F);
        }
        if (wingsuit)
        {
            model.rightLeg.zRot += spread * 0.1F * rightSwing;
            model.leftLeg.zRot -= spread * 0.1F;
            model.rightLeg.xRot += progress * 0.15F * rightSwing;
            model.leftLeg.xRot += progress * 0.15F;
        }
        var iteration = com.fiskmods.heroes.common.hero.HeroTracker.getWornSuit(player);
        if (iteration != null && GLIDE_DIAGNOSTICS.add(iteration.getRegistryName().toString()))
            com.fiskmods.heroes.FiskHeroes.LOGGER.info("Gliding suit pose applied: hero={}, wingsuit={}",
                    iteration.getRegistryName(), wingsuit);
    }

    private static final java.util.Set<String> GLIDE_DIAGNOSTICS = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private static float data(HeroModelData suit, Player player, String variable)
    {
        return suit.evaluateRenderData(new com.google.gson.JsonPrimitive(variable), player, 0.0F);
    }

    private static ResourceLocation animation(HeroModelData suit, String key)
    {
        JsonElement value = suit.getAnimations().get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) return null;
        return ResourceLocation.tryParse(value.getAsString());
    }

    /** Executes the arm and hand transformations used by the bundled body FSK files. */
    private static void apply(ResourceLocation animation, PlayerModel<?> model, float data, float limbSwingAmount)
    {
        float progress = curve(data);
        ModelPart right = model.rightArm;
        ModelPart left = model.leftArm;
        ModelPart head = model.head;

        switch (animation.getPath())
        {
            case "aiming" ->
            {
                right.xRot = lerp(right.xRot, head.xRot - HALF_PI, progress);
                right.yRot = rotLerp(right.yRot, head.yRot, data);
                right.zRot = lerp(right.zRot, head.zRot, progress);
            }
            case "aiming_left" ->
            {
                left.xRot = lerp(left.xRot, 0.9F * (head.xRot - HALF_PI), progress);
                left.yRot = rotLerp(left.yRot, head.yRot + 0.1F, data);
                left.zRot = lerp(left.zRot, head.zRot, progress);
            }
            case "dual_aiming" ->
            {
                right.xRot = lerp(right.xRot, head.xRot - HALF_PI, progress);
                right.yRot = rotLerp(right.yRot, head.yRot - 0.1F, data);
                right.zRot = lerp(right.zRot, head.zRot, progress);
                left.xRot = lerp(left.xRot, 0.9F * (head.xRot - HALF_PI), progress);
                left.yRot = rotLerp(left.yRot, head.yRot + 0.1F, data);
                left.zRot = lerp(left.zRot, head.zRot, progress);
            }
            case "web_aim_right" ->
            {
                right.x -= progress;
                right.y -= progress;
                right.zRot = lerp(right.zRot, -HALF_PI, progress);
                right.xRot = lerp(right.xRot, 0.1F - HALF_PI - head.yRot, progress);
                right.yRot = rotLerp(right.yRot, Math.min(head.xRot + 0.1F, HALF_PI), data);
            }
            case "web_aim_left" ->
            {
                left.x += progress;
                left.y -= progress;
                left.zRot = lerp(left.zRot, HALF_PI, progress);
                left.xRot = lerp(left.xRot, 0.1F - HALF_PI + head.yRot, progress);
                left.yRot = rotLerp(left.yRot, -Math.min(head.xRot + 0.1F, HALF_PI), data);
            }
            case "sword_pose" ->
            {
                float swingScale = progress * (1.0F - limbSwingAmount);
                right.xRot -= 0.7F * swingScale;
                right.yRot -= 0.2F * swingScale;
                right.zRot -= 0.3F * swingScale;
            }
            case "blocking" ->
            {
                right.xRot = lerp(right.xRot, 0.2F * right.xRot - 1.3F, progress);
                right.yRot -= 1.1F * progress;
                right.zRot += 0.3F * progress;
            }
            case "blocking_arm" ->
            {
                right.xRot = lerp(right.xRot, 0.1F * right.xRot - 2.3F, progress);
                right.yRot -= 0.8F * progress;
                right.zRot += 0.2F * progress;
                left.xRot = lerp(left.xRot, 0.1F * left.xRot - 2.3F, progress);
                left.yRot += 0.8F * progress;
                left.zRot -= 0.2F * progress;
            }
            case "dual_hand_beam" ->
            {
                right.xRot = lerp(right.xRot, Math.min(head.xRot - 0.9F, -0.1F), progress);
                right.yRot = rotLerp(right.yRot, head.yRot - 0.4F, data);
                left.xRot = lerp(left.xRot, Math.min(head.xRot - 1.3F, -0.3F), progress);
                left.yRot = rotLerp(left.yRot, head.yRot + 0.3F, data);
            }
            default -> { }
        }
    }

    private static float curve(float value)
    {
        value = Mth.clamp(value, 0.0F, 1.0F);
        return (Mth.sin((value * 2.0F - 1.0F) * Mth.HALF_PI) + 1.0F) * 0.5F;
    }

    private static float lerp(float from, float to, float amount)
    {
        return from + (to - from) * amount;
    }

    private static float rotLerp(float from, float to, float amount)
    {
        float difference = to - from;
        while (difference < -Math.PI) difference += (float) (Math.PI * 2.0D);
        while (difference >= Math.PI) difference -= (float) (Math.PI * 2.0D);
        return from + difference * amount;
    }
}
