package com.fiskmods.heroes.client.render;

import com.fiskmods.heroes.common.hero.HeroIteration;
import com.fiskmods.heroes.common.hero.HeroTracker;
import com.fiskmods.heroes.common.spell.SpellDuplicateEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/** Applies the owner hero's normal layer1/layer2 textures to a spell duplicate. */
final class SpellDuplicateSuitLayer extends RenderLayer<SpellDuplicateEntity, PlayerModel<SpellDuplicateEntity>>
{
    SpellDuplicateSuitLayer(RenderLayerParent<SpellDuplicateEntity, PlayerModel<SpellDuplicateEntity>> parent)
    {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, SpellDuplicateEntity duplicate,
            float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch)
    {
        LivingEntity owner = duplicate.getOwner();
        if (owner == null) return;
        HeroIteration iteration = owner instanceof net.minecraft.world.entity.player.Player player
                ? HeroTracker.getWornSuit(player) : HeroTracker.getHero(owner);
        HeroModelData suit = HeroModelRegistry.get(iteration);
        if (iteration == null || suit == null) return;

        PlayerModel<SpellDuplicateEntity> model = getParentModel();
        for (int slot = 0; slot < 4; ++slot)
        {
            if (iteration.getArmorType(slot) == null) continue;
            ResourceLocation texture = suit.getTexture(slot, owner);
            if (texture == null) continue;

            boolean[] old = hideForSlot(model, suit, slot);
            if (slot == 0 && suit.shouldFixHatLayer(slot)) model.hat.visible = false;
            VertexConsumer vertex = buffers.getBuffer(RenderType.entityTranslucent(texture));
            model.renderToBuffer(pose, vertex, light, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
            restore(model, old);
        }
    }

    private static boolean[] hideForSlot(PlayerModel<SpellDuplicateEntity> model, HeroModelData suit, int slot)
    {
        ModelPart[] parts = { model.head, model.hat, model.body, model.rightArm, model.leftArm, model.rightLeg, model.leftLeg,
                model.jacket, model.rightSleeve, model.leftSleeve, model.rightPants, model.leftPants };
        String[] names = { "head", "headwear", "body", "rightArm", "leftArm", "rightLeg", "leftLeg" };
        boolean[] old = new boolean[parts.length];
        for (int i = 0; i < names.length; i++)
        {
            old[i] = parts[i].visible;
            parts[i].visible = old[i] && suit.showsModelPart(slot, names[i]);
        }
        for (int i = 7; i < parts.length; i++) old[i] = parts[i].visible;
        parts[7].visible = old[7] && model.body.visible;
        parts[8].visible = old[8] && model.rightArm.visible;
        parts[9].visible = old[9] && model.leftArm.visible;
        parts[10].visible = old[10] && model.rightLeg.visible;
        parts[11].visible = old[11] && model.leftLeg.visible;
        return old;
    }

    private static void restore(PlayerModel<SpellDuplicateEntity> model, boolean[] old)
    {
        ModelPart[] parts = { model.head, model.hat, model.body, model.rightArm, model.leftArm, model.rightLeg, model.leftLeg,
                model.jacket, model.rightSleeve, model.leftSleeve, model.rightPants, model.leftPants };
        for (int i = 0; i < parts.length; i++) parts[i].visible = old[i];
    }
}
