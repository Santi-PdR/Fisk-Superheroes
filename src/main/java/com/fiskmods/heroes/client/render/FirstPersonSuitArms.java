package com.fiskmods.heroes.client.render;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.hero.ItemHeroArmor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderArmEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Render the chestpiece's skin-layout arm in first person, over the vanilla arm. */
@Mod.EventBusSubscriber(modid = FiskHeroes.MODID, value = Dist.CLIENT)
public final class FirstPersonSuitArms
{
    private static PlayerModel<?> suitModel;
    private static final java.util.Set<String> DIAGNOSTICS = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private FirstPersonSuitArms() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void render(RenderArmEvent event)
    {
        var player = event.getPlayer();
        var piece = player.getItemBySlot(EquipmentSlot.CHEST);
        var iteration = ItemHeroArmor.getHero(piece);
        if (!(piece.getItem() instanceof ItemHeroArmor armor) || armor.getSlot() != 1
                || iteration == null || !iteration.hasPieceOfSet(1) || player.isInvisible()) return;
        HeroModelData data = HeroModelRegistry.get(iteration);
        boolean right = event.getArm() == HumanoidArm.RIGHT;
        if (data == null || !data.showsModelPart(1, right ? "rightArm" : "leftArm")) return;
        var texture = data.getTexture(1, player);
        float opacity = HeroSuitLayer.suitOpacity(data, player, 1);
        if (texture == null || opacity <= 0.001F) return;
        var renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player);
        if (!(renderer instanceof PlayerRenderer playerRenderer)) return;

        // Forge posts this event *inside* renderRightHand/renderLeftHand. Do not call either
        // method here (that would recursively post the event), and do not cancel it: vanilla
        // still draws the skin exactly once after this armor pass is queued.
        var original = playerRenderer.getModel();
        original.attackTime = 0.0F;
        original.crouching = false;
        original.swimAmount = 0.0F;
        original.setupAnim(player, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
        ModelPart originalArm = right ? original.rightArm : original.leftArm;
        originalArm.xRot = 0.0F; // PlayerRenderer's first-person renderHand does the same.

        if (suitModel == null)
        {
            var mesh = PlayerModel.createMesh(new CubeDeformation(0.55F), false);
            suitModel = new PlayerModel<>(LayerDefinition.create(mesh, 64, 64).bakeRoot(), false);
        }
        ModelPart arm = right ? suitModel.rightArm : suitModel.leftArm;
        ModelPart sleeve = right ? suitModel.rightSleeve : suitModel.leftSleeve;
        arm.copyFrom(originalArm);
        sleeve.copyFrom(arm);
        arm.visible = true;
        sleeve.visible = true;
        var vertex = event.getMultiBufferSource().getBuffer(RenderType.entityTranslucent(texture));
        arm.render(event.getPoseStack(), vertex, event.getPackedLight(), OverlayTexture.NO_OVERLAY, 1, 1, 1, opacity);
        sleeve.render(event.getPoseStack(), vertex, event.getPackedLight(), OverlayTexture.NO_OVERLAY, 1, 1, 1, opacity);
        var lights = data.getLights(1, player);
        if (lights != null)
        {
            vertex = event.getMultiBufferSource().getBuffer(RenderType.eyes(lights));
            arm.render(event.getPoseStack(), vertex, event.getPackedLight(), OverlayTexture.NO_OVERLAY, 1, 1, 1, opacity);
            sleeve.render(event.getPoseStack(), vertex, event.getPackedLight(), OverlayTexture.NO_OVERLAY, 1, 1, 1, opacity);
        }
        if (DIAGNOSTICS.add(iteration.getRegistryName().toString()))
            FiskHeroes.LOGGER.info("First-person suit arm rendered: hero={}, texture={}", iteration.getRegistryName(), texture);
    }
}
