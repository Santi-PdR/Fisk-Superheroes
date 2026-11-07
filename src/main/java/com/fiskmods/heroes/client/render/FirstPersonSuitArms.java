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

/** Render the chestpiece over the exact first-person arm that vanilla just drew. */
public final class FirstPersonSuitArms
{
    private static PlayerModel<?> suitModel;
    private static final java.util.Set<String> DIAGNOSTICS = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private FirstPersonSuitArms() {}

    public static void render(com.mojang.blaze3d.vertex.PoseStack pose,
            net.minecraft.client.renderer.MultiBufferSource buffers, int packedLight,
            net.minecraft.client.player.AbstractClientPlayer player, ModelPart originalArm, ModelPart originalSleeve)
    {
        var piece = player.getItemBySlot(EquipmentSlot.CHEST);
        var iteration = ItemHeroArmor.getHero(piece);
        if (!(piece.getItem() instanceof ItemHeroArmor armor) || armor.getSlot() != 1
                || iteration == null || !iteration.hasPieceOfSet(1) || player.isInvisible()) return;
        HeroModelData data = HeroModelRegistry.get(iteration);
        var renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player);
        if (!(renderer instanceof PlayerRenderer playerRenderer)) return;
        boolean right = originalArm == playerRenderer.getModel().rightArm;
        if (!right && originalArm != playerRenderer.getModel().leftArm) return;
        if (data == null || !data.showsModelPart(1, right ? "rightArm" : "leftArm")) return;
        var texture = data.getTexture(1, player);
        float opacity = HeroSuitLayer.suitOpacity(data, player, 1);
        if (texture == null || opacity <= 0.001F) return;
        // The hook runs after PlayerRenderer.renderHand has reset the first-person x rotation.
        // Reuse its live parts rather than invoking setupAnim again on the shared player model.

        if (suitModel == null)
        {
            var mesh = PlayerModel.createMesh(new CubeDeformation(0.55F), false);
            suitModel = new PlayerModel<>(LayerDefinition.create(mesh, 64, 64).bakeRoot(), false);
        }
        ModelPart arm = right ? suitModel.rightArm : suitModel.leftArm;
        ModelPart sleeve = right ? suitModel.rightSleeve : suitModel.leftSleeve;
        arm.copyFrom(originalArm);
        sleeve.copyFrom(originalSleeve);
        arm.visible = true;
        sleeve.visible = true;
        var vertex = buffers.getBuffer(RenderType.entityTranslucent(texture));
        arm.render(pose, vertex, packedLight, OverlayTexture.NO_OVERLAY, 1, 1, 1, opacity);
        sleeve.render(pose, vertex, packedLight, OverlayTexture.NO_OVERLAY, 1, 1, 1, opacity);
        var lights = data.getLights(1, player);
        if (lights != null)
        {
            vertex = buffers.getBuffer(RenderType.eyes(lights));
            arm.render(pose, vertex, packedLight, OverlayTexture.NO_OVERLAY, 1, 1, 1, opacity);
            sleeve.render(pose, vertex, packedLight, OverlayTexture.NO_OVERLAY, 1, 1, 1, opacity);
        }
        if (DIAGNOSTICS.add(iteration.getRegistryName().toString()))
            FiskHeroes.LOGGER.info("First-person suit arm rendered: hero={}, texture={}, poseAligned={}", iteration.getRegistryName(), texture,
                    arm.x == originalArm.x && arm.y == originalArm.y && arm.z == originalArm.z
                            && arm.xRot == originalArm.xRot && arm.yRot == originalArm.yRot && arm.zRot == originalArm.zRot
                            && sleeve.x == originalSleeve.x && sleeve.y == originalSleeve.y
                            && sleeve.xRot == originalSleeve.xRot && sleeve.yRot == originalSleeve.yRot
                            && sleeve.zRot == originalSleeve.zRot);
    }
}
