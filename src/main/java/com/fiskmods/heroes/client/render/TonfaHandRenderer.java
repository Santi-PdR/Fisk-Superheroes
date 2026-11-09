package com.fiskmods.heroes.client.render;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.item.ModItems;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Draw arms behind held tonfas; vanilla otherwise draws only the item. */
@Mod.EventBusSubscriber(modid = FiskHeroes.MODID, value = Dist.CLIENT)
public final class TonfaHandRenderer
{
    private static final java.util.Set<HumanoidArm> DIAGNOSTICS = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private TonfaHandRenderer() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderHand(RenderHandEvent event)
    {
        var player = Minecraft.getInstance().player;
        if (player == null || player.isInvisible() || player.isScoping()
                || !event.getItemStack().is(ModItems.TACTICAL_TONFA.get())) return;
        var renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player);
        if (!(renderer instanceof PlayerRenderer playerRenderer)) return;
        HumanoidArm arm = event.getHand() == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        float swing = event.getSwingProgress();
        float root = Mth.sqrt(swing);
        var pose = event.getPoseStack();
        pose.pushPose();
        try
        {
            // Vanilla's empty-hand pose, with the grip nudged towards the
            // repositioned first-person tonfa and back from the camera.
            pose.translate(side * (-0.3F * Mth.sin(root * (float) Math.PI) + 0.54F),
                    0.4F * Mth.sin(root * ((float) Math.PI * 2.0F)) - 0.47F - event.getEquipProgress() * 0.6F,
                    -0.4F * Mth.sin(swing * (float) Math.PI) - 0.92F);
            pose.mulPose(Axis.YP.rotationDegrees(side * 45.0F));
            pose.mulPose(Axis.YP.rotationDegrees(side * Mth.sin(root * (float) Math.PI) * 70.0F));
            pose.mulPose(Axis.ZP.rotationDegrees(side * Mth.sin(swing * swing * (float) Math.PI) * -20.0F));
            pose.translate(side * -1.0F, 3.6F, 3.5F);
            pose.mulPose(Axis.ZP.rotationDegrees(side * 120.0F));
            pose.mulPose(Axis.XP.rotationDegrees(200.0F));
            pose.mulPose(Axis.YP.rotationDegrees(side * -135.0F));
            pose.translate(side * 5.6F, 0.0F, 0.0F);
            if (arm == HumanoidArm.RIGHT) playerRenderer.renderRightHand(pose, event.getMultiBufferSource(), event.getPackedLight(), player);
            else playerRenderer.renderLeftHand(pose, event.getMultiBufferSource(), event.getPackedLight(), player);
            if (DIAGNOSTICS.add(arm)) FiskHeroes.LOGGER.info("First-person tonfa arm rendered: {}", arm);
        }
        finally
        {
            pose.popPose();
        }
    }
}
