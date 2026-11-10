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
    private static final java.util.Set<String> DIAGNOSTICS = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private DesertEagleAimPose() {}

    public static void apply(PoseStack pose, Player player, HumanoidArm arm, float partialTick)
    {
        var main = player.getMainHandItem();
        boolean beretta = main.is(ModItems.BERETTA_93R.get());
        if (!beretta && !main.is(ModItems.DESERT_EAGLE.get())) return;
        boolean dual = ItemDualSword.isDual(main) && player.getOffhandItem().isEmpty();
        if (arm != player.getMainArm() && !dual) return;
        var data = SHDataCapabilities.getPlayer(player);
        if (data == null) return;
        float amount = Mth.clamp(data.getData().getInterpolated(Vars.AIMING_TIMER, partialTick), 0.0F, 1.0F);
        amount = amount * amount * (3.0F - 2.0F * amount);
        if (amount <= 0.0F) return;
        float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        float scope = dual || beretta ? 0.0F : Mth.clamp(data.getData().getInterpolated(Vars.SCOPE_TIMER, partialTick), 0.0F, 1.0F);
        scope = scope * scope * (3.0F - 2.0F * scope);
        pose.translate(-side * ((dual ? 0.16F : 0.32F) * amount + 0.10F * scope),
                0.06F * amount + 0.04F * scope, -0.04F * amount - 0.05F * scope);
        if (amount >= 0.95F && DIAGNOSTICS.add(beretta + "|" + arm))
            FiskHeroes.LOGGER.info("{} first-person aiming: arm={}", beretta ? "Beretta" : "Desert eagle", arm);
    }
}
