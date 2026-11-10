package com.fiskmods.heroes.client.render;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.data.var.Vars;
import com.fiskmods.heroes.common.item.ItemDualSword;
import com.fiskmods.heroes.common.item.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;

/** Shared camera-space translation for a first-person pistol and its arm. */
public final class DesertEagleAimPose
{
    private static final java.util.Set<HumanoidArm> DIAGNOSTICS = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private DesertEagleAimPose() {}

    public static void apply(PoseStack pose, Player player, HumanoidArm arm, float partialTick)
    {
        var main = player.getMainHandItem();
        if (!main.is(ModItems.DESERT_EAGLE.get())) return;
        boolean dual = ItemDualSword.isDual(main) && player.getOffhandItem().isEmpty();
        if (arm != player.getMainArm() && !dual) return;
        var data = SHDataCapabilities.getPlayer(player);
        if (data == null) return;
        float amount = Mth.clamp(data.getData().getInterpolated(Vars.AIMING_TIMER, partialTick), 0.0F, 1.0F);
        amount = amount * amount * (3.0F - 2.0F * amount);
        if (amount <= 0.0F) return;
        float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        pose.translate(-side * (dual ? 0.16F : 0.32F) * amount, 0.06F * amount, -0.04F * amount);
        if (amount >= 0.95F && DIAGNOSTICS.add(arm))
            FiskHeroes.LOGGER.info("Desert eagle first-person aiming: arm={}", arm);
    }
}
