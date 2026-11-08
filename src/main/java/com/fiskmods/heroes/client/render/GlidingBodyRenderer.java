package com.fiskmods.heroes.client.render;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.data.var.Vars;
import com.fiskmods.heroes.common.hero.HeroTracker;
import com.fiskmods.heroes.common.util.GlidingPoseMath;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;

/** Rotates the complete player, as in the original gliding renderer. */
public final class GlidingBodyRenderer
{
    private static final Set<String> DIAGNOSTICS = new HashSet<>();

    private GlidingBodyRenderer() { }

    public static void apply(AbstractClientPlayer player, PoseStack pose, float partialTick)
    {
        // Vanilla already rotates these poses. Do not stack the gliding transform on top.
        if (!player.isAlive() || player.isSleeping() || player.isFallFlying()
                || player.isAutoSpinAttack() || player.getSwimAmount(partialTick) > 0.0F) return;
        var iteration = HeroTracker.getWornSuit(player);
        var data = SHDataCapabilities.getPlayer(player);
        if (iteration == null || data == null) return;
        float timer = data.getData().getInterpolated(Vars.GLIDING_TIMER, partialTick);
        if (timer <= 0.0F) return;
        pose.mulPose(Axis.XP.rotationDegrees(GlidingPoseMath.bodyPitch(timer, player.getXRot())));
        var motion = player.getDeltaMovement();
        var look = player.getViewVector(partialTick);
        double motionHorizontal = motion.horizontalDistanceSqr();
        double lookHorizontal = look.horizontalDistanceSqr();
        if (motionHorizontal > 1.0E-10D && lookHorizontal > 1.0E-10D)
        {
            double dot = (motion.x * look.x + motion.z * look.z) / Math.sqrt(motionHorizontal * lookHorizontal);
            double cross = motion.x * look.z - motion.z * look.x;
            pose.mulPose(Axis.YP.rotation((float) (Math.signum(cross) * Math.acos(Mth.clamp(dot, -1.0D, 1.0D)))));
        }
        if (DIAGNOSTICS.add(iteration.getRegistryName().toString()))
            FiskHeroes.LOGGER.info("Gliding body rotation applied: hero={}", iteration.getRegistryName());
    }
}
