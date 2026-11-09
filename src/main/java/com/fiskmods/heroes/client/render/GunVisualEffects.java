package com.fiskmods.heroes.client.render;

import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.item.ModItems;
import com.fiskmods.heroes.common.util.GunVisualMath;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;

/** Accepted shots are displayed on the actual wearer, including remote players. */
public final class GunVisualEffects
{
    private static final Map<Player, Shots> SHOTS = new WeakHashMap<>();
    private static final Set<String> DIAGNOSTICS = ConcurrentHashMap.newKeySet();
    private static final double NEVER = Double.NEGATIVE_INFINITY;

    private GunVisualEffects() {}

    private static final class Shots
    {
        double main = NEVER;
        double off = NEVER;
    }

    public static void onShot(int entityId, boolean offhand)
    {
        var level = Minecraft.getInstance().level;
        if (level == null || !(level.getEntity(entityId) instanceof Player player)) return;
        Shots shots = SHOTS.computeIfAbsent(player, ignored -> new Shots());
        double now = level.getGameTime() + Minecraft.getInstance().getFrameTime();
        if (offhand) shots.off = now; else shots.main = now;
        FiskHeroes.LOGGER.debug("Gun shot effect received: wearer={}, hand={}", player.getName().getString(), offhand ? "OFF_HAND" : "MAIN_HAND");
    }

    /** Called in model space, after the item pose and before drawing the pistol. */
    static void renderDesertEagle(PoseStack pose, MultiBufferSource buffers, ItemDisplayContext context)
    {
        if (!(SuitItemRenderContext.wearer() instanceof Player player) || !player.getMainHandItem().is(ModItems.DESERT_EAGLE.get())) return;
        boolean right = context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
        boolean left = context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
        if (!right && !left) return;
        boolean offhand = (right ? HumanoidArm.RIGHT : HumanoidArm.LEFT) != player.getMainArm();
        // The visual duplicate exists only if the real offhand is empty.
        if (offhand && (!com.fiskmods.heroes.common.item.ItemDualSword.isDual(player.getMainHandItem()) || !player.getOffhandItem().isEmpty())) return;
        Shots shots = SHOTS.get(player);
        if (shots == null) return;
        double start = offhand ? shots.off : shots.main;
        float age = (float) (player.level().getGameTime() + Minecraft.getInstance().getFrameTime() - start);
        float recoil = GunVisualMath.envelope(age, 5.0F);
        if (recoil <= 0.0F) return;
        pose.translate(0.0D, 0.0D, 0.08D * recoil);
        pose.mulPose(Axis.XP.rotationDegrees(-14.0F * recoil));
        float flash = GunVisualMath.envelope(age, 2.5F);
        if (flash > 0.0F) drawFlash(pose, buffers, flash);
        String key = player.getUUID() + "|" + offhand + "|" + context;
        if (DIAGNOSTICS.add(key))
            FiskHeroes.LOGGER.info("Gun shot effect rendered: wearer={}, hand={}, context={}",
                    player.getName().getString(), offhand ? "OFF_HAND" : "MAIN_HAND", context);
    }

    private static void drawFlash(PoseStack pose, MultiBufferSource buffers, float strength)
    {
        VertexConsumer vertex = buffers.getBuffer(RenderType.lightning());
        // The original muzzle is near (0, -2.35, -7.8) model pixels.
        pose.pushPose();
        pose.translate(0.0D, -0.15D, -0.50D);
        float size = 0.16F + strength * 0.08F;
        quad(vertex, pose.last(), -size, 0, 0, -size, size, 0, 0, size, 1.0F, 0.38F, 0.02F, 0.9F);
        quad(vertex, pose.last(), -size * 0.52F, -size * 0.52F, size * 0.52F, -size * 0.52F,
                size * 0.52F, size * 0.52F, -size * 0.52F, size * 0.52F, 1.0F, 0.78F, 0.12F, 0.9F);
        pose.popPose();
    }

    private static void quad(VertexConsumer v, PoseStack.Pose pose, float x1, float y1, float x2, float y2,
            float x3, float y3, float x4, float y4, float r, float g, float b, float a)
    {
        point(v, pose, x1, y1, r, g, b, a);
        point(v, pose, x2, y2, r, g, b, a);
        point(v, pose, x3, y3, r, g, b, a);
        point(v, pose, x4, y4, r, g, b, a);
    }

    private static void point(VertexConsumer v, PoseStack.Pose pose, float x, float y, float r, float g, float b, float a)
    {
        v.vertex(pose.pose(), x, y, 0.0F).color(r, g, b, a).endVertex();
    }
}
