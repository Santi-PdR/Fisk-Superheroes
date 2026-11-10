package com.fiskmods.heroes.client.render;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.data.var.Vars;
import com.fiskmods.heroes.common.item.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;

/** Shared first-person movement for Captain Cold's gun and supporting hand. */
public final class ColdGunAimPose
{
    private static final java.util.Set<HumanoidArm> DIAGNOSTICS = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private ColdGunAimPose() {}

    public static void apply(PoseStack pose, Player player, HumanoidArm arm, float partialTick)
    {
        if (!player.getMainHandItem().is(ModItems.COLD_GUN.get()) || arm != player.getMainArm()) return;
        var data = SHDataCapabilities.getPlayer(player);
        if (data == null) return;
        float amount = Mth.clamp(data.getData().getInterpolated(Vars.AIMING_TIMER, partialTick), 0.0F, 1.0F);
        amount = amount * amount * (3.0F - 2.0F * amount);
        if (amount <= 0.0F) return;
        float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        pose.translate(-side * 0.22F * amount, 0.10F * amount, -0.08F * amount);
        if (amount >= 0.95F && DIAGNOSTICS.add(arm))
            FiskHeroes.LOGGER.info("Cold gun first-person aiming: arm={}", arm);
    }
}
