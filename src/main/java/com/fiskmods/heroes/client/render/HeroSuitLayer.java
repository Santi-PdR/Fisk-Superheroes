package com.fiskmods.heroes.client.render;

import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.hero.HeroTracker;
import com.fiskmods.heroes.common.hero.HeroIteration;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Draws the suit a player is wearing on top of the vanilla player model.
 * <p>
 * Each armour piece of the suit has its own texture (the original mod's {@code layer1}/{@code layer2}
 * scheme) and its own set of visible model parts, both taken from the hero's model JSON. The result
 * is the same layered look the 1.7.10 version produced, built here on top of the modern render
 * pipeline.
 */
public class HeroSuitLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>
{
    private static final ResourceLocation[] SLOT_NAMES = {
            new ResourceLocation("fiskheroes", "helmet"),
            new ResourceLocation("fiskheroes", "chestplate"),
            new ResourceLocation("fiskheroes", "leggings"),
            new ResourceLocation("fiskheroes", "boots")
    };

    public HeroSuitLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent)
    {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch)
    {
        SHPlayerData data = SHDataCapabilities.getPlayer(player);

        if (data == null)
        {
            return;
        }

        HeroIteration iteration = HeroTracker.getWornSuit(player);

        if (iteration == null)
        {
            iteration = data.getHero();
        }

        if (iteration == null)
        {
            return;
        }

        HeroModelData model = HeroModelRegistry.get(iteration.getHero().getRegistryName());

        if (model == null)
        {
            return;
        }

        PlayerModel<AbstractClientPlayer> playerModel = getParentModel();
        float scale = com.fiskmods.heroes.common.data.var.Vars.getScale(player);

        poseStack.pushPose();

        if (scale != 1.0F)
        {
            poseStack.scale(scale, scale, scale);
        }

        for (int slot = 0; slot < 4; ++slot)
        {
            if (iteration.getArmorType(slot) == null || !hasPiece(player, slot))
            {
                continue;
            }

            ResourceLocation texture = model.getTexture(slot);

            if (texture == null)
            {
                continue;
            }

            boolean[] hidden = hidePartsFor(playerModel, model, slot);

            VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(texture));
            playerModel.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);

            restoreParts(playerModel, hidden);
        }

        poseStack.popPose();
    }

    public static boolean hasPiece(AbstractClientPlayer player, int slot)
    {
        return com.fiskmods.heroes.common.hero.ItemHeroArmor.isSuitPiece(player.getInventory().armor.get(3 - slot));
    }

    /** Temporarily hides the parts the given piece must not draw and returns the previous state. */
    private boolean[] hidePartsFor(PlayerModel<AbstractClientPlayer> model, HeroModelData data, int slot)
    {
        ModelPart[] parts = { model.head, model.hat, model.body, model.rightArm, model.leftArm, model.rightLeg, model.leftLeg };
        String[] names = { "head", "headwear", "body", "rightArm", "leftArm", "rightLeg", "leftLeg" };
        boolean[] previous = new boolean[parts.length];

        for (int i = 0; i < parts.length; ++i)
        {
            previous[i] = parts[i].visible;
            parts[i].visible = previous[i] && data.showsModelPart(slot, names[i]);
        }

        // The jacket/sleeve overlays follow the body pieces of the suit
        model.jacket.visible = model.body.visible;
        model.rightSleeve.visible = model.rightArm.visible;
        model.leftSleeve.visible = model.leftArm.visible;
        model.rightPants.visible = model.rightLeg.visible;
        model.leftPants.visible = model.leftLeg.visible;

        return previous;
    }

    private void restoreParts(PlayerModel<AbstractClientPlayer> model, boolean[] previous)
    {
        ModelPart[] parts = { model.head, model.hat, model.body, model.rightArm, model.leftArm, model.rightLeg, model.leftLeg };

        for (int i = 0; i < parts.length && i < previous.length; ++i)
        {
            parts[i].visible = previous[i];
        }

        model.jacket.visible = true;
        model.rightSleeve.visible = true;
        model.leftSleeve.visible = true;
        model.rightPants.visible = true;
        model.leftPants.visible = true;
    }
}
